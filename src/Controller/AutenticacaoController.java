package Controller;

import DAO.UsuarioDAO;
import Model.Usuario;
import Util.SenhaUtil;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AutenticacaoController extends ControllerBase {
    private final UsuarioDAO usuarios;
    private final Clock relogio;
    private final Duration duracao;
    private final SecureRandom random = new SecureRandom();
    private final Map<String, Sessao> sessoes = new ConcurrentHashMap<>();
    private final String hashSimulado = SenhaUtil.gerarHash("ComparacaoInterna-SemConta-2026!");

    private static final class Sessao {
        final long usuarioId;
        final String credencial;
        final Instant expiraEm;

        Sessao(Usuario u, Instant expiraEm) {
            usuarioId = u.getIdUsuario();
            credencial = u.getSenhaHash();
            this.expiraEm = expiraEm;
        }
    }

    public AutenticacaoController() {
        this(new UsuarioDAO());
    }

    public AutenticacaoController(UsuarioDAO usuarios) {
        this(usuarios, Clock.systemUTC(), Duration.ofHours(8));
    }

    public AutenticacaoController(UsuarioDAO usuarios, Clock relogio, Duration duracao) {
        if (usuarios == null || relogio == null || duracao == null || duracao.isZero() || duracao.isNegative()) {
            throw new IllegalArgumentException("Configuracao de autenticacao invalida.");
        }
        this.usuarios = usuarios;
        this.relogio = relogio;
        this.duracao = duracao;
    }

    public Map<String, Object> entrar(String email, String senha) {
        return tratar(() -> {
            Usuario u = usuarios.buscarPorEmail(email);
            boolean confere = SenhaUtil.verificar(senha, u == null ? hashSimulado : u.getSenhaHash());
            if (u == null || !u.isAtivo() || !confere) {
                throw new ErroAplicacao(401, "E-mail ou senha invalidos.");
            }
            Instant agora = relogio.instant();
            sessoes.entrySet().removeIf(e -> !e.getValue().expiraEm.isAfter(agora));
            byte[] bytes = new byte[32];
            random.nextBytes(bytes);
            String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
            Sessao sessao = new Sessao(u, agora.plus(duracao));
            sessoes.put(token, sessao);
            Map<String, Object> resposta = Apresentacao.mapa();
            resposta.put("token", token);
            resposta.put("expiraEm", sessao.expiraEm.toString());
            resposta.put("usuario", Apresentacao.usuario(u));
            return resposta;
        });
    }

    Usuario exigirUsuario(String token) {
        return tratar(() -> {
            if (token == null || !token.matches("[A-Za-z0-9_-]{43}")) {
                throw semSessao();
            }
            Sessao sessao = sessoes.get(token);
            if (sessao == null || !sessao.expiraEm.isAfter(relogio.instant())) {
                sessoes.remove(token);
                throw semSessao();
            }
            // Rele a conta: inativacao, exclusao, nova senha e novo perfil valem nas proximas operacoes.
            Usuario u = usuarios.buscarPorId(sessao.usuarioId);
            if (u == null || !u.isAtivo() || !sessao.credencial.equals(u.getSenhaHash())) {
                sessoes.remove(token);
                throw semSessao();
            }
            return u;
        });
    }

    Usuario exigirAdministrador(String token) {
        Usuario u = exigirUsuario(token);
        if (!u.podeGerenciarCatalogo()) {
            throw new ErroAplicacao(403, "Operacao permitida somente ao administrador.");
        }
        return u;
    }

    public void sair(String token) {
        if (token != null) {
            sessoes.remove(token);
        }
    }

    void invalidarUsuario(long id) {
        sessoes.entrySet().removeIf(e -> e.getValue().usuarioId == id);
    }

    private static ErroAplicacao semSessao() {
        return new ErroAplicacao(401, "Entre novamente para continuar.");
    }
}
