package Web;

import Controller.*;
import DAO.*;
import Model.*;
import Util.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.SQLException;
import java.util.*;

/**
 * Adapta rotas HTTP aos Controllers; nao contem comandos SQL.
 */
public class ApiConexAgro {
    private final AplicacaoController app;
    private final ConfiguracaoInicialDAO inicial;
    private volatile String chaveInicial;

    public ApiConexAgro(String chaveInicial) {
        this(new AplicacaoController(), new ConfiguracaoInicialDAO(), chaveInicial);
    }

    public ApiConexAgro(AplicacaoController app, ConfiguracaoInicialDAO inicial, String chaveInicial) {
        this.app = app;
        this.inicial = inicial;
        this.chaveInicial = chaveInicial;
    }

    public Object executar(String metodo, String caminho, Map<String, Object> b, Map<String, String> q, String token, String chaveSetup) throws SQLException {
        UsuarioController u = app.getUsuarios();
        PecaController p = app.getPecas();
        ApoioController a = app.getApoio();
        if (caminho.equals("/api/status") && metodo.equals("GET"))
            return mapa("nome", "ConexAgro", "configuracaoPendente", inicial.pendente());
        if (caminho.equals("/api/configuracao") && metodo.equals("POST")) {
            String chave = chaveInicial;
            if (chave == null || chaveSetup == null || !MessageDigest.isEqual(chave.getBytes(StandardCharsets.UTF_8), chaveSetup.getBytes(StandardCharsets.UTF_8)))
                throw new ErroAplicacao(403, "Use o endereco de configuracao exibido ao iniciar o sistema.");
            String email = Entrada.texto(b, "email"), senha = Entrada.texto(b, "senha");
            Usuario admin = new Usuario(0, Entrada.texto(b, "nome"), email, SenhaUtil.gerarHash(senha), null, PerfilAcesso.ADMINISTRADOR, true);
            inicial.criarAdministrador(admin);
            chaveInicial = null;
            return app.getAutenticacao().entrar(email, senha);
        }
        if (caminho.equals("/api/login") && metodo.equals("POST"))
            return app.getAutenticacao().entrar(Entrada.texto(b, "email"), Entrada.texto(b, "senha"));
        if (caminho.equals("/api/logout") && metodo.equals("POST")) {
            app.getAutenticacao().sair(token);
            return ok();
        }
        if (caminho.equals("/api/registro") && metodo.equals("POST"))
            return u.registrar(Entrada.texto(b, "nome"), Entrada.texto(b, "email"), Entrada.texto(b, "senha"), Ocupacao.doBanco(Entrada.texto(b, "ocupacao")));
        if (caminho.equals("/api/perfil")) {
            if (metodo.equals("GET")) return u.meuPerfil(token);
            if (metodo.equals("PUT"))
                return u.editarMeuPerfil(token, Entrada.texto(b, "nome"), Entrada.texto(b, "email"), Ocupacao.doBanco(Entrada.texto(b, "ocupacao")));
        }
        if (caminho.equals("/api/perfil/senha") && metodo.equals("POST")) {
            u.alterarMinhaSenha(token, Entrada.texto(b, "atual"), Entrada.texto(b, "nova"));
            return ok();
        }
        if (caminho.equals("/api/apoio") && metodo.equals("GET")) return a.listar(token);
        if (caminho.equals("/api/usuarios")) {
            if (metodo.equals("GET")) return u.listar(token, q.get("termo"));
            if (metodo.equals("POST"))
                return u.cadastrar(token, Entrada.texto(b, "nome"), Entrada.texto(b, "email"), Entrada.texto(b, "senha"), Ocupacao.doBanco(Entrada.texto(b, "ocupacao")), PerfilAcesso.doBanco(Entrada.texto(b, "perfil", "usuario")));
        }
        if ((caminho.equals("/api/pecas") || caminho.equals("/api/admin/pecas")) && metodo.equals("GET"))
            return p.pagina(token, caminho.contains("/admin/"), q.get("termo"), Entrada.numero(q, "marca"), Entrada.numero(q, "categoria"), Entrada.numero(q, "maquina"), Entrada.inteiro(q, "ano"), q.get("estado"), Entrada.inteiro(q, "pagina", 1), Entrada.inteiro(q, "tamanho", 12));
        if (caminho.equals("/api/pecas") && metodo.equals("POST"))
            return p.cadastrarRascunho(token, Entrada.texto(b, "nome"), Entrada.obrigatorio(b, "categoria"), Entrada.numero(b, "marca"), Entrada.texto(b, "descricao"));
        String[] s = caminho.split("/");
        if (s.length >= 4 && s[1].equals("api")) {
            String tipo = s[2];
            long id = Entrada.id(s[3]);
            if (tipo.equals("usuarios") && s.length == 4) {
                if (metodo.equals("GET")) return u.consultar(token, id);
                if (metodo.equals("PUT"))
                    return u.editar(token, id, Entrada.texto(b, "nome"), Entrada.texto(b, "email"), Ocupacao.doBanco(Entrada.texto(b, "ocupacao")), PerfilAcesso.doBanco(Entrada.texto(b, "perfil", "usuario")), Entrada.ativo(b), Entrada.texto(b, "senha"));
                if (metodo.equals("DELETE")) {
                    u.excluir(token, id);
                    return ok();
                }
            }
            if (tipo.equals("pecas")) {
                if (s.length == 4) {
                    if (metodo.equals("GET")) return p.consultar(token, id);
                    if (metodo.equals("PUT"))
                        return p.editar(token, id, Entrada.texto(b, "nome"), Entrada.obrigatorio(b, "categoria"), Entrada.numero(b, "marca"), Entrada.texto(b, "descricao"));
                    if (metodo.equals("DELETE")) {
                        p.excluir(token, id);
                        return ok();
                    }
                }
                if (s.length == 5 && metodo.equals("POST")) {
                    if (s[4].equals("publicar")) {
                        p.publicar(token, id);
                        return ok();
                    }
                    if (s[4].equals("inativar")) {
                        p.inativar(token, id);
                        return ok();
                    }
                }
                if (s.length == 5 || s.length == 6) {
                    long detalhe = s.length == 6 ? Entrada.id(s[5]) : 0;
                    String colecao = s[4];
                    if ((s.length == 5 && metodo.equals("POST")) || (s.length == 6 && metodo.equals("PUT"))) {
                        if (colecao.equals("imagens")) {
                            String url = validarUrlImagem(Entrada.texto(b, "url"));
                            return p.salvarImagem(token, id, detalhe, url, Entrada.texto(b, "descricao"));
                        }
                        if (colecao.equals("especificacoes"))
                            return p.salvarEspecificacao(token, id, detalhe, Entrada.texto(b, "nome"), Entrada.texto(b, "valor"), Entrada.texto(b, "unidade"));
                        if (colecao.equals("codigos"))
                            return p.salvarCodigo(token, id, detalhe, Entrada.obrigatorio(b, "marca"), Entrada.obrigatorio(b, "origem"), Entrada.texto(b, "codigo"), Entrada.texto(b, "tipo", "original"));
                    }
                    if (s.length == 6 && metodo.equals("DELETE")) {
                        if (colecao.equals("imagens")) {
                            p.excluirImagem(token, id, detalhe);
                            return ok();
                        }
                        if (colecao.equals("especificacoes")) {
                            p.excluirEspecificacao(token, id, detalhe);
                            return ok();
                        }
                        if (colecao.equals("codigos")) {
                            p.excluirCodigo(token, id, detalhe);
                            return ok();
                        }
                        if (colecao.equals("compatibilidades")) {
                            p.excluirCompatibilidade(token, id, detalhe);
                            return ok();
                        }
                    }
                    if (colecao.equals("compatibilidades") && s.length == 5 && metodo.equals("POST")) {
                        p.salvarCompatibilidade(token, id, Entrada.obrigatorio(b, "maquina"), Entrada.texto(b, "estado", "pendente"), Entrada.texto(b, "fonte"), Entrada.inteiro(b, "anoInicio"), Entrada.inteiro(b, "anoFim"), Entrada.texto(b, "restricaoSerie"), Entrada.texto(b, "restricaoMotor"), Entrada.texto(b, "restricaoVersao"), Entrada.texto(b, "observacoes"));
                        return ok();
                    }
                }
            }
        }
        if (caminho.startsWith("/api/")) {
            String[] partes = caminho.split("/");
            String tipo = partes.length >= 3 ? partes[2] : "";
            if (Arrays.asList("marcas", "categorias", "origens", "maquinas").contains(tipo)) {
                long id = partes.length == 4 ? Entrada.id(partes[3]) : 0;
                if ((partes.length == 3 && metodo.equals("POST")) || (partes.length == 4 && metodo.equals("PUT"))) {
                    if (tipo.equals("marcas")) return a.salvarMarca(token, id, Entrada.texto(b, "nome"));
                    if (tipo.equals("categorias")) return a.salvarCategoria(token, id, Entrada.texto(b, "nome"));
                    if (tipo.equals("origens"))
                        return a.salvarOrigem(token, id, Entrada.texto(b, "nome"), Entrada.texto(b, "tipo", "fabricante"));
                    return a.salvarMaquina(token, id, Entrada.texto(b, "nome"), Entrada.texto(b, "modelo"), Entrada.texto(b, "tipoMaquina"), Entrada.obrigatorio(b, "marca"), Entrada.texto(b, "descricao"), Entrada.inteiro(b, "ano"), Entrada.texto(b, "serie"), Entrada.texto(b, "motor"), Entrada.texto(b, "versao"), EstadoItem.doBanco(Entrada.texto(b, "estado", "rascunho")));
                }
                if (partes.length == 4 && metodo.equals("DELETE")) {
                    switch (tipo) {
                        case "marcas":
                            a.excluirMarca(token, id);
                            break;
                        case "categorias":
                            a.excluirCategoria(token, id);
                            break;
                        case "origens":
                            a.excluirOrigem(token, id);
                            break;
                        default:
                            a.excluirMaquina(token, id);
                    }
                    return ok();
                }
            }
        }
        throw new ErroAplicacao(404, "Operacao nao encontrada.");
    }

