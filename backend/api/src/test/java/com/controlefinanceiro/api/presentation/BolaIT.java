package com.controlefinanceiro.api.presentation;

import com.controlefinanceiro.api.IntegracaoBase;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class BolaIT extends IntegracaoBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    private long usuarioA;
    private long usuarioB;
    private long contaDeA;
    private long categoriaReceitaDeA;
    private long categoriaDespesaDeA;
    private long contaDeB;

    @BeforeEach
    void prepararCenario() throws Exception {
        usuarioA = criarUsuario("Ana");
        usuarioB = criarUsuario("Beto");

        contaDeA = criarConta(usuarioA, "Nubank");
        categoriaReceitaDeA = criarCategoria(usuarioA, "Salário", "RECEITA");
        categoriaDespesaDeA = criarCategoria(usuarioA, "Alimentação", "DESPESA");
        contaDeB = criarConta(usuarioB, "Itaú");

        // A tem R$ 1000 na conta.
        registrar(usuarioA, contaDeA, "receitas", categoriaReceitaDeA, "1000.00")
                .andExpect(status().isCreated());
    }

    @Test
    void usuarioBNaoPodeRegistrarDespesaNaContaDeA() throws Exception {
        registrar(usuarioB, contaDeA, "despesas", categoriaDespesaDeA, "10.00")
                .andExpect(status().isNotFound());

        conferirContaDeAIntacta();
    }

    @Test
    void usuarioBNaoPodeRegistrarReceitaNaContaDeA() throws Exception {
        registrar(usuarioB, contaDeA, "receitas", categoriaReceitaDeA, "500.00")
                .andExpect(status().isNotFound());

        conferirContaDeAIntacta();
    }

    @Test
    void usuarioBNaoPodeLerOHistoricoDaContaDeA() throws Exception {
        mockMvc.perform(get("/contas/{id}/transacoes", contaDeA)
                        .header("X-Usuario-Id", usuarioB))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensagem").value("Conta não encontrada."));
    }

    @Test
    void usuarioBNaoPodeUsarACategoriaDeANaPropriaConta() throws Exception {
        registrar(usuarioB, contaDeB, "despesas", categoriaDespesaDeA, "10.00")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensagem").value("Categoria não encontrada."));
    }

    @Test
    void usuarioBVeApenasOsProprioDadosNasListagens() throws Exception {
        mockMvc.perform(get("/contas").header("X-Usuario-Id", usuarioB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].nome").value("Itaú"));

        mockMvc.perform(get("/categorias").header("X-Usuario-Id", usuarioB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void naoExistirELerDeOutroUsuarioTemAMesmaResposta() throws Exception {
        // Conta inexistente e conta de outro usuário devem ser indistinguíveis (não vaza quais ids existem).
        mockMvc.perform(get("/contas/{id}/transacoes", 999999999L)
                        .header("X-Usuario-Id", usuarioB))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensagem").value("Conta não encontrada."));
    }

    // ---- verificação no banco ----

    private void conferirContaDeAIntacta() {
        BigDecimal saldo = jdbc.queryForObject(
                "SELECT saldo FROM contas WHERE id = ?", BigDecimal.class, contaDeA);
        Integer transacoes = jdbc.queryForObject(
                "SELECT count(*) FROM transacoes WHERE conta_id = ?", Integer.class, contaDeA);

        assertEquals(0, new BigDecimal("1000.00").compareTo(saldo));
        assertEquals(1, transacoes);
    }

    // ---- helpers que usam a própria API ----

    private long criarUsuario(String nome) throws Exception {
        String email = "bola-" + UUID.randomUUID() + "@teste.com";
        String corpo = mockMvc.perform(post("/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"" + nome + "\",\"email\":\"" + email + "\",\"senha\":\"senha12345\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(corpo, "$.id")).longValue();
    }

    private long criarConta(long usuario, String nome) throws Exception {
        String corpo = mockMvc.perform(post("/contas")
                        .header("X-Usuario-Id", usuario)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"" + nome + "\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(corpo, "$.id")).longValue();
    }

    private long criarCategoria(long usuario, String nome, String tipo) throws Exception {
        String corpo = mockMvc.perform(post("/categorias")
                        .header("X-Usuario-Id", usuario)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"" + nome + "\",\"tipo\":\"" + tipo + "\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(corpo, "$.id")).longValue();
    }

    private org.springframework.test.web.servlet.ResultActions registrar(
            long usuario, long conta, String rota, long categoria, String valor) throws Exception {
        return mockMvc.perform(post("/contas/{id}/{rota}", conta, rota)
                .header("X-Usuario-Id", usuario)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"categoriaId\":" + categoria + ",\"descricao\":\"Teste\",\"valor\":" + valor
                        + ",\"data\":\"2026-10-05\"}"));
    }
}