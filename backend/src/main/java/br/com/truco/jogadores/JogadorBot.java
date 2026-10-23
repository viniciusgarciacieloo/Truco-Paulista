package br.com.truco.jogadores;

import br.com.truco.cartas.Baralho;
import br.com.truco.mao.Jogada;
import br.com.truco.mao.PedidoAumento;
import br.com.truco.mao.RespostaPedido;

import java.util.Random;

/** Jogador automático: repassa cada decisão para a sua {@link EstrategiaBot} (UC16). */
public class JogadorBot extends Jogador {
    private final NivelBot nivel;
    private final EstrategiaBot estrategia;
    private final Random random;

    public JogadorBot(String nome, Posicao posicao, NivelBot nivel, Random random) {
        super(nome, posicao);
        this.nivel = nivel;
        this.random = random;
        this.estrategia = nivel.criarEstrategia(random);
    }

    @Override
    public boolean ehBot() {
        return true;
    }

    public int escolherPontoDeCorte() {
        return 1 + random.nextInt(Baralho.TOTAL_CARTAS - 1);
    }

    public Jogada escolherCarta(VisaoDoJogo visao) {
        return estrategia.escolherCarta(visao);
    }

    public boolean querPedirAumento(VisaoDoJogo visao) {
        return estrategia.querPedirAumento(visao);
    }

    public RespostaPedido responderPedido(VisaoDoJogo visao, PedidoAumento pedido) {
        return estrategia.responderPedido(visao, pedido);
    }

    public boolean decidirMaoDeOnze(VisaoDoJogo visao) {
        return estrategia.decidirMaoDeOnze(visao);
    }

    public NivelBot getNivel() {
        return nivel;
    }
}
