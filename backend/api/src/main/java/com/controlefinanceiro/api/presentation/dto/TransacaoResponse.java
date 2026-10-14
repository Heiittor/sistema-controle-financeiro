package com.controlefinanceiro.api.presentation.dto;

import com.controlefinanceiro.api.domain.TipoTransacao;
import com.controlefinanceiro.api.domain.Transacao;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

// categoriaId, categoriaNome e transferenciaId podem ser nulos (depende do tipo).
public record TransacaoResponse(
        Long id,
        String descricao,
        BigDecimal valor,
        LocalDate data,
        TipoTransacao tipo,
        Long contaId,
        Long categoriaId,
        String categoriaNome,
        UUID transferenciaId
) {

    public static TransacaoResponse de(Transacao transacao) {
        // IMPORTANTE: transferência não tem categoria, então não podemos assumir que ela existe.
        var categoria = transacao.getCategoria();
        return new TransacaoResponse(
                transacao.getId(),
                transacao.getDescricao(),
                transacao.getValor().valor(),
                transacao.getData(),
                transacao.getTipo(),
                transacao.getConta().getId(),
                categoria != null ? categoria.getId() : null,
                categoria != null ? categoria.getNome() : null,
                transacao.getTransferenciaId());
    }
}