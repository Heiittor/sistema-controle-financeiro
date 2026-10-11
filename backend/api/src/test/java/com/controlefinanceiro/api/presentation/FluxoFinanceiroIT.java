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
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class FluxoFinanceiroIT extends IntegracaoBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    private long usuario;
    private long conta;
    private long categoriaReceita;
    private long categoriaDespesa;

    @BeforeEach
    void prepararCenario() throws Exception {
        usuario = criar("/usuarios", null,
                "{\"nome\":\"Ana\",\"email\":\"fluxo-" + UUID.randomUUID()
                        + "@teste.com\",\"senha\":\"senha12345\"}");
        conta = criar("/contas", usuario, "{\"nome\":\"Nubank\"}");
        categoriaReceita = criar("/categorias", usuario, "{\"nome\":\"Salário\",\"tipo\":\"RECEITA\"}");
        categoriaDespesa = criar("/categorias", usuario, "{\"nome\":\"Alimentação\",\"tipo\":\"DESPESA\"}");
    }

    @Test
    void deveAtualizarOSaldoComReceitaEDespesa() throws Exception {
        registrar("receitas", categoriaReceita, "1000.00", "2026-10-01").andExpect(status().isCreated());
        registrar("despesas", categoriaDespesa, "250.50", "2026-10-02")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tipo").value("DESPESA"))
                .andExpect(jsonPath("$.categoriaNome").value("Alimentação"));

        conferir("749.50", 2);
    }

    @Test
    void deveRecusarSaldoInsuficienteSemAlterarNada() throws Exception {
        registrar("receitas", categoriaReceita, "100.00", "2026-10-01").andExpect(status().isCreated());

        registrar("despesas", categoriaDespesa, "100.01", "2026-10-02")
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.mensagem").value("Saldo insuficiente."));

        // IMPORTANTE: a recusa não deixa nada pela metade (nem saldo alterado, nem transação gravada).
        conferir("100.00", 1);
    }

    @Test
    void deveRecusarCategoriaDeTipoErradoSemGravarNada() throws Exception {
        registrar("despesas", categoriaReceita, "10.00", "2026-10-02")
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.mensagem").value("A categoria deve ser do mesmo tipo da transação."));

        conferir("0.00", 0);
    }

    @Test
    void deveRecusarValorComMaisDeDuasCasas() throws Exception {
        registrar("receitas", categoriaReceita, "10.005", "2026-10-02")
                .andExpect(status().isUnprocessableEntity());

        conferir("0.00", 0);
    }

    @Test
    void deveListarHistoricoPaginadoDoMaisRecenteParaOMaisAntigo() throws Exception {
        registrar("receitas", categoriaReceita, "1000.00", "2026-10-01").andExpect(status().isCreated());
        registrar("despesas", categoriaDespesa, "10.00", "2026-10-03").andExpect(status().isCreated());
        registrar("despesas", categoriaDespesa, "20.00", "2026-10-02").andExpect(status().isCreated());

        mockMvc.perform(get("/contas/{id}/transacoes", conta)
                        .header("X-Usuario-Id", usuario)
                        .param("inicio", "2026-10-01").param("fim", "2026-10-31")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElementos").value(3))
                .andExpect(jsonPath("$.totalPaginas").value(2))
                .andExpect(jsonPath("$.conteudo.length()").value(2))
                .andExpect(jsonPath("$.conteudo[0].data").value("2026-10-03"))
                .andExpect(jsonPath("$.conteudo[1].data").value("2026-10-02"));
    }

    @Test
    void deveLimitarOTamanhoDaPaginaEValidarOPeriodo() throws Exception {
        mockMvc.perform(get("/contas/{id}/transacoes", conta)
                        .header("X-Usuario-Id", usuario)
                        .param("inicio", "2026-10-01").param("fim", "2026-10-31")
                        .param("size", "5000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tamanho").value(100));

        mockMvc.perform(get("/contas/{id}/transacoes", conta)
                        .header("X-Usuario-Id", usuario)
                        .param("inicio", "2026-10-31").param("fim", "2026-10-01"))
                .andExpect(status().isUnprocessableEntity());
    }

    // ---- verificação no banco ----

    private void conferir(String saldoEsperado, int transacoesEsperadas) {
        BigDecimal saldo = jdbc.queryForObject(
                "SELECT saldo FROM contas WHERE id = ?", BigDecimal.class, conta);
        Integer transacoes = jdbc.queryForObject(
                "SELECT count(*) FROM transacoes WHERE conta_id = ?", Integer.class, conta);

        assertEquals(0, new BigDecimal(saldoEsperado).compareTo(saldo));
        assertEquals(transacoesEsperadas, transacoes);
    }

    // ---- helpers que usam a própria API ----

    private long criar(String rota, Long usuarioId, String corpo) throws Exception {
        var requisicao = post(rota).contentType(MediaType.APPLICATION_JSON).content(corpo);
        if (usuarioId != null) {
            requisicao.header("X-Usuario-Id", usuarioId);
        }
        String resposta = mockMvc.perform(requisicao)
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(resposta, "$.id")).longValue();
    }

    private ResultActions registrar(String rota, long categoria, String valor, String data) throws Exception {
        return mockMvc.perform(post("/contas/{id}/{rota}", conta, rota)
                .header("X-Usuario-Id", usuario)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"categoriaId\":" + categoria + ",\"descricao\":\"Teste\",\"valor\":" + valor
                        + ",\"data\":\"" + data + "\"}"));
    }
}