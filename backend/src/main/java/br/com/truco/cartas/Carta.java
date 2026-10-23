package br.com.truco.cartas;

import java.util.Objects;

/** Carta imutável (valor + naipe). */
public final class Carta {
    private final Valor valor;
    private final Naipe naipe;

    public Carta(Valor valor, Naipe naipe) {
        this.valor = Objects.requireNonNull(valor);
        this.naipe = Objects.requireNonNull(naipe);
    }

    public Valor getValor() {
        return valor;
    }

    public Naipe getNaipe() {
        return naipe;
    }

    /** Código curto usado pela API, ex.: "QP" (dama de paus), "3C" (três de copas). */
    public String codigo() {
        return valor.getSimbolo() + naipe.getCodigo();
    }

    public static Carta deCodigo(String codigo) {
        if (codigo == null || codigo.length() < 2) {
            throw new IllegalArgumentException("Código de carta inválido: " + codigo);
        }
        Valor valor = Valor.deSimbolo(codigo.substring(0, codigo.length() - 1));
        Naipe naipe = Naipe.deCodigo(Character.toUpperCase(codigo.charAt(codigo.length() - 1)));
        return new Carta(valor, naipe);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Carta outra)) {
            return false;
        }
        return valor == outra.valor && naipe == outra.naipe;
    }

    @Override
    public int hashCode() {
        return Objects.hash(valor, naipe);
    }

    @Override
    public String toString() {
        return valor.getSimbolo() + naipe.getSimbolo();
    }
}
