package com.controlefinanceiro.api.domain;

import jakarta.persistence.Embeddable;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Embeddable
public record Dinheiro(BigDecimal valor) {

    public static final Dinheiro ZERO = new Dinheiro(BigDecimal.ZERO);

    // Construtor compacto: roda sempre que um Dinheiro é criado.
    public Dinheiro {
        if (valor == null) {
            throw new IllegalArgumentException("O valor monetário é obrigatório.");
        }
        try {
            valor = valor.setScale(2, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException e) {
            throw new IllegalArgumentException("O valor deve ter no máximo 2 casas decimais.", e);
        }
        if (valor.signum() < 0) {
            throw new IllegalArgumentException("O valor monetário não pode ser negativo.");
        }
    }

    public Dinheiro somar(Dinheiro outro) {
        return new Dinheiro(this.valor.add(outro.valor));
    }

    public Dinheiro subtrair(Dinheiro outro) {
        return new Dinheiro(this.valor.subtract(outro.valor));
    }

    public boolean maiorQue(Dinheiro outro) {
        return this.valor.compareTo(outro.valor) > 0;
    }

    public boolean ehZero() {
        return this.valor.signum() == 0;
    }
}