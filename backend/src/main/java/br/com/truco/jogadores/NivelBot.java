package br.com.truco.jogadores;

import java.util.Random;

/** Dificuldade dos bots, com o apelido usado na tela. */
public enum NivelBot {
    FACIL("Novato"),
    NORMAL("Pistoleiro"),
    DIFICIL("Xerife");

    private final String apelido;

    NivelBot(String apelido) {
        this.apelido = apelido;
    }

    public String getApelido() {
        return apelido;
    }

    public EstrategiaBot criarEstrategia(Random random) {
        return switch (this) {
            case FACIL -> new EstrategiaFacil(random);
            case NORMAL -> new EstrategiaNormal(random);
            case DIFICIL -> new EstrategiaDificil(random);
        };
    }
}
