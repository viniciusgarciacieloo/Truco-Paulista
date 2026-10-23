package br.com.truco.jogadores;

import br.com.truco.cartas.Carta;
import br.com.truco.mao.Jogada;
import br.com.truco.mao.PedidoAumento;
import br.com.truco.mao.RespostaPedido;

import java.util.List;
import java.util.Random;

/** "Novato": escolhe ao acaso entre as ações válidas. */
public class EstrategiaFacil implements EstrategiaBot {
    protected final Random random;

    public EstrategiaFacil(Random random) {
        this.random = random;
    }

    @Override
    public Jogada escolherCarta(VisaoDoJogo visao) {
        List<Carta> permitidas = visao.cartasPermitidas();
        return new Jogada(visao.eu(), permitidas.get(random.nextInt(permitidas.size())), false);
    }

    @Override
    public boolean querPedirAumento(VisaoDoJogo visao) {
        return visao.podePedirAumento() && random.nextInt(100) < 10;
    }

    @Override
    public RespostaPedido responderPedido(VisaoDoJogo visao, PedidoAumento pedido) {
        int sorteio = random.nextInt(100);
        if (sorteio < 55) {
            return RespostaPedido.ACEITAR;
        }
        if (sorteio < 90 || pedido.getValorPedido().ehMaximo()) {
            return RespostaPedido.CORRER;
        }
        return RespostaPedido.AUMENTAR;
    }

    @Override
    public boolean decidirMaoDeOnze(VisaoDoJogo visao) {
        return random.nextBoolean();
    }
}
