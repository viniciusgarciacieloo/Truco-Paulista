package br.com.truco.mao;

import br.com.truco.jogo.Dupla;
import br.com.truco.jogadores.Jogador;

/** Pedido de Truco, Seis, Nove ou Doze e a resposta da dupla desafiada (UC10, UC11). */
public class PedidoAumento {
    private final Jogador solicitante;
    private final Dupla duplaSolicitante;
    private final NivelAposta valorPedido;
    private RespostaPedido resposta;

    public PedidoAumento(Jogador solicitante, Dupla duplaSolicitante, NivelAposta valorPedido) {
        this.solicitante = solicitante;
        this.duplaSolicitante = duplaSolicitante;
        this.valorPedido = valorPedido;
    }

    public void responder(RespostaPedido resposta) {
        if (this.resposta != null) {
            throw new IllegalStateException("O pedido já foi respondido");
        }
        this.resposta = resposta;
    }

    public boolean estaPendente() {
        return resposta == null;
    }

    public Jogador getSolicitante() {
        return solicitante;
    }

    public Dupla getDuplaSolicitante() {
        return duplaSolicitante;
    }

    public NivelAposta getValorPedido() {
        return valorPedido;
    }

    public RespostaPedido getResposta() {
        return resposta;
    }
}
