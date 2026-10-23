# Truco no Saloon — Truco Paulista Web

Truco Paulista para 4 jogadores em 2 duplas: você (Sul) e o bot **Zé Pistola** (Norte) contra **Bill Calango** (Leste) e **Juca Bigode** (Oeste). O jogo tem o tema de um saloon do Velho Oeste.

- **Frontend:** HTML + CSS + JavaScript puro, em `frontend/`.
- **Backend:** Java 17+ puro, sem dependências, em `backend/`. Usa o servidor HTTP embutido no JDK.
- **Modelagem:** documentos em `docs/`.
  - [casos de uso](docs/casos-de-uso.md)
  - [diagrama de classes](docs/diagrama-de-classes.md)
  - [diagrama de estados](docs/diagrama-de-estados.md)
  - [diagramas de sequência](docs/diagramas-de-sequencia.md)

## Como rodar

Pré-requisito: JDK 17 ou mais novo. No Windows, dá para instalar com `winget install EclipseAdoptium.Temurin.21.JDK`.

```bash
executar.bat
```

O script compila o backend, sobe o servidor e abre <http://localhost:8080> no navegador. Para usar outra porta, rode `executar.bat 9090`.

Para rodar os testes das regras (cenários T1 a T13 dos casos de uso):

```bash
testar.bat
```

## Estrutura

```
backend/src/main/java/br/com/truco/
  cartas/      Naipe, Valor, Carta, Baralho, Vira
  jogadores/   Posicao, Jogador, JogadorHumano, JogadorBot, NivelBot,
               EstrategiaBot (+ Facil, Normal, Dificil), VisaoDoJogo
  jogo/        Partida, Dupla, Configuracao, StatusPartida
  mao/         Mao, Rodada, Jogada, PedidoAumento, NivelAposta, TipoMao, EstadoMao, RespostaPedido
  regras/      Arbitro, ResultadoRodada, JogadaInvalidaException
  aplicacao/   ControladorPartida, EstadoMesa, EventoJogo
  web/         Servidor, ApiHandler, ArquivosEstaticosHandler, Json
backend/src/test/java/br/com/truco/TesteRegras.java
frontend/      index.html, css/saloon.css, js/api.js, js/cartas.js, js/app.js
```

Os pacotes seguem o diagrama de classes. Todas as regras de negócio (RN01 a RN21) ficam no `Arbitro`. A `Mao` implementa o diagrama de estados.

## API

Toda resposta traz o estado da mesa em JSON, visto pelo jogador humano. Erros de regra voltam com status 400 e a mensagem em `erro`.

| Método e rota | Parâmetros | Caso de uso |
|---|---|---|
| `GET /api/estado` | — | UC17 |
| `POST /api/partida/nova` | `nome`, `nivel` (FACIL, NORMAL, DIFICIL) | UC01 |
| `POST /api/embaralhar` | — | UC03 |
| `POST /api/cortar` | `ponto` (1 a 39) | UC04 |
| `POST /api/distribuir` | — | UC03 |
| `POST /api/jogar` | `indice` (0 a 2), `coberta` (true/false) | UC07, UC08 |
| `POST /api/truco` | — | UC10 |
| `POST /api/responder` | `resposta` (ACEITAR, CORRER, AUMENTAR) | UC11 |
| `POST /api/mao-de-onze` | `jogar` (true/false) | UC12 |
| `POST /api/passo` | — | UC16: executa uma ação de bot |
| `POST /api/partida/abandonar` | — | UC19 |
| `POST /api/partida/reiniciar` | — | UC19 |

A tela chama `/api/passo` em laço, com uma pausa entre as chamadas. Assim cada jogada dos bots aparece animada, em vez de todas de uma vez.

## Atalhos

- `1`, `2` e `3` jogam a carta correspondente.
- `T` pede truco (ou seis, nove, doze).
- `Esc` fecha as regras.

## Licença

Distribuído sob a licença MIT. Veja o arquivo [LICENSE](LICENSE).
