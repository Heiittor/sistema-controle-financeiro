package com.controlefinanceiro.api.domain;

public enum TipoTransacao {
    RECEITA,
    DESPESA,
    // Os dois lados de uma transferência entre contas (ligados por transferenciaId).
    TRANSFERENCIA_SAIDA,
    TRANSFERENCIA_ENTRADA
}

//esse tipo foi preferivel porque Enum diferentemente de string(onde alguem pode digitar algo errado) nos usamos o Enum que basicamnete ja tem uma lista fixa de opções permitidas
// ou seja Ele diz: "Neste sistema, uma transação só pode ser RECEITA ou DESPESA, nada além disso!")