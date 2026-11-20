package DAO;

import Model.OrigemCodigo;
import Util.ConnectionFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public class OrigemCodigoDAO {
    private final ConexaoProvider provider;

    public OrigemCodigoDAO() {
        this(ConnectionFactory::getConnection);
    }

    public OrigemCodigoDAO(ConexaoProvider provider) {
        this.provider = provider;
    }

    public OrigemCodigo inserir(OrigemCodigo m) throws SQLException {
        if (m.getIdOrigem() != 0) {
            throw new IllegalArgumentException("Objeto novo deve ter ID zero.");
        }
        long id = Jdbc.transacao(provider, c -> Jdbc.inserir(c, "INSERT INTO origem_codigo(nome_origem,tipo_origem) VALUES(?,?)", m.getNomeOrigem(), m.getTipoOrigem()));
        m.setIdOrigem(id);
        return m;
    }

    public OrigemCodigo buscarPorId(long id) throws SQLException {
        try (Connection c = provider.abrir()) {
            return Jdbc.um(c, "SELECT * FROM origem_codigo WHERE id_origem=?", r -> new OrigemCodigo(r.getLong("id_origem"), r.getString("nome_origem"), r.getString("tipo_origem")), Jdbc.id(id));
        }
    }

    public List<OrigemCodigo> listar() throws SQLException {
        try (Connection c = provider.abrir()) {
            return Jdbc.listar(c, "SELECT * FROM origem_codigo ORDER BY nome_origem,id_origem", r -> new OrigemCodigo(r.getLong("id_origem"), r.getString("nome_origem"), r.getString("tipo_origem")));
        }
    }

    public void atualizar(OrigemCodigo m) throws SQLException {
        Jdbc.transacao(provider, c -> {
            Jdbc.alterarUma(c, "UPDATE origem_codigo SET nome_origem=?,tipo_origem=? WHERE id_origem=?", m.getNomeOrigem(), m.getTipoOrigem(), Jdbc.id(m.getIdOrigem()));
            return null;
        });
    }

    public void excluir(long id) throws SQLException {
        Jdbc.transacao(provider, c -> {
            // A FK do MySQL impede excluir um cadastro ainda referenciado.
            Jdbc.alterarUma(c, "DELETE FROM origem_codigo WHERE id_origem=?", Jdbc.id(id));
            return null;
        });
    }
}
