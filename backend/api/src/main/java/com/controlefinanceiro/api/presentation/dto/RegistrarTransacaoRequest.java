package com.controlefinanceiro.api.presentation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

// IMPORTANTE: não há campo "tipo" nem "contaId". O tipo vem da rota (/receitas ou /despesas)
// e a conta vem da URL, então o cliente não consegue contradizê-los no corpo.
public record RegistrarTransacaoRequest(

        @NotNull(message = "A categoria é obrigatória.")
        Long categoriaId,

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