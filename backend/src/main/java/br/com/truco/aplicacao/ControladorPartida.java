package br.com.truco.aplicacao;

import br.com.truco.cartas.Carta;
import br.com.truco.jogo.Configuracao;
import br.com.truco.jogo.Dupla;
import br.com.truco.jogo.Partida;
import br.com.truco.jogo.StatusPartida;
import br.com.truco.jogadores.Jogador;
import br.com.truco.jogadores.JogadorBot;
import br.com.truco.jogadores.JogadorHumano;
import br.com.truco.jogadores.Posicao;
import br.com.truco.jogadores.VisaoDoJogo;
import br.com.truco.mao.EstadoMao;
import br.com.truco.mao.Jogada;
import br.com.truco.mao.Mao;
import br.com.truco.mao.PedidoAumento;
import br.com.truco.mao.RespostaPedido;
import br.com.truco.mao.Rodada;
import br.com.truco.mao.TipoMao;
import br.com.truco.regras.Arbitro;
import br.com.truco.regras.JogadaInvalidaException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Único ponto de entrada da interface. Cada método público é uma ação do jogador;
 * humano e bots passam pelos mesmos métodos da {@link Mao} (UC16, passo 5).
 */
public class ControladorPartida {
    private static final int MAX_EVENTOS = 80;

    private final Arbitro arbitro = new Arbitro();
    private final List<EventoJogo> eventos = new ArrayList<>();
    private final Random random;
    private Partida partida;
    private Configuracao configuracao;
    private long proximoIdEvento = 1;

    public ControladorPartida() {
        this(new Random());
    }

    public ControladorPartida(Random random) {
        this.random = random;
    }

    // ---------------------------------------------------------------- ações do humano

    /** UC01. */
    public synchronized EstadoMesa novaPartida(Configuracao cfg) {
        if (partida != null && partida.getStatus() == StatusPartida.EM_ANDAMENTO) {
            partida.abandonar();
        }
        configuracao = cfg;
        eventos.clear();
        List<Jogador> jogadores = List.of(
                new JogadorHumano(cfg.nomeJogador(), Posicao.SUL),
                new JogadorBot("Bill Calango", Posicao.LESTE, cfg.nivelBot(), random),
                new JogadorBot("Zé Pistola", Posicao.NORTE, cfg.nivelBot(), random),
                new JogadorBot("Juca Bigode", Posicao.OESTE, cfg.nivelBot(), random));
        partida = new Partida(cfg, jogadores, arbitro, random);
        partida.iniciar();
        evento(EventoJogo.PARTIDA, null, "Nova partida no saloon! " + partida.getPe().getNome()
                + " foi sorteado para dar as cartas.");
        anunciarInicioDaMao(partida.maoAtual());
        return obterEstado();
    }

    /** UC19 (FA2). */
    public synchronized EstadoMesa reiniciar() {
        if (configuracao == null) {
            throw new JogadaInvalidaException("Nenhuma partida para reiniciar");
        }
        return novaPartida(configuracao);
    }

    /** UC03 (passo 2), quando o humano é o pé. */
    public synchronized EstadoMesa embaralhar() {
        Mao mao = maoEmAndamento();
        exigirHumano(mao.getPe(), "Quem embaralha é o pé");
        mao.embaralhar();
        evento(EventoJogo.INFO, Posicao.SUL, "Você embaralhou as cartas.");
        return obterEstado();
    }

    /** UC04, quando o humano é o cortador. */
    public synchronized EstadoMesa cortar(int ponto) {
        Mao mao = maoEmAndamento();
        exigirHumano(mao.cortador(), "Quem corta é o adversário à esquerda do pé");
        mao.cortar(ponto);
        evento(EventoJogo.INFO, Posicao.SUL, "Você cortou o baralho.");
        return obterEstado();
    }

    /** UC03 (passo 5), quando o humano é o pé. */
    public synchronized EstadoMesa distribuir() {
        Mao mao = maoEmAndamento();
        exigirHumano(mao.getPe(), "Quem distribui é o pé");
        mao.distribuir();
        anunciarVira(mao);
        return obterEstado();
    }

    /** UC07 / UC08. Na mão de ferro o humano escolhe pela posição, sem ver a carta. */
    public synchronized EstadoMesa jogarCarta(int indice, boolean coberta) {
        Mao mao = maoEmAndamento();
        Jogador humano = partida.getHumano();
        List<Carta> cartas = humano.getCartas();
        if (indice < 0 || indice >= cartas.size()) {
            throw new JogadaInvalidaException("Carta inválida");
        }
        Carta carta = cartas.get(indice);
        Rodada rodada = mao.rodadaAtual();
        mao.jogarCarta(humano, carta, coberta);
        registrarJogada(mao, rodada, humano, carta, coberta);
        return obterEstado();
    }

