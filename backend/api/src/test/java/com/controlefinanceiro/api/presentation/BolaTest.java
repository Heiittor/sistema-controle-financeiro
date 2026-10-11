package com.controlefinanceiro.api.presentation;

import com.controlefinanceiro.api.IntegracaoBase;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class BolaTest extends IntegracaoBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    private Sessao a;
    private Sessao b;
    private long contaDeA;
    private long categoriaReceitaDeA;
    private long categoriaDespesaDeA;
    private long contaDeB;

    @BeforeEach
    void prepararCenario() throws Exception {
        a = cadastrarELogar(mockMvc, "Ana");
        b = cadastrarELogar(mockMvc, "Beto");

        contaDeA = criar("/contas", a, "{\"nome\":\"Nubank\"}");
        categoriaReceitaDeA = criar("/categorias", a, "{\"nome\":\"Salário\",\"tipo\":\"RECEITA\"}");
        categoriaDespesaDeA = criar("/categorias", a, "{\"nome\":\"Alimentação\",\"tipo\":\"DESPESA\"}");
        contaDeB = criar("/contas", b, "{\"nome\":\"Itaú\"}");

        registrar(a, contaDeA, "receitas", categoriaReceitaDeA, "1000.00").andExpect(status().isCreated());
    }

    @Test
    void usuarioBNaoPodeRegistrarDespesaNaContaDeA() throws Exception {
        registrar(b, contaDeA, "despesas", categoriaDespesaDeA, "10.00").andExpect(status().isNotFound());
        conferirContaDeAIntacta();
    }

    @Test
    void usuarioBNaoPodeRegistrarReceitaNaContaDeA() throws Exception {
        registrar(b, contaDeA, "receitas", categoriaReceitaDeA, "500.00").andExpect(status().isNotFound());
        conferirContaDeAIntacta();
    }

    @Test
    void usuarioBNaoPodeLerOHistoricoDaContaDeA() throws Exception {
        mockMvc.perform(get("/contas/{id}/transacoes", contaDeA).header("Authorization", bearer(b)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensagem").value("Conta não encontrada."));
    }

    @Test
    void usuarioBNaoPodeUsarACategoriaDeANaPropriaConta() throws Exception {
        registrar(b, contaDeB, "despesas", categoriaDespesaDeA, "10.00")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensagem").value("Categoria não encontrada."));
    }

    @Test
    void usuarioBVeApenasOsProprioDadosNasListagens() throws Exception {
        mockMvc.perform(get("/contas").header("Authorization", bearer(b)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].nome").value("Itaú"));

        mockMvc.perform(get("/categorias").header("Authorization", bearer(b)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void naoExistirELerDeOutroUsuarioTemAMesmaResposta() throws Exception {
        mockMvc.perform(get("/contas/{id}/transacoes", 999999999L).header("Authorization", bearer(b)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensagem").value("Conta não encontrada."));
    }

    private void conferirContaDeAIntacta() {
        BigDecimal saldo = jdbc.queryForObject(
                "SELECT saldo FROM contas WHERE id = ?", BigDecimal.class, contaDeA);
        Integer transacoes = jdbc.queryForObject(
                "SELECT count(*) FROM transacoes WHERE conta_id = ?", Integer.class, contaDeA);

        assertEquals(0, new BigDecimal("1000.00").compareTo(saldo));
        assertEquals(1, transacoes);
    }

    private long criar(String rota, Sessao sessao, String corpo) throws Exception {
        String resposta = mockMvc.perform(post(rota)
                        .header("Authorization", bearer(sessao))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(resposta, "$.id")).longValue();
    }

    private ResultActions registrar(Sessao sessao, long conta, String rota, long categoria, String valor)
            throws Exception {
        return mockMvc.perform(post("/contas/{id}/{rota}", conta, rota)
                .header("Authorization", bearer(sessao))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"categoriaId\":" + categoria + ",\"descricao\":\"Teste\",\"valor\":" + valor
                        + ",\"data\":\"2026-10-05\"}"));
    }
}