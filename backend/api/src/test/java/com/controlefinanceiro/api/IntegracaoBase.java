package com.controlefinanceiro.api;

import com.jayway.jsonpath.JsonPath;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
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
    // Usuário de teste já logado: id (para SQL) e token (para as chamadas).
    public record Sessao(long id, String token) {
    }

    // Cadastra um usuário com e-mail único, faz login e devolve id + token.
    protected static Sessao cadastrarELogar(MockMvc mockMvc, String nome) throws Exception {
        String email = "teste-" + java.util.UUID.randomUUID() + "@teste.com";

        String cadastro = mockMvc.perform(post("/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"" + nome + "\",\"email\":\"" + email
                                + "\",\"senha\":\"senha12345\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long id = ((Number) JsonPath.read(cadastro, "$.id")).longValue();

        String login = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"senha\":\"senha12345\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String token = JsonPath.read(login, "$.token");

        return new Sessao(id, token);
    }

    protected static String bearer(Sessao sessao) {
        return "Bearer " + sessao.token();
    }
}