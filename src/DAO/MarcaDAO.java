package DAO;

import Model.Marca;
import Util.ConnectionFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public class MarcaDAO {
    private final ConexaoProvider provider;

    public MarcaDAO() {
        this(ConnectionFactory::getConnection);
    }

    public MarcaDAO(ConexaoProvider provider) {
        this.provider = provider;
    }

    public Marca inserir(Marca m) throws SQLException {
        if (m.getIdMarca() != 0) {
            throw new IllegalArgumentException("Objeto novo deve ter ID zero.");
        }
        long id = Jdbc.transacao(provider, c -> Jdbc.inserir(c, "INSERT INTO marca(nome_marca) VALUES(?)", m.getNomeMarca()));
        m.setIdMarca(id);
        return m;
    }

    public Marca buscarPorId(long id) throws SQLException {
        try (Connection c = provider.abrir()) {
            return Jdbc.um(c, "SELECT * FROM marca WHERE id_marca=?", r -> new Marca(r.getLong("id_marca"), r.getString("nome_marca")), Jdbc.id(id));
        }
    }

    public List<Marca> listar() throws SQLException {
        try (Connection c = provider.abrir()) {
            return Jdbc.listar(c, "SELECT * FROM marca ORDER BY nome_marca,id_marca", r -> new Marca(r.getLong("id_marca"), r.getString("nome_marca")));
        }
    }

    public void atualizar(Marca m) throws SQLException {
        Jdbc.transacao(provider, c -> {
            Jdbc.alterarUma(c, "UPDATE marca SET nome_marca=? WHERE id_marca=?", m.getNomeMarca(), Jdbc.id(m.getIdMarca()));
            return null;
        });
    }

    public void excluir(long id) throws SQLException {
        Jdbc.transacao(provider, c -> {
            // A FK do MySQL impede excluir um cadastro ainda referenciado.
            Jdbc.alterarUma(c, "DELETE FROM marca WHERE id_marca=?", Jdbc.id(id));
            return null;
        });
    }
}
