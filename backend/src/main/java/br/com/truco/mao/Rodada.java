package br.com.truco.mao;

import br.com.truco.jogo.Dupla;
import br.com.truco.jogadores.Jogador;
import br.com.truco.regras.ResultadoRodada;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Fração da mão: cada jogador joga uma carta e a mais forte vence. */
public class Rodada {
    public static final int JOGADAS_POR_RODADA = 4;

    private final int numero;
    private final Jogador jogadorInicial;
    private final boolean desempate;
    private final List<Jogada> jogadas = new ArrayList<>(JOGADAS_POR_RODADA);
    private Dupla vencedora;
    private boolean empatada;
    private Jogada jogadaVencedora;
    private boolean apurada;

    public Rodada(int numero, Jogador jogadorInicial, boolean desempate) {
        this.numero = numero;
        this.jogadorInicial = jogadorInicial;
        this.desempate = desempate;
    }

    public void adicionar(Jogada jogada) {
        if (estaCompleta()) {
            throw new IllegalStateException("A rodada já tem 4 cartas");
        }
        jogadas.add(jogada);
    }

    public boolean estaCompleta() {
        return jogadas.size() == JOGADAS_POR_RODADA;
    }

    public void registrarResultado(ResultadoRodada resultado) {
        this.vencedora = resultado.vencedora();
        this.empatada = resultado.empate();
        this.jogadaVencedora = resultado.jogadaVencedora();
        this.apurada = true;
    }

    /** Rodada seguinte a um empate: começa pelo pé e todos mostram a maior carta (RN21). */
    public boolean ehDesempate() {
        return desempate;
    }

    public int getNumero() {
        return numero;
    }

    public Jogador getJogadorInicial() {
        return jogadorInicial;
    }

    public List<Jogada> getJogadas() {
        return Collections.unmodifiableList(jogadas);
    }

    public Dupla getVencedora() {
        return vencedora;
    }

    public boolean isEmpatada() {
        return empatada;
    }

    public Jogada getJogadaVencedora() {
        return jogadaVencedora;
    }

    public boolean estaApurada() {
        return apurada;
    }
}
