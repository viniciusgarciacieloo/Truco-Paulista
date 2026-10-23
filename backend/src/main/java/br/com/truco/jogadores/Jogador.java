package br.com.truco.jogadores;

import br.com.truco.cartas.Carta;
import br.com.truco.cartas.Vira;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/** Ocupante de um lugar da mesa, humano ou bot. */
public abstract class Jogador {
    private final String nome;
    private final Posicao posicao;
    private final List<Carta> cartas = new ArrayList<>(3);

    protected Jogador(String nome, Posicao posicao) {
        this.nome = nome;
        this.posicao = posicao;
    }

    public void receberCartas(List<Carta> novas) {
        cartas.addAll(novas);
    }

    public void removerCarta(Carta carta) {
        if (!cartas.remove(carta)) {
            throw new IllegalArgumentException(nome + " não tem a carta " + carta);
        }
    }

    /** Devolve as cartas ao início de cada mão. */
    public void descartarTodas() {
        cartas.clear();
    }

    public List<Carta> getCartas() {
        return Collections.unmodifiableList(cartas);
    }

    public boolean temCartas() {
        return !cartas.isEmpty();
    }

    /** Carta mais forte da mão, usada na rodada de desempate (RN21). */
    public Carta maiorCarta(Vira vira) {
        return cartas.stream()
                .max(Comparator.comparingInt(vira::forca))
                .orElseThrow(() -> new IllegalStateException(nome + " não tem cartas"));
    }

    public abstract boolean ehBot();

    public String getNome() {
        return nome;
    }

    public Posicao getPosicao() {
        return posicao;
    }

    @Override
    public String toString() {
        return nome;
    }
}
