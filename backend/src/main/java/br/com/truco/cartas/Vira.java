package br.com.truco.cartas;

import java.util.Objects;

/** Carta virada após a distribuição. Define as manilhas e a força das cartas da mão (RN03, RN04). */
public final class Vira {
    private final Carta carta;

    public Vira(Carta carta) {
        this.carta = Objects.requireNonNull(carta);
    }

    public Carta getCarta() {
        return carta;
    }

    public Valor valorManilha() {
        return carta.getValor().proximo();
    }

    public boolean ehManilha(Carta c) {
        return c.getValor() == valorManilha();
    }

    /** Carta comum: 1..10 conforme {@link Valor#ordem()}. Manilha: 11..14 conforme o naipe. */
    public int forca(Carta c) {
        return ehManilha(c) ? 10 + c.getNaipe().forcaManilha() : c.getValor().ordem();
    }
}
