package com.controlefinanceiro.api.presentation.dto;

import com.controlefinanceiro.api.application.TransacaoComEstorno;
import com.controlefinanceiro.api.domain.TipoTransacao;
import com.controlefinanceiro.api.domain.Transacao;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

// categoriaId, categoriaNome, transferenciaId e estornoDeId podem ser nulos (depende do tipo).
public record TransacaoResponse(
        Long id,
        String descricao,
        BigDecimal valor,
        LocalDate data,
        TipoTransacao tipo,
        Long contaId,
        Long categoriaId,
        String categoriaNome,
        UUID transferenciaId,
        Long estornoDeId,
        @Schema(description = "true se esta transação (receita ou despesa) já foi estornada. "
                + "É false no próprio estorno, em transferências e em tudo que não foi estornado.")
        boolean estornada
) {

    public static TransacaoResponse de(TransacaoComEstorno visao) {
        Transacao transacao = visao.transacao();
        // IMPORTANTE: transferência e estorno não têm categoria, então não podemos assumir que ela existe.
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
                transacao.getTransferenciaId(),
                transacao.getEstornoDeId(),
                visao.estornada());
    }
}