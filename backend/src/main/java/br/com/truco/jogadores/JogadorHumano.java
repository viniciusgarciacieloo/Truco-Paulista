package br.com.truco.jogadores;

/** O usuário. As decisões dele chegam pela tela, através do ControladorPartida (RN20). */
public class JogadorHumano extends Jogador {

    public JogadorHumano(String nome, Posicao posicao) {
        super(nome, posicao);
    }

    @Override
    public boolean ehBot() {
        return false;
    }
}
