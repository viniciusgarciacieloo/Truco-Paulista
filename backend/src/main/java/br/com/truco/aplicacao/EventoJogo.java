package br.com.truco.aplicacao;

import br.com.truco.jogadores.Posicao;

import java.time.Instant;

/**
 * Linha do histórico da mesa (UC17).
 *
 * @param posicao quem falou, para a tela mostrar o balão; null para avisos gerais
 */
public record EventoJogo(long id, String tipo, Posicao posicao, String descricao, Instant momento) {
    public static final String INFO = "INFO";
    public static final String FALA = "FALA";
    public static final String RODADA = "RODADA";
    public static final String MAO = "MAO";
    public static final String PARTIDA = "PARTIDA";
}
