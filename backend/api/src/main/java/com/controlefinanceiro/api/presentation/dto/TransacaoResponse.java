package com.controlefinanceiro.api.presentation.dto;

import com.controlefinanceiro.api.domain.TipoTransacao;
import com.controlefinanceiro.api.domain.Transacao;

import java.math.BigDecimal;
import java.time.LocalDate;

// IMPORTANTE: só ids de conta e categoria. Ler o id de um proxy LAZY não vai ao banco (evita N+1).
public record TransacaoResponse(
        Long id,
        String descricao,
        BigDecimal valor,
        LocalDate data,
        TipoTransacao tipo,
        Long contaId,
        Long categoriaId,
        String categoriaNome
) {

    public static TransacaoResponse de(Transacao transacao) {
        return new TransacaoResponse(
                transacao.getId(),
                transacao.getDescricao(),
                transacao.getValor().valor(),
                transacao.getData(),
                transacao.getTipo(),
                transacao.getConta().getId(),
                transacao.getCategoria().getId(),
                transacao.getCategoria().getNome());
    }
}