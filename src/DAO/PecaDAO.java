package DAO;

import Model.*;
import Util.ConnectionFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class PecaDAO {
    private final ConexaoProvider provider;

    public PecaDAO() {
        this(ConnectionFactory::getConnection);
    }

    public PecaDAO(ConexaoProvider provider) {
        this.provider = provider;
    }

    public Peca inserir(Peca p) throws SQLException {
        RegrasPeca.exigirNovoRascunho(p);
        long id = Jdbc.transacao(provider, c -> {
            long novo = Jdbc.inserir(c, "INSERT INTO item_catalogo(tipo_item,nome,descricao,estado,"
                            + "id_marca,id_categoria,id_usuario_cadastro) VALUES('peca',?,?,'rascunho',?,?,?)",
                    p.getNome(), p.getDescricao(), Mapeamento.marcaId(p), Mapeamento.categoriaId(p), Mapeamento.autorId(p));
            Jdbc.executar(c, "INSERT INTO peca(id_peca) VALUES(?)", novo);
            return novo;
        });
        // O ID so muda no objeto depois do COMMIT das duas tabelas.
        p.setIdItem(id);
        return p;
    }

    public Peca buscarPorId(long id) throws SQLException {
        try (Connection c = provider.abrir()) {
            return buscar(c, Jdbc.id(id));
        }
    }

    static Peca buscar(Connection c, long id) throws SQLException {
        return Jdbc.um(c, Mapeamento.SELECT_PECA + "WHERE i.id_item=?", Mapeamento::peca, id);
    }

    public List<Peca> listar() throws SQLException {
        try (Connection c = provider.abrir()) {
            return Jdbc.listar(c, Mapeamento.SELECT_PECA + "ORDER BY i.nome,i.id_item", Mapeamento::peca);
        }
    }

    public void atualizar(Peca p) throws SQLException {
        Jdbc.transacao(provider, c -> {
            bloquear(c, p.getIdItem());
            Jdbc.alterarUma(c, "UPDATE item_catalogo SET nome=?,descricao=?,estado=?,id_marca=?,"
                            + "id_categoria=?,id_usuario_cadastro=? WHERE id_item=? AND tipo_item='peca'",
                    p.getNome(), p.getDescricao(), p.getEstado().getValorBanco(), Mapeamento.marcaId(p),
                    Mapeamento.categoriaId(p), Mapeamento.autorId(p), p.getIdItem());
            validarSePublicada(c, p.getIdItem());
            return null;
        });
    }

    public void publicar(long id) throws SQLException {
        Jdbc.transacao(provider, c -> {
            bloquear(c, id);
            Peca p = buscar(c, id);
            validarPublicacao(c, p);
            Jdbc.alterarUma(c, "UPDATE item_catalogo SET estado='publicado' WHERE id_item=?", id);
            return null;
        });
    }

    public void inativar(long id) throws SQLException {
        Jdbc.transacao(provider, c -> {
            bloquear(c, id);
            Jdbc.alterarUma(c, "UPDATE item_catalogo SET estado='inativo' WHERE id_item=?", id);
            return null;
        });
    }

    public void excluir(long id) throws SQLException {
        Jdbc.transacao(provider, c -> {
            EstadoItem estado = bloquear(c, id);
            long refs = Jdbc.numero(c, "SELECT (SELECT COUNT(*) FROM favorito WHERE id_item=?)"
                            + "+(SELECT COUNT(*) FROM catalogo_item WHERE id_item=?)"
                            + "+(SELECT COUNT(*) FROM compatibilidade WHERE id_peca=?)"
                            + "+(SELECT COUNT(*) FROM equivalencia_peca WHERE id_peca_a=? OR id_peca_b=?)",
                    id, id, id, id, id);
            RegrasPeca.validarExclusao(estado, refs);
            Jdbc.executar(c, "DELETE FROM imagem WHERE id_item=?", id);
            Jdbc.executar(c, "DELETE FROM especificacao WHERE id_item=?", id);
            Jdbc.executar(c, "DELETE FROM codigo_peca WHERE id_peca=?", id);
            Jdbc.alterarUma(c, "DELETE FROM peca WHERE id_peca=?", id);
            Jdbc.alterarUma(c, "DELETE FROM item_catalogo WHERE id_item=?", id);
            return null;
        });
    }

    static EstadoItem bloquear(Connection c, long id) throws SQLException {
        EstadoItem estado = Jdbc.um(c, "SELECT i.estado FROM item_catalogo i "
                        + "JOIN peca p ON p.id_peca=i.id_item WHERE i.id_item=? FOR UPDATE",
                r -> EstadoItem.doBanco(r.getString(1)), Jdbc.id(id));
        if (estado == null) {
            throw new SQLException("Peca nao encontrada.", "02000");
        }
        return estado;
    }

    static void validarSePublicada(Connection c, long id) throws SQLException {
        Peca p = buscar(c, id);
        if (p.getEstado() == EstadoItem.PUBLICADO) {
            validarPublicacao(c, p);
        }
    }

    private static void validarPublicacao(Connection c, Peca p) throws SQLException {
        long id = p.getIdItem();
        RegrasPeca.validarPublicacao(p,
                Jdbc.numero(c, "SELECT COUNT(*) FROM codigo_peca WHERE id_peca=?", id),
                Jdbc.numero(c, "SELECT COUNT(*) FROM imagem WHERE id_item=?", id),
                Jdbc.numero(c, "SELECT COUNT(*) FROM especificacao WHERE id_item=?", id),
                Jdbc.numero(c, "SELECT COUNT(*) FROM compatibilidade WHERE id_peca=? AND estado='confirmada'", id));
    }

    /**
     * Consulta para usuarios do site: somente pecas publicadas.
     */
    public List<Peca> pesquisar(String termo, Long marca, Long categoria, Long maquina, Integer ano) throws SQLException {
        String texto = termo == null ? "" : termo.trim();
        String codigo = texto.replaceAll("\\s+", "").toUpperCase(Locale.ROOT);
        StringBuilder sql = new StringBuilder(Mapeamento.SELECT_PECA)
                .append("WHERE i.estado='publicado' AND (i.nome LIKE ? ESCAPE '!' ")
                .append("OR i.descricao LIKE ? ESCAPE '!' OR EXISTS(SELECT 1 FROM codigo_peca cp ")
                .append("WHERE cp.id_peca=p.id_peca AND cp.codigo_normalizado LIKE ? ESCAPE '!')) ");
        List<Object> args = new ArrayList<>();
        args.add(Jdbc.trecho(texto));
        args.add(Jdbc.trecho(texto));
        args.add(Jdbc.trecho(codigo));
        if (marca != null) {
            sql.append("AND i.id_marca=? ");
            args.add(Jdbc.id(marca));
        }
        if (categoria != null) {
            sql.append("AND i.id_categoria=? ");
            args.add(Jdbc.id(categoria));
        }
        if (maquina != null || ano != null) {
            sql.append("AND EXISTS(SELECT 1 FROM compatibilidade co WHERE co.id_peca=p.id_peca AND co.estado='confirmada' ");
            if (maquina != null) {
                sql.append("AND co.id_maquina=? ");
                args.add(Jdbc.id(maquina));
            }
            if (ano != null) {
                if (ano < 1900 || ano > 2200) {
                    throw new IllegalArgumentException("Ano invalido.");
                }
                sql.append("AND (co.ano_inicio IS NULL OR co.ano_inicio<=?) AND (co.ano_fim IS NULL OR co.ano_fim>=?) ");
                args.add(ano);
                args.add(ano);
            }
            sql.append(") ");
        }
        sql.append("ORDER BY i.nome,i.id_item");
        try (Connection c = provider.abrir()) {
            return Jdbc.listar(c, sql.toString(), Mapeamento::peca, args.toArray());
        }
    }

    public List<Peca> pesquisar(String termo) throws SQLException {
        return pesquisar(termo, null, null, null, null);
    }

    public FichaPeca buscarFichaPorId(long id) throws SQLException {
        return ficha(id, false);
    }

    public FichaPeca buscarFichaPublica(long id) throws SQLException {
        return ficha(id, true);
    }

    private FichaPeca ficha(long id, boolean publica) throws SQLException {
        return Jdbc.transacao(provider, c -> {
            // Mesmo bloqueio usado nas alteracoes: as listas correspondem ao mesmo estado do pai.
            bloquear(c, id);
            Peca p = buscar(c, id);
            if (publica && p.getEstado() != EstadoItem.PUBLICADO) {
                return null;
            }
            return new FichaPeca(p, DetalhesPecaDAO.imagens(c, id), DetalhesPecaDAO.especificacoes(c, id),
                    DetalhesPecaDAO.codigos(c, id), DetalhesPecaDAO.compatibilidades(c, id, publica));
        });
    }
}
