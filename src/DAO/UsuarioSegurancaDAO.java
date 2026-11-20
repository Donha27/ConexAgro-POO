package DAO;

import Model.*;
import Util.ConnectionFactory;
import Util.SenhaUtil;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/**
 * Operacoes de usuario que precisam de campos restritos e bloqueio transacional.
 */
public class UsuarioSegurancaDAO {
    private final ConexaoProvider provider;

    public UsuarioSegurancaDAO() {
        this(ConnectionFactory::getConnection);
    }

    public UsuarioSegurancaDAO(ConexaoProvider provider) {
        this.provider = provider;
    }

    public Usuario atualizarPerfil(Usuario dados) throws SQLException {
        return Jdbc.transacao(provider, c -> {
            Usuario atual = Jdbc.um(c, "SELECT * FROM usuario WHERE id_usuario=? FOR UPDATE",
                    Mapeamento::usuario, Jdbc.id(dados.getIdUsuario()));
            if (atual == null) {
                throw new SQLException("Usuario nao encontrado.", "02000");
            }
            Usuario validado = new Usuario(atual.getIdUsuario(), dados.getNomeCompleto(), dados.getEmail(),
                    atual.getSenhaHash(), dados.getOcupacao(), atual.getPerfilAcesso(), atual.isAtivo());
            // Edicao pessoal nunca escreve perfil, estado de atividade ou senha.
            Jdbc.alterarUma(c, "UPDATE usuario SET nome_completo=?,email=?,ocupacao=? WHERE id_usuario=?",
                    validado.getNomeCompleto(), validado.getEmail(), ocupacao(validado), validado.getIdUsuario());
            return consultar(c, validado.getIdUsuario());
        });
    }

    public void alterarSenha(long id, String hashEsperado, String novoHash) throws SQLException {
        validarHash(novoHash);
        Jdbc.transacao(provider, c -> {
            int alterados = Jdbc.executar(c, "UPDATE usuario SET senha_hash=? WHERE id_usuario=? AND BINARY senha_hash=BINARY ? AND ativo=1",
                    novoHash, Jdbc.id(id), hashEsperado);
            if (alterados != 1) {
                throw new OperacaoBloqueadaException("As credenciais mudaram. Entre novamente para alterar a senha.");
            }
            return null;
        });
    }

    public Usuario atualizarComoAdministrador(long atorId, Usuario dados, String novoHash) throws SQLException {
        if (novoHash != null) {
            validarHash(novoHash);
        }
        return Jdbc.transacao(provider, c -> {
            // A ordem unica dos bloqueios serializa mudancas de perfil e exclusoes.
            List<Usuario> todos = Jdbc.listar(c, "SELECT * FROM usuario ORDER BY id_usuario FOR UPDATE", Mapeamento::usuario);
            Usuario ator = encontrar(todos, Jdbc.id(atorId));
            Usuario atual = encontrar(todos, Jdbc.id(dados.getIdUsuario()));
            if (atual == null) {
                throw new SQLException("Usuario nao encontrado.", "02000");
            }
            RegrasUsuario.validarAlteracao(ator, atual, dados, contarAdministradores(todos));
            Jdbc.alterarUma(c, "UPDATE usuario SET nome_completo=?,email=?,ocupacao=?,perfil_acesso=?,ativo=? WHERE id_usuario=?",
                    dados.getNomeCompleto(), dados.getEmail(), ocupacao(dados), dados.getPerfilAcesso().getValorBanco(),
                    dados.isAtivo(), dados.getIdUsuario());
            if (novoHash != null) {
                Jdbc.alterarUma(c, "UPDATE usuario SET senha_hash=? WHERE id_usuario=?", novoHash, dados.getIdUsuario());
            }
            return consultar(c, dados.getIdUsuario());
        });
    }

    public void excluirComoAdministrador(long atorId, long alvoId) throws SQLException {
        Jdbc.transacao(provider, c -> {
            List<Usuario> todos = Jdbc.listar(c, "SELECT * FROM usuario ORDER BY id_usuario FOR UPDATE", Mapeamento::usuario);
            Usuario ator = encontrar(todos, Jdbc.id(atorId));
            Usuario alvo = encontrar(todos, Jdbc.id(alvoId));
            if (alvo == null) {
                throw new SQLException("Usuario nao encontrado.", "02000");
            }
            RegrasUsuario.validarExclusao(ator, alvo, contarAdministradores(todos));
            Jdbc.alterarUma(c, "DELETE FROM usuario WHERE id_usuario=?", alvoId);
            return null;
        });
    }

    private static Usuario consultar(Connection c, long id) throws SQLException {
        return Jdbc.um(c, "SELECT * FROM usuario WHERE id_usuario=?", Mapeamento::usuario, id);
    }

    private static Usuario encontrar(List<Usuario> todos, long id) {
        for (Usuario u : todos) {
            if (u.getIdUsuario() == id) {
                return u;
            }
        }
        return null;
    }

    private static int contarAdministradores(List<Usuario> todos) {
        int total = 0;
        for (Usuario u : todos) {
            if (u.podeGerenciarCatalogo()) {
                total++;
            }
        }
        return total;
    }

    private static Object ocupacao(Usuario u) {
        return u.getOcupacao() == null ? null : u.getOcupacao().getValorBanco();
    }

    private static void validarHash(String hash) {
        if (!SenhaUtil.formatoValido(hash)) {
            throw new IllegalArgumentException("Hash de senha invalido.");
        }
    }
}
