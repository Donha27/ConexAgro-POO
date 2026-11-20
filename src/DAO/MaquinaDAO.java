package DAO;

import Model.*;
import Util.ConnectionFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public class MaquinaDAO {
    private final ConexaoProvider provider;

    public MaquinaDAO() {
        this(ConnectionFactory::getConnection);
    }

    public MaquinaDAO(ConexaoProvider provider) {
        this.provider = provider;
    }

    public Maquina inserir(Maquina m) throws SQLException {
        if (m.getIdItem() != 0) {
            throw new IllegalArgumentException("Maquina nova deve ter ID zero.");
        }
        long id = Jdbc.transacao(provider, c -> {
            long novo = Jdbc.inserir(c, "INSERT INTO item_catalogo(tipo_item,nome,descricao,estado,"
                            + "id_marca,id_categoria,id_usuario_cadastro) VALUES('maquina',?,?,?,?,?,?)", m.getNome(),
                    m.getDescricao(), m.getEstado().getValorBanco(), Mapeamento.marcaId(m),
                    Mapeamento.categoriaId(m), Mapeamento.autorId(m));
            Jdbc.executar(c, "INSERT INTO maquina(id_maquina,modelo,tipo_maquina,ano,serie,motor,versao) VALUES(?,?,?,?,?,?,?)",
                    novo, m.getModelo(), m.getTipoMaquina(), m.getAno(), m.getSerie(), m.getMotor(), m.getVersao());
            return novo;
        });
        m.setIdItem(id);
        return m;
    }

    public Maquina buscarPorId(long id) throws SQLException {
        try (Connection c = provider.abrir()) {
            return Jdbc.um(c, Mapeamento.SELECT_MAQUINA + "WHERE i.id_item=?", Mapeamento::maquina, Jdbc.id(id));
        }
    }

    public List<Maquina> listar() throws SQLException {
        try (Connection c = provider.abrir()) {
            return Jdbc.listar(c, Mapeamento.SELECT_MAQUINA + "ORDER BY q.modelo,i.id_item", Mapeamento::maquina);
        }
    }

    public void atualizar(Maquina m) throws SQLException {
        Jdbc.transacao(provider, c -> {
            bloquear(c, m.getIdItem());
            Jdbc.alterarUma(c, "UPDATE item_catalogo SET nome=?,descricao=?,estado=?,id_marca=?,"
                            + "id_categoria=?,id_usuario_cadastro=? WHERE id_item=?", m.getNome(), m.getDescricao(),
                    m.getEstado().getValorBanco(), Mapeamento.marcaId(m), Mapeamento.categoriaId(m), Mapeamento.autorId(m), m.getIdItem());
            Jdbc.alterarUma(c, "UPDATE maquina SET modelo=?,tipo_maquina=?,ano=?,serie=?,motor=?,versao=? WHERE id_maquina=?",
                    m.getModelo(), m.getTipoMaquina(), m.getAno(), m.getSerie(), m.getMotor(), m.getVersao(), m.getIdItem());
            return null;
        });
    }

    public void excluir(long id) throws SQLException {
        Jdbc.transacao(provider, c -> {
            EstadoItem estado = bloquear(c, id);
            long refs = Jdbc.numero(c, "SELECT (SELECT COUNT(*) FROM compatibilidade WHERE id_maquina=?)"
                    + "+(SELECT COUNT(*) FROM favorito WHERE id_item=?)+(SELECT COUNT(*) FROM catalogo_item WHERE id_item=?)", id, id, id);
            RegrasPeca.validarExclusao(estado, refs);
            Jdbc.executar(c, "DELETE FROM imagem WHERE id_item=?", id);
            Jdbc.executar(c, "DELETE FROM especificacao WHERE id_item=?", id);
            Jdbc.alterarUma(c, "DELETE FROM maquina WHERE id_maquina=?", id);
            Jdbc.alterarUma(c, "DELETE FROM item_catalogo WHERE id_item=?", id);
            return null;
        });
    }

    private static EstadoItem bloquear(Connection c, long id) throws SQLException {
        EstadoItem estado = Jdbc.um(c, "SELECT i.estado FROM item_catalogo i JOIN maquina q "
                        + "ON q.id_maquina=i.id_item WHERE i.id_item=? FOR UPDATE",
                r -> EstadoItem.doBanco(r.getString(1)), Jdbc.id(id));
        if (estado == null) {
            throw new SQLException("Maquina nao encontrada.", "02000");
        }
        return estado;
    }
}
