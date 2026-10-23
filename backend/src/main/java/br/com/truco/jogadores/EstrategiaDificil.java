package br.com.truco.jogadores;

import br.com.truco.cartas.Baralho;
import br.com.truco.cartas.Carta;
import br.com.truco.cartas.Naipe;
import br.com.truco.cartas.Valor;
import br.com.truco.mao.NivelAposta;
import br.com.truco.mao.PedidoAumento;
import br.com.truco.mao.RespostaPedido;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** "Xerife": heurística do Pistoleiro, mais blefe e contagem das cartas já jogadas. */
public class EstrategiaDificil extends EstrategiaNormal {
    private static final int CHANCE_BLEFE = 12;

    public EstrategiaDificil(Random random) {
        super(random);
    }

    @Override
    public boolean querPedirAumento(VisaoDoJogo visao) {
        if (!visao.podePedirAumento()) {
            return false;
        }
        if (super.querPedirAumento(visao)) {
            return true;
        }
        if (temAMaiorCartaRestante(visao) && visao.rodadasGanhas() >= 1) {
            return true;
        }
        return visao.aposta() == NivelAposta.UM && random.nextInt(100) < CHANCE_BLEFE;
    }

    @Override
    public RespostaPedido responderPedido(VisaoDoJogo visao, PedidoAumento pedido) {
        if (temAMaiorCartaRestante(visao) && visao.rodadasPerdidas() == 0) {
            return pedido.getValorPedido().ehMaximo() ? RespostaPedido.ACEITAR : RespostaPedido.AUMENTAR;
        }
        return super.responderPedido(visao, pedido);
    }

    /** Nenhuma carta mais forte que a minha melhor ainda pode aparecer na mesa. */
    private boolean temAMaiorCartaRestante(VisaoDoJogo visao) {
        if (visao.minhasCartas().isEmpty()) {
            return false;
        }
        int minhaMelhor = visao.minhasCartas().stream().mapToInt(visao::forca).max().orElse(0);
        List<Carta> vistas = new ArrayList<>(visao.cartasJaJogadas());
        vistas.addAll(visao.minhasCartas());
        vistas.add(visao.vira().getCarta());
        for (Carta c : todasAsCartas()) {
            if (!vistas.contains(c) && visao.forca(c) > minhaMelhor) {
                return false;
            }
        }
        return true;
    }

    private static List<Carta> todasAsCartas() {
        List<Carta> cartas = new ArrayList<>(Baralho.TOTAL_CARTAS);
        for (Naipe n : Naipe.values()) {
            for (Valor v : Valor.values()) {
                cartas.add(new Carta(v, n));
            }
        }
        return cartas;
    }
}
