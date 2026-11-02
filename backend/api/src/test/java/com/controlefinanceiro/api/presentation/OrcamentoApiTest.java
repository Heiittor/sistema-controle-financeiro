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
import java.util.concurrent.Executors;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class OrcamentoApiTest extends IntegracaoBase {
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired jakarta.persistence.EntityManagerFactory emf;
    Sessao ana, beto;
    long conta, outra, alimentacao, transporte, saude, receita;
    @BeforeEach void preparar() throws Exception {
        ana = cadastrarELogar(mvc, "Ana");
        beto = cadastrarELogar(mvc, "Beto");
        conta = criar("/contas", ana, "{\"nome\":\"Principal\"}");
        outra = criar("/contas", ana, "{\"nome\":\"Reserva\"}");
        alimentacao = categoria("Alimentação", "DESPESA");
        transporte = categoria("Transporte", "DESPESA");
        saude = categoria("Saúde", "DESPESA");
        receita = categoria("Salário", "RECEITA");
        criar("/contas/" + conta + "/receitas", ana,
                "{\"categoriaId\":%d,\"descricao\":\"Salário\",\"valor\":5000,\"data\":\"2026-10-01\"}".formatted(receita));
    }
    @Test void devePersistirEditarESemAlterarSaldoOuIdentidade() throws Exception {
        long id = criar("/orcamentos", ana, corpo(alimentacao, "2026-10", "1200"));
        mvc.perform(patch("/orcamentos/{id}", id).header("Authorization", bearer(ana))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"limite\":1206.35}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.limite").value(1206.35))
                .andExpect(jsonPath("$.categoriaId").value(alimentacao))
                .andExpect(jsonPath("$.mes").value("2026-10"));
        listar(ana, "2026-10").andExpect(status().isOk())
                .andExpect(jsonPath("$.conteudo[0].limite").value(1206.35))
                .andExpect(jsonPath("$.conteudo[0].gasto").value(0));
        assertEquals(0, new BigDecimal("5000").compareTo(jdbc.queryForObject(
                "select saldo from contas where id = ?", BigDecimal.class, conta)));
        assertEquals(2, jdbc.queryForObject("select count(*) from auditoria where usuario_id = ? and entidade = 'ORCAMENTO'",
                Integer.class, ana.id()));
    }
    @Test void deveResumirTodoOMesMesmoComPaginaDeUmaLinhaESemNMaisUm() throws Exception {
        criar("/orcamentos", ana, corpo(alimentacao, "2026-10", "1200"));
        criar("/orcamentos", ana, corpo(transporte, "2026-10", "500"));
        criar("/orcamentos", ana, corpo(saude, "2026-10", "300"));
        despesa(conta, alimentacao, "960", "2026-10-15");
        despesa(conta, transporte, "325", "2026-10-15");
        despesa(conta, saude, "350", "2026-10-15");
        var stats = emf.unwrap(org.hibernate.SessionFactory.class).getStatistics();
        boolean anterior = stats.isStatisticsEnabled();
        stats.setStatisticsEnabled(true);
        try {
            long antes = stats.getPrepareStatementCount();
            mvc.perform(get("/orcamentos").header("Authorization", bearer(ana)).param("mes", "2026-10").param("size", "1"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.totalElementos").value(3))
                    .andExpect(jsonPath("$.totalPaginas").value(3)).andExpect(jsonPath("$.resumo.ativos").value(3))
                    .andExpect(jsonPath("$.resumo.dentroDoPlanejado").value(2))
                    .andExpect(jsonPath("$.resumo.acimaDoLimite").value(1))
                    .andExpect(jsonPath("$.conteudo[0].categoriaNome").value("Alimentação"))
                    .andExpect(jsonPath("$.conteudo[0].gasto").value(960))
                    .andExpect(jsonPath("$.conteudo[0].situacao").value("ATENCAO"));
            assertEquals(3, stats.getPrepareStatementCount() - antes);
        } finally { stats.setStatisticsEnabled(anterior); }
    }
    @Test void consumoDeveSomarContasEExcluirTransferenciasForaDoMesEOriginaisEstornadas() throws Exception {
        criar("/orcamentos", ana, corpo(alimentacao, "2026-10", "100"));
        mvc.perform(post("/transferencias").header("Authorization", bearer(ana)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"contaOrigemId\":%d,\"contaDestinoId\":%d,\"valor\":100,\"descricao\":\"Reserva\",\"data\":\"2026-10-01\"}".formatted(conta, outra)))
                .andExpect(status().isCreated());
        despesa(conta, alimentacao, "20", "2026-10-10");
        despesa(outra, alimentacao, "30", "2026-10-10");
        despesa(conta, alimentacao, "10", "2026-09-30");
        long estornada = despesa(conta, alimentacao, "15", "2026-10-10");
        mvc.perform(post("/contas/{id}/transacoes/{tid}/estorno", conta, estornada)
                        .header("Authorization", bearer(ana))).andExpect(status().isCreated());
        listar(ana, "2026-10").andExpect(status().isOk())
                .andExpect(jsonPath("$.conteudo[0].gasto").value(50))
                .andExpect(jsonPath("$.conteudo[0].restante").value(50))
                .andExpect(jsonPath("$.conteudo[0].percentualConsumido").value(50));
    }
    @Test void deveIsolarConsultaCriacaoEEdicaoPorDono() throws Exception {
        long id = criar("/orcamentos", ana, corpo(alimentacao, "2026-10", "100"));
        listar(beto, "2026-10").andExpect(status().isOk()).andExpect(jsonPath("$.resumo.ativos").value(0));
        mvc.perform(post("/orcamentos").header("Authorization", bearer(beto)).contentType(MediaType.APPLICATION_JSON)
                .content(corpo(alimentacao, "2026-10", "100"))).andExpect(status().isNotFound());
        for (long alvo : new long[]{id, Long.MAX_VALUE}) {
            mvc.perform(patch("/orcamentos/{id}", alvo).header("Authorization", bearer(beto))
                    .contentType(MediaType.APPLICATION_JSON).content("{\"limite\":20}"))
                    .andExpect(status().isNotFound()).andExpect(jsonPath("$.mensagem").value("Orçamento não encontrado."));
        }
        mvc.perform(get("/orcamentos").param("mes", "2026-10")).andExpect(status().isUnauthorized());
    }
    @Test void deveGarantirUnicidadeInclusiveEmCriacoesConcorrentes() throws Exception {
        try (var pool = Executors.newFixedThreadPool(2)) {
            java.util.concurrent.Callable<Integer> chamada = () -> mvc.perform(post("/orcamentos")
                    .header("Authorization", bearer(ana)).contentType(MediaType.APPLICATION_JSON)
                    .content(corpo(alimentacao, "2026-10", "100"))).andReturn().getResponse().getStatus();
            var futuros = pool.invokeAll(java.util.List.of(chamada, chamada));
            var resultados = java.util.List.of(futuros.get(0).get(), futuros.get(1).get()).stream().sorted().toList();
            assertEquals(java.util.List.of(201, 409), resultados);
        }
        criar("/orcamentos", ana, corpo(alimentacao, "2026-11", "100"));
        listar(ana, "2026-10").andExpect(status().isOk()).andExpect(jsonPath("$.totalElementos").value(1));
    }
    @Test void deveValidarCategoriaMesLimiteEPaginacao() throws Exception {
        mvc.perform(post("/orcamentos").header("Authorization", bearer(ana)).contentType(MediaType.APPLICATION_JSON)
                .content(corpo(receita, "2026-10", "100"))).andExpect(status().isUnprocessableEntity());
        for (String limite : new String[]{"0", "-1", "null"}) {
            mvc.perform(post("/orcamentos").header("Authorization", bearer(ana)).contentType(MediaType.APPLICATION_JSON)
                    .content(corpo(alimentacao, "2026-10", limite))).andExpect(status().isBadRequest());
        }
        mvc.perform(post("/orcamentos").header("Authorization", bearer(ana)).contentType(MediaType.APPLICATION_JSON)
                .content(corpo(alimentacao, "2026-10", "10.001"))).andExpect(status().isUnprocessableEntity());
        listar(ana, "2026-13").andExpect(status().isBadRequest());
        listar(ana, "0000-01").andExpect(status().isUnprocessableEntity());
        mvc.perform(get("/orcamentos").header("Authorization", bearer(ana))).andExpect(status().isBadRequest());
        mvc.perform(get("/orcamentos").header("Authorization", bearer(ana)).param("mes", "2026-10")
                        .param("size", "1000").param("page", "-1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.tamanho").value(100)).andExpect(jsonPath("$.pagina").value(0));
    }
    private ResultActions listar(Sessao s, String mes) throws Exception {
        return mvc.perform(get("/orcamentos").header("Authorization", bearer(s)).param("mes", mes));
    }
    private String corpo(long categoria, String mes, String limite) {
        return "{\"categoriaId\":%d,\"mes\":\"%s\",\"limite\":%s}".formatted(categoria, mes, limite);
    }
    private long categoria(String nome, String tipo) throws Exception {
        return criar("/categorias", ana, "{\"nome\":\"%s\",\"tipo\":\"%s\"}".formatted(nome, tipo));
    }
    private long despesa(long conta, long categoria, String valor, String data) throws Exception {
        return criar("/contas/" + conta + "/despesas", ana,
                "{\"categoriaId\":%d,\"descricao\":\"Despesa\",\"valor\":%s,\"data\":\"%s\"}".formatted(categoria, valor, data));
    }
    private long criar(String rota, Sessao sessao, String corpo) throws Exception {
        String resposta = mvc.perform(post(rota).header("Authorization", bearer(sessao))
                        .contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(resposta, "$.id")).longValue();
    }
}
