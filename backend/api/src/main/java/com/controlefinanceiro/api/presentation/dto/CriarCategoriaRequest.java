package com.controlefinanceiro.api.presentation.dto;

import com.controlefinanceiro.api.domain.TipoTransacao;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CriarCategoriaRequest(

        @NotBlank(message = "O nome da categoria é obrigatório.")
        @Size(max = 100, message = "O nome da categoria deve ter no máximo 100 caracteres.")
        String nome,

        // IMPORTANTE: só aceita RECEITA ou DESPESA. Outro valor vira 400 pelo handler de JSON ilegível.
        @NotNull(message = "O tipo da categoria é obrigatório.")
        TipoTransacao tipo
) {
}