package com.controlefinanceiro.api.presentation;

import com.controlefinanceiro.api.IntegracaoBase;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ResumoTest extends IntegracaoBase {

    @Autowired
    private MockMvc mockMvc;

    private Sessao ana;
    private long conta;
    private long contaDestino;
    private long salario;
    private long alimentacao;

    @BeforeEach
    void preparar() throws Exception {
        ana = cadastrarELogar(mockMvc, "Ana");
        conta = criar("/contas", "{\"nome\":\"Nubank\"}");
        contaDestino = criar("/contas", "{\"nome\":\"Itaú\"}");
        salario = criar("/categorias", "{\"nome\":\"Salário\",\"tipo\":\"RECEITA\"}");
        alimentacao = criar("/categorias", "{\"nome\":\"Alimentação\",\"tipo\":\"DESPESA\"}");
    }

    @Test
    void deveSomarReceitasDespesasEPorCategoria() throws Exception {
        registrar("receitas", salario, "1000.00", "2026-10-01");
        registrar("despesas", alimentacao, "100.50", "2026-10-02");
        registrar("despesas", alimentacao, "50.00", "2026-10-03");

        resumo(ana, conta, "2026-10-01", "2026-10-31")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalReceitas").value(1000.00))
                .andExpect(jsonPath("$.totalDespesas").value(150.50))
                .andExpect(jsonPath("$.saldoDoPeriodo").value(849.50))
                .andExpect(jsonPath("$.porCategoria.length()").value(2));
    }

    @Test
    void naoDeveContarTransferenciasNemTransacoesForaDoPeriodo() throws Exception {
        registrar("receitas", salario, "1000.00", "2026-10-01");
        registrar("receitas", salario, "500.00", "2026-11-05"); // fora do período
        mockMvc.perform(post("/transferencias")
                        .header("Authorization", bearer(ana))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"contaOrigemId\":" + conta + ",\"contaDestinoId\":" + contaDestino
                                + ",\"descricao\":\"Reserva\",\"valor\":200.00,\"data\":\"2026-10-05\"}"))
                .andExpect(status().isCreated());

        resumo(ana, conta, "2026-10-01", "2026-10-31")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalReceitas").value(1000.00))
                .andExpect(jsonPath("$.totalDespesas").value(0.00))
                .andExpect(jsonPath("$.saldoDoPeriodo").value(1000.00));
    }

    @Test
    void periodoSemMovimentoDeveDevolverZeros() throws Exception {
        resumo(ana, conta, "2026-01-01", "2026-01-31")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalReceitas").value(0.00))
                .andExpect(jsonPath("$.porCategoria.length()").value(0));
    }

    @Test
    void naoDeveResumirContaDeOutroUsuario() throws Exception {
        Sessao beto = cadastrarELogar(mockMvc, "Beto");

        resumo(beto, conta, "2026-10-01", "2026-10-31")
                .andExpect(status().isNotFound());
    }

    @Test
    void deveRecusarPeriodoInvertidoEExigirToken() throws Exception {
        resumo(ana, conta, "2026-10-31", "2026-10-01")
                .andExpect(status().isUnprocessableEntity());

        mockMvc.perform(get("/contas/{id}/resumo", conta))
                .andExpect(status().isUnauthorized());
    }

    private ResultActions resumo(Sessao sessao, long contaId, String inicio, String fim) throws Exception {
        return mockMvc.perform(get("/contas/{id}/resumo", contaId)
                .header("Authorization", bearer(sessao))
                .param("inicio", inicio).param("fim", fim));
    }

    private void registrar(String rota, long categoria, String valor, String data) throws Exception {
        mockMvc.perform(post("/contas/{id}/{rota}", conta, rota)
                        .header("Authorization", bearer(ana))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"categoriaId\":" + categoria + ",\"descricao\":\"Teste\",\"valor\":" + valor
                                + ",\"data\":\"" + data + "\"}"))
                .andExpect(status().isCreated());
    }

    private long criar(String rota, String corpo) throws Exception {
        String resposta = mockMvc.perform(post(rota)
                        .header("Authorization", bearer(ana))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(resposta, "$.id")).longValue();
    }
}