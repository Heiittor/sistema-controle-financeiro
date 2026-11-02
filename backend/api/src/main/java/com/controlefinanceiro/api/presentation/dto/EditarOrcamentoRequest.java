package com.controlefinanceiro.api.presentation.dto;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
public record EditarOrcamentoRequest(
        @NotNull(message = "Informe o limite.") @Positive(message = "O limite deve ser maior que zero.") BigDecimal limite) {}
