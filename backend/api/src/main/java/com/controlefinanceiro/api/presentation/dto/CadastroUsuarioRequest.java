package com.controlefinanceiro.api.presentation.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CadastroUsuarioRequest(

        @NotBlank(message = "O nome é obrigatório.")
        @Size(max = 255, message = "O nome deve ter no máximo 255 caracteres.")
        String nome,

        @NotBlank(message = "O e-mail é obrigatório.")
        @Email(message = "O e-mail é inválido.")
        @Size(max = 255, message = "O e-mail deve ter no máximo 255 caracteres.")
        String email,

        // IMPORTANTE: máximo de 72 porque o BCrypt ignora o que passar disso.
        @NotBlank(message = "A senha é obrigatória.")
        @Size(min = 8, max = 72, message = "A senha deve ter entre 8 e 72 caracteres.")
        String senha
) {

    // IMPORTANTE: sem isto, o toString do record imprimiria a senha em qualquer log.
    @Override
    public String toString() {
        return "CadastroUsuarioRequest[nome=" + nome + ", email=" + email + ", senha=***]";
    }
}