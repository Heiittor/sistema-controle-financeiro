package com.controlefinanceiro.api.presentation;

import com.controlefinanceiro.api.IntegracaoBase;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuditoriaFluxoTest extends IntegracaoBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    private Sessao ana;
    private long conta;
    private long outraConta;
    private long salario;
    private long alimentacao;

    @BeforeEach
    void preparar() throws Exception {
        ana = cadastrarELogar(mockMvc, "Ana");
        conta = criar("/contas", "{\"nome\":\"Nubank\"}");
        outraConta = criar("/contas", "{\"nome\":\"Itaú\"}");
        salario = criar("/categorias", "{\"nome\":\"Salário\",\"tipo\":\"RECEITA\"}");
        alimentacao = criar("/categorias", "{\"nome\":\"Alimentação\",\"tipo\":\"DESPESA\"}");
    }

    @Test
    void cadastroEhAuditadoSemDadosSensiveis() {
        assertEquals(1, contar("CADASTRAR_USUARIO"));

        List<String> detalhes = jdbc.queryForList(
                "SELECT coalesce(detalhes, '') FROM auditoria WHERE usuario_id = ?", String.class, ana.id());
        for (String texto : detalhes) {
            assertFalse(texto.contains("senha12345"), "a senha não pode aparecer na auditoria");
            assertFalse(texto.contains("@"), "o e-mail não pode aparecer na auditoria");
        }
    }

    @Test
    void contaECategoriaSaoAuditadas() {
        assertEquals(2, contar("CRIAR_CONTA"));
        assertEquals(2, contar("CRIAR_CATEGORIA"));
    }

    @Test
    void receitaDespesaETransferenciaSaoAuditadas() throws Exception {
        registrar("receitas", salario, "1000.00");
        registrar("despesas", alimentacao, "100.00");
        mockMvc.perform(post("/transferencias")
                        .header("Authorization", bearer(ana))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"contaOrigemId\":" + conta + ",\"contaDestinoId\":" + outraConta
                                + ",\"descricao\":\"Reserva\",\"valor\":50.00,\"data\":\"2026-10-05\"}"))
                .andExpect(status().isCreated());

        assertEquals(1, contar("REGISTRAR_RECEITA"));
        assertEquals(1, contar("REGISTRAR_DESPESA"));
        // Uma transferência gera duas transações, mas UM registro de auditoria.
        assertEquals(1, contar("TRANSFERIR"));
    }

    @Test
    void estornoEEdicaoSaoAuditados() throws Exception {
        registrar("receitas", salario, "1000.00");
        long despesa = registrar("despesas", alimentacao, "100.00");
        long outraDespesa = criar("/categorias", "{\"nome\":\"Mercado\",\"tipo\":\"DESPESA\"}");

        mockMvc.perform(patch("/contas/{c}/transacoes/{t}", conta, despesa)
                        .header("Authorization", bearer(ana))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"categoriaId\":" + outraDespesa + ",\"descricao\":\"Novo texto\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/contas/{c}/transacoes/{t}/estorno", conta, despesa)
                        .header("Authorization", bearer(ana)))
                .andExpect(status().isCreated());

        // Os dois registros ficam no id da transação original.
        assertEquals(1, jdbc.queryForObject(
                "SELECT count(*) FROM auditoria WHERE acao = 'ESTORNAR' AND entidade_id = ?",
                Integer.class, despesa));
        assertEquals(1, jdbc.queryForObject(
                "SELECT count(*) FROM auditoria WHERE acao = 'EDITAR_TRANSACAO' AND entidade_id = ?",
                Integer.class, despesa));
    }

    @Test
    void operacaoRecusadaNaoDeixaRegistro() throws Exception {
        // Sem saldo: a despesa é recusada (422) e o rollback leva a auditoria junto.
        mockMvc.perform(post("/contas/{id}/despesas", conta)
                        .header("Authorization", bearer(ana))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"categoriaId\":" + alimentacao
                                + ",\"descricao\":\"Teste\",\"valor\":10.00,\"data\":\"2026-10-05\"}"))
                .andExpect(status().isUnprocessableEntity());

        assertEquals(0, contar("REGISTRAR_DESPESA"));
    }

    // ---- apoio ----

    private int contar(String acao) {
        return jdbc.queryForObject(
                "SELECT count(*) FROM auditoria WHERE usuario_id = ? AND acao = ?",
                Integer.class, ana.id(), acao);
    }

    private long registrar(String rota, long categoria, String valor) throws Exception {
        String resposta = mockMvc.perform(post("/contas/{id}/{rota}", conta, rota)
                        .header("Authorization", bearer(ana))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"categoriaId\":" + categoria + ",\"descricao\":\"Teste\",\"valor\":"
                                + valor + ",\"data\":\"2026-10-05\"}"))
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