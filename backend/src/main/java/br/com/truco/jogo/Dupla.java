package br.com.truco.jogo;

import br.com.truco.jogadores.Jogador;

import java.util.List;

/** Dupla "Nós" ou "Eles", com o seu placar. */
public class Dupla {
    private final String nome;
    private final List<Jogador> jogadores;
    private int pontos;

    public Dupla(String nome, Jogador primeiro, Jogador segundo) {
        this.nome = nome;
        this.jogadores = List.of(primeiro, segundo);
    }

    public void adicionarPontos(int quantidade) {
        pontos += quantidade;
    }

    public int getPontos() {
        return pontos;
    }

    public boolean temOnze() {
        return pontos == 11;
    }

    public boolean contem(Jogador jogador) {
        return jogadores.contains(jogador);
    }

    public Jogador parceiroDe(Jogador jogador) {
        return jogadores.get(0) == jogador ? jogadores.get(1) : jogadores.get(0);
    }

    public List<Jogador> getJogadores() {
        return jogadores;
    }

    public String getNome() {
        return nome;
    }

    @Override
    public String toString() {
        return nome;
    }
}
