package DAO;

import Model.*;
import Util.ConnectionFactory;

import java.sql.*;
import java.util.*;

public class ConsultaPecaDAO {
    private final ConexaoProvider provider;

    public ConsultaPecaDAO() {
        this(ConnectionFactory::getConnection);
    }

    public ConsultaPecaDAO(ConexaoProvider provider) {
        this.provider = provider;
    }

    public List<Peca> administracao(String termo, Long marca, Long categoria, String estado) throws SQLException {
        String t = termo == null ? "" : termo.trim();
        List<Object> args = new ArrayList<>();
        StringBuilder sql = new StringBuilder(Mapeamento.SELECT_PECA).append("WHERE (i.nome LIKE ? ESCAPE '!' OR i.descricao LIKE ? ESCAPE '!' ")
                .append("OR EXISTS(SELECT 1 FROM codigo_peca cp WHERE cp.id_peca=p.id_peca AND cp.codigo_normalizado LIKE ? ESCAPE '!')) ");
        args.add(Jdbc.trecho(t));
        args.add(Jdbc.trecho(t));
        args.add(Jdbc.trecho(t.replaceAll("\\s+", "").toUpperCase(Locale.ROOT)));
        if (marca != null) {
            sql.append("AND i.id_marca=? ");
            args.add(Jdbc.id(marca));
        }
        if (categoria != null) {
            sql.append("AND i.id_categoria=? ");
            args.add(Jdbc.id(categoria));
        }
        if (estado != null && !estado.isEmpty()) {
            EstadoItem.doBanco(estado);
            sql.append("AND i.estado=? ");
            args.add(estado);
        }
        sql.append("ORDER BY i.nome,i.id_item");
        try (Connection c = provider.abrir()) {
            return Jdbc.listar(c, sql.toString(), Mapeamento::peca, args.toArray());
        }
    }

    public Map<Long, ResumoPeca> resumos(List<Long> ids) throws SQLException {
        Map<Long, ResumoPeca> r = new HashMap<>();
        if (ids.isEmpty()) return r;
        String marcadores = String.join(",", Collections.nCopies(ids.size(), "?"));
        String sql = "SELECT i.id_item,(SELECT im.url_imagem FROM imagem im WHERE im.id_item=i.id_item ORDER BY im.id_imagem LIMIT 1) AS imagem,"
                + "(SELECT cp.codigo FROM codigo_peca cp WHERE cp.id_peca=i.id_item ORDER BY cp.id_codigo LIMIT 1) AS codigo "
                + "FROM item_catalogo i WHERE i.id_item IN (" + marcadores + ")";
        try (Connection c = provider.abrir(); PreparedStatement s = c.prepareStatement(sql)) {
            for (int j = 0; j < ids.size(); j++) s.setLong(j + 1, Jdbc.id(ids.get(j)));
            try (ResultSet dados = s.executeQuery()) {
                while (dados.next())
                    r.put(dados.getLong("id_item"), new ResumoPeca(dados.getString("imagem"), dados.getString("codigo")));
            }
        }
        return r;
    }
}
