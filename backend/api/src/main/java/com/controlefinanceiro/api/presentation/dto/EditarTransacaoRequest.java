package com.controlefinanceiro.api.presentation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// IMPORTANTE: só descrição e categoria. Valor, data, tipo e conta não mudam (para corrigir, estorna).
public record EditarTransacaoRequest(

        @NotNull(message = "A categoria é obrigatória.")
        Long categoriaId,

        @NotBlank(message = "A descrição é obrigatória.")
        @Size(max = 255, message = "A descrição deve ter no máximo 255 caracteres.")
        String descricao
) {
}