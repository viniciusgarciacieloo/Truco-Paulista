package br.com.truco.regras;

import br.com.truco.cartas.Carta;
import br.com.truco.cartas.Vira;
import br.com.truco.jogo.Dupla;
import br.com.truco.jogo.Partida;
import br.com.truco.jogadores.Jogador;
import br.com.truco.mao.EstadoMao;
import br.com.truco.mao.Jogada;
import br.com.truco.mao.Mao;
import br.com.truco.mao.Rodada;
import br.com.truco.mao.TipoMao;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Serviço que concentra as regras de negócio (RN) para poderem ser testadas isoladamente. */
public class Arbitro {

    /** Coberta = 0; comum = 1..10; manilha = 11..14 (RN02, RN04, RN16). */
    public int forca(Jogada jogada, Vira vira) {
        return jogada.coberta() ? 0 : vira.forca(jogada.carta());
    }

    /** RN10: vence a dupla da maior carta; maiores iguais em duplas diferentes = empate. */
    public ResultadoRodada apurarRodada(Rodada rodada, Vira vira, Partida partida) {
        List<Jogada> jogadas = rodada.getJogadas();
        int maior = jogadas.stream().mapToInt(j -> forca(j, vira)).max().orElse(0);
        if (maior == 0) {
            return new ResultadoRodada(null, true, null);
        }
        List<Jogada> maiores = jogadas.stream().filter(j -> forca(j, vira) == maior).toList();
        Set<Dupla> duplas = new LinkedHashSet<>();
        for (Jogada j : maiores) {
            duplas.add(partida.duplaDe(j.jogador()));
        }
        if (duplas.size() == 1) {
            return new ResultadoRodada(duplas.iterator().next(), false, maiores.get(0));
        }
        return new ResultadoRodada(null, true, maiores.get(0));
    }

    /** RN11 e RN12: a mão termina assim que o vencedor (ou o empate total) está definido. */
    public boolean maoDefinida(List<Rodada> rodadas) {
        return desfecho(rodadas).definida();
    }

    /** Dupla vencedora da mão, ou null se as três rodadas empataram (RN11-e). */
    public Dupla vencedoraDaMao(List<Rodada> rodadas) {
        return desfecho(rodadas).vencedora();
    }

    /** RN09 e RN21: começa quem venceu a rodada; depois de empate, começa o pé. */
    public Jogador quemComecaProximaRodada(Mao mao) {
        Rodada anterior = mao.rodadaAtual();
        if (anterior.isEmpatada()) {
            return mao.getPe();
        }
        return anterior.getJogadaVencedora().jogador();
    }

    /** RN21: na rodada de desempate só vale jogar a(s) carta(s) mais forte(s). */
    public List<Carta> cartasPermitidas(Jogador jogador, Mao mao) {
        List<Carta> cartas = jogador.getCartas();
        Rodada rodada = mao.rodadaAtual();
        if (rodada == null || !rodada.ehDesempate() || mao.getTipo() == TipoMao.MAO_DE_FERRO || cartas.isEmpty()) {
            return List.copyOf(cartas);
        }
        Vira vira = mao.getVira();
        int maior = cartas.stream().mapToInt(vira::forca).max().orElseThrow();
        return cartas.stream().filter(c -> vira.forca(c) == maior).toList();
    }

    /** RN16, RN18, RN21. */
    public boolean podeCobrir(Mao mao) {
        Rodada rodada = mao.rodadaAtual();
        return rodada != null
                && rodada.getNumero() >= 2
                && !rodada.ehDesempate()
                && mao.getTipo() != TipoMao.MAO_DE_FERRO;
    }

    public boolean podePedirAumento(Jogador jogador, Mao mao, Partida partida) {
        return motivoBloqueioAumento(jogador, mao, partida) == null;
    }

    /** Motivo pelo qual o jogador não pode pedir aumento agora, ou null se pode (RN14, RN17, RN18). */
    public String motivoBloqueioAumento(Jogador jogador, Mao mao, Partida partida) {
        if (mao.getTipo() == TipoMao.MAO_DE_ONZE) {
            return "Mão de onze: ninguém pode pedir truco";
        }
        if (mao.getTipo() == TipoMao.MAO_DE_FERRO) {
            return "Mão de ferro: ninguém pode pedir truco";
        }
        if (mao.getEstado() != EstadoMao.EM_RODADA) {
            return "Não é hora de pedir aumento";
        }
        if (mao.getJogadorDaVez() != jogador) {
            return "Só dá para pedir na sua vez";
        }
        if (mao.getAposta().ehMaximo()) {
            return "A mão já vale 12";
        }
        Dupla podeAumentar = mao.getDuplaQuePodeAumentar();
        if (podeAumentar != null && podeAumentar != partida.duplaDe(jogador)) {
            return "Sua dupla fez o último pedido: agora só os adversários podem aumentar";
        }
        return null;
    }

    /** Uma dupla com 11 → mão de onze; as duas → mão de ferro. */
    public TipoMao tipoDaMao(Partida partida) {
        boolean nos = partida.getNos().temOnze();
        boolean eles = partida.getEles().temOnze();
        if (nos && eles) {
            return TipoMao.MAO_DE_FERRO;
        }
        if (nos || eles) {
            return TipoMao.MAO_DE_ONZE;
        }
        return TipoMao.NORMAL;
    }

    private record Desfecho(boolean definida, Dupla vencedora) {
        static final Desfecho INDEFINIDA = new Desfecho(false, null);

        static Desfecho de(Dupla vencedora) {
            return new Desfecho(true, vencedora);
        }
    }

    private Desfecho desfecho(List<Rodada> todas) {
        List<Rodada> rodadas = todas.stream().filter(Rodada::estaApurada).toList();

        Map<Dupla, Integer> vitorias = new HashMap<>();
        for (Rodada r : rodadas) {
            if (!r.isEmpatada() && vitorias.merge(r.getVencedora(), 1, Integer::sum) == 2) {
                return Desfecho.de(r.getVencedora());
            }
        }
        if (rodadas.size() < 2) {
            return Desfecho.INDEFINIDA;
        }

        Rodada primeira = rodadas.get(0);
        Rodada segunda = rodadas.get(1);
        if (rodadas.size() == 2) {
            if (primeira.isEmpatada() && !segunda.isEmpatada()) {
                return Desfecho.de(segunda.getVencedora());        // RN11-a
            }
            if (!primeira.isEmpatada() && segunda.isEmpatada()) {
                return Desfecho.de(primeira.getVencedora());       // RN11-b
            }
            return Desfecho.INDEFINIDA;                             // 1x1 ou empate duplo: vai para a 3ª
        }

        Rodada terceira = rodadas.get(2);
        if (!terceira.isEmpatada()) {
            return Desfecho.de(terceira.getVencedora());           // RN11-c
        }
        if (!primeira.isEmpatada()) {
            return Desfecho.de(primeira.getVencedora());           // RN11-d
        }
        return Desfecho.de(null);                                  // RN11-e: ninguém pontua
    }
}
