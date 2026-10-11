package com.controlefinanceiro.api.domain.exception;

// Lançada quando algo pedido não existe OU não pertence ao usuário (vira 404).
// IMPORTANTE: para o cliente, "não existe" e "é de outra pessoa" têm a mesma resposta.
// Assim ninguém descobre quais ids existem no sistema.
public class RecursoNaoEncontradoException extends RuntimeException {

    public RecursoNaoEncontradoException(String mensagem) {
        super(mensagem);
    }
}