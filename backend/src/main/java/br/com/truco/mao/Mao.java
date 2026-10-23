package br.com.truco.mao;

import br.com.truco.cartas.Baralho;
import br.com.truco.cartas.Carta;
import br.com.truco.cartas.Vira;
import br.com.truco.jogo.Dupla;
import br.com.truco.jogo.Partida;
import br.com.truco.jogadores.Jogador;
import br.com.truco.jogadores.Posicao;
import br.com.truco.regras.Arbitro;
import br.com.truco.regras.JogadaInvalidaException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Uma mão de truco (melhor de 3 rodadas). Toda alteração passa por aqui e respeita
 * as transições do diagrama de estados (docs/diagrama-de-estados.md).
 */
public class Mao {
    private final int numero;
    private final TipoMao tipo;
    private final Partida partida;
    private final Arbitro arbitro;
    private final Random random;
    private final Jogador pe;
    private final Jogador jogadorMao;
    private final Baralho baralho = new Baralho();
    private final List<Rodada> rodadas = new ArrayList<>(3);
    private final List<Carta> cartasJaJogadas = new ArrayList<>();

    private EstadoMao estado = EstadoMao.EMBARALHANDO;
    private Jogador jogadorDaVez;
    private Vira vira;
    private NivelAposta aposta = NivelAposta.UM;
    /** Dupla que pode pedir o próximo aumento; null = qualquer uma (RN14). */
    private Dupla duplaQuePodeAumentar;
    private PedidoAumento pedidoPendente;
    private Dupla vencedora;
    private int pontosGanhos;
    private boolean pontosSomados;

    /** T01: Partida.novaMao(). */
    public Mao(int numero, TipoMao tipo, Partida partida, Jogador pe, Arbitro arbitro, Random random) {
        this.numero = numero;
        this.tipo = tipo;
        this.partida = partida;
        this.pe = pe;
        this.arbitro = arbitro;
        this.random = random;
        this.jogadorMao = partida.jogadorEm(pe.getPosicao().direita());
        for (Jogador j : partida.getJogadores()) {
            j.descartarTodas();
        }
    }

    /** Adversário à esquerda do pé (RN06). */
    public Jogador cortador() {
        return partida.jogadorEm(pe.getPosicao().esquerda());
    }

    /** T02. */
    public void embaralhar() {
        exigirEstado(EstadoMao.EMBARALHANDO);
        baralho.embaralhar(random);
        estado = EstadoMao.CORTANDO;
    }

    /** T03. Um ponto fora de 1..39 é ajustado para o valor válido mais próximo (UC04 FE1). */
    public void cortar(int ponto) {
        exigirEstado(EstadoMao.CORTANDO);
        int ajustado = Math.max(1, Math.min(baralho.tamanho() - 1, ponto));
        baralho.cortar(ajustado);
        estado = EstadoMao.DISTRIBUINDO;
    }

    /** T04/T05: 3 cartas a cada um, começando pelo jogador mão, e vira a vira (RN07). */
    public void distribuir() {
        exigirEstado(EstadoMao.DISTRIBUINDO);
        Posicao posicao = jogadorMao.getPosicao();
        for (int i = 0; i < 4; i++) {
            partida.jogadorEm(posicao).receberCartas(List.of(baralho.comprar(), baralho.comprar(), baralho.comprar()));
            posicao = posicao.direita();
        }
        vira = new Vira(baralho.comprar());
        if (tipo == TipoMao.MAO_DE_ONZE) {
            estado = EstadoMao.DECISAO_MAO_DE_ONZE;
        } else {
            iniciarRodadas();
        }
    }

    /** T06/T07 (RN17). */
    public void decidirMaoDeOnze(boolean jogar) {
        exigirEstado(EstadoMao.DECISAO_MAO_DE_ONZE);
        if (jogar) {
            aposta = NivelAposta.TRUCO;
            iniciarRodadas();
        } else {
            encerrar(partida.adversariaDe(duplaComOnze()), 1);
        }
    }

    /** T08/T09 e, na 4ª carta, a apuração da rodada (T10, T11, T12). */
    public void jogarCarta(Jogador jogador, Carta carta, boolean coberta) {
        if (estado == EstadoMao.AGUARDANDO_RESPOSTA) {
            throw new JogadaInvalidaException("Responda o pedido de "
                    + pedidoPendente.getValorPedido().getNomePedido() + " antes de jogar");
        }
        exigirEstado(EstadoMao.EM_RODADA);
        if (jogador != jogadorDaVez) {
            throw new JogadaInvalidaException("Aguarde sua vez");
        }
        if (!jogador.getCartas().contains(carta)) {
            throw new JogadaInvalidaException("Essa carta não está na sua mão");
        }
        if (!arbitro.cartasPermitidas(jogador, this).contains(carta)) {
            throw new JogadaInvalidaException("Empachou! Você deve mostrar sua maior carta");
        }
        Rodada rodada = rodadaAtual();
        if (coberta && !arbitro.podeCobrir(this)) {
            throw new JogadaInvalidaException(rodada.ehDesempate()
                    ? "Empachou: é obrigatório mostrar a maior carta"
                    : rodada.getNumero() == 1
                    ? "Não é permitido cobrir carta na 1ª rodada"
                    : "Não é permitido cobrir carta nesta mão");
        }

        jogador.removerCarta(carta);
        rodada.adicionar(new Jogada(jogador, carta, coberta));
        if (!coberta) {
            cartasJaJogadas.add(carta);
        }
        if (!rodada.estaCompleta()) {
            jogadorDaVez = partida.jogadorEm(jogador.getPosicao().direita());
            return;
        }

        rodada.registrarResultado(arbitro.apurarRodada(rodada, vira, partida));
        if (arbitro.maoDefinida(rodadas)) {
            encerrar(arbitro.vencedoraDaMao(rodadas), aposta.pontos());
        } else {
            novaRodada(arbitro.quemComecaProximaRodada(this), rodada.isEmpatada());
        }
    }

