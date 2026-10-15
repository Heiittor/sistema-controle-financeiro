package com.controlefinanceiro.api.presentation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TransferirRequest(

        @NotNull(message = "A conta de origem é obrigatória.")
        Long contaOrigemId,

        @NotNull(message = "A conta de destino é obrigatória.")
        Long contaDestinoId,

        @NotBlank(message = "A descrição é obrigatória.")
        @Size(max = 255, message = "A descrição deve ter no máximo 255 caracteres.")
        String descricao,

        @NotNull(message = "O valor é obrigatório.")
        @Positive(message = "O valor deve ser maior que zero.")
        BigDecimal valor,

        @NotNull(message = "A data é obrigatória.")
        LocalDate data
) {
}