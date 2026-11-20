package DAO;

import Model.Categoria;
import Util.ConnectionFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public class CategoriaDAO {
    private final ConexaoProvider provider;

    public CategoriaDAO() {
        this(ConnectionFactory::getConnection);
    }

    public CategoriaDAO(ConexaoProvider provider) {
        this.provider = provider;
    }

    public Categoria inserir(Categoria m) throws SQLException {
        if (m.getIdCategoria() != 0) {
            throw new IllegalArgumentException("Objeto novo deve ter ID zero.");
        }
        long id = Jdbc.transacao(provider, c -> Jdbc.inserir(c, "INSERT INTO categoria(nome_categoria) VALUES(?)", m.getNomeCategoria()));
        m.setIdCategoria(id);
        return m;
    }

    public Categoria buscarPorId(long id) throws SQLException {
        try (Connection c = provider.abrir()) {
            return Jdbc.um(c, "SELECT * FROM categoria WHERE id_categoria=?", r -> new Categoria(r.getLong("id_categoria"), r.getString("nome_categoria")), Jdbc.id(id));
        }
    }

    public List<Categoria> listar() throws SQLException {
        try (Connection c = provider.abrir()) {
            return Jdbc.listar(c, "SELECT * FROM categoria ORDER BY nome_categoria,id_categoria", r -> new Categoria(r.getLong("id_categoria"), r.getString("nome_categoria")));
        }
    }

    public void atualizar(Categoria m) throws SQLException {
        Jdbc.transacao(provider, c -> {
            Jdbc.alterarUma(c, "UPDATE categoria SET nome_categoria=? WHERE id_categoria=?", m.getNomeCategoria(), Jdbc.id(m.getIdCategoria()));
            return null;
        });
    }

    public void excluir(long id) throws SQLException {
        Jdbc.transacao(provider, c -> {
            // A FK do MySQL impede excluir um cadastro ainda referenciado.
            Jdbc.alterarUma(c, "DELETE FROM categoria WHERE id_categoria=?", Jdbc.id(id));
            return null;
        });
    }
}
