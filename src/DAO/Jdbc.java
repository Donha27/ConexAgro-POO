package DAO;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Infraestrutura JDBC compartilhada. Nenhuma regra de negocio fica aqui.
 */
final class Jdbc {
    private Jdbc() {
    }

    interface Trabalho<T> {
        T executar(Connection c) throws SQLException;
    }

    interface Leitor<T> {
        T ler(ResultSet r) throws SQLException;
    }

    static <T> T transacao(ConexaoProvider provider, Trabalho<T> trabalho) throws SQLException {
        try (Connection c = provider.abrir()) {
            c.setAutoCommit(false);
            try {
                T resposta = trabalho.executar(c);
                c.commit();
                return resposta;
            } catch (SQLException | RuntimeException e) {
                try {
                    c.rollback();
                } catch (SQLException rollback) {
                    e.addSuppressed(rollback);
                }
                throw e;
            }
        }
    }

    private static void parametros(PreparedStatement s, Object... valores) throws SQLException {
        for (int i = 0; i < valores.length; i++) {
            Object valor = valores[i];
            if (valor instanceof LocalDateTime) {
                valor = Timestamp.valueOf((LocalDateTime) valor);
            }
            s.setObject(i + 1, valor);
        }
    }

    static int executar(Connection c, String sql, Object... valores) throws SQLException {
        try (PreparedStatement s = c.prepareStatement(sql)) {
            parametros(s, valores);
            return s.executeUpdate();
        }
    }

    static void alterarUma(Connection c, String sql, Object... valores) throws SQLException {
        if (executar(c, sql, valores) != 1) {
            throw new SQLException("Registro nao encontrado para a operacao.", "02000");
        }
    }

    static long inserir(Connection c, String sql, Object... valores) throws SQLException {
        try (PreparedStatement s = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            parametros(s, valores);
            s.executeUpdate();
            try (ResultSet r = s.getGeneratedKeys()) {
                if (!r.next()) {
                    throw new SQLException("O banco nao retornou o ID gerado.");
                }
                return r.getLong(1);
            }
        }
    }

    static <T> List<T> listar(Connection c, String sql, Leitor<T> leitor, Object... valores) throws SQLException {
        List<T> lista = new ArrayList<>();
        try (PreparedStatement s = c.prepareStatement(sql)) {
            parametros(s, valores);
            try (ResultSet r = s.executeQuery()) {
                while (r.next()) {
                    lista.add(leitor.ler(r));
                }
            }
        }
        return lista;
    }

    static <T> T um(Connection c, String sql, Leitor<T> leitor, Object... valores) throws SQLException {
        List<T> lista = listar(c, sql, leitor, valores);
        return lista.isEmpty() ? null : lista.get(0);
    }

    static long numero(Connection c, String sql, Object... valores) throws SQLException {
        Long valor = um(c, sql, r -> r.getLong(1), valores);
        return valor == null ? 0 : valor;
    }

    static long id(long id) {
        if (id < 1 || id > 4294967295L) {
            throw new IllegalArgumentException("Informe um ID persistido valido.");
        }
        return id;
    }

    static String trecho(String termo) {
        String valor = termo == null ? "" : termo.trim();
        return "%" + valor.replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
    }

    static LocalDateTime data(ResultSet r, String coluna) throws SQLException {
        Timestamp valor = r.getTimestamp(coluna);
        return valor == null ? null : valor.toLocalDateTime();
    }

    static Integer inteiroNulo(ResultSet r, String coluna) throws SQLException {
        int valor = r.getInt(coluna);
        return r.wasNull() ? null : valor;
    }

    static Long longNulo(ResultSet r, String coluna) throws SQLException {
        long valor = r.getLong(coluna);
        return r.wasNull() ? null : valor;
    }
}
