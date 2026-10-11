package com.controlefinanceiro.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ContextoIntegracaoTest extends IntegracaoBase {

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void deveSubirComFlywayAplicado() {
        Integer migracoes = jdbc.queryForObject(
                "SELECT count(*) FROM flyway_schema_history WHERE success = true", Integer.class);

        assertEquals(4, migracoes);
    }
}