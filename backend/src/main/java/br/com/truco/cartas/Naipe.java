package br.com.truco.cartas;

/** Naipes do baralho. A ordem só importa para desempatar manilhas (RN04). */
public enum Naipe {
    OUROS('O', "♦", 1),
    ESPADAS('E', "♠", 2),
    COPAS('C', "♥", 3),
    PAUS('P', "♣", 4);

    private final char codigo;
    private final String simbolo;
    private final int forcaManilha;

    Naipe(char codigo, String simbolo, int forcaManilha) {
        this.codigo = codigo;
        this.simbolo = simbolo;
        this.forcaManilha = forcaManilha;
    }

    public char getCodigo() {
        return codigo;
    }

    public String getSimbolo() {
        return simbolo;
    }

    /** Ouros = 1 (pica-fumo) ... Paus = 4 (zap). */
    public int forcaManilha() {
        return forcaManilha;
    }

    public boolean ehVermelho() {
        return this == OUROS || this == COPAS;
    }

    public static Naipe deCodigo(char codigo) {
        for (Naipe n : values()) {
            if (n.codigo == codigo) {
                return n;
            }
        }
        throw new IllegalArgumentException("Naipe inválido: " + codigo);
    }
}
