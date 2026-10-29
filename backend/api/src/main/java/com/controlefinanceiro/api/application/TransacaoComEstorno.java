package com.controlefinanceiro.api.application;

import com.controlefinanceiro.api.domain.Transacao;

// Uma transação junto do estado "já foi estornada?". Esse estado não é campo da entidade:
// vem de uma consulta, para existir uma única fonte da verdade (a existência da linha de estorno).
public record TransacaoComEstorno(Transacao transacao, boolean estornada) {
}