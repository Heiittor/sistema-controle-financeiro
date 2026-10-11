package com.controlefinanceiro.api.presentation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(

        @NotBlank(message = "O e-mail é obrigatório.")
        @Size(max = 255, message = "O e-mail deve ter no máximo 255 caracteres.")
        String email,

        // Limite de 72 evita mandar textos gigantes para o BCrypt (consumo de CPU).
        @NotBlank(message = "A senha é obrigatória.")
        @Size(max = 72, message = "A senha deve ter no máximo 72 caracteres.")
        String senha
) {

    // IMPORTANTE: sem isto, o toString do record imprimiria a senha em qualquer log.
    @Override
    public String toString() {
        return "LoginRequest[email=" + email + ", senha=***]";
    }
}