    /** T13 (UC10). */
    public PedidoAumento pedirAumento(Jogador jogador) {
        String motivo = arbitro.motivoBloqueioAumento(jogador, this, partida);
        if (motivo != null) {
            throw new JogadaInvalidaException(motivo);
        }
        pedidoPendente = new PedidoAumento(jogador, partida.duplaDe(jogador), aposta.proximo());
        estado = EstadoMao.AGUARDANDO_RESPOSTA;
        return pedidoPendente;
    }

    /**
     * T14/T15/T16 (UC11). Ao aumentar, o pedido anterior conta como aceito, então quem
     * correr do novo pedido entrega o valor anterior (RN15).
     *
     * @return o novo pedido pendente (após AUMENTAR) ou null
     */
    public PedidoAumento responderPedido(Jogador jogador, RespostaPedido resposta) {
        exigirEstado(EstadoMao.AGUARDANDO_RESPOSTA);
        Dupla duplaQueResponde = partida.duplaDe(jogador);
        PedidoAumento pedido = pedidoPendente;
        if (duplaQueResponde == pedido.getDuplaSolicitante()) {
            throw new JogadaInvalidaException("Quem responde o pedido é a dupla adversária");
        }
        switch (resposta) {
            case ACEITAR -> {
                pedido.responder(resposta);
                aposta = pedido.getValorPedido();
                duplaQuePodeAumentar = duplaQueResponde;
                pedidoPendente = null;
                estado = EstadoMao.EM_RODADA;
            }
            case CORRER -> {
                pedido.responder(resposta);
                encerrar(pedido.getDuplaSolicitante(), pedido.getValorPedido().pontosSeCorrer());
            }
            case AUMENTAR -> {
                if (pedido.getValorPedido().ehMaximo()) {
                    throw new JogadaInvalidaException("A mão já vale 12: não dá para aumentar");
                }
                pedido.responder(resposta);
                aposta = pedido.getValorPedido();
                duplaQuePodeAumentar = duplaQueResponde;
                pedidoPendente = new PedidoAumento(jogador, duplaQueResponde, aposta.proximo());
            }
        }
        return pedidoPendente;
    }

    /** Dupla com 11 pontos na mão de onze; null nos outros tipos de mão. */
    public Dupla duplaComOnze() {
        if (tipo != TipoMao.MAO_DE_ONZE) {
            return null;
        }
        return partida.getNos().temOnze() ? partida.getNos() : partida.getEles();
    }

    public void registrarPontosSomados() {
        if (estado != EstadoMao.ENCERRADA) {
            throw new IllegalStateException("A mão ainda não terminou");
        }
        pontosSomados = true;
    }

    private void iniciarRodadas() {
        estado = EstadoMao.EM_RODADA;
        novaRodada(jogadorMao, false);
    }

    private void novaRodada(Jogador inicial, boolean desempate) {
        rodadas.add(new Rodada(rodadas.size() + 1, inicial, desempate));
        jogadorDaVez = inicial;
    }

    private void encerrar(Dupla vencedora, int pontos) {
        this.vencedora = vencedora;
        this.pontosGanhos = vencedora == null ? 0 : pontos;
        this.pedidoPendente = null;
        this.jogadorDaVez = null;
        this.estado = EstadoMao.ENCERRADA;
    }

    private void exigirEstado(EstadoMao esperado) {
        if (estado != esperado) {
            throw new JogadaInvalidaException("Ação não permitida agora");
        }
    }

    public Rodada rodadaAtual() {
        return rodadas.isEmpty() ? null : rodadas.get(rodadas.size() - 1);
    }

    public boolean estaEncerrada() {
        return estado == EstadoMao.ENCERRADA;
    }

    public int getNumero() {
        return numero;
    }

    public TipoMao getTipo() {
        return tipo;
    }

    public EstadoMao getEstado() {
        return estado;
    }

    public Jogador getPe() {
        return pe;
    }

    public Jogador getJogadorMao() {
        return jogadorMao;
    }

    public Jogador getJogadorDaVez() {
        return jogadorDaVez;
    }

    public Vira getVira() {
        return vira;
    }

    public NivelAposta getAposta() {
        return aposta;
    }

    public Dupla getDuplaQuePodeAumentar() {
        return duplaQuePodeAumentar;
    }

    public PedidoAumento getPedidoPendente() {
        return pedidoPendente;
    }

    public List<Rodada> getRodadas() {
        return Collections.unmodifiableList(rodadas);
    }

    public List<Carta> getCartasJaJogadas() {
        return Collections.unmodifiableList(cartasJaJogadas);
    }

    public Dupla getVencedora() {
        return vencedora;
    }

    public int getPontosGanhos() {
        return pontosGanhos;
    }

    public boolean isPontosSomados() {
        return pontosSomados;
    }
}
