package com.controlefinanceiro.api.domain.exception;

// Lançada quando os dados são válidos no formato, mas o negócio não permite (vira 422).
// Exemplos: saldo insuficiente, categoria de tipo errado, valor zero.
// IMPORTANTE: estende RuntimeException para o @Transactional fazer rollback sozinho.
public class RegraDeNegocioException extends RuntimeException {

    public RegraDeNegocioException(String mensagem) {
        super(mensagem);
    }
}