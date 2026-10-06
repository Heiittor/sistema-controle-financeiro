package com.controlefinanceiro.api.domain;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import com.controlefinanceiro.api.domain.exception.RegraDeNegocioException;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Objects;

@Entity
@Table(name = "transacoes")
public class Transacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String descricao;

    @Embedded
    @AttributeOverride(name = "valor",
            column = @Column(name = "valor", nullable = false, precision = 19, scale = 2))
    private Dinheiro valor;

    @Column(nullable = false)
    private LocalDate data;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TipoTransacao tipo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conta_id", nullable = false)
    private Conta conta;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "categoria_id", nullable = false)
    private Categoria categoria;

    @Column(name = "criado_em", insertable = false, updatable = false)
    private OffsetDateTime criadoEm;

    // Exigido pelo JPA.
    protected Transacao() {
    }

    // Sem "public": só a Conta (mesmo pacote) cria transações.
    Transacao(Conta conta, TipoTransacao tipo, String descricao, Dinheiro valor,
              LocalDate data, Categoria categoria) {
        if (conta == null || tipo == null || data == null || categoria == null) {
            throw new RegraDeNegocioException("Conta, tipo, data e categoria são obrigatórios.");
        }
        if (valor == null || valor.ehZero()) {
            throw new RegraDeNegocioException("O valor da transação deve ser maior que zero.");
        }
        if (categoria.getTipo() != tipo) {
            throw new RegraDeNegocioException("A categoria deve ser do mesmo tipo da transação.");
        }
        if (!Objects.equals(categoria.getUsuario().getId(), conta.getUsuario().getId())) {
            throw new RegraDeNegocioException("A categoria não pertence ao dono da conta.");
        }
        this.conta = conta;
        this.tipo = tipo;
        this.descricao = normalizarDescricao(descricao);
        this.valor = valor;
        this.data = data;
        this.categoria = categoria;
    }

    private static String normalizarDescricao(String descricao) {
        if (descricao == null || descricao.isBlank()) {
            throw new RegraDeNegocioException("A descrição da transação é obrigatória.");
        }
        String limpa = descricao.trim();
        if (limpa.length() > 255) {
            throw new RegraDeNegocioException("A descrição deve ter no máximo 255 caracteres.");
        }
        return limpa;
    }

    public Long getId() { return id; }
    public String getDescricao() { return descricao; }
    public Dinheiro getValor() { return valor; }
    public LocalDate getData() { return data; }
    public TipoTransacao getTipo() { return tipo; }
    public Conta getConta() { return conta; }
    public Categoria getCategoria() { return categoria; }
    public OffsetDateTime getCriadoEm() { return criadoEm; }
}