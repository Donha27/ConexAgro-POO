package Util;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Centraliza a criacao das conexoes JDBC, como no projeto da aula.
 */
public final class ConnectionFactory {

    private ConnectionFactory() {
        // Esta classe e utilizada por meio do metodo static getConnection().
    }

    public static Connection getConnection() throws SQLException {
        Properties config = carregarConfiguracao();

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new SQLException(
                    "Driver do MySQL nao encontrado. Adicione o arquivo "
                            + "mysql-connector-j-....jar nas bibliotecas do projeto.", e);
        }

        String url = config.getProperty("db.url");
        String usuario = config.getProperty("db.user");
        String senha = config.getProperty("db.password", "");

        if (url == null || url.trim().isEmpty()
                || usuario == null || usuario.trim().isEmpty()) {
            throw new SQLException(
                    "Preencha db.url e db.user no arquivo conexao.properties.");
        }

        return DriverManager.getConnection(url.trim(), usuario.trim(), senha);
    }

    private static Properties carregarConfiguracao() throws SQLException {
        Properties config = new Properties();

        try (Reader arquivo = Files.newBufferedReader(
                Paths.get("conexao.properties"), StandardCharsets.UTF_8)) {
            config.load(arquivo);
            return config;
        } catch (IOException e) {
            throw new SQLException(
                    "Nao foi possivel ler conexao.properties. Copie "
                            + "conexao.properties.example para conexao.properties "
                            + "na raiz do projeto e preencha os dados do MySQL.", e);
        }
    }
}
