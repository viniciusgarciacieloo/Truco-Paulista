package br.com.truco.web;

import br.com.truco.aplicacao.ControladorPartida;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Executors;

/**
 * Sobe o saloon: API em /api/ e o frontend em /.
 * Uso: java -cp build/classes br.com.truco.web.Servidor [porta] [pastaDoFrontend]
 */
public final class Servidor {

    private Servidor() {
    }

    public static void main(String[] args) throws IOException {
        int porta = args.length > 0 ? Integer.parseInt(args[0]) : 8080;
        Path frontend = Path.of(args.length > 1 ? args[1] : "frontend").toAbsolutePath().normalize();
        if (!Files.isDirectory(frontend)) {
            System.err.println("Pasta do frontend não encontrada: " + frontend);
            System.exit(1);
        }

        ControladorPartida controlador = new ControladorPartida();
        HttpServer servidor = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), porta), 0);
        servidor.createContext("/api/", new ApiHandler(controlador));
        servidor.createContext("/", new ArquivosEstaticosHandler(frontend));
        servidor.setExecutor(Executors.newFixedThreadPool(4));
        servidor.start();
        System.out.println("Saloon aberto em http://localhost:" + porta + "  (Ctrl+C para fechar)");
    }
}
