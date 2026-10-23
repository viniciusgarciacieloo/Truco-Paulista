package br.com.truco.regras;

import br.com.truco.jogo.Dupla;
import br.com.truco.mao.Jogada;

/**
 * @param vencedora       dupla vencedora, ou null se empatou
 * @param jogadaVencedora maior carta da rodada (a primeira jogada, se houve empate)
 */
public record ResultadoRodada(Dupla vencedora, boolean empate, Jogada jogadaVencedora) {
}
