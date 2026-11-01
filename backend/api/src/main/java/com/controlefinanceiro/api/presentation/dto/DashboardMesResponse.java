package com.controlefinanceiro.api.presentation.dto;

import com.controlefinanceiro.api.application.DashboardService.Mes;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

public record DashboardMesResponse(
        @Schema(type = "string", example = "2026-10", description = "Mês civil AAAA-MM") String mes,
        BigDecimal receitas, BigDecimal despesas) {
    public static DashboardMesResponse de(Mes mes) {
        return new DashboardMesResponse(mes.mes(), mes.receitas(), mes.despesas());
    }
}
