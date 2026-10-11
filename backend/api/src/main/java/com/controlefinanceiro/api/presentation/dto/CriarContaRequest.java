package com.controlefinanceiro.api.presentation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// IMPORTANTE: só existe "nome". Não há campo de saldo: a conta sempre nasce com zero.
public record CriarContaRequest(

        @NotBlank(message = "O nome da conta é obrigatório.")
        @Size(max = 100, message = "O nome da conta deve ter no máximo 100 caracteres.")
        String nome
) {
}