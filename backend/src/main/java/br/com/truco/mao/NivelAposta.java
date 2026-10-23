package br.com.truco.mao;

/** Valor da mão: 1 → Truco 3 → Seis 6 → Nove 9 → Doze 12 (RN13). */
public enum NivelAposta {
    UM(1, null),
    TRUCO(3, "Truco"),
    SEIS(6, "Seis"),
    NOVE(9, "Nove"),
    DOZE(12, "Doze");

    private final int pontos;
    private final String nomePedido;

    NivelAposta(int pontos, String nomePedido) {
        this.pontos = pontos;
        this.nomePedido = nomePedido;
    }

    public int pontos() {
        return pontos;
    }

    public String getNomePedido() {
        return nomePedido;
    }

    public boolean ehMaximo() {
        return this == DOZE;
    }

    public NivelAposta proximo() {
        if (ehMaximo()) {
            throw new IllegalStateException("A aposta já está no máximo");
        }
        return values()[ordinal() + 1];
    }

    /** Pontos de quem pediu este valor quando o adversário corre: o valor anterior (RN15). */
    public int pontosSeCorrer() {
        if (this == UM) {
            throw new IllegalStateException("Não existe pedido de 1 ponto");
        }
        return values()[ordinal() - 1].pontos;
    }
}
