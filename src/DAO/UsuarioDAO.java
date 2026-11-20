package DAO;

import Model.Usuario;
import Util.ConnectionFactory;
import Util.SenhaUtil;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Locale;

public class UsuarioDAO {
    private final ConexaoProvider provider;

    public UsuarioDAO() {
        this(ConnectionFactory::getConnection);
    }

    public UsuarioDAO(ConexaoProvider provider) {
        this.provider = provider;
    }

    public Usuario inserir(Usuario u) throws SQLException {
        if (u.getIdUsuario() != 0) {
            throw new IllegalArgumentException("Usuario novo deve ter ID zero.");
        }
        validarHash(u);
        long id = Jdbc.transacao(provider, c -> Jdbc.inserir(c,
                "INSERT INTO usuario(nome_completo,email,senha_hash,ocupacao,perfil_acesso,ativo) VALUES(?,?,?,?,?,?)",
                u.getNomeCompleto(), u.getEmail(), u.getSenhaHash(), ocupacao(u),
                u.getPerfilAcesso().getValorBanco(), u.isAtivo()));
        u.setIdUsuario(id);
        return u;
    }

    public Usuario buscarPorId(long id) throws SQLException {
        try (Connection c = provider.abrir()) {
            return Jdbc.um(c, "SELECT * FROM usuario WHERE id_usuario=?", Mapeamento::usuario, Jdbc.id(id));
        }
    }

    public Usuario buscarPorEmail(String email) throws SQLException {
        if (email == null) {
            return null;
        }
        try (Connection c = provider.abrir()) {
            return Jdbc.um(c, "SELECT * FROM usuario WHERE email=?", Mapeamento::usuario,
                    email.trim().toLowerCase(Locale.ROOT));
        }
    }

    public List<Usuario> listar() throws SQLException {
        return pesquisar("");
    }

    public List<Usuario> pesquisar(String termo) throws SQLException {
        try (Connection c = provider.abrir()) {
            return Jdbc.listar(c, "SELECT * FROM usuario WHERE nome_completo LIKE ? ESCAPE '!' "
                            + "OR email LIKE ? ESCAPE '!' ORDER BY nome_completo,id_usuario", Mapeamento::usuario,
                    Jdbc.trecho(termo), Jdbc.trecho(termo));
        }
    }

    public void atualizar(Usuario u) throws SQLException {
        validarHash(u);
        Jdbc.transacao(provider, c -> {
            Jdbc.alterarUma(c, "UPDATE usuario SET nome_completo=?,email=?,senha_hash=?,ocupacao=?,"
                            + "perfil_acesso=?,ativo=? WHERE id_usuario=?", u.getNomeCompleto(), u.getEmail(),
                    u.getSenhaHash(), ocupacao(u), u.getPerfilAcesso().getValorBanco(), u.isAtivo(), Jdbc.id(u.getIdUsuario()));
            return null;
        });
    }

    public void excluir(long id) throws SQLException {
        Jdbc.transacao(provider, c -> {
            // O CASCADE remove somente favoritos. O autor do item vira NULL, preservando o catalogo.
            Jdbc.alterarUma(c, "DELETE FROM usuario WHERE id_usuario=?", Jdbc.id(id));
            return null;
        });
    }

    private static Object ocupacao(Usuario u) {
        return u.getOcupacao() == null ? null : u.getOcupacao().getValorBanco();
    }

    private static void validarHash(Usuario u) {
        if (!SenhaUtil.formatoValido(u.getSenhaHash())) {
            throw new IllegalArgumentException("Gere o hash com SenhaUtil.gerarHash antes de salvar o usuario.");
        }
    }
}
