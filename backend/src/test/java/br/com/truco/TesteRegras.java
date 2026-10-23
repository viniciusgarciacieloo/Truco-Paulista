package br.com.truco;

import br.com.truco.aplicacao.ControladorPartida;
import br.com.truco.aplicacao.EstadoMesa;
import br.com.truco.cartas.Carta;
import br.com.truco.cartas.Naipe;
import br.com.truco.cartas.Valor;
import br.com.truco.cartas.Vira;
import br.com.truco.jogo.Configuracao;
import br.com.truco.jogo.Dupla;
import br.com.truco.jogo.Partida;
import br.com.truco.jogo.StatusPartida;
import br.com.truco.jogadores.Jogador;
import br.com.truco.jogadores.JogadorBot;
import br.com.truco.jogadores.JogadorHumano;
import br.com.truco.jogadores.NivelBot;
import br.com.truco.jogadores.Posicao;
import br.com.truco.mao.EstadoMao;
import br.com.truco.mao.Jogada;
import br.com.truco.mao.Mao;
import br.com.truco.mao.RespostaPedido;
import br.com.truco.mao.Rodada;
import br.com.truco.mao.TipoMao;
import br.com.truco.regras.Arbitro;
import br.com.truco.regras.JogadaInvalidaException;
import br.com.truco.regras.ResultadoRodada;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Testes das regras de negócio (cenários T1–T13 de docs/casos-de-uso.md) sem dependências externas.
 * Rodar com: testar.bat
 */
public final class TesteRegras {
    private static int sucessos;
    private static final List<String> falhas = new ArrayList<>();

    private static final Arbitro ARBITRO = new Arbitro();

    public static void main(String[] args) {
        teste("T1  vira 7 → manilhas Q; Q♣ > Q♥ > Q♠ > Q♦ > 3", TesteRegras::t1);
        teste("T2  vira 3 → manilha 4 (sequência circular)", TesteRegras::t2);
        teste("T3  ganha a 1ª e empata a 2ª → mão definida sem 3ª", TesteRegras::t3);
        teste("T4  empata a 1ª e perde a 2ª → mão deles", TesteRegras::t4);
        teste("T5  três empates → ninguém pontua", TesteRegras::t5);
        teste("T6  truco aceito, seis pedido e corrido → quem pediu seis ganha 3", TesteRegras::t6);
        teste("T7  mão de onze e a dupla corre → adversária +1", TesteRegras::t7);
        teste("T8  11 × 11 → mão de ferro, sem truco e cartas ocultas", TesteRegras::t8);
        teste("T9  cobrir carta na 1ª rodada é proibido", TesteRegras::t9);
        teste("T10 parceiros empatam com a maior → rodada da dupla", TesteRegras::t10);
        teste("T11 empate na 1ª → 2ª começa pelo pé e segue anti-horário", TesteRegras::t11);
        teste("T12 rodada de desempate → só a maior carta é permitida", TesteRegras::t12);
        teste("T13 empate na 1ª e na 2ª → 3ª também é de desempate", TesteRegras::t13);
        teste("Extra: RN11-d (1x1 e 3ª empatada → quem venceu a 1ª)", TesteRegras::rn11d);
        teste("Extra: RN14 a dupla que pediu não pode pedir de novo em seguida", TesteRegras::rn14);
        teste("Extra: partidas completas entre bots terminam com 12+ pontos", TesteRegras::partidasCompletas);

        System.out.println();
        System.out.println(sucessos + " teste(s) passaram, " + falhas.size() + " falharam.");
        if (!falhas.isEmpty()) {
            falhas.forEach(f -> System.out.println("  ✗ " + f));
            System.exit(1);
        }
    }

    // ------------------------------------------------------------------ cenários

    private static void t1() {
        Vira vira = new Vira(c("7C"));
        verificar(vira.valorManilha() == Valor.DAMA, "manilha deveria ser Q");
        verificar(vira.forca(c("QP")) > vira.forca(c("QC")), "zap > copas");
        verificar(vira.forca(c("QC")) > vira.forca(c("QE")), "copas > espadas");
        verificar(vira.forca(c("QE")) > vira.forca(c("QO")), "espadas > ouros");
        verificar(vira.forca(c("QO")) > vira.forca(c("3P")), "pica-fumo > 3");
        verificar(vira.forca(c("3P")) == vira.forca(c("3O")), "naipe não desempata carta comum");
    }

    private static void t2() {
        verificar(new Vira(c("3O")).valorManilha() == Valor.QUATRO, "vira 3 → manilha 4");
    }

