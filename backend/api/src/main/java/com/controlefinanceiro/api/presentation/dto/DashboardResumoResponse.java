package com.controlefinanceiro.api.presentation.dto;

import com.controlefinanceiro.api.application.DashboardService.Resumo;
import com.controlefinanceiro.api.domain.TipoTransacao;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record DashboardResumoResponse(
        LocalDate inicio, LocalDate fim,
        @Schema(description = "Soma dos saldos atuais das contas; independente do período") BigDecimal saldoTotal,
        BigDecimal totalReceitas, BigDecimal totalDespesas, BigDecimal saldoDoPeriodo,
        @Schema(description = "Somente despesas elegíveis, agrupadas por categoria") List<Categoria> porCategoria) {
    public record Categoria(Long categoriaId, String categoriaNome, TipoTransacao tipo, BigDecimal total) {}

    public static DashboardResumoResponse de(Resumo resumo) {
        return new DashboardResumoResponse(resumo.inicio(), resumo.fim(), resumo.saldoTotal(),
                resumo.totalReceitas(), resumo.totalDespesas(), resumo.saldoDoPeriodo(),
                resumo.porCategoria().stream().map(t -> new Categoria(
                        t.getCategoriaId(), t.getCategoriaNome(), t.getTipo(), t.getTotal())).toList());
    }
}