    /** UC10. */
    public synchronized EstadoMesa pedirAumento() {
        Mao mao = maoEmAndamento();
        PedidoAumento pedido = mao.pedirAumento(partida.getHumano());
        registrarPedido(pedido);
        return obterEstado();
    }

    /** UC11: o humano responde pela sua dupla (RN20). */
    public synchronized EstadoMesa responderPedido(RespostaPedido resposta) {
        Mao mao = maoEmAndamento();
        PedidoAumento pedido = mao.getPedidoPendente();
        if (pedido == null) {
            throw new JogadaInvalidaException("Não há pedido para responder");
        }
        if (pedido.getDuplaSolicitante() == partida.getNos()) {
            throw new JogadaInvalidaException("Aguarde a resposta dos adversários");
        }
        mao.responderPedido(partida.getHumano(), resposta);
        registrarResposta(mao, partida.getHumano(), pedido, resposta);
        return obterEstado();
    }

    /** UC12, quando a dupla com 11 é a do humano. */
    public synchronized EstadoMesa decidirMaoDeOnze(boolean jogar) {
        Mao mao = maoEmAndamento();
        if (mao.getEstado() != EstadoMao.DECISAO_MAO_DE_ONZE || mao.duplaComOnze() != partida.getNos()) {
            throw new JogadaInvalidaException("Não há mão de onze para a sua dupla decidir");
        }
        mao.decidirMaoDeOnze(jogar);
        registrarDecisaoMaoDeOnze(mao, partida.getHumano(), jogar);
        return obterEstado();
    }

    /** UC19. */
    public synchronized EstadoMesa abandonar() {
        if (partida == null || partida.getStatus() != StatusPartida.EM_ANDAMENTO) {
            throw new JogadaInvalidaException("Não há partida em andamento");
        }
        partida.abandonar();
        evento(EventoJogo.PARTIDA, Posicao.SUL, "Você levantou da mesa. A partida foi para Eles.");
        return obterEstado();
    }

    // ---------------------------------------------------------------- turno dos bots (UC16)

    /**
     * Executa uma única ação que cabe ao sistema (um bot ou o fim da mão).
     * A tela chama este método em laço, com pausa entre as chamadas, para animar os bots.
     *
     * @return false quando a próxima ação é do humano ou a partida terminou
     */
    public synchronized boolean executarPassoDoBot() {
        if (!proximaAcaoEhDoSistema()) {
            return false;
        }
        Mao mao = partida.maoAtual();
        switch (mao.getEstado()) {
            case EMBARALHANDO -> {
                mao.embaralhar();
                evento(EventoJogo.INFO, mao.getPe().getPosicao(), mao.getPe().getNome() + " embaralhou as cartas.");
            }
            case CORTANDO -> {
                JogadorBot cortador = (JogadorBot) mao.cortador();
                mao.cortar(cortador.escolherPontoDeCorte());
                evento(EventoJogo.INFO, cortador.getPosicao(), cortador.getNome() + " cortou o baralho.");
            }
            case DISTRIBUINDO -> {
                mao.distribuir();
                anunciarVira(mao);
            }
            case DECISAO_MAO_DE_ONZE -> {
                JogadorBot bot = (JogadorBot) mao.duplaComOnze().getJogadores().get(0);
                boolean jogar = bot.decidirMaoDeOnze(montarVisao(bot));
                mao.decidirMaoDeOnze(jogar);
                registrarDecisaoMaoDeOnze(mao, bot, jogar);
            }
            case AGUARDANDO_RESPOSTA -> {
                PedidoAumento pedido = mao.getPedidoPendente();
                JogadorBot bot = (JogadorBot) partida.jogadorEm(pedido.getSolicitante().getPosicao().direita());
                RespostaPedido resposta = bot.responderPedido(montarVisao(bot), pedido);
                if (resposta == RespostaPedido.AUMENTAR && pedido.getValorPedido().ehMaximo()) {
                    resposta = RespostaPedido.ACEITAR;
                }
                mao.responderPedido(bot, resposta);
                registrarResposta(mao, bot, pedido, resposta);
            }
            case EM_RODADA -> jogarVezDoBot(mao, (JogadorBot) mao.getJogadorDaVez());
            case ENCERRADA -> finalizarMao();
        }
        return true;
    }

    /** Roda os bots até a próxima ação ser do humano (versão sem animação, usada nos testes). */
    public synchronized void executarTurnoDosBots() {
        int limite = 1000;
        while (executarPassoDoBot()) {
            if (--limite == 0) {
                throw new IllegalStateException("Os bots entraram em laço infinito");
            }
        }
    }

