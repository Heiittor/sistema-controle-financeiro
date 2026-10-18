package com.controlefinanceiro.api.presentation;

import com.controlefinanceiro.api.IntegracaoBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ActuatorTest extends IntegracaoBase {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void healthDevePermanecerPublicoESemDetalhes() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.components").doesNotExist());
    }

    @Test
    void metricsDeveExigirToken() throws Exception {
        mockMvc.perform(get("/actuator/metrics"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void metricsDeveFuncionarComToken() throws Exception {
        Sessao ana = cadastrarELogar(mockMvc, "Ana");

        mockMvc.perform(get("/actuator/metrics").header("Authorization", bearer(ana)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.names").isArray());
    }

    @Test
    void endpointsSensiveisNaoDevemEstarExpostos() throws Exception {
        Sessao ana = cadastrarELogar(mockMvc, "Ana");

        for (String caminho : new String[]{"/actuator/env", "/actuator/beans", "/actuator/heapdump"}) {
            mockMvc.perform(get(caminho).header("Authorization", bearer(ana)))
                    .andExpect(status().isNotFound());
        }
    }
}