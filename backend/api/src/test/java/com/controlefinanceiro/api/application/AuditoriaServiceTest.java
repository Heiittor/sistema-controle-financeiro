package com.controlefinanceiro.api.application;

import com.controlefinanceiro.api.IntegracaoBase;
import com.controlefinanceiro.api.domain.AcaoAuditoria;
import com.controlefinanceiro.api.domain.EntidadeAuditada;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AuditoriaServiceTest extends IntegracaoBase {

    @Autowired
    private AuditoriaService auditoriaService;

    @Autowired
    private TransactionTemplate transacao;

    @Autowired
    private JdbcTemplate jdbc;

    // Id único por teste: o banco é compartilhado entre as classes de teste.
    private final long entidadeId = UUID.randomUUID().getMostSignificantBits() & Long.MAX_VALUE;

    private void registrar(String detalhes) {
        auditoriaService.registrar(1L, AcaoAuditoria.ESTORNAR, EntidadeAuditada.TRANSACAO, entidadeId, detalhes);
    }

    private int contar() {
        return jdbc.queryForObject(
                "SELECT count(*) FROM auditoria WHERE entidade_id = ?", Integer.class, entidadeId);
    }

    @Test
    void deveExigirTransacaoAberta() {
        assertThrows(IllegalTransactionStateException.class, () -> registrar("sem transação"));

        assertEquals(0, contar());
    }

    @Test
    void deveGravarQuandoATransacaoConfirma() {
        transacao.executeWithoutResult(status -> registrar("valor=10.00"));

        assertEquals(1, contar());
    }

    @Test
    void deveSumirQuandoATransacaoReverte() {
        transacao.executeWithoutResult(status -> {
            registrar("valor=10.00");
            status.setRollbackOnly();
        });

        assertEquals(0, contar());
    }

    @Test
    void deveCortarDetalhesLongosEmVezDeFalhar() {
        transacao.executeWithoutResult(status -> registrar("x".repeat(600)));

        Integer tamanho = jdbc.queryForObject(
                "SELECT length(detalhes) FROM auditoria WHERE entidade_id = ?", Integer.class, entidadeId);
        assertEquals(500, tamanho);
    }
}