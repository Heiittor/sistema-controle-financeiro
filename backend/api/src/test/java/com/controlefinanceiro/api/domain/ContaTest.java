package com.controlefinanceiro.api.domain;

import com.controlefinanceiro.api.domain.exception.RegraDeNegocioException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ContaTest {

    private Usuario usuario;
    private Conta conta;

    @BeforeEach
    void criarConta() {
        usuario = new Usuario("Ana", "ana@teste.com", "senha12345");
        conta = new Conta("Nubank", usuario);
    }

    private static Dinheiro reais(String valor) {
        return new Dinheiro(new BigDecimal(valor));
    }

    @Test
    void deveNascerComSaldoZero() {
        assertEquals(Dinheiro.ZERO, conta.getSaldo());
    }

    @Test
    void deveCreditar() {
        conta.creditar(reais("100"));
        conta.creditar(reais("50.25"));

        assertEquals(reais("150.25"), conta.getSaldo());
    }

    @Test
    void deveDebitar() {
        conta.creditar(reais("100"));
        conta.debitar(reais("40"));

        assertEquals(reais("60"), conta.getSaldo());
    }

    @Test
    void devePermitirDebitarExatamenteOSaldo() {
        conta.creditar(reais("100"));
        conta.debitar(reais("100"));

        assertEquals(Dinheiro.ZERO, conta.getSaldo());
    }

    @Test
    void deveRejeitarDebitoComSaldoInsuficienteSemAlterarOSaldo() {
        conta.creditar(reais("100"));

        assertThrows(RegraDeNegocioException.class, () -> conta.debitar(reais("100.01")));
        assertEquals(reais("100"), conta.getSaldo());
    }

    @Test
    void deveRejeitarValorZeroOuNulo() {
        assertThrows(RegraDeNegocioException.class, () -> conta.creditar(Dinheiro.ZERO));
        assertThrows(RegraDeNegocioException.class, () -> conta.debitar(null));
    }

    @Test
    void deveRejeitarNomeVazioEUsuarioNulo() {
        assertThrows(RegraDeNegocioException.class, () -> new Conta("   ", usuario));
        assertThrows(RegraDeNegocioException.class, () -> new Conta("Nubank", null));
    }

    @Test
    void deveRemoverEspacosDoNome() {
        assertEquals("Nubank", new Conta("  Nubank  ", usuario).getNome());
    }
}