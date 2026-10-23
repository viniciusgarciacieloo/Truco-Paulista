# Truco Paulista Web — Diagramas de Sequência

> Deriva de [casos-de-uso.md](casos-de-uso.md), [diagrama-de-classes.md](diagrama-de-classes.md) e [diagrama-de-estados.md](diagrama-de-estados.md).
> Todas as mensagens são métodos que existem no diagrama de classes, e as mudanças de `estado` seguem as transições T01 a T17 do diagrama de estados.

**Participantes**

| Participante | Papel |
|---|---|
| `Jogador` (ator) | O usuário humano |
| `Tela` (boundary) | Interface web: mostra o `EstadoMesa` e envia as ações do jogador |
| `ControladorPartida` (control) | Único ponto de entrada do domínio |
| `Partida`, `Mao`, `Rodada`, `Dupla`, `PedidoAumento` | Entidades do domínio |
| `Arbitro` | Serviço que concentra as regras de negócio |
| `JogadorBot` + `EstrategiaBot` | Decisão automática dos bots |

---

## 1. UC07 — Jogar Carta (inclui UC09 — Apurar Rodada)

![Sequência Jogar Carta](seq-jogar-carta.png)

```plantuml
@startuml
title UC07 Jogar Carta (+ UC09 Apurar Rodada)
autonumber
skinparam sequenceMessageAlign center

actor Jogador
boundary "Tela" as T
control "ControladorPartida" as C
participant "partida: Partida" as P
participant "mao: Mao" as M
participant ": Arbitro" as A
participant "rodada: Rodada" as R
participant "humano: JogadorHumano" as H

Jogador -> T : clica numa carta\n(marca "coberta" ou não)
T -> C : jogarCarta(carta, coberta)
C -> P : maoAtual()
P --> C : mao

C -> M : jogarCarta(humano, carta, coberta)

group Validações (fluxos de exceção do UC07)
  alt estado = AGUARDANDO_RESPOSTA
    M --> C : erro "Responda o pedido de truco antes" (FE2)
  else jogadorDaVez != humano
    M --> C : erro "Aguarde sua vez" (FE1)
  else
    M -> A : cartasPermitidas(humano, mao)
    A --> M : cartas permitidas\n(só a maior, se for rodada de desempate)
    opt coberta = true
      M -> A : podeCobrir(mao)
      A --> M : true / false
    end
  end
end

alt carta fora das permitidas ou cobrir não permitido
  M --> C : erro "Mostre sua maior carta" (FE3)\nou "Não pode cobrir" (UC08)
  C --> T : mensagem de erro
  T --> Jogador : aviso; estado não muda
else jogada válida
  M -> H : removerCarta(carta)
  M -> R : adicionar(new Jogada(humano, carta, coberta))
  M -> R : estaCompleta()
  R --> M : completa?

  alt menos de 4 cartas na mesa (T08)
    M -> M : jogadorDaVez = posição.direita()
  else 4ª carta da rodada (T09) — UC09 Apurar Rodada
    M -> A : apurarRodada(rodada, vira, partida)
    A -> A : forca(jogada, vira) para as 4 jogadas
    A --> M : ResultadoRodada(vencedora | empate)
    M -> R : registrarResultado(resultado)
    M -> A : maoDefinida(rodadas)
    A --> M : definida?
    alt mão definida (T12)
      M -> A : vencedoraDaMao(rodadas)
      A --> M : dupla vencedora (ou nenhuma)
      M -> M : estado = ENCERRADA
    else mão continua (T10 / T11)
      M -> A : quemComecaProximaRodada(mao)
      A --> M : vencedor da rodada\nou pé (se empatou)
      M -> M : nova Rodada(desempate = empate)
    end
  end
  M --> C : ok

  opt mao.estaEncerrada()
    C -> C : finalizarMao()\n(UC14 Marcar Pontos)
  end
  C -> C : executarTurnoDosBots()\n(ver diagrama 3)
  C -> C : obterEstado()
  C --> T : EstadoMesa
  T --> Jogador : mesa, placar e histórico atualizados
end
@enduml
```

**Como ler**
- O bloco **Validações** implementa os fluxos de exceção FE1, FE2 e FE3 do UC07 e o FE1 do UC08. Quando a jogada é recusada, a `Mao` não muda de estado.
- A **rodada de desempate** (RN21) aparece em dois pontos: em `cartasPermitidas()`, que devolve só a maior carta, e em `quemComecaProximaRodada()`, que devolve o pé.
- Depois da jogada do humano, o controlador chama `executarTurnoDosBots()`. Os bots jogam até chegar de novo a vez do humano, e só então a tela é atualizada.

---

