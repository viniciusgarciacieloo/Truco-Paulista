package br.com.truco.mao;

/** Estados do diagrama de estados da mão (docs/diagrama-de-estados.md). */
public enum EstadoMao {
    EMBARALHANDO,
    CORTANDO,
    DISTRIBUINDO,
    DECISAO_MAO_DE_ONZE,
    EM_RODADA,
    AGUARDANDO_RESPOSTA,
    ENCERRADA
}
