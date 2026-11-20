package Web;

import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.util.concurrent.*;

public class ServidorWeb implements AutoCloseable {
    private final HttpServer servidor;
    private final ExecutorService executor = Executors.newFixedThreadPool(8);

    public ServidorWeb(ApiConexAgro api, Path web, Path imagens, int porta) throws IOException {
        servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", porta), 64);
        servidor.createContext("/", new HandlerWeb(api, web, imagens, porta));
        servidor.setExecutor(executor);
    }

    public void iniciar() {
        servidor.start();
    }

    @Override
    public void close() {
        servidor.stop(1);
        executor.shutdown();
    }
}