## 2. UC10 — Pedir Truco (inclui UC11 — Responder Pedido)

Cenário: o **humano pede truco** e a **dupla de bots adversária** responde.

![Sequência Pedir Truco](seq-pedir-truco.png)

```plantuml
@startuml
title UC10 Pedir Truco (+ UC11 Responder Pedido de Aumento)
autonumber
skinparam sequenceMessageAlign center

actor Jogador
boundary "Tela" as T
control "ControladorPartida" as C
participant "mao: Mao" as M
participant ": Arbitro" as A
participant "leste: JogadorBot" as B
participant "estrategia: EstrategiaBot" as E
participant "nos: Dupla" as N

Jogador -> T : clica "Truco!"
T -> C : pedirAumento()
C -> M : pedirAumento(humano)
M -> A : podePedirAumento(humano, mao, partida)
A --> M : true / false

alt não pode pedir (UC10 FE1)
  M --> C : erro (ex.: "Mão de onze: sem truco")
  C --> T : mensagem
  T --> Jogador : botão desabilitado com o motivo
else pedido permitido (T13)
  create "pedido: PedidoAumento" as PA
  M -> PA : new(humano, nos, TRUCO)
  M -> M : estado = AGUARDANDO_RESPOSTA
  M --> C : pedido
  C --> T : EstadoMesa (balão "Truco!")

  == UC11 Responder: a dupla adversária é de bots (RN20) ==
  C -> C : montarVisao(leste)
  C -> B : responderPedido(visao, pedido)
  B -> E : responderPedido(visao, pedido)
  E --> B : resposta
  B --> C : resposta
  C -> M : responderPedido(leste, resposta)

  alt ACEITAR (T15)
    M -> PA : responder(ACEITAR)
    M -> M : aposta = TRUCO (3)\nduplaQuePodeAumentar = eles\nestado = EM_RODADA
    M --> C : ok
    C --> T : EstadoMesa ("Eles aceitaram, vale 3")
    T --> Jogador : libera escolher a carta (segue no UC07)
  else CORRER (T16)
    M -> PA : responder(CORRER)
    M -> M : vencedora = nos\npontosGanhos = TRUCO.pontosSeCorrer() (1)\nestado = ENCERRADA
    M --> C : ok
    C -> C : finalizarMao()
    C -> N : adicionarPontos(1)
    C --> T : EstadoMesa ("Eles correram, +1 para Nós")
  else AUMENTAR (T14)
    M -> PA : responder(AUMENTAR)
    create "pedido2: PedidoAumento" as PB
    M -> PB : new(leste, eles, SEIS)
    M --> C : novo pedido
    C --> T : EstadoMesa ("Seis!")\nbotões Aceitar / Correr / Nove
    T --> Jogador : pede a resposta
    Jogador -> T : escolhe a resposta
    T -> C : responderPedido(resposta)
    C -> M : responderPedido(humano, resposta)
    note over C, M
      Mesma lógica dos ramos acima,
      com os papéis invertidos. Se o humano
      pedir "Nove!", a resposta volta aos bots.
    end note
  end
end
@enduml
```

**Como ler**
- O pedido só é criado se `Arbitro.podePedirAumento()` permitir (RN14, RN17, RN18).
- **Quem responde pela dupla de bots:** o adversário à direita do humano (`leste`), que é o próximo na ordem. Pela RN20, quando o pedido é para a dupla do humano, **sempre o humano** responde, mesmo que quem esteja na vez seja o parceiro.
- `NivelAposta.pontosSeCorrer()` devolve o valor anterior ao pedido: correr do Truco dá 1, do Seis dá 3, e assim por diante (RN15).
- Ao aceitar, `duplaQuePodeAumentar` passa a ser a dupla que aceitou. A dupla que pediu fica impedida de pedir de novo em seguida.

---

## 3. UC16 — Executar Turno do Bot

Este é o laço que o controlador roda depois de cada ação do humano. Ele também roda no início de cada mão, quando o pé ou o cortador é um bot.

![Sequência Turno do Bot](seq-turno-bot.png)

