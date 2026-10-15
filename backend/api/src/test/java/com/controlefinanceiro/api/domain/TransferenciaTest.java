package com.controlefinanceiro.api.domain;

import com.controlefinanceiro.api.domain.exception.RegraDeNegocioException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TransferenciaTest {

    private static final LocalDate DATA = LocalDate.of(2026, 10, 5);

    private Conta origem;
    private Conta destino;

    private static Dinheiro reais(String valor) {
        return new Dinheiro(new BigDecimal(valor));
    }

    @BeforeEach
    void preparar() {
        Usuario usuario = new Usuario("Ana", "ana@teste.com", "hash");
        origem = new Conta("Nubank", usuario);
        destino = new Conta("Itaú", usuario);
        origem.creditar(reais("100"));
    }

    @Test
    void deveMoverODinheiroEntreAsContas() {
        origem.transferirPara(destino, "Reserva", reais("40"), DATA);

        assertEquals(reais("60"), origem.getSaldo());
        assertEquals(reais("40"), destino.getSaldo());
    }

    @Test
    void deveCriarDuasTransacoesLigadasPeloMesmoId() {
        var par = origem.transferirPara(destino, "Reserva", reais("40"), DATA);

        assertEquals(TipoTransacao.TRANSFERENCIA_SAIDA, par.saida().getTipo());
        assertEquals(TipoTransacao.TRANSFERENCIA_ENTRADA, par.entrada().getTipo());
        assertNotNull(par.saida().getTransferenciaId());
        assertEquals(par.saida().getTransferenciaId(), par.entrada().getTransferenciaId());
    }

    @Test
    void deveRecusarSaldoInsuficienteSemAlterarNada() {
        assertThrows(RegraDeNegocioException.class,
                () -> origem.transferirPara(destino, "Reserva", reais("100.01"), DATA));

        assertEquals(reais("100"), origem.getSaldo());
        assertEquals(Dinheiro.ZERO, destino.getSaldo());
    }

    @Test
    void deveRecusarMesmaConta() {
        assertThrows(RegraDeNegocioException.class,
                () -> origem.transferirPara(origem, "Reserva", reais("10"), DATA));
    }

    @Test
    void deveRecusarDestinoNulo() {
        assertThrows(RegraDeNegocioException.class,
                () -> origem.transferirPara(null, "Reserva", reais("10"), DATA));
    }

    @Test
    void deveRecusarValorZeroEDescricaoVazia() {
        assertThrows(RegraDeNegocioException.class,
                () -> origem.transferirPara(destino, "Reserva", Dinheiro.ZERO, DATA));
        assertThrows(RegraDeNegocioException.class,
                () -> origem.transferirPara(destino, "  ", reais("10"), DATA));
        assertEquals(reais("100"), origem.getSaldo());
    }
}