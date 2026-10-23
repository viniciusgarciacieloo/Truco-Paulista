package br.com.truco.jogo;

import br.com.truco.jogadores.Jogador;
import br.com.truco.jogadores.Posicao;
import br.com.truco.mao.Mao;
import br.com.truco.regras.Arbitro;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/** Raiz do jogo: jogadores, duplas, pé atual e o histórico de mãos (UC02). */
public class Partida {
    public static final int PONTOS_PARA_VENCER = 12;

    private final Configuracao configuracao;
    private final Jogador[] jogadores = new Jogador[4];
    private final Dupla nos;
    private final Dupla eles;
    private final List<Mao> maos = new ArrayList<>();
    private final Arbitro arbitro;
    private final Random random;
    private Jogador pe;
    private StatusPartida status = StatusPartida.EM_ANDAMENTO;
    private Dupla vencedora;

    /** @param jogadores os 4 jogadores, um em cada {@link Posicao} */
    public Partida(Configuracao configuracao, List<Jogador> jogadores, Arbitro arbitro, Random random) {
        this.configuracao = configuracao;
        this.arbitro = arbitro;
        this.random = random;
        for (Jogador j : jogadores) {
            this.jogadores[j.getPosicao().ordinal()] = j;
        }
        this.nos = new Dupla("Nós", jogadorEm(Posicao.SUL), jogadorEm(Posicao.NORTE));
        this.eles = new Dupla("Eles", jogadorEm(Posicao.LESTE), jogadorEm(Posicao.OESTE));
    }

    public void iniciar() {
        sortearPe();
        novaMao();
    }

    /** RN08: na 1ª mão o pé é sorteado. */
    public void sortearPe() {
        pe = jogadores[random.nextInt(jogadores.length)];
    }

    public Mao novaMao() {
        Mao mao = new Mao(maos.size() + 1, arbitro.tipoDaMao(this), this, pe, arbitro, random);
        maos.add(mao);
        return mao;
    }

    public Mao maoAtual() {
        return maos.isEmpty() ? null : maos.get(maos.size() - 1);
    }

    /** RN08: o pé passa para o jogador da direita. */
    public void passarPe() {
        pe = jogadorEm(pe.getPosicao().direita());
    }

    /** RN19. */
    public boolean verificarFim() {
        return nos.getPontos() >= PONTOS_PARA_VENCER || eles.getPontos() >= PONTOS_PARA_VENCER;
    }

    public void encerrar(Dupla vencedora) {
        this.vencedora = vencedora;
        this.status = StatusPartida.ENCERRADA;
    }

    /** UC19: abandonar conta como derrota da dupla do jogador. */
    public void abandonar() {
        this.vencedora = eles;
        this.status = StatusPartida.ABANDONADA;
    }

    public Jogador jogadorEm(Posicao posicao) {
        return jogadores[posicao.ordinal()];
    }

    public Dupla duplaDe(Jogador jogador) {
        return nos.contem(jogador) ? nos : eles;
    }

    public Dupla adversariaDe(Dupla dupla) {
        return dupla == nos ? eles : nos;
    }

    public List<Jogador> getJogadores() {
        return List.of(jogadores);
    }

    public Jogador getHumano() {
        return jogadorEm(Posicao.SUL);
    }

    public Dupla getNos() {
        return nos;
    }

    public Dupla getEles() {
        return eles;
    }

    public Jogador getPe() {
        return pe;
    }

    public List<Mao> getMaos() {
        return Collections.unmodifiableList(maos);
    }

    public StatusPartida getStatus() {
        return status;
    }

    public Dupla getVencedora() {
        return vencedora;
    }

    public Configuracao getConfiguracao() {
        return configuracao;
    }
}
