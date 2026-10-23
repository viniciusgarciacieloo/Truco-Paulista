package br.com.truco.aplicacao;

import br.com.truco.cartas.Carta;
import br.com.truco.cartas.Vira;
import br.com.truco.jogo.Partida;
import br.com.truco.jogo.StatusPartida;
import br.com.truco.jogadores.Jogador;
import br.com.truco.mao.EstadoMao;
import br.com.truco.mao.Jogada;
import br.com.truco.mao.Mao;
import br.com.truco.mao.PedidoAumento;
import br.com.truco.mao.Rodada;
import br.com.truco.mao.TipoMao;
import br.com.truco.regras.Arbitro;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Tudo que a tela precisa desenhar, do ponto de vista do humano (UC17).
 * Nunca leva as cartas dos bots, só a quantidade.
 */
public final class EstadoMesa {
    private static final int EVENTOS_NA_TELA = 40;

    private final Map<String, Object> dados;

    private EstadoMesa(Map<String, Object> dados) {
        this.dados = Collections.unmodifiableMap(dados);
    }

    public Map<String, Object> getDados() {
        return dados;
    }

    public Object get(String chave) {
        return dados.get(chave);
    }

    static EstadoMesa montar(Partida partida, Arbitro arbitro, List<EventoJogo> eventos, boolean aguardandoSistema) {
        Map<String, Object> d = new LinkedHashMap<>();
        if (partida == null) {
            d.put("status", "SEM_PARTIDA");
            d.put("eventos", List.of());
            return new EstadoMesa(d);
        }

        Mao mao = partida.maoAtual();
        Jogador humano = partida.getHumano();
        Vira vira = mao.getVira();
        Rodada rodada = mao.rodadaAtual();
        boolean ferro = mao.getTipo() == TipoMao.MAO_DE_FERRO;
        boolean minhaVez = mao.getEstado() == EstadoMao.EM_RODADA && mao.getJogadorDaVez() == humano;
        List<Carta> permitidas = minhaVez ? arbitro.cartasPermitidas(humano, mao) : List.of();

        d.put("status", partida.getStatus().name());
        d.put("nivel", partida.getConfiguracao().nivelBot().name());
        d.put("nivelApelido", partida.getConfiguracao().nivelBot().getApelido());
        d.put("aguardandoSistema", aguardandoSistema);
        d.put("numeroMao", mao.getNumero());
        d.put("tipoMao", mao.getTipo().name());
        d.put("estadoMao", mao.getEstado().name());
        d.put("pe", mao.getPe().getPosicao().name());
        d.put("jogadorMao", mao.getJogadorMao().getPosicao().name());
        d.put("cortador", mao.cortador().getPosicao().name());
        d.put("vez", mao.getJogadorDaVez() == null ? null : mao.getJogadorDaVez().getPosicao().name());

        List<Object> jogadores = new ArrayList<>();
        for (Jogador j : partida.getJogadores()) {
            jogadores.add(mapa(
                    "posicao", j.getPosicao().name(),
                    "nome", j.getNome(),
                    "bot", j.ehBot(),
                    "dupla", partida.duplaDe(j).getNome(),
                    "qtdCartas", j.getCartas().size()));
        }
        d.put("jogadores", jogadores);

        d.put("placar", mapa("nos", partida.getNos().getPontos(), "eles", partida.getEles().getPontos()));
        d.put("aposta", mapa("pontos", mao.getAposta().pontos(), "nome", mao.getAposta().getNomePedido()));
        d.put("proximoPedido", mao.getAposta().ehMaximo() ? null : mao.getAposta().proximo().getNomePedido());
        d.put("vira", vira == null ? null : carta(vira.getCarta(), vira));
        d.put("manilha", vira == null ? null : vira.valorManilha().getSimbolo());

        List<Object> minhasCartas = new ArrayList<>();
        List<Carta> cartas = humano.getCartas();
        for (int i = 0; i < cartas.size(); i++) {
            Carta c = cartas.get(i);
            Map<String, Object> m = ferro ? mapa("oculta", true) : carta(c, vira);
            m.put("indice", i);
            m.put("permitida", minhaVez && permitidas.contains(c));
            minhasCartas.add(m);
        }
        d.put("minhasCartas", minhasCartas);

        boolean decidindoOnze = mao.getEstado() == EstadoMao.DECISAO_MAO_DE_ONZE;
        d.put("maoDeOnze", mao.getTipo() == TipoMao.MAO_DE_ONZE
                ? mapa("dupla", mao.duplaComOnze().getNome(), "minhaDupla", mao.duplaComOnze() == partida.getNos())
                : null);
        if (decidindoOnze && mao.duplaComOnze() == partida.getNos()) {
            List<Object> doParceiro = new ArrayList<>();
            for (Carta c : partida.getNos().parceiroDe(humano).getCartas()) {
                doParceiro.add(carta(c, vira));
            }
            d.put("cartasParceiro", doParceiro);
        }

        d.put("rodada", rodada == null ? null : mapa(
                "numero", rodada.getNumero(),
                "desempate", rodada.ehDesempate(),
                "comeca", rodada.getJogadorInicial().getPosicao().name()));
        d.put("mesa", rodada == null ? List.of() : jogadas(rodada, vira, partida));
        Rodada anterior = ultimaApurada(mao);
        d.put("ultimaRodada", anterior == null || anterior == rodada ? null : mapa(
                "numero", anterior.getNumero(),
                "jogadas", jogadas(anterior, vira, partida),
                "resultado", resultado(anterior, partida)));

        List<Object> resultados = new ArrayList<>();
        for (Rodada r : mao.getRodadas()) {
            if (r.estaApurada()) {
                resultados.add(resultado(r, partida));
            }
        }
        d.put("resultadosRodadas", resultados);

        PedidoAumento pedido = mao.getPedidoPendente();
        d.put("pedido", pedido == null ? null : mapa(
                "solicitante", pedido.getSolicitante().getPosicao().name(),
                "nomeSolicitante", pedido.getSolicitante().getNome(),
                "pedido", pedido.getValorPedido().getNomePedido(),
                "valor", pedido.getValorPedido().pontos(),
                "seCorrer", pedido.getValorPedido().pontosSeCorrer(),
                "paraMinhaDupla", pedido.getDuplaSolicitante() == partida.getEles(),
                "proximo", pedido.getValorPedido().ehMaximo() ? null : pedido.getValorPedido().proximo().getNomePedido()));

        d.put("acoes", acoes(partida, mao, arbitro, humano, minhaVez));
        d.put("motivoSemAumento", minhaVez ? arbitro.motivoBloqueioAumento(humano, mao, partida) : null);

        d.put("maoEncerrada", mao.estaEncerrada() ? mapa(
                "vencedora", mao.getVencedora() == null ? null : mao.getVencedora().getNome(),
                "pontos", mao.getPontosGanhos()) : null);
        d.put("vencedoraPartida", partida.getVencedora() == null ? null : partida.getVencedora().getNome());

        List<EventoJogo> recentes = eventos.subList(Math.max(0, eventos.size() - EVENTOS_NA_TELA), eventos.size());
        List<Object> listaEventos = new ArrayList<>();
        for (EventoJogo e : recentes) {
            listaEventos.add(mapa(
                    "id", e.id(),
                    "tipo", e.tipo(),
                    "posicao", e.posicao() == null ? null : e.posicao().name(),
                    "descricao", e.descricao()));
        }
        d.put("eventos", listaEventos);
        return new EstadoMesa(d);
    }

