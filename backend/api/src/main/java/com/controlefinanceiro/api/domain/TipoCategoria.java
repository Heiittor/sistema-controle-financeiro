package com.controlefinanceiro.api.domain;

// Categoria só classifica receita ou despesa. Transferência não tem categoria.
public enum TipoCategoria {
    RECEITA,
    DESPESA;

    // IMPORTANTE: é aqui que mora "a categoria combina com a transação?".
    public boolean aceita(TipoTransacao tipo) {
        return this.name().equals(tipo.name());
    }
}