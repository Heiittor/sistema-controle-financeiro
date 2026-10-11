package com.controlefinanceiro.api;

import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.boot.test.context.TestConfiguration;
import org.testcontainers.containers.PostgreSQLContainer;

// Base de todos os testes de integração: aplicação inteira + PostgreSQL real em container.
@SpringBootTest(properties = "app.jwt.secret=chave-falsa-apenas-para-testes-0123456789")@AutoConfigureMockMvc
@Import(IntegracaoBase.ConfiguracaoContainer.class)
public abstract class IntegracaoBase {

    // IMPORTANTE: o container é um bean do Spring. Quem o inicia e encerra é o próprio contexto,
    // e como o contexto é reaproveitado entre as classes, o container vive durante toda a suíte.
    @TestConfiguration(proxyBeanMethods = false)
    static class ConfiguracaoContainer {

        @Bean
        @ServiceConnection
        PostgreSQLContainer<?> postgres() {
            return new PostgreSQLContainer<>("postgres:16-alpine");
        }
    }
}