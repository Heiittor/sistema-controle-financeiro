package com.controlefinanceiro.api.domain.exception;

// Lançada quando há tentativas de login demais (vira 429 com Retry-After).
public class TentativasExcedidasException extends RuntimeException {

    private final long segundosParaLiberar;

    public TentativasExcedidasException(long segundosParaLiberar) {
        super("Muitas tentativas de login.");
        // Nunca menos de 1: Retry-After 0 faria o cliente repetir em loop.
        this.segundosParaLiberar = Math.max(1, segundosParaLiberar);
    }

    public long getSegundosParaLiberar() {
        return segundosParaLiberar;
    }
}