```plantuml
@startuml
title UC16 Executar Turno do Bot (ControladorPartida.executarTurnoDosBots)
autonumber
skinparam sequenceMessageAlign center

boundary "Tela" as T
control "ControladorPartida" as C
participant "partida: Partida" as P
participant "mao: Mao" as M
participant ": Arbitro" as A
participant "bot: JogadorBot" as B
participant "estrategia: EstrategiaBot" as E

loop enquanto a próxima ação couber a um bot e a partida estiver EM_ANDAMENTO
  C -> P : maoAtual()
  P --> C : mao
  C -> M : estado / jogadorDaVez
  M --> C : estado atual e bot responsável

  C -> C : montarVisao(bot)
  activate C
  C -> A : cartasPermitidas(bot, mao)
  C -> A : podeCobrir(mao)
  C -> A : podePedirAumento(bot, mao, partida)
  note right of C
    VisaoDoJogo leva só o que o bot pode ver.
    As cartas do parceiro só aparecem
    na mão de onze (RN17).
  end note
  deactivate C

  alt EMBARALHANDO (bot é o pé)
    C -> M : embaralhar()
  else CORTANDO (bot é o cortador)
    C -> B : escolherPontoDeCorte()
    B --> C : ponto
    C -> M : cortar(ponto)
  else DISTRIBUINDO (bot é o pé)
    C -> M : distribuir()
  else DECISAO_MAO_DE_ONZE (dupla de bots com 11)
    C -> B : decidirMaoDeOnze(visao)
    B -> E : decidirMaoDeOnze(visao)
    E --> B : jogar?
    B --> C : jogar?
    C -> M : decidirMaoDeOnze(jogar)
  else EM_RODADA (vez do bot)
    C -> B : querPedirAumento(visao)
    B -> E : querPedirAumento(visao)
    E --> B : sim / não
    B --> C : sim / não
    opt bot quer pedir aumento
      C -> M : pedirAumento(bot)
      alt pedido é para a dupla do humano (RN20)
        C --> T : EstadoMesa ("Truco!" + botões de resposta)
        break aguarda o humano responder (diagrama 2, papéis invertidos)
        end
      else pedido é para a dupla de bots (o parceiro do humano pediu)
        C -> B : responderPedido(visao, pedido)\n(bot adversário)
        B --> C : resposta
        C -> M : responderPedido(bot adversário, resposta)
      end
    end
    opt mão continua (ninguém correu)
      C -> B : escolherCarta(visao)
      B -> E : escolherCarta(visao)
      E --> B : jogada (carta, coberta)
      B --> C : jogada
      C -> M : jogarCarta(bot, carta, coberta)
      note right of M
        Mesmas validações e apuração
        do diagrama 1 (UC16 FE1: se a
        jogada for inválida, o bot joga
        a primeira carta permitida).
      end note
    end
  else ENCERRADA
    C -> C : finalizarMao()
    C -> P : verificarFim()
    alt alguma dupla com 12 pontos
      C -> P : encerrar(vencedora)
    else partida continua
      C -> P : passarPe()
      C -> P : novaMao()
    end
  end

  C -> C : registrar EventoJogo
  C --> T : EstadoMesa (atualização parcial)
  T -> T : pausa de 0,8 a 1,5 s\n(animação do bot "pensando")
end

C --> T : EstadoMesa final (vez do humano ou fim de partida)
@enduml
```

**Como ler**
- O laço avança a `Mao` pelo diagrama de estados **até a próxima ação caber ao humano**: a vez dele de jogar, a resposta a um pedido, a decisão da mão de onze ou embaralhar/cortar quando ele é o pé ou o cortador.
- O bot usa **os mesmos métodos da `Mao`** que o humano (`jogarCarta`, `pedirAumento`...). Por isso as validações são idênticas para todos (UC16, passo 5).
- O bloco `break` mostra a saída antecipada do laço: um bot pede truco para a dupla do humano, e o sistema precisa esperar a resposta dele.
- O ramo **ENCERRADA** cuida da troca de mão: marca os pontos (UC14), verifica o fim da partida (UC15) e passa o pé (RN08).

---

## 4. Conferência com o diagrama de classes

| Mensagem nos diagramas | Classe / método |
|---|---|
| `jogarCarta`, `pedirAumento`, `responderPedido`, `obterEstado`, `executarTurnoDosBots`, `montarVisao`, `finalizarMao` | `ControladorPartida` |
| `maoAtual`, `verificarFim`, `encerrar`, `passarPe`, `novaMao` | `Partida` |
| `jogarCarta`, `pedirAumento`, `responderPedido`, `embaralhar`, `cortar`, `distribuir`, `decidirMaoDeOnze` | `Mao` |
| `adicionar`, `estaCompleta`, `registrarResultado` | `Rodada` |
| `cartasPermitidas`, `podeCobrir`, `podePedirAumento`, `apurarRodada`, `forca`, `maoDefinida`, `vencedoraDaMao`, `quemComecaProximaRodada` | `Arbitro` |
| `escolherPontoDeCorte`, `escolherCarta`, `querPedirAumento`, `responderPedido`, `decidirMaoDeOnze` | `JogadorBot` / `EstrategiaBot` |
| `responder` | `PedidoAumento` |
| `adicionarPontos` | `Dupla` |
| `removerCarta` | `Jogador` |
