package Web;

import Controller.ErroAplicacao;
import com.sun.net.httpserver.*;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/**
 * Contratos HTTP sem abrir portas e sem modificar o MySQL. Execute esta classe no IntelliJ.
 */
public final class TesteWeb {
    private static int verificacoes;

    private static void conferir(boolean v, String nome) {
        if (!v) throw new AssertionError(nome);
        verificacoes++;
    }

    private static void jsonInvalido(String v) {
        try {
            Json.objeto(v);
            throw new AssertionError("JSON aceito: " + v);
        } catch (IllegalArgumentException esperado) {
            verificacoes++;
        }
    }

    public static void main(String[] args) throws Exception {
        Map<String, Object> dados = new LinkedHashMap<>();
        dados.put("nome", "Peça \"agrícola\"\n<teste>");
        dados.put("id", 4294967295L);
        dados.put("ativo", true);
        dados.put("vazio", null);
        dados.put("lista", List.of(1, "ação"));
        Map<String, Object> volta = Json.objeto(Json.escrever(dados));
        conferir(volta.get("nome").equals(dados.get("nome")), "JSON preserva texto e escapes");
        conferir(volta.get("id").toString().equals("4294967295"), "ID unsigned preservado");
        conferir(Boolean.TRUE.equals(volta.get("ativo")) && volta.containsKey("vazio") && volta.get("vazio") == null, "Booleano e nulo");
        conferir(Json.escrever(dados).contains("\\u003c"), "Escape de menor que");
        for (String invalido : List.of("[]", "{}x", "{\"id\":1,\"id\":2}", "{\"id\":01}", "{\"id\":NaN}", "{\"id\":+1}", "{\"id\":1,}", "{\"id\":\"\\x\"}", "{\"id\":[1,]}", "{\"id\":truefalse}", "{\"id\":\"\n\"}", "{\"id\":" + "[".repeat(34) + "0" + "]".repeat(34) + "}"))
            jsonInvalido(invalido);
        conferir(HandlerWeb.parametros("termo=filtro+de+%C3%B3leo&marca=2").get("termo").equals("filtro de óleo"), "Query UTF-8");
        try {
            HandlerWeb.parametros("marca=1&marca=2");
            throw new AssertionError();
        } catch (ErroAplicacao e) {
            conferir(e.getStatus() == 400, "Parametro ambiguo recusado");
        }
        Path raiz = Files.createTempDirectory("conexagro-web-");
        try {
            Path web = Files.createDirectory(raiz.resolve("web")), uploads = raiz.resolve("uploads");
            Files.writeString(web.resolve("index.html"), "<!doctype html><title>ConexAgro</title>");
            ApiTeste api = new ApiTeste();
            HandlerWeb handler = new HandlerWeb(api, web, uploads, 8080);
            Troca x = trocar(handler, "GET", "/", null, null);
            conferir(x.codigo == 200 && x.texto().contains("ConexAgro"), "Pagina inicial");
            conferir(x.resposta.getFirst("Content-Security-Policy").contains("script-src 'self'"), "Politica dos scripts");
            conferir(x.resposta.getFirst("X-Content-Type-Options").equals("nosniff"), "Tipo de arquivo fixo");
            x = trocar(handler, "HEAD", "/", null, null);
            conferir(x.codigo == 200 && x.saida.size() == 0, "HEAD sem corpo");
            for (String path : List.of("/conexao.properties", "/src/Principal.java", "/../conexao.properties", "/uploads/../../conexao.properties")) {
                x = trocar(handler, "GET", path, null, null);
                conferir(x.codigo == 404, "Arquivo privado inacessivel: " + path);
            }
            x = new Troca("GET", "/api/status", null);
            x.entrada.set("Host", "site-externo.test:8080");
            handler.handle(x);
            conferir(x.codigo == 403, "Host externo recusado");
            x = trocar(handler, "POST", "/api/login", "{}", null);
            conferir(x.codigo == 403, "Mutacao exige cabecalho");
            x = new Troca("POST", "/api/login", "{}");
            x.entrada.set("X-ConexAgro-Request", "1");
            x.entrada.set("Origin", "https://outro.example");
            handler.handle(x);
            conferir(x.codigo == 403, "Origem externa recusada");
            x = new Troca("POST", "/api/login", "{}");
            x.entrada.set("X-ConexAgro-Request", "1");
            x.entrada.set("Content-Type", "text/plain");
            handler.handle(x);
            conferir(x.codigo == 415, "Formulario nao JSON recusado");
            x = trocar(handler, "POST", "/api/login", "{\"email\":\"teste@example.test\"}", "1");
            conferir(x.codigo == 200, "Login via adaptador");
            conferir(!x.texto().contains("segredo-de-teste"), "Token nao exposto ao JavaScript");
            String cookie = x.resposta.getFirst("Set-Cookie");
            conferir(cookie.contains("HttpOnly") && cookie.contains("SameSite=Strict"), "Sessao HttpOnly e SameSite");
            x = new Troca("GET", "/api/perfil", null);
            x.entrada.set("Cookie", cookie.split(";", 2)[0]);
            handler.handle(x);
            conferir(api.token.equals("segredo-de-teste"), "Cookie encaminhado ao Controller");
            x = trocar(handler, "GET", "/api/negado", null, null);
            conferir(x.codigo == 401 && x.resposta.getFirst("Set-Cookie").contains("Max-Age=0"), "Sessao invalida removida");
            x = trocar(handler, "POST", "/api/logout", "{}", "1");
            conferir(x.codigo == 200 && x.resposta.getFirst("Set-Cookie").contains("Max-Age=0"), "Logout remove cookie");
            x = trocar(handler, "POST", "/api/login", "{\"email\":1,\"email\":2}", "1");
            conferir(x.codigo == 400, "JSON ambiguo recusado no HTTP");
            x = trocar(handler, "POST", "/api/login", "{\"texto\":\"" + "a".repeat(65536) + "\"}", "1");
            conferir(x.codigo == 413, "Limite de corpo JSON");
            x = trocar(handler, "PATCH", "/api/status", "{}", "1");
            conferir(x.codigo == 405, "Metodo nao exposto");
            byte[] png = Base64.getDecoder().decode("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+/l9sAAAAASUVORK5CYII=");
            x = upload(handler, png);
            conferir(x.codigo == 201 && api.uploadAutorizado, "Upload autorizado antes de salvar");
            String url = (String) Json.objeto(x.texto()).get("url");
            x = trocar(handler, "GET", url, null, null);
            conferir(x.codigo == 200 && Arrays.equals(png, x.saida.toByteArray()), "Imagem enviada pode ser consultada");
            x = upload(handler, "<svg onload=alert(1)>".getBytes(StandardCharsets.UTF_8));
            conferir(x.codigo == 400, "SVG e arquivo nao suportado recusados");
            long antes;
            try (var paths = Files.list(uploads)) {
                antes = paths.count();
            }
            api.falharAnexo = true;
            x = upload(handler, png);
            conferir(x.codigo == 409, "Erro do cadastro preservado");
            try (var paths = Files.list(uploads)) {
                conferir(paths.count() == antes, "Falha no banco desfaz arquivo enviado");
            }
            api.uploadAutorizado = false;
            api.negarUpload = true;
            x = upload(handler, png);
            conferir(x.codigo == 403 && !api.uploadAutorizado, "Usuario sem permissao nao envia imagem");
        } finally {
            try (var itens = Files.walk(raiz)) {
                for (Path p : itens.sorted(Comparator.reverseOrder()).toList()) Files.delete(p);
            }
        }
        System.out.println("TesteWeb: " + verificacoes + " verificacoes aprovadas (HTTP em memoria; sem MySQL).");
    }

