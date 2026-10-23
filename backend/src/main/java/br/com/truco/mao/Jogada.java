package br.com.truco.mao;

import br.com.truco.cartas.Carta;
import br.com.truco.jogadores.Jogador;

/** Uma carta colocada na mesa por um jogador, aberta ou coberta (RN16). */
public record Jogada(Jogador jogador, Carta carta, boolean coberta) {
}
