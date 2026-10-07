package com.controlefinanceiro.api.presentation.dto;

import com.controlefinanceiro.api.domain.Conta;

import java.math.BigDecimal;

// IMPORTANTE: só os campos declarados aqui saem na API. O dono (usuario) não está na lista.
public record ContaResponse(Long id, String nome, BigDecimal saldo) {

    // Único ponto de conversão Conta -> JSON. O saldo é desembrulhado do Dinheiro.
    public static ContaResponse de(Conta conta) {
        return new ContaResponse(conta.getId(), conta.getNome(), conta.getSaldo().valor());
    }
}