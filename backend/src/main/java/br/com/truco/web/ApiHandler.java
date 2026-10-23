package br.com.truco.web;

import br.com.truco.aplicacao.ControladorPartida;
import br.com.truco.aplicacao.EstadoMesa;
import br.com.truco.jogo.Configuracao;
import br.com.truco.jogadores.NivelBot;
import br.com.truco.mao.RespostaPedido;
import br.com.truco.regras.JogadaInvalidaException;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * API REST do jogo. Toda resposta é o {@link EstadoMesa} em JSON; erros de regra
 * voltam com status 400, a mensagem em "erro" e o estado atual em "estado".
 */
public class ApiHandler implements HttpHandler {
    private final ControladorPartida controlador;

    public ApiHandler(ControladorPartida controlador) {
        this.controlador = controlador;
    }

    @Override
    public void handle(HttpExchange troca) throws IOException {
        try {
            String metodo = troca.getRequestMethod().toUpperCase(Locale.ROOT);
            String rota = troca.getRequestURI().getPath().substring("/api".length());
            Map<String, String> p = lerParametros(troca);

            EstadoMesa estado = switch (metodo + " " + rota) {
                case "GET /estado" -> controlador.obterEstado();
                case "POST /partida/nova" -> controlador.novaPartida(new Configuracao(
                        p.get("nome"), nivel(p.get("nivel"))));
                case "POST /partida/reiniciar" -> controlador.reiniciar();
                case "POST /partida/abandonar" -> controlador.abandonar();
                case "POST /embaralhar" -> controlador.embaralhar();
                case "POST /cortar" -> controlador.cortar(inteiro(p.get("ponto"), "ponto"));
                case "POST /distribuir" -> controlador.distribuir();
                case "POST /jogar" -> controlador.jogarCarta(inteiro(p.get("indice"), "indice"),
                        Boolean.parseBoolean(p.get("coberta")));
                case "POST /truco" -> controlador.pedirAumento();
                case "POST /responder" -> controlador.responderPedido(resposta(p.get("resposta")));
                case "POST /mao-de-onze" -> controlador.decidirMaoDeOnze(Boolean.parseBoolean(p.get("jogar")));
                case "POST /passo" -> {
                    controlador.executarPassoDoBot();
                    yield controlador.obterEstado();
                }
                default -> null;
            };

            if (estado == null) {
                responder(troca, 404, Json.escrever(Map.of("erro", "Rota não encontrada: " + metodo + " " + rota)));
            } else {
                responder(troca, 200, Json.escrever(estado.getDados()));
            }
        } catch (JogadaInvalidaException | IllegalArgumentException | IllegalStateException e) {
            Map<String, Object> corpo = new LinkedHashMap<>();
            corpo.put("erro", e.getMessage());
            corpo.put("estado", controlador.obterEstado().getDados());
            responder(troca, 400, Json.escrever(corpo));
        } catch (RuntimeException e) {
            e.printStackTrace();
            responder(troca, 500, Json.escrever(Map.of("erro", "Erro interno: " + e)));
        }
    }

    private static NivelBot nivel(String valor) {
        return valor == null || valor.isBlank() ? NivelBot.NORMAL : NivelBot.valueOf(valor.toUpperCase(Locale.ROOT));
    }

    private static RespostaPedido resposta(String valor) {
        if (valor == null) {
            throw new IllegalArgumentException("Informe a resposta: ACEITAR, CORRER ou AUMENTAR");
        }
        return RespostaPedido.valueOf(valor.toUpperCase(Locale.ROOT));
    }

    private static int inteiro(String valor, String nome) {
        try {
            return Integer.parseInt(valor);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Parâmetro inválido: " + nome);
        }
    }

    /** Junta os parâmetros da query string e do corpo (application/x-www-form-urlencoded). */
    private static Map<String, String> lerParametros(HttpExchange troca) throws IOException {
        Map<String, String> parametros = new HashMap<>();
        adicionar(parametros, troca.getRequestURI().getRawQuery());
        try (InputStream corpo = troca.getRequestBody()) {
            adicionar(parametros, new String(corpo.readAllBytes(), StandardCharsets.UTF_8));
        }
        return parametros;
    }

    private static void adicionar(Map<String, String> parametros, String texto) {
        if (texto == null || texto.isBlank()) {
            return;
        }
        for (String par : texto.split("&")) {
            int igual = par.indexOf('=');
            String chave = igual < 0 ? par : par.substring(0, igual);
            String valor = igual < 0 ? "" : par.substring(igual + 1);
            parametros.put(URLDecoder.decode(chave, StandardCharsets.UTF_8), URLDecoder.decode(valor, StandardCharsets.UTF_8));
        }
    }

    private static void responder(HttpExchange troca, int status, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        troca.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        troca.getResponseHeaders().set("Cache-Control", "no-store");
        troca.sendResponseHeaders(status, bytes.length);
        try (OutputStream saida = troca.getResponseBody()) {
            saida.write(bytes);
        }
    }
}
