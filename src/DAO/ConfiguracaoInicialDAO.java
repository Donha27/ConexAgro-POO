package DAO;

import Model.*;
import Util.ConnectionFactory;
import Util.SenhaUtil;

import java.sql.*;
import java.util.List;

public class ConfiguracaoInicialDAO {
    private final ConexaoProvider provider;

    public ConfiguracaoInicialDAO() {
        this(ConnectionFactory::getConnection);
    }

    public ConfiguracaoInicialDAO(ConexaoProvider provider) {
        this.provider = provider;
    }

    public boolean pendente() throws SQLException {
        try (Connection c = provider.abrir()) {
            return Jdbc.numero(c, "SELECT COUNT(*) FROM usuario WHERE perfil_acesso='administrador' AND ativo=1") == 0;
        }
    }

    public Usuario criarAdministrador(Usuario u) throws SQLException {
        if (!u.podeGerenciarCatalogo() || !SenhaUtil.formatoValido(u.getSenhaHash()) || u.getIdUsuario() != 0)
            throw new IllegalArgumentException("Administrador inicial invalido.");
        long id = Jdbc.transacao(provider, c -> {
            List<Usuario> usuarios = Jdbc.listar(c, "SELECT * FROM usuario ORDER BY id_usuario FOR UPDATE", Mapeamento::usuario);
            for (Usuario atual : usuarios)
                if (atual.podeGerenciarCatalogo())
                    throw new OperacaoBloqueadaException("O acesso administrativo ja foi configurado.");
            return Jdbc.inserir(c, "INSERT INTO usuario(nome_completo,email,senha_hash,ocupacao,perfil_acesso,ativo) VALUES(?,?,?,NULL,'administrador',1)", u.getNomeCompleto(), u.getEmail(), u.getSenhaHash());
        });
        u.setIdUsuario(id);
        return u;
    }
}
