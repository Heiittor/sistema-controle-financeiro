package com.controlefinanceiro.api.infrastructure.seguranca;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LimitadorTentativasLoginTest {

    private static final String CHAVE = "ana@teste.com|10.0.0.1";

    private RelogioControlavel relogio;
    private LimitadorTentativasLogin limitador;

    // Relógio falso: o teste decide quando o tempo passa.
    static class RelogioControlavel extends Clock {
        private Instant agora = Instant.parse("2026-10-11T12:00:00Z");

        void avancar(Duration d) {
            agora = agora.plus(d);
        }

        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return agora;
        }
    }

    @BeforeEach
    void preparar() {
        relogio = new RelogioControlavel();
        limitador = new LimitadorTentativasLogin(relogio);
    }

    private void falhar(int vezes) {
        for (int i = 0; i < vezes; i++) {
            limitador.registrarFalha(CHAVE);
        }
    }

    @Test
    void naoBloqueiaAbaixoDoLimite() {
        falhar(4);

        assertTrue(limitador.tempoDeBloqueio(CHAVE).isEmpty());
    }

    @Test
    void bloqueiaNaQuintaFalha() {
        falhar(5);

        assertTrue(limitador.tempoDeBloqueio(CHAVE).isPresent());
    }

    @Test
    void informaQuantoFaltaParaLiberar() {
        falhar(5);
        relogio.avancar(Duration.ofMinutes(5));

        assertEquals(Duration.ofMinutes(10), limitador.tempoDeBloqueio(CHAVE).orElseThrow());
    }

    @Test
    void liberaDepoisDaJanela() {
        falhar(5);
        relogio.avancar(Duration.ofMinutes(15));

        assertTrue(limitador.tempoDeBloqueio(CHAVE).isEmpty());
    }

    @Test
    void loginCorretoZeraOContador() {
        falhar(5);
        limitador.limpar(CHAVE);

        assertTrue(limitador.tempoDeBloqueio(CHAVE).isEmpty());
    }

    @Test
    void chavesDiferentesNaoSeInfluenciam() {
        falhar(5);

        assertTrue(limitador.tempoDeBloqueio("outra@teste.com|10.0.0.1").isEmpty());
    }

    @Test
    void falhasAntigasSaemDaJanelaGradualmente() {
        falhar(3);
        relogio.avancar(Duration.ofMinutes(10));
        falhar(2); // 5 falhas no total, mas 3 delas vão expirar em 5 minutos

        assertTrue(limitador.tempoDeBloqueio(CHAVE).isPresent());

        relogio.avancar(Duration.ofMinutes(5)); // as 3 primeiras completam 15 minutos

        assertTrue(limitador.tempoDeBloqueio(CHAVE).isEmpty());
    }
}