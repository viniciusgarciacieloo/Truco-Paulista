package br.com.truco.jogadores;

/**
 * Lugares da mesa, já na ordem de jogo anti-horária (RN05): SUL → LESTE → NORTE → OESTE.
 * O humano senta no SUL; LESTE fica à direita dele e OESTE à esquerda.
 */
public enum Posicao {
    SUL("Sul"),
    LESTE("Leste"),
    NORTE("Norte"),
    OESTE("Oeste");

    private final String nome;

    Posicao(String nome) {
        this.nome = nome;
    }

    public String getNome() {
        return nome;
    }

    /** Jogador à direita = próximo a jogar no sentido anti-horário. */
    public Posicao direita() {
        return values()[(ordinal() + 1) % 4];
    }

    public Posicao esquerda() {
        return values()[(ordinal() + 3) % 4];
    }

    public Posicao parceiro() {
        return values()[(ordinal() + 2) % 4];
    }

    public boolean mesmaDupla(Posicao outra) {
        return this == outra || parceiro() == outra;
    }
}
