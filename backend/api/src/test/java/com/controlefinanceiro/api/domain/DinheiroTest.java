package com.controlefinanceiro.api.domain;

import com.controlefinanceiro.api.domain.exception.RegraDeNegocioException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DinheiroTest {

    @Test
    void deveNormalizarEscalaParaDuasCasas() {
        Dinheiro a = new Dinheiro(new BigDecimal("10.0"));
        Dinheiro b = new Dinheiro(new BigDecimal("10.00"));

        assertEquals(a, b);
        assertEquals("10.00", a.valor().toString());
    }

    @Test
    void deveRejeitarValorNulo() {
        assertThrows(RegraDeNegocioException.class, () -> new Dinheiro(null));
    }

    @Test
    void deveRejeitarValorNegativo() {
        assertThrows(RegraDeNegocioException.class, () -> new Dinheiro(new BigDecimal("-0.01")));
    }

    @Test
    void deveRejeitarMaisDeDuasCasasDecimais() {
        assertThrows(RegraDeNegocioException.class, () -> new Dinheiro(new BigDecimal("10.005")));
    }

    @Test
    void deveSomar() {
        Dinheiro resultado = new Dinheiro(new BigDecimal("10.50")).somar(new Dinheiro(new BigDecimal("0.50")));

        assertEquals(new Dinheiro(new BigDecimal("11")), resultado);
    }

    @Test
    void deveRejeitarSubtracaoQueResultaEmNegativo() {
        Dinheiro cinco = new Dinheiro(new BigDecimal("5"));
        Dinheiro dez = new Dinheiro(new BigDecimal("10"));

        assertThrows(RegraDeNegocioException.class, () -> cinco.subtrair(dez));
    }

    @Test
    void deveCompararValores() {
        assertTrue(new Dinheiro(new BigDecimal("10")).maiorQue(new Dinheiro(new BigDecimal("9.99"))));
        assertTrue(Dinheiro.ZERO.ehZero());
    }
}