    private void jogarVezDoBot(Mao mao, JogadorBot bot) {
        VisaoDoJogo visao = montarVisao(bot);
        if (visao.podePedirAumento() && bot.querPedirAumento(visao)) {
            registrarPedido(mao.pedirAumento(bot));
            return;
        }
        Rodada rodada = mao.rodadaAtual();
        Jogada escolha = bot.escolherCarta(visao);
        try {
            mao.jogarCarta(bot, escolha.carta(), escolha.coberta());
            registrarJogada(mao, rodada, bot, escolha.carta(), escolha.coberta());
        } catch (JogadaInvalidaException e) {
            // UC16 FE1: decisão inválida → primeira carta permitida.
            Carta carta = arbitro.cartasPermitidas(bot, mao).get(0);
            mao.jogarCarta(bot, carta, false);
            registrarJogada(mao, rodada, bot, carta, false);
        }
    }

    /** UC14 e UC15: soma os pontos, verifica o fim da partida e prepara a próxima mão. */
    private void finalizarMao() {
        Mao mao = partida.maoAtual();
        if (mao.isPontosSomados()) {
            return;
        }
        if (mao.getVencedora() != null) {
            mao.getVencedora().adicionarPontos(mao.getPontosGanhos());
        }
        mao.registrarPontosSomados();

        if (partida.verificarFim()) {
            Dupla vencedora = partida.getNos().getPontos() >= Partida.PONTOS_PARA_VENCER ? partida.getNos() : partida.getEles();
            partida.encerrar(vencedora);
            evento(EventoJogo.PARTIDA, null, vencedora == partida.getNos()
                    ? "Fim de jogo: Nós vencemos a partida por " + placar() + "!"
                    : "Fim de jogo: Eles venceram a partida por " + placar() + ".");
            return;
        }
        partida.passarPe();
        anunciarInicioDaMao(partida.novaMao());
    }

    private boolean proximaAcaoEhDoSistema() {
        if (partida == null || partida.getStatus() != StatusPartida.EM_ANDAMENTO) {
            return false;
        }
        Mao mao = partida.maoAtual();
        return switch (mao.getEstado()) {
            case EMBARALHANDO, DISTRIBUINDO -> mao.getPe().ehBot();
            case CORTANDO -> mao.cortador().ehBot();
            case DECISAO_MAO_DE_ONZE -> mao.duplaComOnze() == partida.getEles();
            case AGUARDANDO_RESPOSTA -> mao.getPedidoPendente().getDuplaSolicitante() == partida.getNos();
            case EM_RODADA -> mao.getJogadorDaVez().ehBot();
            case ENCERRADA -> true;
        };
    }

    /** Só o que o bot pode ver (RN17: cartas do parceiro apenas na mão de onze da sua dupla). */
    private VisaoDoJogo montarVisao(JogadorBot bot) {
        Mao mao = partida.maoAtual();
        Dupla minhaDupla = partida.duplaDe(bot);
        Jogador parceiro = minhaDupla.parceiroDe(bot);
        boolean veParceiro = mao.getTipo() == TipoMao.MAO_DE_ONZE && mao.duplaComOnze() == minhaDupla;

        List<Integer> resultados = new ArrayList<>();
        for (Rodada r : mao.getRodadas()) {
            if (r.estaApurada()) {
                resultados.add(r.isEmpatada() ? 0 : r.getVencedora() == minhaDupla ? 1 : -1);
            }
        }
        Rodada rodada = mao.rodadaAtual();
        return new VisaoDoJogo(
                bot,
                List.copyOf(bot.getCartas()),
                veParceiro ? List.copyOf(parceiro.getCartas()) : List.of(),
                mao.getVira(),
                rodada == null ? List.of() : List.copyOf(rodada.getJogadas()),
                resultados,
                rodada == null ? 0 : rodada.getNumero(),
                rodada != null && rodada.ehDesempate(),
                mao.getAposta(),
                minhaDupla.getPontos(),
                partida.adversariaDe(minhaDupla).getPontos(),
                rodada == null ? List.of() : arbitro.cartasPermitidas(bot, mao),
                rodada != null && arbitro.podeCobrir(mao),
                arbitro.podePedirAumento(bot, mao, partida),
                List.copyOf(mao.getCartasJaJogadas()));
    }

    public synchronized EstadoMesa obterEstado() {
        return EstadoMesa.montar(partida, arbitro, eventos, proximaAcaoEhDoSistema());
    }

    // ---------------------------------------------------------------- histórico (EventoJogo)

    private void registrarJogada(Mao mao, Rodada rodada, Jogador jogador, Carta carta, boolean coberta) {
        evento(EventoJogo.INFO, jogador.getPosicao(), jogador.getNome()
                + (coberta ? " jogou uma carta coberta." : " jogou " + carta + "."));
        if (rodada.estaApurada()) {
            anunciarRodada(mao, rodada);
        }
        if (mao.estaEncerrada()) {
            anunciarFimDaMao(mao);
        }
    }

