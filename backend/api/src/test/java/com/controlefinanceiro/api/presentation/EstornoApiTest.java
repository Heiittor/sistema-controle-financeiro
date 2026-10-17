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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class EstornoApiTest extends IntegracaoBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    private Sessao ana;
    private long conta;
    private long outraConta;
    private long salario;
    private long alimentacao;
    private long receitaInicial;

    @BeforeEach
    void preparar() throws Exception {
        ana = cadastrarELogar(mockMvc, "Ana");
        conta = criar("/contas", ana, "{\"nome\":\"Nubank\"}");
        outraConta = criar("/contas", ana, "{\"nome\":\"Itaú\"}");
        salario = criar("/categorias", ana, "{\"nome\":\"Salário\",\"tipo\":\"RECEITA\"}");
        alimentacao = criar("/categorias", ana, "{\"nome\":\"Alimentação\",\"tipo\":\"DESPESA\"}");
        receitaInicial = registrar("receitas", salario, "1000.00", "2026-10-01");
    }

    // ---------- estorno ----------

    @Test
    void deveEstornarDespesaEDevolverOSaldo() throws Exception {
        long despesa = registrar("despesas", alimentacao, "100.00", "2026-10-02");
        conferirSaldo(conta, "900.00");

        estornar(ana, conta, despesa)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tipo").value("ESTORNO_DESPESA"))
                .andExpect(jsonPath("$.estornoDeId").value(despesa))
                .andExpect(jsonPath("$.descricao").value("Estorno: Teste"))
                .andExpect(jsonPath("$.valor").value(100.00));

        conferirSaldo(conta, "1000.00");
    }

    @Test
    void deveEstornarReceitaDebitandoAConta() throws Exception {
        estornar(ana, conta, receitaInicial)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tipo").value("ESTORNO_RECEITA"));

        conferirSaldo(conta, "0.00");
    }

    @Test
    void deveRecusarEstornoDeReceitaJaGastaSemAlterarNada() throws Exception {
        registrar("despesas", alimentacao, "100.00", "2026-10-02");

        estornar(ana, conta, receitaInicial)
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.mensagem").value("Saldo insuficiente."));

        conferirSaldo(conta, "900.00");
        assertEquals(0, contarEstornos());
    }

    @Test
    void deveRecusarEstornoDuplicado() throws Exception {
        long despesa = registrar("despesas", alimentacao, "100.00", "2026-10-02");
        estornar(ana, conta, despesa).andExpect(status().isCreated());

        estornar(ana, conta, despesa)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensagem").value("Esta transação já foi estornada."));

        conferirSaldo(conta, "1000.00");
        assertEquals(1, contarEstornos());
    }

    @Test
    void deveRecusarEstornarTransferencia() throws Exception {
        transferir(ana, conta, outraConta, "100.00").andExpect(status().isCreated());
        long saida = jdbc.queryForObject(
                "SELECT id FROM transacoes WHERE conta_id = ? AND tipo = 'TRANSFERENCIA_SAIDA'",
                Long.class, conta);

        estornar(ana, conta, saida).andExpect(status().isUnprocessableEntity());

        conferirSaldo(conta, "900.00");
        conferirSaldo(outraConta, "100.00");
        assertEquals(0, contarEstornos());
    }

    @Test
    void deveRecusarEstornarUmEstorno() throws Exception {
        long despesa = registrar("despesas", alimentacao, "100.00", "2026-10-02");
        String resposta = estornar(ana, conta, despesa)
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long estorno = ((Number) JsonPath.read(resposta, "$.id")).longValue();

        estornar(ana, conta, estorno).andExpect(status().isUnprocessableEntity());

        conferirSaldo(conta, "1000.00");
        assertEquals(1, contarEstornos());
    }

    // ---------- BOLA ----------

    @Test
    void usuarioBNaoPodeEstornarNemEditarTransacaoDeA() throws Exception {
        Sessao beto = cadastrarELogar(mockMvc, "Beto");
        long despesa = registrar("despesas", alimentacao, "100.00", "2026-10-02");
        long categoriaDoBeto = criar("/categorias", beto, "{\"nome\":\"Mercado\",\"tipo\":\"DESPESA\"}");

        estornar(beto, conta, despesa)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensagem").value("Conta não encontrada."));
        editar(beto, conta, despesa, categoriaDoBeto, "Invadida")
                .andExpect(status().isNotFound());

        conferirSaldo(conta, "900.00");
        assertEquals(0, contarEstornos());
        assertEquals("Teste", jdbc.queryForObject(
                "SELECT descricao FROM transacoes WHERE id = ?", String.class, despesa));
    }

    @Test
    void naoDeveEstornarTransacaoUsandoAContaErradaDaUrl() throws Exception {
        long despesa = registrar("despesas", alimentacao, "100.00", "2026-10-02");

        // A transação é da "Nubank", mas a URL aponta para a "Itaú" (também da Ana).
        estornar(ana, outraConta, despesa)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensagem").value("Transação não encontrada."));

        conferirSaldo(conta, "900.00");
    }

    @Test
    void deveExigirToken() throws Exception {
        mockMvc.perform(post("/contas/{c}/transacoes/{t}/estorno", conta, receitaInicial))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(patch("/contas/{c}/transacoes/{t}", conta, receitaInicial)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"categoriaId\":" + salario + ",\"descricao\":\"X\"}"))
                .andExpect(status().isUnauthorized());
    }

    // ---------- edição ----------

    @Test
    void deveEditarDescricaoECategoriaSemMexerNoSaldo() throws Exception {
        long freelance = criar("/categorias", ana, "{\"nome\":\"Freelance\",\"tipo\":\"RECEITA\"}");

        editar(ana, conta, receitaInicial, freelance, "  Pagamento extra  ")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.descricao").value("Pagamento extra"))
                .andExpect(jsonPath("$.categoriaNome").value("Freelance"))
                .andExpect(jsonPath("$.valor").value(1000.00));

        conferirSaldo(conta, "1000.00");
    }

    @Test
    void deveRecusarEdicaoComCategoriaDeTipoErrado() throws Exception {
        editar(ana, conta, receitaInicial, alimentacao, "Novo")
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.mensagem").value("A categoria deve ser do mesmo tipo da transação."));
    }

    @Test
    void deveRecusarEdicaoComCategoriaDeOutroUsuario() throws Exception {
        Sessao beto = cadastrarELogar(mockMvc, "Beto");
        long categoriaDoBeto = criar("/categorias", beto, "{\"nome\":\"Bônus\",\"tipo\":\"RECEITA\"}");

        editar(ana, conta, receitaInicial, categoriaDoBeto, "Novo")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensagem").value("Categoria não encontrada."));
    }

    @Test
    void deveRecusarEditarTransferencia() throws Exception {
        transferir(ana, conta, outraConta, "100.00").andExpect(status().isCreated());
        long saida = jdbc.queryForObject(
                "SELECT id FROM transacoes WHERE conta_id = ? AND tipo = 'TRANSFERENCIA_SAIDA'",
                Long.class, conta);

        editar(ana, conta, saida, salario, "Novo").andExpect(status().isUnprocessableEntity());
    }

    // ---------- resumo ----------

    @Test
    void resumoNaoDeveContarDespesaEstornada() throws Exception {
        long despesa = registrar("despesas", alimentacao, "100.00", "2026-10-02");
        estornar(ana, conta, despesa).andExpect(status().isCreated());

        resumo()
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalReceitas").value(1000.00))
                .andExpect(jsonPath("$.totalDespesas").value(0.00))
                .andExpect(jsonPath("$.saldoDoPeriodo").value(1000.00))
                .andExpect(jsonPath("$.porCategoria.length()").value(1));
    }

    @Test
    void resumoNaoDeveContarReceitaEstornada() throws Exception {
        estornar(ana, conta, receitaInicial).andExpect(status().isCreated());

        resumo()
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalReceitas").value(0.00))
                .andExpect(jsonPath("$.saldoDoPeriodo").value(0.00))
                .andExpect(jsonPath("$.porCategoria.length()").value(0));
    }

    // ---------- apoio ----------

    private ResultActions estornar(Sessao sessao, long contaId, long transacao) throws Exception {
        return mockMvc.perform(post("/contas/{c}/transacoes/{t}/estorno", contaId, transacao)
                .header("Authorization", bearer(sessao)));
    }

    private ResultActions editar(Sessao sessao, long contaId, long transacao,
                                 long categoria, String descricao) throws Exception {
        return mockMvc.perform(patch("/contas/{c}/transacoes/{t}", contaId, transacao)
                .header("Authorization", bearer(sessao))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"categoriaId\":" + categoria + ",\"descricao\":\"" + descricao + "\"}"));
    }

    private ResultActions transferir(Sessao sessao, long origem, long destino, String valor) throws Exception {
        return mockMvc.perform(post("/transferencias")
                .header("Authorization", bearer(sessao))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"contaOrigemId\":" + origem + ",\"contaDestinoId\":" + destino
                        + ",\"descricao\":\"Reserva\",\"valor\":" + valor + ",\"data\":\"2026-10-05\"}"));
    }

    private ResultActions resumo() throws Exception {
        return mockMvc.perform(get("/contas/{c}/resumo", conta)
                .header("Authorization", bearer(ana))
                .param("inicio", "2026-10-01").param("fim", "2026-10-31"));
    }

    private void conferirSaldo(long contaId, String esperado) {
        BigDecimal saldo = jdbc.queryForObject(
                "SELECT saldo FROM contas WHERE id = ?", BigDecimal.class, contaId);
        assertEquals(0, new BigDecimal(esperado).compareTo(saldo));
    }

    private int contarEstornos() {
        return jdbc.queryForObject(
                "SELECT count(*) FROM transacoes WHERE conta_id IN (?, ?) AND estorno_de_id IS NOT NULL",
                Integer.class, conta, outraConta);
    }

    // Registra receita ou despesa na conta principal e devolve o id da transação.
    private long registrar(String rota, long categoria, String valor, String data) throws Exception {
        String resposta = mockMvc.perform(post("/contas/{id}/{rota}", conta, rota)
                        .header("Authorization", bearer(ana))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"categoriaId\":" + categoria + ",\"descricao\":\"Teste\",\"valor\":"
                                + valor + ",\"data\":\"" + data + "\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(resposta, "$.id")).longValue();
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