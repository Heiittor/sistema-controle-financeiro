
package com.controlefinanceiro.api;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

// Base de todos os testes de integração: aplicação inteira + PostgreSQL real em container.
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
public abstract class IntegracaoBase {

    // Um único container compartilhado pelos testes.
    // @ServiceConnection configura automaticamente a conexão com o PostgreSQL.
    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine");
}