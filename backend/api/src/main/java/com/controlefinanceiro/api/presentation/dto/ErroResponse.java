package com.controlefinanceiro.api.presentation.dto;

import java.time.Instant;
import java.util.List;

// IMPORTANTE: este é o formato de TODO erro da API. O frontend trata um só molde.
public record ErroResponse(int status, String mensagem, Instant timestamp, List<CampoErro> campos) {

    // Detalhe de um campo específico que falhou na validação (ex.: "senha" muito curta).
    public record CampoErro(String campo, String mensagem) {
    }

    // Erro comum: sem detalhe por campo. A lista vai vazia (nunca null).
    public static ErroResponse de(int status, String mensagem) {
        return new ErroResponse(status, mensagem, Instant.now(), List.of());
    }

    // Erro de validação: aponta cada campo que falhou.
    public static ErroResponse deValidacao(int status, String mensagem, List<CampoErro> campos) {
        return new ErroResponse(status, mensagem, Instant.now(), campos);
    }
}