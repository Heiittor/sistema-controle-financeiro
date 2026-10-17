package com.controlefinanceiro.api.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

// Registro imutável de uma ação importante. Sem setters; o repository só expõe save.
@Entity
@Table(name = "auditoria")
public class Auditoria {

    private static final int TAMANHO_MAXIMO_DETALHES = 500;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Sem chave estrangeira: o registro sobrevive mesmo que o usuário seja removido.
    @Column(name = "usuario_id", nullable = false, updatable = false)
    private Long usuarioId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40, updatable = false)
    private AcaoAuditoria acao;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40, updatable = false)
    private EntidadeAuditada entidade;

    @Column(name = "entidade_id", nullable = false, updatable = false)
    private Long entidadeId;

    @Column(length = TAMANHO_MAXIMO_DETALHES, updatable = false)
    private String detalhes;

    @Column(name = "criado_em", insertable = false, updatable = false)
    private OffsetDateTime criadoEm;

    // Exigido pelo JPA.
    protected Auditoria() {
    }

    public Auditoria(Long usuarioId, AcaoAuditoria acao, EntidadeAuditada entidade,
                     Long entidadeId, String detalhes) {
        // IllegalArgumentException (e não RegraDeNegocioException): auditoria incompleta é bug nosso,
        // não erro do usuário. O cliente recebe 500 genérico e o detalhe vai para o log.
        if (usuarioId == null || acao == null || entidade == null || entidadeId == null) {
            throw new IllegalArgumentException("Usuário, ação, entidade e id da entidade são obrigatórios na auditoria.");
        }
        this.usuarioId = usuarioId;
        this.acao = acao;
        this.entidade = entidade;
        this.entidadeId = entidadeId;
        this.detalhes = abreviar(detalhes);
    }

    // IMPORTANTE: um texto longo não pode derrubar a operação financeira por causa da auditoria.
    private static String abreviar(String texto) {
        if (texto == null) {
            return null;
        }
        String limpo = texto.trim();
        return limpo.length() > TAMANHO_MAXIMO_DETALHES
                ? limpo.substring(0, TAMANHO_MAXIMO_DETALHES)
                : limpo;
    }

    public Long getId() { return id; }
    public Long getUsuarioId() { return usuarioId; }
    public AcaoAuditoria getAcao() { return acao; }
    public EntidadeAuditada getEntidade() { return entidade; }
    public Long getEntidadeId() { return entidadeId; }
    public String getDetalhes() { return detalhes; }
    public OffsetDateTime getCriadoEm() { return criadoEm; }
}