package com.controlefinanceiro.api.presentation;

import com.controlefinanceiro.api.IntegracaoBase;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class DashboardTest extends IntegracaoBase {
    @Autowired private MockMvc mvc;
    private Sessao ana;
    private long conta;
    private long outraConta;
    private long receita;
    private long despesa;

    @BeforeEach
    void preparar() throws Exception {
        ana = cadastrarELogar(mvc, "Ana");
        conta = criar(ana, "/contas", "{\"nome\":\"Principal\"}");
        outraConta = criar(ana, "/contas", "{\"nome\":\"Reserva\"}");
        receita = criar(ana, "/categorias", "{\"nome\":\"Salário\",\"tipo\":\"RECEITA\"}");
        despesa = criar(ana, "/categorias", "{\"nome\":\"Mercado\",\"tipo\":\"DESPESA\"}");
    }

    @Test
    void usuarioSemContasRecebeZerosEMesesSemLacunasNaViradaDoAno() throws Exception {
        Sessao vazio = cadastrarELogar(mvc, "Vazio");
        consultar(vazio, "resumo", "2025-12-15", "2026-02-10")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.saldoTotal").value(0))
                .andExpect(jsonPath("$.totalReceitas").value(0))
                .andExpect(jsonPath("$.totalDespesas").value(0))
                .andExpect(jsonPath("$.saldoDoPeriodo").value(0))
                .andExpect(jsonPath("$.porCategoria").isEmpty());
        consultar(vazio, "serie-mensal", "2025-12-15", "2026-02-10")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].mes").value("2025-12"))
                .andExpect(jsonPath("$[1].mes").value("2026-01"))
                .andExpect(jsonPath("$[2].mes").value("2026-02"))
                .andExpect(jsonPath("$[0].receitas").value(0))
                .andExpect(jsonPath("$[2].despesas").value(0));
    }

    @Test
    void consolidaContasComCentavosEFiltraDespesasPorCategoria() throws Exception {
        registrar(ana, conta, "receitas", receita, "100.10", "2026-01-10");
        registrar(ana, outraConta, "receitas", receita, "200.20", "2026-01-11");
        registrar(ana, conta, "despesas", despesa, "10.01", "2026-01-12");
        registrar(ana, outraConta, "despesas", despesa, "20.02", "2026-01-13");

        consultar(ana, "resumo", "2026-01-01", "2026-01-31")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.inicio").value("2026-01-01"))
                .andExpect(jsonPath("$.fim").value("2026-01-31"))
                .andExpect(jsonPath("$.saldoTotal").value(270.27))
                .andExpect(jsonPath("$.totalReceitas").value(300.30))
                .andExpect(jsonPath("$.totalDespesas").value(30.03))
                .andExpect(jsonPath("$.saldoDoPeriodo").value(270.27))
                .andExpect(jsonPath("$.porCategoria.length()").value(1))
                .andExpect(jsonPath("$.porCategoria[0].categoriaId").value(despesa))
                .andExpect(jsonPath("$.porCategoria[0].categoriaNome").value("Mercado"))
                .andExpect(jsonPath("$.porCategoria[0].tipo").value("DESPESA"))
                .andExpect(jsonPath("$.porCategoria[0].total").value(30.03));
        consultar(ana, "serie-mensal", "2026-01-01", "2026-02-28")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].receitas").value(300.30))
                .andExpect(jsonPath("$[0].despesas").value(30.03))
                .andExpect(jsonPath("$[1].receitas").value(0));
    }

    @Test
    void naoIncluiDadosDeOutroDonoMesmoQuandoRecebeUsuarioIdNaQuery() throws Exception {
        registrar(ana, conta, "receitas", receita, "100", "2026-01-10");
        Sessao beto = cadastrarELogar(mvc, "Beto");
        long contaBeto = criar(beto, "/contas", "{\"nome\":\"Principal\"}");
        long receitaBeto = criar(beto, "/categorias", "{\"nome\":\"Salário\",\"tipo\":\"RECEITA\"}");
        long despesaBeto = criar(beto, "/categorias", "{\"nome\":\"Privada\",\"tipo\":\"DESPESA\"}");
        registrar(beto, contaBeto, "receitas", receitaBeto, "9000", "2026-01-10");
        registrar(beto, contaBeto, "despesas", despesaBeto, "1000", "2026-01-11");
        mvc.perform(get("/dashboard/resumo").header("Authorization", bearer(ana))
                        .param("inicio", "2026-01-01").param("fim", "2026-01-31")
                        .param("usuarioId", String.valueOf(beto.id())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.saldoTotal").value(100))
                .andExpect(jsonPath("$.totalReceitas").value(100))
                .andExpect(jsonPath("$.totalDespesas").value(0))
                .andExpect(jsonPath("$.porCategoria").isEmpty());
        consultar(ana, "serie-mensal", "2026-01-01", "2026-01-31")
                .andExpect(jsonPath("$[0].receitas").value(100))
                .andExpect(jsonPath("$[0].despesas").value(0));
        consultar(beto, "resumo", "2026-01-01", "2026-01-31")
                .andExpect(jsonPath("$.saldoTotal").value(8000));
    }

    @Test
    void excluiTransferenciasEOriginaisEstornadasMesmoComEstornoForaDoPeriodo() throws Exception {
        registrar(ana, conta, "receitas", receita, "1000", "2020-01-01");
        long receitaEstornada = registrar(ana, conta, "receitas", receita, "100", "2020-01-02");
        long despesaEstornada = registrar(ana, conta, "despesas", despesa, "50", "2020-01-03");
        registrar(ana, conta, "despesas", despesa, "20", "2020-01-04");
        for (long id : new long[]{receitaEstornada, despesaEstornada}) {
            mvc.perform(post("/contas/{id}/transacoes/{tid}/estorno", conta, id)
                            .header("Authorization", bearer(ana)))
                    .andExpect(status().isCreated());
        }
        mvc.perform(post("/transferencias").header("Authorization", bearer(ana))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"contaOrigemId":%d,"contaDestinoId":%d,"descricao":"Reserva","valor":200,"data":"2020-01-05"}
                                """.formatted(conta, outraConta)))
                .andExpect(status().isCreated());
        consultar(ana, "resumo", "2020-01-01", "2020-01-31")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.saldoTotal").value(980))
                .andExpect(jsonPath("$.totalReceitas").value(1000))
                .andExpect(jsonPath("$.totalDespesas").value(20))
                .andExpect(jsonPath("$.porCategoria[0].total").value(20));
        consultar(ana, "serie-mensal", "2020-01-01", "2020-01-31")
                .andExpect(jsonPath("$[0].receitas").value(1000))
                .andExpect(jsonPath("$[0].despesas").value(20));
        // O período dos estornos também não contabiliza os lançamentos inversos como receita/despesa.
        String hoje = java.time.LocalDate.now(java.time.ZoneId.of("America/Sao_Paulo")).toString();
        consultar(ana, "serie-mensal", hoje, hoje)
                .andExpect(jsonPath("$[0].receitas").value(0))
                .andExpect(jsonPath("$[0].despesas").value(0));
    }

    @Test
    void respeitaDatasInclusivasEMesesParciaisSemConfundirSaldoAtual() throws Exception {
        registrar(ana, conta, "receitas", receita, "100", "2025-12-14");
        registrar(ana, conta, "receitas", receita, "10", "2025-12-15");
        registrar(ana, conta, "despesas", despesa, "5", "2026-02-10");
        registrar(ana, conta, "receitas", receita, "200", "2026-02-11");
        consultar(ana, "resumo", "2025-12-15", "2026-02-10")
                .andExpect(jsonPath("$.saldoTotal").value(305))
                .andExpect(jsonPath("$.saldoDoPeriodo").value(5));
        consultar(ana, "serie-mensal", "2025-12-15", "2026-02-10")
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].mes").value("2025-12"))
                .andExpect(jsonPath("$[0].receitas").value(10))
                .andExpect(jsonPath("$[1].mes").value("2026-01"))
                .andExpect(jsonPath("$[1].receitas").value(0))
                .andExpect(jsonPath("$[2].mes").value("2026-02"))
                .andExpect(jsonPath("$[2].receitas").value(0))
                .andExpect(jsonPath("$[2].despesas").value(5));
    }

    @Test
    void validaParametrosObrigatoriosFormatoOrdemELimiteDeMeses() throws Exception {
        for (String rota : new String[]{"resumo", "serie-mensal"}) {
            mvc.perform(get("/dashboard/" + rota).header("Authorization", bearer(ana)))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.campos").isArray());
            mvc.perform(get("/dashboard/" + rota).header("Authorization", bearer(ana)).param("inicio", "2026-01-01"))
                    .andExpect(status().isBadRequest());
            consultar(ana, rota, "", "2026-01-31").andExpect(status().isBadRequest());
            consultar(ana, rota, "abc", "2026-01-31").andExpect(status().isBadRequest());
            consultar(ana, rota, "2026-02-30", "2026-03-31").andExpect(status().isBadRequest());
            consultar(ana, rota, "2026-02-01", "2026-01-31").andExpect(status().isUnprocessableEntity());
            consultar(ana, rota, "2024-01-31", "2026-01-01").andExpect(status().isUnprocessableEntity());
            consultar(ana, rota, "2024-01-01", "2025-12-31").andExpect(status().isOk());
            consultar(ana, rota, "2026-01-01", "2026-01-01").andExpect(status().isOk());
        }
    }

    @Test
    void exigeAutenticacaoNosDoisEndpoints() throws Exception {
        for (String rota : new String[]{"resumo", "serie-mensal"}) {
            mvc.perform(get("/dashboard/" + rota).param("inicio", "2026-01-01").param("fim", "2026-01-31"))
                    .andExpect(status().isUnauthorized());
            mvc.perform(get("/dashboard/" + rota).header("Authorization", "Bearer invalido")
                            .param("inicio", "2026-01-01").param("fim", "2026-01-31"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Test
    void documentaRotasParametrosObrigatoriosERespostasNaOpenApi() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/dashboard/resumo'].get.responses['200']").exists())
                .andExpect(jsonPath("$.paths['/dashboard/resumo'].get.responses['400']").exists())
                .andExpect(jsonPath("$.paths['/dashboard/serie-mensal'].get").exists())
                .andExpect(jsonPath("$.paths['/dashboard/serie-mensal'].get.responses['200']").exists())
                .andExpect(jsonPath("$.paths['/dashboard/resumo'].get.parameters[0].required").value(true))
                .andExpect(jsonPath("$.components.schemas.DashboardResumoResponse.properties.saldoTotal").exists())
                .andExpect(jsonPath("$.components.schemas.DashboardMesResponse.properties.mes").exists());
    }

    private ResultActions consultar(Sessao sessao, String rota, String inicio, String fim) throws Exception {
        return mvc.perform(get("/dashboard/" + rota).header("Authorization", bearer(sessao))
                .param("inicio", inicio).param("fim", fim));
    }

    private long registrar(Sessao sessao, long contaId, String rota, long categoria, String valor, String data) throws Exception {
        return criar(sessao, "/contas/" + contaId + "/" + rota,
                """
                {"categoriaId":%d,"descricao":"Teste","valor":%s,"data":"%s"}
                """.formatted(categoria, valor, data));
    }

    private long criar(Sessao sessao, String rota, String corpo) throws Exception {
        String resposta = mvc.perform(post(rota).header("Authorization", bearer(sessao))
                        .contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(resposta, "$.id")).longValue();
    }
}
