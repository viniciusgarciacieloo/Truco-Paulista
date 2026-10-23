/* Tela do jogo: desenha o EstadoMesa e envia as ações do jogador (UC01–UC19). */
(() => {
  const $ = (id) => document.getElementById(id);
  const ORDEM = ["SUL", "LESTE", "NORTE", "OESTE"];
  const direita = (p) => ORDEM[(ORDEM.indexOf(p) + 1) % 4];
  const esperar = (ms) => new Promise((r) => setTimeout(r, ms));

  let estado = null;
  let ocupado = false;
  let ultimoEventoVisto = 0;
  let ultimoEventoNoDiario = -1;
  let chaveMinhaMao = "";
  let placarAnterior = null;
  let acaoConfirmada = null;

  // ------------------------------------------------------------ início

  async function iniciar() {
    ligarEventos();
    const r = await Api.estado();
    if (r.erro || !r.estado || r.estado.status === "SEM_PARTIDA") {
      if (r.erro) aviso(r.erro);
      mostrarInicio();
      return;
    }
    ultimoEventoVisto = maiorId(r.estado.eventos);
    desenhar(r.estado);
    await rodarSistema();
  }

  function mostrarInicio() {
    fecharModais();
    $("modal-inicio").hidden = false;
    try {
      const salvo = localStorage.getItem("truco-nome");
      if (salvo && !$("campo-nome").value) $("campo-nome").value = salvo;
    } catch (e) { /* armazenamento indisponível */ }
    $("campo-nome").focus();
  }

  // ------------------------------------------------------------ ações e laço dos bots

  async function acao(chamada) {
    if (ocupado) return;
    ocupado = true;
    try {
      const r = await chamada();
      if (r.erro) aviso(r.erro);
      if (r.estado) desenhar(r.estado);
      await rodarSistema();
    } finally {
      ocupado = false;
    }
  }

  /** Executa um passo do sistema por vez, com pausa, para os bots "pensarem" (UC16). */
  async function rodarSistema() {
    while (estado && estado.status === "EM_ANDAMENTO" && estado.aguardandoSistema) {
      await esperar(pausaPara(estado));
      const r = await Api.passo();
      if (r.erro) aviso(r.erro);
      if (!r.estado) break;
      desenhar(r.estado);
    }
  }

  function pausaPara(e) {
    if (e.estadoMao === "ENCERRADA") return 2600;
    if (e.mesa.length === 0 && e.ultimaRodada) return 1700;
    if (["EMBARALHANDO", "CORTANDO", "DISTRIBUINDO"].includes(e.estadoMao)) return 650;
    if (e.estadoMao === "AGUARDANDO_RESPOSTA") return 1300;
    return 950;
  }

  function jogar(indice) {
    if (!estado || !estado.acoes.includes("JOGAR")) return;
    const coberta = $("opcao-cobrir").checked && estado.acoes.includes("COBRIR");
    $("opcao-cobrir").checked = false;
    acao(() => Api.jogar(indice, coberta));
  }

  // ------------------------------------------------------------ desenho

  function desenhar(e) {
    estado = e;
    if (e.status === "SEM_PARTIDA") {
      $("area-jogo").hidden = true;
      mostrarInicio();
      return;
    }
    $("area-jogo").hidden = false;
    desenharPlacar(e);
    desenharInfo(e);
    desenharAssentos(e);
    desenharCentro(e);
    desenharMinhaMao(e);
    desenharAcoes(e);
    desenharDiario(e);
    desenharModais(e);
    mostrarFalas(e);
  }

  function desenharPlacar(e) {
    for (const [lado, id] of [["nos", "pontos-nos"], ["eles", "pontos-eles"]]) {
      const el = $(id);
      const valor = e.placar[lado];
      el.textContent = valor;
      if (placarAnterior && valor > placarAnterior[lado]) {
        el.classList.remove("subiu");
        void el.offsetWidth;
        el.classList.add("subiu");
      }
    }
    placarAnterior = { ...e.placar };
  }

  function desenharInfo(e) {
    $("numero-mao").textContent = e.numeroMao;
    const valor = $("valor-mao");
    valor.textContent = e.aposta.pontos + (e.aposta.nome ? ` (${e.aposta.nome})` : "");
    valor.classList.toggle("vale-mais", e.aposta.pontos > 1);
    $("nivel-bots").textContent = e.nivelApelido;

    const marcas = $("marcas-rodadas");
    marcas.innerHTML = "";
    const simbolo = { V: "✔", D: "✖", E: "═" };
    const titulo = { V: "Rodada nossa", D: "Rodada deles", E: "Empachou" };
    for (let i = 0; i < 3; i++) {
      const r = e.resultadosRodadas[i];
      const m = document.createElement("span");
      m.className = "marca " + (r ? "marca-" + r : "marca-vazia");
      m.textContent = r ? simbolo[r] : "";
      m.title = r ? titulo[r] : `${i + 1}ª rodada`;
      marcas.appendChild(m);
    }

    const selo = $("selo-tipo");
    selo.hidden = e.tipoMao === "NORMAL";
    selo.textContent = e.tipoMao === "MAO_DE_ONZE" ? "Mão de Onze" : "Mão de Ferro";
  }

  function quemAge(e) {
    switch (e.estadoMao) {
      case "EMBARALHANDO":
      case "DISTRIBUINDO": return e.pe;
      case "CORTANDO": return e.cortador;
      case "EM_RODADA": return e.vez;
      case "AGUARDANDO_RESPOSTA": return e.pedido.paraMinhaDupla ? "SUL" : direita(e.pedido.solicitante);
      case "DECISAO_MAO_DE_ONZE": return e.maoDeOnze && e.maoDeOnze.minhaDupla ? "SUL" : "LESTE";
      default: return null;
    }
  }

  function desenharAssentos(e) {
    const ativo = e.status === "EM_ANDAMENTO" ? quemAge(e) : null;
    for (const j of e.jogadores) {
      const el = document.querySelector(`.assento[data-posicao="${j.posicao}"]`);
      if (!el.dataset.montado) {
        el.innerHTML = `
          <div class="retrato" style="position:relative">
            <div class="avatar">${Desenho.avatar(j.posicao, j.dupla)}</div>
            <div class="selos"></div>
          </div>
          <div class="info-assento">
            <div class="placa-nome ${j.dupla === "Nós" ? "dupla-nos" : "dupla-eles"}"></div>
            ${j.bot ? '<div class="mini-cartas"></div>' : ""}
          </div>`;
        el.dataset.montado = "1";
      }
      el.querySelector(".placa-nome").textContent = j.bot ? j.nome : `${j.nome} (você)`;
      el.classList.toggle("vez", j.posicao === ativo);

      const selos = el.querySelector(".selos");
      selos.innerHTML = (e.pe === j.posicao ? '<span class="selo-pe" title="Pé: embaralha e distribui">PÉ</span>' : "")
        + (e.jogadorMao === j.posicao ? '<span class="selo-mao" title="Mão: joga primeiro">MÃO</span>' : "");

      const mini = el.querySelector(".mini-cartas");
      if (mini) {
        mini.innerHTML = '<span class="mini"></span>'.repeat(j.qtdCartas);
      }
    }
  }

  function desenharCentro(e) {
    const jogadas = e.mesa.length ? e.mesa : (e.ultimaRodada ? e.ultimaRodada.jogadas : []);
    for (const slot of document.querySelectorAll(".slot")) {
      const pos = slot.dataset.slot;
      const j = jogadas.find((x) => x.posicao === pos);
      const chave = j
        ? [e.numeroMao, e.mesa.length ? e.rodada && e.rodada.numero : "u" + e.ultimaRodada.numero, j.coberta ? "c" : j.carta.codigo, j.vencedora].join("|")
        : "";
      if (slot.dataset.chave === chave) continue;
      slot.dataset.chave = chave;
      slot.innerHTML = "";
      if (j) {
        const el = Desenho.carta(j.coberta ? null : j.carta, ["na-mesa"]);
        if (j.vencedora) el.classList.add("vencedora");
        el.title = j.nome + (j.coberta ? " (coberta)" : "");
        slot.appendChild(el);
      }
    }

    const vira = $("vira-carta");
    const chaveVira = e.vira ? e.numeroMao + e.vira.codigo : "";
    if (vira.dataset.chave !== chaveVira) {
      vira.dataset.chave = chaveVira;
      vira.innerHTML = "";
      if (e.vira) vira.appendChild(Desenho.carta(e.vira, ["na-mesa"]));
    }
    $("vira-legenda").textContent = e.manilha ? `Manilha: ${e.manilha}` : "";

    desenharFaixa(e);
    desenharPreparo(e);
  }

  function desenharFaixa(e) {
    const faixa = $("faixa");
    let texto = "";
    let tipo = "";
    if (e.maoEncerrada) {
      const m = e.maoEncerrada;
      if (!m.vencedora) { texto = "Mão empatada! Ninguém pontua."; tipo = "neutra"; }
      else if (m.vencedora === "Nós") { texto = `Mão nossa! +${m.pontos} ${m.pontos === 1 ? "ponto" : "pontos"}`; tipo = "boa"; }
      else { texto = `Mão deles: +${m.pontos} ${m.pontos === 1 ? "ponto" : "pontos"}`; }
    } else if (e.mesa.length === 0 && e.ultimaRodada && e.ultimaRodada.resultado === "E") {
      texto = "Empachou! Mostrem a maior";
      tipo = "neutra";
    }
    if (faixa.textContent !== texto || faixa.hidden !== !texto) {
      faixa.hidden = !texto;
      faixa.textContent = texto;
      faixa.className = "faixa " + tipo;
    }
  }

  function desenharPreparo(e) {
    const preparo = $("preparo");
    const nome = (pos) => e.jogadores.find((j) => j.posicao === pos).nome;
    let html = "";
    if (e.status === "EM_ANDAMENTO") {
      switch (e.estadoMao) {
        case "EMBARALHANDO":
          html = e.acoes.includes("EMBARALHAR")
            ? 'Você é o pé. <button class="btn btn-madeira" data-preparo="embaralhar">Embaralhar</button>'
            : `${nome(e.pe)} está embaralhando…`;
          break;
        case "CORTANDO":
          html = e.acoes.includes("CORTAR") ? "Sua vez de cortar." : `${nome(e.cortador)} está cortando…`;
          break;
        case "DISTRIBUINDO":
          html = e.acoes.includes("DISTRIBUIR")
            ? 'Baralho cortado. <button class="btn btn-madeira" data-preparo="distribuir">Distribuir</button>'
            : `${nome(e.pe)} está distribuindo…`;
          break;
        case "DECISAO_MAO_DE_ONZE":
          if (!e.acoes.includes("DECIDIR_MAO_DE_ONZE")) html = "Eles estão decidindo a mão de onze…";
          break;
        default:
      }
    }
    if (preparo.dataset.html !== html) {
      preparo.dataset.html = html;
      preparo.innerHTML = html;
      preparo.hidden = !html;
    }
  }

  function desenharMinhaMao(e) {
    const mao = $("minha-mao");
    const minhaVez = e.acoes.includes("JOGAR");
    const chave = JSON.stringify([e.numeroMao, e.minhasCartas, minhaVez]);
    if (chave === chaveMinhaMao) return;
    chaveMinhaMao = chave;
    mao.innerHTML = "";
    for (const c of e.minhasCartas) {
      const el = Desenho.carta(c.oculta ? null : c);
      if (c.permitida) {
        el.classList.add("jogavel");
        el.tabIndex = 0;
        el.title = `Jogar (tecla ${c.indice + 1})`;
        el.addEventListener("click", () => jogar(c.indice));
        el.addEventListener("keydown", (ev) => {
          if (ev.key === "Enter" || ev.key === " ") { ev.preventDefault(); jogar(c.indice); }
        });
      } else if (minhaVez) {
        el.classList.add("bloqueada");
        el.title = "Empachou: só a maior carta pode ser jogada";
      }
      mao.appendChild(el);
    }
  }

  function desenharAcoes(e) {
    const minhaVez = e.acoes.includes("JOGAR");
    const podeCobrir = e.acoes.includes("COBRIR");
    $("chave-cobrir").hidden = !podeCobrir;
    if (!podeCobrir) $("opcao-cobrir").checked = false;
    $("minha-mao").classList.toggle("cobrindo", $("opcao-cobrir").checked);

    const truco = $("btn-truco");
    truco.hidden = !(minhaVez && e.tipoMao === "NORMAL" && e.proximoPedido);
    truco.disabled = !e.acoes.includes("PEDIR_AUMENTO");
    truco.textContent = (e.proximoPedido || "Truco") + "!";
    truco.title = e.motivoSemAumento || `Pedir ${e.proximoPedido}: a mão passa a valer mais`;

    $("dica").textContent = dica(e);
  }

  function dica(e) {
    if (e.status !== "EM_ANDAMENTO") return "";
    const nome = (pos) => e.jogadores.find((j) => j.posicao === pos).nome;
    if (e.acoes.includes("JOGAR")) {
      if (e.tipoMao === "MAO_DE_FERRO") return "Mão de ferro: escolha uma carta no escuro.";
      if (e.rodada && e.rodada.desempate) return "Empachou! Você é obrigado a mostrar sua maior carta.";
      return `Sua vez na ${e.rodada.numero}ª rodada: escolha uma carta${e.acoes.includes("PEDIR_AUMENTO") ? " ou peça " + e.proximoPedido : ""}.`;
    }
    if (e.acoes.includes("RESPONDER")) return "Responda o pedido, forasteiro!";
    if (e.acoes.includes("EMBARALHAR")) return "Você é o pé: embaralhe as cartas.";
    if (e.acoes.includes("DISTRIBUIR")) return "Distribua as cartas.";
    if (e.acoes.includes("CORTAR")) return "Corte o baralho.";
    if (e.estadoMao === "AGUARDANDO_RESPOSTA" && !e.pedido.paraMinhaDupla) return "Esperando a resposta dos adversários…";
    if (e.estadoMao === "EM_RODADA" && e.vez) return `Vez de ${nome(e.vez)}…`;
    return "";
  }

  function desenharDiario(e) {
    const ultimo = maiorId(e.eventos);
    if (ultimo === ultimoEventoNoDiario) return;
    ultimoEventoNoDiario = ultimo;
    const lista = $("diario");
    lista.innerHTML = "";
    for (const ev of e.eventos) {
      const li = document.createElement("li");
      li.className = ev.tipo;
      li.textContent = ev.descricao;
      lista.appendChild(li);
    }
    lista.scrollTop = lista.scrollHeight;
  }

  function desenharModais(e) {
    // UC11: responder pedido
    const responder = e.acoes.includes("RESPONDER");
    $("modal-pedido").hidden = !responder;
    if (responder) {
      const p = e.pedido;
      $("pedido-grito").textContent = p.pedido.toUpperCase() + "!";
      $("pedido-texto").textContent = `${p.nomeSolicitante} pediu ${p.pedido}! Aceitando, a mão passa a valer ${p.valor}. `
        + `Correndo, Eles levam ${p.seCorrer} ${p.seCorrer === 1 ? "ponto" : "pontos"}.`;
      const aumentar = $("btn-aumentar");
      aumentar.hidden = !p.proximo;
      aumentar.textContent = (p.proximo || "") + "!";
    }

    // UC12: mão de onze
    const onze = e.acoes.includes("DECIDIR_MAO_DE_ONZE");
    $("modal-onze").hidden = !onze;
    if (onze) {
      preencher($("onze-minhas"), e.minhasCartas);
      preencher($("onze-parceiro"), e.cartasParceiro || []);
    }

    // UC04: cortar
    const cortar = e.acoes.includes("CORTAR");
    if (cortar && $("modal-corte").hidden) atualizarCorte();
    $("modal-corte").hidden = !cortar;

    // UC15: fim de partida
    const fim = e.status === "ENCERRADA" || e.status === "ABANDONADA";
    if (fim && $("modal-fim").hidden) {
      const venceu = e.vencedoraPartida === "Nós";
      $("fim-titulo").textContent = venceu ? "Vitória!" : e.status === "ABANDONADA" ? "Debandada" : "Derrota";
      $("fim-placar").textContent = `${e.placar.nos} × ${e.placar.eles}`;
      $("fim-texto").textContent = venceu
        ? "Você e Zé Pistola limparam a mesa. O xerife manda lembranças."
        : e.status === "ABANDONADA"
          ? "Você levantou da mesa e deixou as fichas para trás."
          : "Bill Calango e Juca Bigode levaram o ouro desta vez. Que tal uma revanche?";
    }
    $("modal-fim").hidden = !fim;
  }

  function preencher(alvo, cartas) {
    alvo.innerHTML = "";
    for (const c of cartas) alvo.appendChild(Desenho.carta(c, ["mini-onze"]));
  }

  function mostrarFalas(e) {
    for (const ev of e.eventos) {
      if (ev.id <= ultimoEventoVisto) continue;
      if (ev.tipo === "FALA" && ev.posicao) {
        const texto = ev.descricao.slice(ev.descricao.indexOf(":") + 1).trim();
        balao(ev.posicao, texto);
      }
    }
    ultimoEventoVisto = Math.max(ultimoEventoVisto, maiorId(e.eventos));
  }

  function balao(posicao, texto) {
    const assento = document.querySelector(`.assento[data-posicao="${posicao}"]`);
    if (!assento) return;
    assento.querySelectorAll(".balao").forEach((b) => b.remove());
    const b = document.createElement("div");
    b.className = `balao balao-${posicao.toLowerCase()}`;
    if (/^[A-ZÇÃÉ ]+!$/.test(texto)) b.classList.add("grito");
    b.textContent = texto;
    assento.appendChild(b);
    setTimeout(() => b.remove(), 2700);
  }

  // ------------------------------------------------------------ utilidades

  function maiorId(eventos) {
    return (eventos || []).reduce((m, ev) => Math.max(m, ev.id), 0);
  }

  function aviso(texto) {
    const a = document.createElement("div");
    a.className = "aviso";
    a.textContent = texto;
    $("avisos").appendChild(a);
    setTimeout(() => a.remove(), 3700);
  }

  function fecharModais() {
    document.querySelectorAll(".modal").forEach((m) => { m.hidden = true; });
  }

  function atualizarCorte() {
    const v = Number($("corte-ponto").value);
    $("corte-valor").textContent = v;
    $("corte-monte").style.setProperty("--corte", `${(v / 40) * 100}%`);
  }

  function confirmar(titulo, texto, fazer) {
    $("confirmar-titulo").textContent = titulo;
    $("confirmar-texto").textContent = texto;
    acaoConfirmada = fazer;
    $("modal-confirmar").hidden = false;
  }

  // ------------------------------------------------------------ eventos da página

  function ligarEventos() {
    $("form-inicio").addEventListener("submit", (ev) => {
      ev.preventDefault();
      const nome = $("campo-nome").value.trim();
      const nivel = new FormData(ev.target).get("nivel") || "NORMAL";
      try { localStorage.setItem("truco-nome", nome); } catch (e) { /* ignora */ }
      $("modal-inicio").hidden = true;
      placarAnterior = null;
      acao(() => Api.novaPartida(nome, nivel));
    });

    $("btn-truco").addEventListener("click", () => acao(Api.truco));
    $("opcao-cobrir").addEventListener("change", () => {
      $("minha-mao").classList.toggle("cobrindo", $("opcao-cobrir").checked);
    });

    document.querySelectorAll("#modal-pedido [data-resposta]").forEach((b) => {
      b.addEventListener("click", () => {
        $("modal-pedido").hidden = true;
        acao(() => Api.responder(b.dataset.resposta));
      });
    });

    $("btn-onze-jogar").addEventListener("click", () => { $("modal-onze").hidden = true; acao(() => Api.maoDeOnze(true)); });
    $("btn-onze-correr").addEventListener("click", () => { $("modal-onze").hidden = true; acao(() => Api.maoDeOnze(false)); });

    $("corte-ponto").addEventListener("input", atualizarCorte);
    $("btn-cortar").addEventListener("click", () => {
      $("modal-corte").hidden = true;
      acao(() => Api.cortar($("corte-ponto").value));
    });

    $("preparo").addEventListener("click", (ev) => {
      const botao = ev.target.closest("[data-preparo]");
      if (!botao) return;
      acao(botao.dataset.preparo === "embaralhar" ? Api.embaralhar : Api.distribuir);
    });

    $("btn-regras").addEventListener("click", () => { $("modal-regras").hidden = false; });
    $("btn-fechar-regras").addEventListener("click", () => { $("modal-regras").hidden = true; });
    $("modal-regras").addEventListener("click", (ev) => {
      if (ev.target === ev.currentTarget) ev.currentTarget.hidden = true;
    });

    $("btn-abandonar").addEventListener("click", () => {
      if (!estado || estado.status !== "EM_ANDAMENTO") return;
      confirmar("Levantar da mesa?", "Se abandonar, a partida vai para Eles.", () => acao(Api.abandonar));
    });
    $("btn-reiniciar").addEventListener("click", () => {
      confirmar("Recomeçar?", "A partida atual será descartada e uma nova começa com os mesmos jogadores.", () => {
        placarAnterior = null;
        acao(Api.reiniciar);
      });
    });
    $("btn-confirmar-sim").addEventListener("click", () => {
      $("modal-confirmar").hidden = true;
      if (acaoConfirmada) acaoConfirmada();
      acaoConfirmada = null;
    });
    $("btn-confirmar-nao").addEventListener("click", () => {
      $("modal-confirmar").hidden = true;
      acaoConfirmada = null;
    });

    $("btn-jogar-novamente").addEventListener("click", () => {
      $("modal-fim").hidden = true;
      placarAnterior = null;
      acao(Api.reiniciar);
    });
    $("btn-menu").addEventListener("click", () => {
      $("modal-fim").hidden = true;
      $("area-jogo").hidden = true;
      mostrarInicio();
    });

    document.addEventListener("keydown", (ev) => {
      if (ev.key === "Escape") $("modal-regras").hidden = true;
      if (!estado || ev.target.tagName === "INPUT") return;
      if (["1", "2", "3"].includes(ev.key)) {
        const c = estado.minhasCartas[Number(ev.key) - 1];
        if (c && c.permitida) jogar(c.indice);
      }
      if ((ev.key === "t" || ev.key === "T") && estado.acoes.includes("PEDIR_AUMENTO")) acao(Api.truco);
    });
  }

  iniciar();
})();
