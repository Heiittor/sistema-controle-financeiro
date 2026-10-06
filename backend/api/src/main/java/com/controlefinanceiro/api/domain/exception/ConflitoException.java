package com.controlefinanceiro.api.domain.exception;

// Lançada quando a operação conflita com algo que já existe (vira 409).
// Exemplos: e-mail já cadastrado, conta ou categoria com nome repetido.
public class ConflitoException extends RuntimeException {

    public ConflitoException(String mensagem) {
        super(mensagem);
    }
}