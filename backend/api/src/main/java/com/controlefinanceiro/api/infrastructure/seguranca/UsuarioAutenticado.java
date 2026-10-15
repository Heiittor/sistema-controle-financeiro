package com.controlefinanceiro.api.infrastructure.seguranca;

import org.springframework.security.oauth2.jwt.Jwt;

// Ponto único que transforma o token validado no id do usuário.
public final class UsuarioAutenticado {

    private UsuarioAutenticado() {
    }

    // O "sub" do token é o id do usuário, gravado pelo TokenService.
    public static Long id(Jwt jwt) {
        return Long.parseLong(jwt.getSubject());
    }
}