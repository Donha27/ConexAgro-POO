package DAO;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Abre uma nova conexao por chamada. O DAO fecha a conexao que receber.
 */
@FunctionalInterface
public interface ConexaoProvider {
    Connection abrir() throws SQLException;
}
