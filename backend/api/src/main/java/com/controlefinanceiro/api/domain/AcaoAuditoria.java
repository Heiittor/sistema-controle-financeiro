package com.controlefinanceiro.api.domain;

// Só ações com efeito. Leituras (listar, resumir) e tentativas de login não entram aqui.
public enum AcaoAuditoria {
    CADASTRAR_USUARIO,
    CRIAR_CONTA,
    CRIAR_CATEGORIA,
    REGISTRAR_RECEITA,
    REGISTRAR_DESPESA,
    TRANSFERIR,
    ESTORNAR,
    EDITAR_TRANSACAO
}