    private static Troca trocar(HandlerWeb h, String m, String p, String b, String cab) throws IOException {
        Troca x = new Troca(m, p, b);
        if (cab != null) x.entrada.set("X-ConexAgro-Request", cab);
        h.handle(x);
        return x;
    }

    private static Troca upload(HandlerWeb h, byte[] b) throws IOException {
        Troca x = new Troca("POST", "/api/pecas/1/imagens/upload?descricao=Foto", null);
        x.corpo = new ByteArrayInputStream(b);
        x.entrada.set("X-ConexAgro-Request", "1");
        x.entrada.set("Content-Type", "image/png");
        h.handle(x);
        return x;
    }

    private static final class ApiTeste extends ApiConexAgro {
        String token;
        boolean uploadAutorizado, negarUpload, falharAnexo;

        ApiTeste() {
            super(null, null, null);
        }

        @Override
        public Object executar(String m, String p, Map<String, Object> b, Map<String, String> q, String t, String s) {
            token = t;
            if (p.equals("/api/negado")) throw new ErroAplicacao(401, "Sessao expirada.");
            return p.equals("/api/login") ? mapa("token", "segredo-de-teste", "usuario", mapa("id", 1)) : ok();
        }

        @Override
        public void permitirUpload(String t) {
            if (negarUpload) throw new ErroAplicacao(403, "Sem permissao.");
            uploadAutorizado = true;
        }

        @Override
        public Map<String, Object> anexarImagem(String t, long id, String url, String descricao) {
            if (falharAnexo) throw new ErroAplicacao(409, "Teste de falha ao salvar.");
            return mapa("id", 1, "url", url);
        }
    }

    private static final class Troca extends HttpExchange {
        final Headers entrada = new Headers(), resposta = new Headers();
        final String metodo;
        final URI uri;
        InputStream corpo;
        final ByteArrayOutputStream saida = new ByteArrayOutputStream();
        int codigo = -1;

        Troca(String m, String path, String b) {
            metodo = m;
            uri = URI.create(path);
            corpo = new ByteArrayInputStream((b == null ? "" : b).getBytes(StandardCharsets.UTF_8));
            entrada.set("Host", "127.0.0.1:8080");
            entrada.set("Content-Type", "application/json");
        }

        String texto() {
            return saida.toString(StandardCharsets.UTF_8);
        }

        public Headers getRequestHeaders() {
            return entrada;
        }

        public Headers getResponseHeaders() {
            return resposta;
        }

        public URI getRequestURI() {
            return uri;
        }

        public String getRequestMethod() {
            return metodo;
        }

        public HttpContext getHttpContext() {
            return null;
        }

        public void close() {
        }

        public InputStream getRequestBody() {
            return corpo;
        }

        public OutputStream getResponseBody() {
            return saida;
        }

        public void sendResponseHeaders(int c, long n) {
            codigo = c;
        }

        public InetSocketAddress getRemoteAddress() {
            return new InetSocketAddress("127.0.0.1", 8888);
        }

        public int getResponseCode() {
            return codigo;
        }

        public InetSocketAddress getLocalAddress() {
            return new InetSocketAddress("127.0.0.1", 8080);
        }

        public String getProtocol() {
            return "HTTP/1.1";
        }

        public Object getAttribute(String k) {
            return null;
        }

        public void setAttribute(String k, Object v) {
        }

        public void setStreams(InputStream i, OutputStream o) {
        }

        public HttpPrincipal getPrincipal() {
            return null;
        }
    }
}
