package br.com.truco.jogadores;

import br.com.truco.mao.Jogada;
import br.com.truco.mao.PedidoAumento;
import br.com.truco.mao.RespostaPedido;

/** Padrão Strategy: cada nível de dificuldade decide de um jeito (UC16, passo 3). */
public interface EstrategiaBot {

    Jogada escolherCarta(VisaoDoJogo visao);

    boolean querPedirAumento(VisaoDoJogo visao);

    RespostaPedido responderPedido(VisaoDoJogo visao, PedidoAumento pedido);

    boolean decidirMaoDeOnze(VisaoDoJogo visao);
}
