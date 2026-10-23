package br.com.truco.cartas;

/** Valores do baralho de truco, em ordem crescente de força (RN02). */
public enum Valor {
    QUATRO("4"),
    CINCO("5"),
    SEIS("6"),
    SETE("7"),
    DAMA("Q"),
    VALETE("J"),
    REI("K"),
    AS("A"),
    DOIS("2"),
    TRES("3");

    private final String simbolo;

    Valor(String simbolo) {
        this.simbolo = simbolo;
    }

    public String getSimbolo() {
        return simbolo;
    }

    /** 1 (QUATRO) a 10 (TRES). */
    public int ordem() {
        return ordinal() + 1;
    }

    /** Próximo valor na sequência circular: TRES volta para QUATRO (RN03). */
    public Valor proximo() {
        return values()[(ordinal() + 1) % values().length];
    }

    public static Valor deSimbolo(String simbolo) {
        for (Valor v : values()) {
            if (v.simbolo.equalsIgnoreCase(simbolo)) {
                return v;
            }
        }
        throw new IllegalArgumentException("Valor inválido: " + simbolo);
    }
}