    private static List<String> acoes(Partida partida, Mao mao, Arbitro arbitro, Jogador humano, boolean minhaVez) {
        List<String> acoes = new ArrayList<>();
        if (partida.getStatus() != StatusPartida.EM_ANDAMENTO) {
            return acoes;
        }
        switch (mao.getEstado()) {
            case EMBARALHANDO -> {
                if (mao.getPe() == humano) {
                    acoes.add("EMBARALHAR");
                }
            }
            case CORTANDO -> {
                if (mao.cortador() == humano) {
                    acoes.add("CORTAR");
                }
            }
            case DISTRIBUINDO -> {
                if (mao.getPe() == humano) {
                    acoes.add("DISTRIBUIR");
                }
            }
            case DECISAO_MAO_DE_ONZE -> {
                if (mao.duplaComOnze() == partida.getNos()) {
                    acoes.add("DECIDIR_MAO_DE_ONZE");
                }
            }
            case AGUARDANDO_RESPOSTA -> {
                if (mao.getPedidoPendente().getDuplaSolicitante() == partida.getEles()) {
                    acoes.add("RESPONDER");
                }
            }
            case EM_RODADA -> {
                if (minhaVez) {
                    acoes.add("JOGAR");
                    if (arbitro.podeCobrir(mao)) {
                        acoes.add("COBRIR");
                    }
                    if (arbitro.podePedirAumento(humano, mao, partida)) {
                        acoes.add("PEDIR_AUMENTO");
                    }
                }
            }
            case ENCERRADA -> {
            }
        }
        return acoes;
    }

    private static List<Object> jogadas(Rodada rodada, Vira vira, Partida partida) {
        List<Object> lista = new ArrayList<>();
        for (Jogada j : rodada.getJogadas()) {
            boolean venceu = rodada.estaApurada() && j == rodada.getJogadaVencedora() && !rodada.isEmpatada();
            lista.add(mapa(
                    "posicao", j.jogador().getPosicao().name(),
                    "nome", j.jogador().getNome(),
                    "coberta", j.coberta(),
                    "carta", j.coberta() ? null : carta(j.carta(), vira),
                    "vencedora", venceu));
        }
        return lista;
    }

    /** "V" = Nós venceram, "D" = Eles venceram, "E" = empate. */
    private static String resultado(Rodada r, Partida partida) {
        if (r.isEmpatada()) {
            return "E";
        }
        return r.getVencedora() == partida.getNos() ? "V" : "D";
    }

    private static Rodada ultimaApurada(Mao mao) {
        List<Rodada> rodadas = mao.getRodadas();
        for (int i = rodadas.size() - 1; i >= 0; i--) {
            if (rodadas.get(i).estaApurada()) {
                return rodadas.get(i);
            }
        }
        return null;
    }

    private static Map<String, Object> carta(Carta c, Vira vira) {
        return mapa(
                "codigo", c.codigo(),
                "valor", c.getValor().getSimbolo(),
                "naipe", c.getNaipe().name(),
                "simbolo", c.getNaipe().getSimbolo(),
                "vermelha", c.getNaipe().ehVermelho(),
                "manilha", vira != null && vira.ehManilha(c));
    }

    private static Map<String, Object> mapa(Object... chavesEValores) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i < chavesEValores.length; i += 2) {
            m.put((String) chavesEValores[i], chavesEValores[i + 1]);
        }
        return m;
    }
}
