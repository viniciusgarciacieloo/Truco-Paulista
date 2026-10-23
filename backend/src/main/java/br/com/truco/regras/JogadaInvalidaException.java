package br.com.truco.regras;

/** Ação recusada pelas regras. A mensagem é mostrada ao jogador. */
public class JogadaInvalidaException extends RuntimeException {

    public JogadaInvalidaException(String mensagem) {
        super(mensagem);
    }
}