    private void registrarPedido(PedidoAumento pedido) {
        Jogador quem = pedido.getSolicitante();
        evento(EventoJogo.FALA, quem.getPosicao(), quem.getNome() + ": "
                + pedido.getValorPedido().getNomePedido().toUpperCase() + "!");
    }

    private void registrarResposta(Mao mao, Jogador quem, PedidoAumento pedido, RespostaPedido resposta) {
        String fala = switch (resposta) {
            case ACEITAR -> "Cai dentro! (aceitou, a mão vale " + pedido.getValorPedido().pontos() + ")";
            case CORRER -> "Tô fora! (correu)";
            case AUMENTAR -> pedido.getValorPedido().proximo().getNomePedido().toUpperCase() + "!";
        };
        evento(EventoJogo.FALA, quem.getPosicao(), quem.getNome() + ": " + fala);
        if (mao.estaEncerrada()) {
            anunciarFimDaMao(mao);
        }
    }

    private void registrarDecisaoMaoDeOnze(Mao mao, Jogador quem, boolean jogar) {
        evento(EventoJogo.MAO, quem.getPosicao(), jogar
                ? "Mão de onze: " + quem.getNome() + " decidiu jogar, valendo 3!"
                : "Mão de onze: " + quem.getNome() + " decidiu correr.");
        if (mao.estaEncerrada()) {
            anunciarFimDaMao(mao);
        }
    }

    private void anunciarInicioDaMao(Mao mao) {
        String texto = "Mão " + mao.getNumero() + ": " + mao.getPe().getNome() + " é o pé e "
                + mao.cortador().getNome() + " corta.";
        evento(EventoJogo.MAO, null, texto);
        if (mao.getTipo() == TipoMao.MAO_DE_ONZE) {
            Dupla comOnze = partida.getNos().temOnze() ? partida.getNos() : partida.getEles();
            evento(EventoJogo.MAO, null, "Mão de onze para " + comOnze.getNome()
                    + ": a dupla vê as cartas do parceiro e decide se joga. Sem truco nesta mão.");
        } else if (mao.getTipo() == TipoMao.MAO_DE_FERRO) {
            evento(EventoJogo.MAO, null, "Mão de ferro! Cartas no escuro e sem truco. Quem vencer leva a partida.");
        }
    }

    private void anunciarVira(Mao mao) {
        evento(EventoJogo.INFO, mao.getPe().getPosicao(), mao.getPe().getNome() + " distribuiu. A vira é "
                + mao.getVira().getCarta() + ": a manilha é " + mao.getVira().valorManilha().getSimbolo() + ".");
    }

    private void anunciarRodada(Mao mao, Rodada rodada) {
        String texto;
        if (rodada.isEmpatada()) {
            texto = rodada.getNumero() + "ª rodada: empachou!";
            if (!mao.estaEncerrada()) {
                texto += " Na próxima, " + mao.getPe().getNome() + " (o pé) começa e todos mostram a maior carta.";
            }
        } else {
            Jogada j = rodada.getJogadaVencedora();
            texto = rodada.getNumero() + "ª rodada para " + rodada.getVencedora().getNome() + ", com "
                    + j.carta() + " de " + j.jogador().getNome() + ".";
        }
        evento(EventoJogo.RODADA, null, texto);
    }

    private void anunciarFimDaMao(Mao mao) {
        if (mao.getVencedora() == null) {
            evento(EventoJogo.MAO, null, "Mão empatada: ninguém pontua.");
        } else {
            int pts = mao.getPontosGanhos();
            evento(EventoJogo.MAO, null, "Mão para " + mao.getVencedora().getNome() + ": +" + pts
                    + (pts == 1 ? " ponto." : " pontos."));
        }
    }

    private String placar() {
        return partida.getNos().getPontos() + " a " + partida.getEles().getPontos();
    }

    private void evento(String tipo, Posicao posicao, String descricao) {
        eventos.add(new EventoJogo(proximoIdEvento++, tipo, posicao, descricao, Instant.now()));
        if (eventos.size() > MAX_EVENTOS) {
            eventos.remove(0);
        }
    }

    // ---------------------------------------------------------------- utilitários

    private Mao maoEmAndamento() {
        if (partida == null || partida.getStatus() != StatusPartida.EM_ANDAMENTO) {
            throw new JogadaInvalidaException("Não há partida em andamento");
        }
        return partida.maoAtual();
    }

    private void exigirHumano(Jogador esperado, String mensagem) {
        if (esperado != partida.getHumano()) {
            throw new JogadaInvalidaException(mensagem);
        }
    }

    public synchronized Partida getPartida() {
        return partida;
    }

    public synchronized List<EventoJogo> getEventos() {
        return Collections.unmodifiableList(new ArrayList<>(eventos));
    }
}
