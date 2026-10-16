package com.controlefinanceiro.api.presentation.dto;

import com.controlefinanceiro.api.application.ResumoService.Resumo;
import com.controlefinanceiro.api.domain.TipoTransacao;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record ResumoResponse(
        LocalDate inicio,
        LocalDate fim,
        BigDecimal totalReceitas,
        BigDecimal totalDespesas,
        BigDecimal saldoDoPeriodo,
        List<TotalCategoria> porCategoria
) {

    public record TotalCategoria(Long categoriaId, String categoriaNome, TipoTransacao tipo, BigDecimal total) {
    }

    public static ResumoResponse de(Resumo resumo) {
        return new ResumoResponse(
                resumo.inicio(), resumo.fim(),
                resumo.totalReceitas(), resumo.totalDespesas(), resumo.saldoDoPeriodo(),
                resumo.porCategoria().stream()
                        .map(t -> new TotalCategoria(
                                t.getCategoriaId(), t.getCategoriaNome(), t.getTipo(), t.getTotal()))
                        .toList());
    }
}