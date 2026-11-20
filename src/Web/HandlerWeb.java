package Web;

import Controller.ErroAplicacao;
import com.sun.net.httpserver.*;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

public class HandlerWeb implements HttpHandler {
    private final ApiConexAgro api;
    private final Path web;
    private final ArmazemImagens imagens;
    private final int porta;

    public HandlerWeb(ApiConexAgro api, Path web, Path uploads, int porta) {
        this.api = api;
        this.web = web.toAbsolutePath().normalize();
        imagens = new ArmazemImagens(uploads);
        this.porta = porta;
    }

    @Override
    public void handle(HttpExchange x) throws IOException {
        try {
            cabecalhos(x);
            String host = x.getRequestHeaders().getFirst("Host");
            if (!("localhost:" + porta).equalsIgnoreCase(host) && !("127.0.0.1:" + porta).equals(host))
                throw new ErroAplicacao(403, "Acesso permitido somente pelo endereco local do sistema.");
            String caminho = x.getRequestURI().getPath(), metodo = x.getRequestMethod();
            if (caminho.startsWith("/api/")) {
                if (!Arrays.asList("GET", "POST", "PUT", "DELETE").contains(metodo))
                    throw new ErroAplicacao(405, "Metodo nao permitido.");
                if (!metodo.equals("GET")) validarOrigem(x);
                String token = cookie(x), chave = x.getRequestHeaders().getFirst("X-ConexAgro-Setup");
                Map<String, String> query = parametros(x.getRequestURI().getRawQuery());
                if (caminho.matches("/api/pecas/[0-9]+/imagens/upload") && metodo.equals("POST")) {
                    api.permitirUpload(token);
                    String url = imagens.guardar(ler(x.getRequestBody(), 8 * 1024 * 1024));
                    try {
                        responderJson(x, 201, api.anexarImagem(token, Entrada.id(caminho.split("/")[3]), url, query.get("descricao")));
                    } catch (RuntimeException e) {
                        imagens.desfazer(url);
                        throw e;
                    }
                    return;
                }
                Map<String, Object> corpo = Collections.emptyMap();
                if (!metodo.equals("GET")) {
                    String tipo = x.getRequestHeaders().getFirst("Content-Type");
                    if (tipo == null || !tipo.toLowerCase(Locale.ROOT).startsWith("application/json"))
                        throw new ErroAplicacao(415, "Envie os dados em JSON.");
                    byte[] bytes = ler(x.getRequestBody(), 64 * 1024);
                    corpo = bytes.length == 0 ? Collections.emptyMap() : Json.objeto(new String(bytes, StandardCharsets.UTF_8));
                }
                Object resposta = api.executar(metodo, caminho, corpo, query, token, chave);
                if (resposta instanceof Map && ((Map<?, ?>) resposta).containsKey("token")) {
                    Map<String, Object> copia = new LinkedHashMap<>();
                    for (Map.Entry<?, ?> e : ((Map<?, ?>) resposta).entrySet())
                        copia.put((String) e.getKey(), e.getValue());
                    String sessao = (String) copia.remove("token");
                    x.getResponseHeaders().set("Set-Cookie", "conexagro_sessao=" + sessao + "; Path=/; HttpOnly; SameSite=Strict; Max-Age=28800");
                    resposta = copia;
                }
                if (caminho.equals("/api/logout") || caminho.equals("/api/perfil/senha")) limparCookie(x);
                responderJson(x, caminho.equals("/api/registro") ? 201 : 200, resposta);
                return;
            }
            if (!metodo.equals("GET") && !metodo.equals("HEAD")) throw new ErroAplicacao(405, "Metodo nao permitido.");
            Path arquivo;
            if (caminho.startsWith("/uploads/")) arquivo = imagens.localizar(caminho);
            else {
                String nome = caminho.equals("/") ? "index.html" : caminho.substring(1);
                if (!Arrays.asList("index.html", "app.js", "style.css", "logo.svg").contains(nome))
                    throw new ErroAplicacao(404, "Pagina nao encontrada.");
                arquivo = web.resolve(nome);
                if (!Files.isRegularFile(arquivo))
                    throw new ErroAplicacao(404, "Arquivo do site nao encontrado. Confira a pasta web.");
            }
            byte[] bytes = Files.readAllBytes(arquivo);
            x.getResponseHeaders().set("Content-Type", tipoArquivo(arquivo.getFileName().toString()));
            if (metodo.equals("HEAD")) {
                x.sendResponseHeaders(200, -1);
                return;
            }
            x.sendResponseHeaders(200, bytes.length);
            x.getResponseBody().write(bytes);
        } catch (ErroAplicacao e) {
            if (e.getStatus() == 401) limparCookie(x);
            responderJson(x, e.getStatus(), ApiConexAgro.mapa("erro", e.getMessage()));
        } catch (IllegalArgumentException e) {
            responderJson(x, 400, ApiConexAgro.mapa("erro", e.getMessage() == null ? "Dados invalidos." : e.getMessage()));
        } catch (Model.OperacaoBloqueadaException e) {
            responderJson(x, 409, ApiConexAgro.mapa("erro", e.getMessage()));
        } catch (java.sql.SQLException e) {
            int status = e.getErrorCode() == 1062 ? 409 : 500;
            responderJson(x, status, ApiConexAgro.mapa("erro", status == 409 ? "Este e-mail ja esta cadastrado." : "Nao foi possivel acessar o banco. Confira a conexao no IntelliJ."));
        } catch (Exception e) {
            System.err.println("Falha no atendimento web: " + e.getClass().getSimpleName());
            responderJson(x, 500, ApiConexAgro.mapa("erro", "Nao foi possivel concluir a operacao."));
        } finally {
            x.close();
        }
    }