    public Map<String, Object> anexarImagem(String token, long peca, String url, String descricao) {
        return app.getPecas().salvarImagem(token, peca, 0, url, descricao);
    }

    public void permitirUpload(String token) {
        Map<String, Object> u = app.getUsuarios().meuPerfil(token);
        if (!"administrador".equals(u.get("perfil")))
            throw new ErroAplicacao(403, "Somente administradores podem enviar imagens.");
    }

    static String validarUrlImagem(String url) {
        if (url == null || url.trim().isEmpty()) throw new ErroAplicacao(400, "Informe uma imagem.");
        String v = url.trim();
        if (v.startsWith("/") && !v.startsWith("//")) return v;
        try {
            java.net.URI u = new java.net.URI(v);
            if (("https".equalsIgnoreCase(u.getScheme()) || "http".equalsIgnoreCase(u.getScheme())) && u.getHost() != null && u.getUserInfo() == null)
                return v;
        } catch (Exception ignored) {
        }
        throw new ErroAplicacao(400, "Use uma imagem enviada ou um link HTTP/HTTPS.");
    }

    static Map<String, Object> mapa(Object... pares) {
        Map<String, Object> r = new LinkedHashMap<>();
        for (int i = 0; i < pares.length; i += 2) r.put((String) pares[i], pares[i + 1]);
        return r;
    }

    static Map<String, Object> ok() {
        return mapa("ok", true);
    }
}
