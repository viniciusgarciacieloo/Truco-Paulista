package br.com.truco.cartas;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/** Monte de 40 cartas: baralho comum sem 8, 9, 10 e coringas (RN01). O índice 0 é o topo. */
public class Baralho {
    public static final int TOTAL_CARTAS = 40;

    private final List<Carta> cartas = new ArrayList<>(TOTAL_CARTAS);

    public Baralho() {
        for (Naipe naipe : Naipe.values()) {
            for (Valor valor : Valor.values()) {
                cartas.add(new Carta(valor, naipe));
            }
        }
    }

    public void embaralhar(Random random) {
        Collections.shuffle(cartas, random);
    }

    /** Corte seco: as {@code ponto} cartas de cima vão para baixo do monte (RN06). */
    public void cortar(int ponto) {
        if (ponto < 1 || ponto >= cartas.size()) {
            throw new IllegalArgumentException("Ponto de corte inválido: " + ponto);
        }
        List<Carta> topo = new ArrayList<>(cartas.subList(0, ponto));
        cartas.subList(0, ponto).clear();
        cartas.addAll(topo);
    }

    public Carta comprar() {
        if (cartas.isEmpty()) {
            throw new IllegalStateException("O baralho acabou");
        }
        return cartas.remove(0);
    }

    public int tamanho() {
        return cartas.size();
    }
}
