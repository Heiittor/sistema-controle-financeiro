package com.controlefinanceiro.api.presentation.dto;

import com.controlefinanceiro.api.application.TokenService.TokenEmitido;

public record TokenResponse(String token, String tipo, long expiraEmSegundos) {

    // "Bearer" diz ao cliente como enviar: Authorization: Bearer <token>
    public static TokenResponse de(TokenEmitido emitido) {
        return new TokenResponse(emitido.token(), "Bearer", emitido.expiraEmSegundos());
    }
}