package com.controlefinanceiro.api.domain;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.LocalDate;
import java.time.OffsetDateTime;

import java.time.OffsetDateTime;

@Entity
@Table(name = "contas")
public class Conta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nome;

    @Embedded
    @AttributeOverride(name = "valor",
            column = @Column(name = "saldo", nullable = false, precision = 19, scale = 2))
    private Dinheiro saldo;

    @Version
    private Long version;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(name = "criado_em", insertable = false, updatable = false)
    private OffsetDateTime criadoEm;

    // Exigido pelo JPA.
    protected Conta() {
    }

    public Conta(String nome, Usuario usuario) {
        this.nome = normalizarNome(nome);
        if (usuario == null) {
            throw new IllegalArgumentException("O usuário da conta é obrigatório.");
        }
        this.usuario = usuario;
        this.saldo = Dinheiro.ZERO;
    }

    public void creditar(Dinheiro valor) {
        exigirValorPositivo(valor);
        this.saldo = this.saldo.somar(valor);
    }

    public void debitar(Dinheiro valor) {
        exigirValorPositivo(valor);
        if (valor.maiorQue(this.saldo)) {
            throw new IllegalStateException("Saldo insuficiente.");
        }
        this.saldo = this.saldo.subtrair(valor);
    }

    public Transacao registrarReceita(String descricao, Dinheiro valor, LocalDate data, Categoria categoria) {
        Transacao transacao = new Transacao(this, TipoTransacao.RECEITA, descricao, valor, data, categoria);
        creditar(valor);
        return transacao;
    }

    public Transacao registrarDespesa(String descricao, Dinheiro valor, LocalDate data, Categoria categoria) {
        Transacao transacao = new Transacao(this, TipoTransacao.DESPESA, descricao, valor, data, categoria);
        debitar(valor);
        return transacao;
    }

    public void renomear(String novoNome) {
        this.nome = normalizarNome(novoNome);
    }

    private static void exigirValorPositivo(Dinheiro valor) {
        if (valor == null || valor.ehZero()) {
            throw new IllegalArgumentException("O valor da operação deve ser maior que zero.");
        }
    }

    private static String normalizarNome(String nome) {
        if (nome == null || nome.isBlank()) {
            throw new IllegalArgumentException("O nome da conta é obrigatório.");
        }
        String limpo = nome.trim();
        if (limpo.length() > 100) {
            throw new IllegalArgumentException("O nome da conta deve ter no máximo 100 caracteres.");
        }
        return limpo;
    }

    public Long getId() { return id; }
    public String getNome() { return nome; }
    public Dinheiro getSaldo() { return saldo; }
    public Usuario getUsuario() { return usuario; }
    public OffsetDateTime getCriadoEm() { return criadoEm; }
}