    private static void t3() {
        Mesa m = new Mesa();
        Vira vira = new Vira(c("4O"));                  // manilha = 5
        Rodada r1 = m.rodada(1, vira, "3P", "4C", "6C", "7C");   // Sul (Nós) vence
        Rodada r2 = m.rodada(2, vira, "KP", "KC", "QC", "JC");   // Sul e Leste empatam com K
        verificar(r1.getVencedora() == m.partida.getNos(), "1ª é de Nós");
        verificar(r2.isEmpatada(), "2ª deveria empatar");
        verificar(ARBITRO.maoDefinida(List.of(r1, r2)), "mão deveria estar definida");
        verificar(ARBITRO.vencedoraDaMao(List.of(r1, r2)) == m.partida.getNos(), "mão de Nós");
    }

    private static void t4() {
        Mesa m = new Mesa();
        Vira vira = new Vira(c("4O"));
        Rodada r1 = m.rodada(1, vira, "3P", "3C", "6C", "7C");   // empate entre duplas
        Rodada r2 = m.rodada(2, vira, "6P", "AC", "QC", "JC");   // Leste (Eles) vence
        verificar(r1.isEmpatada(), "1ª empatada");
        verificar(ARBITRO.vencedoraDaMao(List.of(r1, r2)) == m.partida.getEles(), "mão de Eles");
    }

    private static void t5() {
        Mesa m = new Mesa();
        Vira vira = new Vira(c("4O"));
        Rodada r1 = m.rodada(1, vira, "3P", "3C", "6C", "7C");
        Rodada r2 = m.rodada(2, vira, "2P", "2C", "6E", "7E");
        Rodada r3 = m.rodada(3, vira, "AP", "AC", "6O", "7O");
        List<Rodada> rodadas = List.of(r1, r2, r3);
        verificar(ARBITRO.maoDefinida(rodadas), "definida");
        verificar(ARBITRO.vencedoraDaMao(rodadas) == null, "ninguém deveria pontuar");
    }

    private static void t6() {
        Partida partida = novaPartida(42);
        Mao mao = prepararMao(partida);
        Jogador vez = mao.getJogadorDaVez();
        Dupla duplaDaVez = partida.duplaDe(vez);
        Jogador adversario = partida.jogadorEm(vez.getPosicao().direita());

        mao.pedirAumento(vez);                                   // Truco!
        mao.responderPedido(adversario, RespostaPedido.AUMENTAR);// Seis!
        mao.responderPedido(vez, RespostaPedido.CORRER);         // corre do seis
        verificar(mao.estaEncerrada(), "mão encerrada");
        verificar(mao.getVencedora() == partida.adversariaDe(duplaDaVez), "quem pediu seis vence");
        verificar(mao.getPontosGanhos() == 3, "correr do seis dá 3, deu " + mao.getPontosGanhos());
    }

    private static void t7() {
        Partida partida = novaPartida(7);
        partida.getNos().adicionarPontos(11);
        partida.getEles().adicionarPontos(5);
        Mao mao = partida.novaMao();
        verificar(mao.getTipo() == TipoMao.MAO_DE_ONZE, "deveria ser mão de onze");
        mao.embaralhar();
        mao.cortar(15);
        mao.distribuir();
        verificar(mao.getEstado() == EstadoMao.DECISAO_MAO_DE_ONZE, "aguardando decisão");
        mao.decidirMaoDeOnze(false);
        verificar(mao.getVencedora() == partida.getEles() && mao.getPontosGanhos() == 1, "Eles +1");
    }

    private static void t8() {
        ControladorPartida controlador = new ControladorPartida(new Random(3));
        controlador.novaPartida(new Configuracao("Teste", NivelBot.NORMAL));
        Partida partida = controlador.getPartida();
        partida.getNos().adicionarPontos(11);
        partida.getEles().adicionarPontos(11);
        Mao mao = partida.novaMao();
        verificar(mao.getTipo() == TipoMao.MAO_DE_FERRO, "deveria ser mão de ferro");
        mao.embaralhar();
        mao.cortar(10);
        mao.distribuir();
        verificar(!ARBITRO.podePedirAumento(mao.getJogadorDaVez(), mao, partida), "sem truco na mão de ferro");
        EstadoMesa estado = controlador.obterEstado();
        String json = estado.getDados().get("minhasCartas").toString();
        verificar(json.contains("oculta=true") && !json.contains("codigo"), "cartas do humano devem estar ocultas");
    }

    private static void t9() {
        Partida partida = novaPartida(11);
        Mao mao = prepararMao(partida);
        Jogador vez = mao.getJogadorDaVez();
        verificar(!ARBITRO.podeCobrir(mao), "não pode cobrir na 1ª");
        lancaErro(() -> mao.jogarCarta(vez, vez.getCartas().get(0), true), "cobrir na 1ª rodada");
    }

