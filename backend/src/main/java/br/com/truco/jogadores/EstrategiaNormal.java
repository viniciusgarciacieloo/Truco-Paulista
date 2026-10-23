package br.com.truco.jogadores;

import br.com.truco.cartas.Carta;
import br.com.truco.cartas.Valor;
import br.com.truco.cartas.Vira;
import br.com.truco.mao.Jogada;
import br.com.truco.mao.PedidoAumento;
import br.com.truco.mao.RespostaPedido;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Random;

/** "Pistoleiro": heurística baseada na força da mão e no que já está na mesa. */
public class EstrategiaNormal implements EstrategiaBot {
    protected final Random random;

    public EstrategiaNormal(Random random) {
        this.random = random;
    }

    @Override
    public Jogada escolherCarta(VisaoDoJogo visao) {
        List<Carta> cartas = visao.cartasPermitidas().stream()
                .sorted(Comparator.comparingInt(visao::forca))
                .toList();
        Carta maisFraca = cartas.get(0);
        Carta maisForte = cartas.get(cartas.size() - 1);
        Optional<Jogada> maiorNaMesa = visao.maiorNaMesa();

        if (maiorNaMesa.isEmpty()) {
            // Abrindo a rodada: na 1ª guarda a manilha se tiver outra carta boa para sair.
            if (visao.numeroRodada() == 1 && cartas.size() >= 2 && visao.vira().ehManilha(maisForte)) {
                return jogar(visao, cartas.get(cartas.size() - 2), false);
            }
            return jogar(visao, maisForte, false);
        }

        boolean souUltimo = visao.cartasNaMesa().size() == 3;
        if (visao.parceiroGanhandoAMesa()) {
            return jogar(visao, maisFraca, souUltimo && visao.podeCobrir());
        }

        int forcaDaMesa = visao.forca(maiorNaMesa.get());
        for (Carta c : cartas) {
            if (visao.forca(c) > forcaDaMesa) {
                return jogar(visao, c, false);
            }
        }
        for (Carta c : cartas) {
            if (visao.forca(c) == forcaDaMesa) {
                return jogar(visao, c, false);
            }
        }
        return jogar(visao, maisFraca, visao.podeCobrir());
    }

    @Override
    public boolean querPedirAumento(VisaoDoJogo visao) {
        if (!visao.podePedirAumento()) {
            return false;
        }
        if (ganhaComCerteza(visao)) {
            return true;
        }
        double limite = switch (visao.aposta()) {
            case UM -> 5.0;
            case TRUCO -> 6.0;
            case SEIS -> 7.0;
            case NOVE -> 8.0;
            case DOZE -> Double.MAX_VALUE;
        };
        return pontuacaoComContexto(visao) >= limite;
    }

    @Override
    public RespostaPedido responderPedido(VisaoDoJogo visao, PedidoAumento pedido) {
        double pontuacao = pontuacaoComContexto(visao);
        double limiteAceitar = 2.5 + pedido.getValorPedido().pontos() / 6.0;
        double limiteAumentar = limiteAceitar + 3.0;
        if (pontuacao >= limiteAumentar && !pedido.getValorPedido().ehMaximo()) {
            return RespostaPedido.AUMENTAR;
        }
        if (pontuacao >= limiteAceitar) {
            return RespostaPedido.ACEITAR;
        }
        // Se correr já entrega a partida, não há o que perder.
        if (visao.pontosAdversarios() + pedido.getValorPedido().pontosSeCorrer() >= 12) {
            return RespostaPedido.ACEITAR;
        }
        return RespostaPedido.CORRER;
    }

    @Override
    public boolean decidirMaoDeOnze(VisaoDoJogo visao) {
        double total = pontuacao(visao.minhasCartas(), visao.vira())
                + pontuacao(visao.cartasDoParceiro(), visao.vira());
        return total >= 5.0;
    }

    protected Jogada jogar(VisaoDoJogo visao, Carta carta, boolean coberta) {
        return new Jogada(visao.eu(), carta, coberta);
    }

    /** Pontuação da mão ajustada pelas rodadas já ganhas ou perdidas. */
    protected double pontuacaoComContexto(VisaoDoJogo visao) {
        return pontuacao(visao.minhasCartas(), visao.vira())
                + 1.5 * visao.rodadasGanhas()
                - 1.5 * visao.rodadasPerdidas();
    }

    /** Manilha ≈ 3,5..5; 3 = 2; 2 = 1,5; Ás = 1; demais = 0,3. Mão média ≈ 3. */
    protected double pontuacao(List<Carta> cartas, Vira vira) {
        double total = 0;
        for (Carta c : cartas) {
            if (vira.ehManilha(c)) {
                total += 3.0 + c.getNaipe().forcaManilha() * 0.5;
            } else if (c.getValor() == Valor.TRES) {
                total += 2.0;
            } else if (c.getValor() == Valor.DOIS) {
                total += 1.5;
            } else if (c.getValor() == Valor.AS) {
                total += 1.0;
            } else {
                total += 0.3;
            }
        }
        return total;
    }

    /** Última carta, último a jogar, a dupla já tem uma rodada e a carta vence a mesa. */
    protected boolean ganhaComCerteza(VisaoDoJogo visao) {
        if (visao.minhasCartas().size() != 1 || visao.cartasNaMesa().size() != 3 || visao.rodadasGanhas() < 1) {
            return false;
        }
        int minha = visao.forca(visao.minhasCartas().get(0));
        return visao.maiorNaMesa().map(j -> minha > visao.forca(j)).orElse(true);
    }
}