    private void validarOrigem(HttpExchange x) {
        String origem = x.getRequestHeaders().getFirst("Origin");
        if (!"1".equals(x.getRequestHeaders().getFirst("X-ConexAgro-Request")))
            throw new ErroAplicacao(403, "Requisicao nao autorizada.");
        if (origem != null && !origem.equals("http://localhost:" + porta) && !origem.equals("http://127.0.0.1:" + porta))
            throw new ErroAplicacao(403, "Origem nao autorizada.");
    }

    private static void cabecalhos(HttpExchange x) {
        Headers h = x.getResponseHeaders();
        h.set("X-Content-Type-Options", "nosniff");
        h.set("Referrer-Policy", "same-origin");
        h.set("Cache-Control", "no-store");
        h.set("X-Frame-Options", "DENY");
        h.set("Content-Security-Policy", "default-src 'self'; script-src 'self'; style-src 'self'; img-src 'self' http: https:; connect-src 'self'; object-src 'none'; base-uri 'none'; frame-ancestors 'none'; form-action 'self'");
    }

    private static void limparCookie(HttpExchange x) {
        x.getResponseHeaders().set("Set-Cookie", "conexagro_sessao=; Path=/; HttpOnly; SameSite=Strict; Max-Age=0");
    }

    private static String cookie(HttpExchange x) {
        String h = x.getRequestHeaders().getFirst("Cookie");
        if (h != null) for (String parte : h.split(";")) {
            String p = parte.trim();
            if (p.startsWith("conexagro_sessao=")) return p.substring("conexagro_sessao=".length());
        }
        return null;
    }

    static Map<String, String> parametros(String raw) {
        Map<String, String> r = new LinkedHashMap<>();
        if (raw == null || raw.isEmpty()) return r;
        for (String p : raw.split("&")) {
            String[] v = p.split("=", 2);
            String k = URLDecoder.decode(v[0], StandardCharsets.UTF_8);
            if (r.containsKey(k)) throw new ErroAplicacao(400, "Parametro repetido.");
            r.put(k, v.length == 1 ? "" : URLDecoder.decode(v[1], StandardCharsets.UTF_8));
        }
        return r;
    }

    private static byte[] ler(InputStream in, int limite) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) != -1) {
            if (out.size() + n > limite) throw new ErroAplicacao(413, "Arquivo ou requisicao muito grande.");
            out.write(buf, 0, n);
        }
        return out.toByteArray();
    }

    private static void responderJson(HttpExchange x, int status, Object resposta) throws IOException {
        byte[] bytes = Json.escrever(resposta).getBytes(StandardCharsets.UTF_8);
        x.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        x.sendResponseHeaders(status, bytes.length);
        x.getResponseBody().write(bytes);
    }

    private static String tipoArquivo(String nome) {
        if (nome.endsWith(".html")) return "text/html; charset=utf-8";
        if (nome.endsWith(".css")) return "text/css; charset=utf-8";
        if (nome.endsWith(".js")) return "text/javascript; charset=utf-8";
        if (nome.endsWith(".svg")) return "image/svg+xml";
        if (nome.endsWith(".png")) return "image/png";
        if (nome.endsWith(".webp")) return "image/webp";
        return "image/jpeg";
    }
}