    private static void t10() {
        Mesa m = new Mesa();
        Vira vira = new Vira(c("4O"));
        Rodada r = m.rodada(1, vira, "3P", "6C", "3C", "7C");   // Sul e Norte (parceiros) com 3
        verificar(!r.isEmpatada(), "não é empate");
        verificar(r.getVencedora() == m.partida.getNos(), "rodada de Nós");
    }

    private static void t11() {
        Partida partida = novaPartida(5);
        Mao mao = prepararMao(partida);
        forcarEmpateNaRodada(partida, mao);
        Rodada segunda = mao.rodadaAtual();
        verificar(segunda.getNumero() == 2 && segunda.ehDesempate(), "2ª deveria ser de desempate");
        verificar(mao.getJogadorDaVez() == mao.getPe(), "quem começa deveria ser o pé");
        Jogador primeiro = mao.getJogadorDaVez();
        jogarMaior(mao, primeiro);
        verificar(mao.getJogadorDaVez().getPosicao() == primeiro.getPosicao().direita(), "segue anti-horário");
    }

    private static void t12() {
        Partida partida = novaPartida(9);
        Mao mao = prepararMao(partida);
        forcarEmpateNaRodada(partida, mao);
        Jogador vez = mao.getJogadorDaVez();
        Carta manilha = new Carta(mao.getVira().valorManilha(), Naipe.PAUS);
        Carta comum = new Carta(cartaBaixa(mao.getVira()), Naipe.OUROS);
        vez.descartarTodas();
        vez.receberCartas(List.of(comum, manilha));
        verificar(ARBITRO.cartasPermitidas(vez, mao).equals(List.of(manilha)), "só a manilha deveria ser permitida");
        verificar(!ARBITRO.podeCobrir(mao), "não pode cobrir no desempate");
        lancaErro(() -> mao.jogarCarta(vez, comum, false), "jogar carta menor no desempate");
        mao.jogarCarta(vez, manilha, false);
    }

    private static void t13() {
        Partida partida = novaPartida(13);
        Mao mao = prepararMao(partida);
        forcarEmpateNaRodada(partida, mao);
        forcarEmpateNaRodada(partida, mao);
        Rodada terceira = mao.rodadaAtual();
        verificar(terceira.getNumero() == 3 && terceira.ehDesempate(), "3ª deveria ser de desempate");
        verificar(mao.getJogadorDaVez() == mao.getPe(), "3ª começa pelo pé");
    }

    private static void rn11d() {
        Mesa m = new Mesa();
        Vira vira = new Vira(c("4O"));
        Rodada r1 = m.rodada(1, vira, "3P", "6C", "6E", "7C");   // Nós
        Rodada r2 = m.rodada(2, vira, "6P", "3C", "7E", "7O");   // Eles
        Rodada r3 = m.rodada(3, vira, "AP", "AC", "6O", "4C");   // empate
        verificar(ARBITRO.vencedoraDaMao(List.of(r1, r2, r3)) == m.partida.getNos(), "quem venceu a 1ª leva");
    }

    private static void rn14() {
        Partida partida = novaPartida(21);
        Mao mao = prepararMao(partida);
        Jogador vez = mao.getJogadorDaVez();
        Jogador adversario = partida.jogadorEm(vez.getPosicao().direita());
        mao.pedirAumento(vez);
        mao.responderPedido(adversario, RespostaPedido.ACEITAR);
        verificar(!ARBITRO.podePedirAumento(vez, mao, partida), "quem pediu não pode pedir seis em seguida");
        jogarMaior(mao, vez);
        verificar(ARBITRO.podePedirAumento(mao.getJogadorDaVez(), mao, partida), "adversário pode pedir seis");
    }

    private static void partidasCompletas() {
        for (NivelBot nivel : NivelBot.values()) {
            for (int semente = 0; semente < 30; semente++) {
                ControladorPartida controlador = new ControladorPartida(new Random(semente));
                controlador.novaPartida(new Configuracao("Teste", nivel));
                Partida partida = controlador.getPartida();
                // o "humano" é trocado por decisões automáticas simples
                int limite = 5000;
                while (partida.getStatus() == StatusPartida.EM_ANDAMENTO && limite-- > 0) {
                    controlador.executarTurnoDosBots();
                    if (partida.getStatus() != StatusPartida.EM_ANDAMENTO) {
                        break;
                    }
                    jogarPeloHumano(controlador, partida);
                }
                verificar(partida.getStatus() == StatusPartida.ENCERRADA, nivel + "/" + semente + " não terminou");
                int maior = Math.max(partida.getNos().getPontos(), partida.getEles().getPontos());
                verificar(maior >= 12, nivel + "/" + semente + " terminou sem 12 pontos");
            }
        }
    }

    // ------------------------------------------------------------------ apoio

