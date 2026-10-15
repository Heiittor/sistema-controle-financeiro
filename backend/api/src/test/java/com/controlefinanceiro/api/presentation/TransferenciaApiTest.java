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

class TransferenciaApiTest extends IntegracaoBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    private Sessao ana;
    private Sessao beto;
    private long contaA1;
    private long contaA2;
    private long contaDoBeto;

    @BeforeEach
    void prepararCenario() throws Exception {
        ana = cadastrarELogar(mockMvc, "Ana");
        beto = cadastrarELogar(mockMvc, "Beto");

        contaA1 = criar("/contas", ana, "{\"nome\":\"Nubank\"}");
        contaA2 = criar("/contas", ana, "{\"nome\":\"Itaú\"}");
        contaDoBeto = criar("/contas", beto, "{\"nome\":\"Inter\"}");
        long categoria = criar("/categorias", ana, "{\"nome\":\"Salário\",\"tipo\":\"RECEITA\"}");

        mockMvc.perform(post("/contas/{id}/receitas", contaA1)
                        .header("Authorization", bearer(ana))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"categoriaId\":" + categoria
                                + ",\"descricao\":\"Salario\",\"valor\":100.00,\"data\":\"2026-10-01\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    void deveTransferirEntreContasDoMesmoUsuario() throws Exception {
        transferir(ana, contaA1, contaA2, "40.00")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.transferenciaId").isNotEmpty())
                .andExpect(jsonPath("$.contaOrigemId").value(contaA1))
                .andExpect(jsonPath("$.contaDestinoId").value(contaA2));

        conferirSaldo(contaA1, "60.00");
        conferirSaldo(contaA2, "40.00");
        assertEquals(2, contarTransferencias());
    }

    @Test
    void deveAparecerNoHistoricoDasDuasContasSemCategoria() throws Exception {
        transferir(ana, contaA1, contaA2, "40.00").andExpect(status().isCreated());

        mockMvc.perform(get("/contas/{id}/transacoes", contaA1)
                        .header("Authorization", bearer(ana))
                        .param("inicio", "2026-10-01").param("fim", "2026-10-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conteudo[?(@.tipo=='TRANSFERENCIA_SAIDA')].categoriaId").value(
                        org.hamcrest.Matchers.contains((Object) null)));

        mockMvc.perform(get("/contas/{id}/transacoes", contaA2)
                        .header("Authorization", bearer(ana))
                        .param("inicio", "2026-10-01").param("fim", "2026-10-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElementos").value(1))
                .andExpect(jsonPath("$.conteudo[0].tipo").value("TRANSFERENCIA_ENTRADA"));
    }

    @Test
    void deveRecusarSaldoInsuficienteSemAlterarNada() throws Exception {
        transferir(ana, contaA1, contaA2, "100.01")
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.mensagem").value("Saldo insuficiente."));

        conferirSaldo(contaA1, "100.00");
        conferirSaldo(contaA2, "0.00");
        assertEquals(0, contarTransferencias());
    }

    @Test
    void deveRecusarMesmaConta() throws Exception {
        transferir(ana, contaA1, contaA1, "10.00")
                .andExpect(status().isUnprocessableEntity());

        conferirSaldo(contaA1, "100.00");
        assertEquals(0, contarTransferencias());
    }

    @Test
    void naoDevePoderTransferirParaContaDeOutroUsuario() throws Exception {
        transferir(ana, contaA1, contaDoBeto, "10.00")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensagem").value("Conta não encontrada."));

        conferirSaldo(contaA1, "100.00");
        conferirSaldo(contaDoBeto, "0.00");
        assertEquals(0, contarTransferencias());
    }

    @Test
    void naoDevePoderTransferirDaContaDeOutroUsuario() throws Exception {
        transferir(beto, contaA1, contaDoBeto, "10.00")
                .andExpect(status().isNotFound());

        conferirSaldo(contaA1, "100.00");
        assertEquals(0, contarTransferencias());
    }

    @Test
    void deveRecusarValorInvalido() throws Exception {
        transferir(ana, contaA1, contaA2, "0").andExpect(status().isBadRequest());
        transferir(ana, contaA1, contaA2, "10.005").andExpect(status().isUnprocessableEntity());

        conferirSaldo(contaA1, "100.00");
        assertEquals(0, contarTransferencias());
    }

    @Test
    void deveExigirToken() throws Exception {
        mockMvc.perform(post("/transferencias")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo(contaA1, contaA2, "10.00")))
                .andExpect(status().isUnauthorized());
    }

    // ---- apoio ----

    private ResultActions transferir(Sessao sessao, long origem, long destino, String valor) throws Exception {
        return mockMvc.perform(post("/transferencias")
                .header("Authorization", bearer(sessao))
                .contentType(MediaType.APPLICATION_JSON)
                .content(corpo(origem, destino, valor)));
    }

    private String corpo(long origem, long destino, String valor) {
        return "{\"contaOrigemId\":" + origem + ",\"contaDestinoId\":" + destino
                + ",\"descricao\":\"Reserva\",\"valor\":" + valor + ",\"data\":\"2026-10-05\"}";
    }

    private void conferirSaldo(long conta, String esperado) {
        BigDecimal saldo = jdbc.queryForObject(
                "SELECT saldo FROM contas WHERE id = ?", BigDecimal.class, conta);
        assertEquals(0, new BigDecimal(esperado).compareTo(saldo));
    }

    // Linhas de transferência nas contas deste cenário (cada transferência gera 2).
    private int contarTransferencias() {
        return jdbc.queryForObject(
                "SELECT count(*) FROM transacoes WHERE transferencia_id IS NOT NULL "
                        + "AND conta_id IN (?, ?, ?)", Integer.class, contaA1, contaA2, contaDoBeto);
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
}