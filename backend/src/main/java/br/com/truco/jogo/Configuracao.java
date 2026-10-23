package br.com.truco.jogo;

import br.com.truco.jogadores.NivelBot;

/** Dados informados ao iniciar a partida (UC01). */
public record Configuracao(String nomeJogador, NivelBot nivelBot) {

    public Configuracao {
        nomeJogador = nomeJogador == null || nomeJogador.isBlank() ? "Forasteiro" : nomeJogador.strip();
        if (nomeJogador.length() > 20) {
            nomeJogador = nomeJogador.substring(0, 20);
        }
        nivelBot = nivelBot == null ? NivelBot.NORMAL : nivelBot;
    }
}
