/* Comunicação com o backend Java (ApiHandler). Toda resposta traz o EstadoMesa. */
const Api = (() => {
  async function chamar(metodo, rota, parametros) {
    const opcoes = { method: metodo, headers: {} };
    if (parametros) {
      opcoes.body = new URLSearchParams(parametros);
      opcoes.headers["Content-Type"] = "application/x-www-form-urlencoded; charset=UTF-8";
    }
    let resposta;
    try {
      resposta = await fetch("/api" + rota, opcoes);
    } catch (e) {
      return { erro: "O saloon fechou as portas: o servidor não respondeu.", estado: null };
    }
    const corpo = await resposta.json().catch(() => ({}));
    if (!resposta.ok) {
      return { erro: corpo.erro || "Erro " + resposta.status, estado: corpo.estado || null };
    }
    return { erro: null, estado: corpo };
  }

  return {
    estado: () => chamar("GET", "/estado"),
    novaPartida: (nome, nivel) => chamar("POST", "/partida/nova", { nome, nivel }),
    reiniciar: () => chamar("POST", "/partida/reiniciar", {}),
    abandonar: () => chamar("POST", "/partida/abandonar", {}),
    embaralhar: () => chamar("POST", "/embaralhar", {}),
    cortar: (ponto) => chamar("POST", "/cortar", { ponto }),
    distribuir: () => chamar("POST", "/distribuir", {}),
    jogar: (indice, coberta) => chamar("POST", "/jogar", { indice, coberta }),
    truco: () => chamar("POST", "/truco", {}),
    responder: (resposta) => chamar("POST", "/responder", { resposta }),
    maoDeOnze: (jogar) => chamar("POST", "/mao-de-onze", { jogar }),
    passo: () => chamar("POST", "/passo", {}),
  };
})();