    private static void jogarPeloHumano(ControladorPartida controlador, Partida partida) {
        Mao mao = partida.maoAtual();
        Jogador humano = partida.getHumano();
        switch (mao.getEstado()) {
            case EMBARALHANDO -> controlador.embaralhar();
            case CORTANDO -> controlador.cortar(20);
            case DISTRIBUINDO -> controlador.distribuir();
            case DECISAO_MAO_DE_ONZE -> controlador.decidirMaoDeOnze(true);
            case AGUARDANDO_RESPOSTA -> controlador.responderPedido(RespostaPedido.ACEITAR);
            case EM_RODADA -> {
                Carta carta = ARBITRO.cartasPermitidas(humano, mao).get(0);
                controlador.jogarCarta(humano.getCartas().indexOf(carta), false);
            }
            default -> throw new IllegalStateException("estado inesperado: " + mao.getEstado());
        }
    }

    /** Partida com bots em todas as posições, menos o SUL humano; sem iniciar a mão. */
    private static Partida novaPartida(long semente) {
        Random random = new Random(semente);
        List<Jogador> jogadores = List.of(
                new JogadorHumano("Sul", Posicao.SUL),
                new JogadorBot("Leste", Posicao.LESTE, NivelBot.NORMAL, random),
                new JogadorBot("Norte", Posicao.NORTE, NivelBot.NORMAL, random),
                new JogadorBot("Oeste", Posicao.OESTE, NivelBot.NORMAL, random));
        Partida partida = new Partida(new Configuracao("Sul", NivelBot.NORMAL), jogadores, ARBITRO, random);
        partida.iniciar();
        return partida;
    }

    private static Mao prepararMao(Partida partida) {
        Mao mao = partida.maoAtual();
        mao.embaralhar();
        mao.cortar(20);
        mao.distribuir();
        return mao;
    }

    /**
     * Troca as cartas dos jogadores para que os dois primeiros a jogar (duplas diferentes)
     * empatem com a maior carta, e joga a rodada. As demais cartas viram cartas baixas,
     * para a jogada valer também numa rodada de desempate.
     */
    private static void forcarEmpateNaRodada(Partida partida, Mao mao) {
        Vira vira = mao.getVira();
        Valor manilha = vira.valorManilha();
        Valor alta = manilha == Valor.TRES ? Valor.DOIS : Valor.TRES;
        Valor baixa = cartaBaixa(vira);
        for (int k = 0; k < 4; k++) {
            Jogador j = mao.getJogadorDaVez();
            Naipe naipe = Naipe.values()[k];
            Carta escolhida = new Carta(k < 2 ? alta : baixa, naipe);
            List<Carta> novas = new ArrayList<>();
            novas.add(escolhida);
            for (int n = 1; n < j.getCartas().size(); n++) {
                novas.add(new Carta(baixa, naipe));
            }
            j.descartarTodas();
            j.receberCartas(novas);
            mao.jogarCarta(j, escolhida, false);
        }
    }

    private static Valor cartaBaixa(Vira vira) {
        return vira.valorManilha() == Valor.QUATRO ? Valor.CINCO : Valor.QUATRO;
    }

    private static void jogarMaior(Mao mao, Jogador jogador) {
        mao.jogarCarta(jogador, jogador.maiorCarta(mao.getVira()), false);
    }

    private static Carta c(String codigo) {
        return Carta.deCodigo(codigo);
    }

    /** Mesa fixa para montar rodadas à mão: SUL e NORTE = Nós; LESTE e OESTE = Eles. */
    private static final class Mesa {
        final Partida partida = novaPartida(1);

        /** Cartas na ordem SUL, LESTE, NORTE, OESTE. */
        Rodada rodada(int numero, Vira vira, String sul, String leste, String norte, String oeste) {
            Rodada r = new Rodada(numero, partida.jogadorEm(Posicao.SUL), false);
            String[] cartas = {sul, leste, norte, oeste};
            for (Posicao p : Posicao.values()) {
                r.adicionar(new Jogada(partida.jogadorEm(p), c(cartas[p.ordinal()]), false));
            }
            ResultadoRodada resultado = ARBITRO.apurarRodada(r, vira, partida);
            r.registrarResultado(resultado);
            return r;
        }
    }

    private static void teste(String nome, Runnable corpo) {
        try {
            corpo.run();
            sucessos++;
            System.out.println("  ✓ " + nome);
        } catch (AssertionError | RuntimeException e) {
            falhas.add(nome + " → " + e.getMessage());
            System.out.println("  ✗ " + nome + " → " + e.getMessage());
        }
    }

    private static void verificar(boolean condicao, String mensagem) {
        if (!condicao) {
            throw new AssertionError(mensagem);
        }
    }

    private static void lancaErro(Runnable acao, String descricao) {
        try {
            acao.run();
        } catch (JogadaInvalidaException e) {
            return;
        }
        throw new AssertionError("deveria recusar: " + descricao);
    }
}
