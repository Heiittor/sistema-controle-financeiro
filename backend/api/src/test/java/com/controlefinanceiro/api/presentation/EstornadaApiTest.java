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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class EstornadaApiTest extends IntegracaoBase {

    @Autowired
    private MockMvc mockMvc;

    private Sessao ana;
    private long conta;
    private long outraConta;
    private long salario;
    private long alimentacao;
    private long mercado;
    private long receita;

    @BeforeEach
    void preparar() throws Exception {
        ana = cadastrarELogar(mockMvc, "Ana");
        conta = criar("/contas", "{\"nome\":\"Nubank\"}");
        outraConta = criar("/contas", "{\"nome\":\"Itaú\"}");
        salario = criar("/categorias", "{\"nome\":\"Salário\",\"tipo\":\"RECEITA\"}");
        alimentacao = criar("/categorias", "{\"nome\":\"Alimentação\",\"tipo\":\"DESPESA\"}");
        mercado = criar("/categorias", "{\"nome\":\"Mercado\",\"tipo\":\"DESPESA\"}");
        receita = registrar("receitas", salario, "1000.00", "2026-10-01");
    }

    @Test
    void soAOriginalEstornadaVemComoEstornada() throws Exception {
        long despesa = registrar("despesas", alimentacao, "100.00", "2026-10-02");
        long livre = registrar("despesas", alimentacao, "10.00", "2026-10-03");

        estornar(despesa)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estornada").value(false));

        historico("2026-01-01", "2099-12-31", 100)
                .andExpect(status().isOk())
                .andExpect(jsonPath(porId(despesa)).value(true))
                .andExpect(jsonPath(porId(livre)).value(false))
                .andExpect(jsonPath(porId(receita)).value(false))
                .andExpect(jsonPath("$.conteudo[?(@.tipo=='ESTORNO_DESPESA')].estornada").value(false));
    }

    @Test
    void estornadaApareceMesmoComOEstornoForaDoPeriodoConsultado() throws Exception {
        long despesa = registrar("despesas", alimentacao, "100.00", "2026-10-02");
        estornar(despesa).andExpect(status().isCreated());

        // O estorno tem a data de hoje, fora de 02/10/2026. A original precisa vir marcada mesmo assim.
        historico("2026-10-02", "2026-10-02", 20)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conteudo.length()").value(1))
                .andExpect(jsonPath("$.conteudo[0].id").value(despesa))
                .andExpect(jsonPath("$.conteudo[0].estornada").value(true));
    }

    @Test
    void transacaoRecemCriadaNaoEstaEstornada() throws Exception {
        mockMvc.perform(post("/contas/{id}/despesas", conta)
                        .header("Authorization", bearer(ana))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"categoriaId\":" + alimentacao
                                + ",\"descricao\":\"Teste\",\"valor\":10.00,\"data\":\"2026-10-02\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estornada").value(false));
    }

    @Test
    void editarUmaTransacaoJaEstornadaContinuaEstornada() throws Exception {
        long despesa = registrar("despesas", alimentacao, "100.00", "2026-10-02");
        estornar(despesa).andExpect(status().isCreated());

        mockMvc.perform(patch("/contas/{c}/transacoes/{t}", conta, despesa)
                        .header("Authorization", bearer(ana))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"categoriaId\":" + mercado + ",\"descricao\":\"Novo texto\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estornada").value(true));
    }

    @Test
    void transferenciaNuncaEhEstornada() throws Exception {
        mockMvc.perform(post("/transferencias")
                        .header("Authorization", bearer(ana))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"contaOrigemId\":" + conta + ",\"contaDestinoId\":" + outraConta
                                + ",\"descricao\":\"Reserva\",\"valor\":100.00,\"data\":\"2026-10-05\"}"))
                .andExpect(status().isCreated());

        historico("2026-10-05", "2026-10-05", 20)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conteudo[?(@.tipo=='TRANSFERENCIA_SAIDA')].estornada").value(false));
    }

    // ---- apoio ----

    private static String porId(long id) {
        return "$.conteudo[?(@.id==" + id + ")].estornada";
    }

    private ResultActions historico(String inicio, String fim, int tamanho) throws Exception {
        return mockMvc.perform(get("/contas/{id}/transacoes", conta)
                .header("Authorization", bearer(ana))
                .param("inicio", inicio).param("fim", fim).param("size", String.valueOf(tamanho)));
    }

    private ResultActions estornar(long transacao) throws Exception {
        return mockMvc.perform(post("/contas/{c}/transacoes/{t}/estorno", conta, transacao)
                .header("Authorization", bearer(ana)));
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