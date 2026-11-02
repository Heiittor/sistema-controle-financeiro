package com.controlefinanceiro.api.presentation.dto;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.YearMonth;
public record CriarOrcamentoRequest(
        @NotNull(message = "Escolha a categoria.") Long categoriaId,
        @NotNull(message = "Informe o mês.") YearMonth mes,
        @NotNull(message = "Informe o limite.") @Positive(message = "O limite deve ser maior que zero.") BigDecimal limite) {}
