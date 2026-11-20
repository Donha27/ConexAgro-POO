import DAO.ConfiguracaoInicialDAO;
import Util.ConnectionFactory;
import Web.*;

import java.nio.file.*;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.sql.Connection;
import java.util.*;

/**
 * Ponto de entrada do site local.
 */
public class Principal {
    public static void main(String[] args) {
        try {
            Path raiz = Paths.get("").toAbsolutePath().normalize();
            if (!Files.isRegularFile(raiz.resolve("web/index.html")))
                throw new IllegalStateException("A pasta web precisa ficar na raiz do projeto, ao lado de src.");
            try (Connection c = ConnectionFactory.getConnection()) {
                if (!"conexagro".equalsIgnoreCase(c.getCatalog()))
                    throw new IllegalStateException("Configure a conexao com o banco conexagro.");
            }
            Properties config = new Properties();
            try (Reader r = Files.newBufferedReader(raiz.resolve("conexao.properties"), StandardCharsets.UTF_8)) {
                config.load(r);
            }
            int porta = Integer.parseInt(config.getProperty("web.port", "8080"));
            if (porta < 1024 || porta > 65535) throw new IllegalArgumentException("Use uma porta entre 1024 e 65535.");
            String chave = null;
            if (new ConfiguracaoInicialDAO().pendente()) {
                byte[] bytes = new byte[32];
                new SecureRandom().nextBytes(bytes);
                chave = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
            }
            ServidorWeb web = new ServidorWeb(new ApiConexAgro(chave), raiz.resolve("web"), raiz.resolve("dados/imagens"), porta);
            Runtime.getRuntime().addShutdownHook(new Thread(web::close));
            web.iniciar();
            String url = "http://127.0.0.1:" + porta;
            System.out.println("\nCONEXAGRO INICIADA\nAbra no navegador: " + url);
            if (chave != null)
                System.out.println("\nPRIMEIRO ACESSO - configure seu administrador neste endereco:\n" + url + "/#configurar=" + chave + "\nO endereco e temporario e encerra apos a configuracao.");
            System.out.println("\nMantenha esta execucao aberta enquanto utilizar o site.\n");
        } catch (java.net.BindException e) {
            System.err.println("A porta esta em uso. Adicione web.port=8081 ao conexao.properties e execute novamente.");
            System.exit(1);
        } catch (Exception e) {
            System.err.println("Nao foi possivel iniciar a ConexAgro: " + e.getMessage());
            System.exit(1);
        }
    }
}
