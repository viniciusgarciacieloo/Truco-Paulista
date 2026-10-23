package br.com.truco.jogadores;

import br.com.truco.cartas.Carta;
import br.com.truco.cartas.Vira;
import br.com.truco.mao.Jogada;
import br.com.truco.mao.NivelAposta;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Fotografia do jogo com apenas o que o bot pode ver.
 * As cartas do parceiro só vêm preenchidas na mão de onze da dupla do bot (RN17).
 *
 * @param resultadosRodadas uma entrada por rodada apurada: 1 = dupla do bot venceu,
 *                          -1 = adversários venceram, 0 = empate
 */
public record VisaoDoJogo(
        Jogador eu,
        List<Carta> minhasCartas,
        List<Carta> cartasDoParceiro,
        Vira vira,
        List<Jogada> cartasNaMesa,
        List<Integer> resultadosRodadas,
        int numeroRodada,
        boolean rodadaDesempate,
        NivelAposta aposta,
        int pontosMinhaDupla,
        int pontosAdversarios,
        List<Carta> cartasPermitidas,
        boolean podeCobrir,
        boolean podePedirAumento,
        List<Carta> cartasJaJogadas) {

    public int forca(Carta carta) {
        return vira.forca(carta);
    }

    public int forca(Jogada jogada) {
        return jogada.coberta() ? 0 : vira.forca(jogada.carta());
    }

    public Optional<Jogada> maiorNaMesa() {
        return cartasNaMesa.stream().max(Comparator.comparingInt(this::forca));
    }

    public boolean parceiroGanhandoAMesa() {
        return maiorNaMesa()
                .map(j -> j.jogador() != eu && j.jogador().getPosicao().mesmaDupla(eu.getPosicao()))
                .orElse(false);
    }

    public long rodadasGanhas() {
        return resultadosRodadas.stream().filter(r -> r == 1).count();
    }

    public long rodadasPerdidas() {
        return resultadosRodadas.stream().filter(r -> r == -1).count();
    }
}
