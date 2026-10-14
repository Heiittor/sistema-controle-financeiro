package com.controlefinanceiro.api.presentation.dto;

import com.controlefinanceiro.api.domain.Conta.ParTransferencia;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record TransferenciaResponse(
        UUID transferenciaId,
        Long contaOrigemId,
        Long contaDestinoId,
        String descricao,
        BigDecimal valor,
        LocalDate data
) {

    public static TransferenciaResponse de(ParTransferencia par) {
        return new TransferenciaResponse(
                par.saida().getTransferenciaId(),
                par.saida().getConta().getId(),
                par.entrada().getConta().getId(),
                par.saida().getDescricao(),
                par.saida().getValor().valor(),
                par.saida().getData());
    }
}