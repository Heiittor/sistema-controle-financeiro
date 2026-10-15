package com.controlefinanceiro.api.presentation;

import com.controlefinanceiro.api.IntegracaoBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DocumentacaoTest extends IntegracaoBase {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void especificacaoOpenApiDeveSerPublicaEListarOsEndpoints() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("Sistema de Controle Financeiro"))
                .andExpect(jsonPath("$.paths['/transferencias']").exists())
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme").value("bearer"));
    }
}