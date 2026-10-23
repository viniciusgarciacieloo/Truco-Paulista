/* Desenho das cartas e dos avatares dos cowboys. */
const Desenho = (() => {
  const FIGURAS = { Q: "Q", J: "J", K: "K" };

  /** Cria o elemento de uma carta. `carta` pode ser null para desenhar o verso. */
  function carta(dados, classes = []) {
    const el = document.createElement("div");
    el.classList.add("carta", ...classes);
    if (!dados || dados.oculta) {
      el.classList.add("verso");
      el.setAttribute("aria-label", "carta virada para baixo");
      return el;
    }
    if (dados.vermelha) el.classList.add("vermelha");
    if (dados.manilha) el.classList.add("manilha");
    el.setAttribute("aria-label", `${dados.valor} de ${nomeNaipe(dados.naipe)}${dados.manilha ? " (manilha)" : ""}`);

    const canto = `<span class="v">${dados.valor}</span><span class="n">${dados.simbolo}</span>`;
    const centro = FIGURAS[dados.valor]
      ? `<div class="figura"><span class="letra">${dados.valor}</span><span class="naipe-peq">${dados.simbolo}</span></div>`
      : `<div class="centro-naipe">${dados.simbolo}</div>`;
    el.innerHTML = `<div class="canto">${canto}</div>${centro}<div class="canto inferior">${canto}</div>`
      + (dados.manilha ? `<span class="estrela" title="Manilha"></span>` : "");
    return el;
  }

  function nomeNaipe(naipe) {
    return { OUROS: "ouros", ESPADAS: "espadas", COPAS: "copas", PAUS: "paus" }[naipe] || naipe;
  }

  /** Silhueta de cowboy com lenço na cor da dupla. */
  function avatar(posicao, dupla) {
    const lenco = dupla === "Nós" ? "#2f6aa0" : "#a8321f";
    const chapeu = { SUL: "#3a2414", NORTE: "#5b3a1e", LESTE: "#1f1712", OESTE: "#6b4a2a" }[posicao] || "#3a2414";
    const fundo = { SUL: "#e9cf98", NORTE: "#dcc08a", LESTE: "#d8b27a", OESTE: "#e3c48c" }[posicao] || "#e3c48c";
    return `
      <svg viewBox="0 0 100 100" aria-hidden="true">
        <rect width="100" height="100" fill="${fundo}"/>
        <path d="M18 100 C20 78 34 70 50 70 C66 70 80 78 82 100 Z" fill="#5a3a22"/>
        <path d="M36 72 L50 90 L64 72 Z" fill="${lenco}"/>
        <ellipse cx="50" cy="54" rx="15" ry="18" fill="#c58d5b"/>
        <path d="M35 58 Q50 70 65 58 L65 62 Q50 76 35 62 Z" fill="#6b4226" opacity="0.55"/>
        <ellipse cx="50" cy="39" rx="38" ry="8" fill="${chapeu}"/>
        <path d="M30 39 C30 18 38 14 50 20 C62 14 70 18 70 39 Z" fill="${chapeu}"/>
        <path d="M31 33 L69 33 L69 37 L31 37 Z" fill="#c99a4f" opacity="0.85"/>
      </svg>`;
  }

  return { carta, avatar };
})();
