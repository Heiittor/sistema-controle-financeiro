package com.controlefinanceiro.api.domain.exception;

// Lançada quando e-mail ou senha não conferem no login (vira 401).
public class CredenciaisInvalidasException extends RuntimeException {

    public CredenciaisInvalidasException(String mensagem) {
        super(mensagem);
    }
}