package br.com.truco.web;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;

/** Serve os arquivos do frontend (HTML, CSS, JS, imagens). */
public class ArquivosEstaticosHandler implements HttpHandler {
    private static final Map<String, String> TIPOS = Map.of(
            "html", "text/html; charset=utf-8",
            "css", "text/css; charset=utf-8",
            "js", "text/javascript; charset=utf-8",
            "svg", "image/svg+xml",
            "png", "image/png",
            "jpg", "image/jpeg",
            "ico", "image/x-icon",
            "json", "application/json; charset=utf-8");

    private final Path raiz;

    public ArquivosEstaticosHandler(Path raiz) {
        this.raiz = raiz.toAbsolutePath().normalize();
    }

    @Override
    public void handle(HttpExchange troca) throws IOException {
        String caminho = troca.getRequestURI().getPath();
        if (caminho.equals("/") || caminho.isEmpty()) {
            caminho = "/index.html";
        }
        Path arquivo = raiz.resolve(caminho.substring(1)).normalize();
        if (!arquivo.startsWith(raiz) || !Files.isRegularFile(arquivo)) {
            byte[] corpo = "404 - Esse caminho não leva a saloon nenhum.".getBytes(StandardCharsets.UTF_8);
            troca.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");
            troca.sendResponseHeaders(404, corpo.length);
            try (OutputStream saida = troca.getResponseBody()) {
                saida.write(corpo);
            }
            return;
        }
        String nome = arquivo.getFileName().toString();
        String extensao = nome.substring(nome.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
        byte[] corpo = Files.readAllBytes(arquivo);
        troca.getResponseHeaders().set("Content-Type", TIPOS.getOrDefault(extensao, "application/octet-stream"));
        troca.getResponseHeaders().set("Cache-Control", "no-cache");
        troca.sendResponseHeaders(200, corpo.length);
        try (OutputStream saida = troca.getResponseBody()) {
            saida.write(corpo);
        }
    }
}
