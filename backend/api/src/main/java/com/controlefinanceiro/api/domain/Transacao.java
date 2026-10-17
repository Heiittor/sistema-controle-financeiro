package com.controlefinanceiro.api.domain;

import com.controlefinanceiro.api.domain.exception.RegraDeNegocioException;
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

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

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
    @Column(nullable = false, length = 25)
    private TipoTransacao tipo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conta_id", nullable = false)
    private Conta conta;

    // Opcional: transferências e estornos não têm categoria.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "categoria_id")
    private Categoria categoria;

    // Liga as duas pontas de uma transferência. Nulo nos demais tipos.
    @Column(name = "transferencia_id")
    private UUID transferenciaId;

    // Só no estorno: aponta para a transação original que ele desfaz.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "estorno_de_id")
    private Transacao estornoDe;

    @Column(name = "criado_em", insertable = false, updatable = false)
    private OffsetDateTime criadoEm;

    // Exigido pelo JPA.
    protected Transacao() {
    }

    // Receita e despesa. Sem "public": só a Conta (mesmo pacote) cria transações.
    Transacao(Conta conta, TipoTransacao tipo, String descricao, Dinheiro valor,
              LocalDate data, Categoria categoria) {
        if (conta == null || tipo == null || data == null || categoria == null) {
            throw new RegraDeNegocioException("Conta, tipo, data e categoria são obrigatórios.");
        }
        if (tipo != TipoTransacao.RECEITA && tipo != TipoTransacao.DESPESA) {
            throw new RegraDeNegocioException("Tipo inválido para transação com categoria.");
        }
        if (valor == null || valor.ehZero()) {
            throw new RegraDeNegocioException("O valor da transação deve ser maior que zero.");
        }
        validarCategoria(conta, tipo, categoria);
        this.conta = conta;
        this.tipo = tipo;
        this.descricao = normalizarDescricao(descricao);
        this.valor = valor;
        this.data = data;
        this.categoria = categoria;
    }

    // Transferência: sem categoria, com transferenciaId. Só a Conta cria.
    Transacao(Conta conta, TipoTransacao tipo, String descricao, Dinheiro valor,
              LocalDate data, UUID transferenciaId) {
        if (conta == null || tipo == null || data == null || transferenciaId == null) {
            throw new RegraDeNegocioException("Conta, tipo, data e transferência são obrigatórios.");
        }
        if (tipo != TipoTransacao.TRANSFERENCIA_SAIDA && tipo != TipoTransacao.TRANSFERENCIA_ENTRADA) {
            throw new RegraDeNegocioException("Tipo inválido para transferência.");
        }
        if (valor == null || valor.ehZero()) {
            throw new RegraDeNegocioException("O valor da transação deve ser maior que zero.");
        }
        this.conta = conta;
        this.tipo = tipo;
        this.descricao = normalizarDescricao(descricao);
        this.valor = valor;
        this.data = data;
        this.transferenciaId = transferenciaId;
    }

    // Estorno: lançamento inverso da original. Tipo e valor vêm dela, o chamador não escolhe.
    Transacao(Transacao original, LocalDate data) {
        if (original == null || data == null) {
            throw new RegraDeNegocioException("Transação original e data são obrigatórias.");
        }
        if (!original.ehEstornavel()) {
            throw new RegraDeNegocioException("Só receitas e despesas podem ser estornadas.");
        }
        this.conta = original.conta;
        this.tipo = original.tipo == TipoTransacao.RECEITA
                ? TipoTransacao.ESTORNO_RECEITA
                : TipoTransacao.ESTORNO_DESPESA;
        this.descricao = normalizarDescricao(abreviar("Estorno: " + original.descricao));
        this.valor = original.valor;
        this.data = data;
        this.estornoDe = original;
    }

    // Só receita e despesa podem ser estornadas (transferência e estorno não).
    boolean ehEstornavel() {
        return tipo == TipoTransacao.RECEITA || tipo == TipoTransacao.DESPESA;
    }

    // Edição: só descrição e categoria. Valor, data, tipo e conta são fatos financeiros (para corrigir, estorna).
    void editar(String novaDescricao, Categoria novaCategoria) {
        if (!ehEstornavel()) {
            throw new RegraDeNegocioException("Só receitas e despesas podem ser editadas.");
        }
        if (novaCategoria == null) {
            throw new RegraDeNegocioException("A categoria é obrigatória.");
        }
        validarCategoria(this.conta, this.tipo, novaCategoria);
        this.descricao = normalizarDescricao(novaDescricao);
        this.categoria = novaCategoria;
    }

    private static void validarCategoria(Conta conta, TipoTransacao tipo, Categoria categoria) {
        if (!categoria.getTipo().aceita(tipo)) {
            throw new RegraDeNegocioException("A categoria deve ser do mesmo tipo da transação.");
        }
        if (!Objects.equals(categoria.getUsuario().getId(), conta.getUsuario().getId())) {
            throw new RegraDeNegocioException("A categoria não pertence ao dono da conta.");
        }
    }

    // Descrições longas não podem estourar o limite de 255 ao receber o prefixo "Estorno: ".
    private static String abreviar(String texto) {
        return texto.length() > 255 ? texto.substring(0, 255) : texto;
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
    public UUID getTransferenciaId() { return transferenciaId; }
    public OffsetDateTime getCriadoEm() { return criadoEm; }

    // Só o id da original: ler o id de um proxy LAZY não consulta o banco.
    public Long getEstornoDeId() { return estornoDe != null ? estornoDe.getId() : null; }
}