package com.controlefinanceiro.api.infrastructure;

import com.controlefinanceiro.api.IntegracaoBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Clock;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RelogioConfigTest extends IntegracaoBase {

    @Autowired
    private Clock relogio;

    @Test
    void deveUsarOFusoDeNegocioEmVezDoFusoDoServidor() {
        assertEquals(ZoneId.of("America/Sao_Paulo"), relogio.getZone());
    }
}