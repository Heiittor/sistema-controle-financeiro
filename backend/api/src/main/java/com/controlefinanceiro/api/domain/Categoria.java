package com.controlefinanceiro.api.domain;

import jakarta.persistence.Column;
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

import java.time.OffsetDateTime;

@Entity
@Table(name = "categorias")
public class Categoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nome;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TipoTransacao tipo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(name = "criado_em", insertable = false, updatable = false)
    private OffsetDateTime criadoEm;

    // Exigido pelo JPA. protected: o resto do código não cria categoria vazia.
    protected Categoria() {
    }

    public Categoria(String nome, TipoTransacao tipo, Usuario usuario) {
        this.nome = normalizarNome(nome);
        this.tipo = exigir(tipo, "O tipo da categoria é obrigatório.");
        this.usuario = exigir(usuario, "O usuário da categoria é obrigatório.");
    }

    public void renomear(String novoNome) {
        this.nome = normalizarNome(novoNome);
    }

    private static String normalizarNome(String nome) {
        if (nome == null || nome.isBlank()) {
            throw new IllegalArgumentException("O nome da categoria é obrigatório.");
        }
        String limpo = nome.trim();
        if (limpo.length() > 100) {
            throw new IllegalArgumentException("O nome da categoria deve ter no máximo 100 caracteres.");
        }
        return limpo;
    }

    private static <T> T exigir(T valor, String mensagem) {
        if (valor == null) {
            throw new IllegalArgumentException(mensagem);
        }
        return valor;
    }

    public Long getId() { return id; }
    public String getNome() { return nome; }
    public TipoTransacao getTipo() { return tipo; }
    public Usuario getUsuario() { return usuario; }
    public OffsetDateTime getCriadoEm() { return criadoEm; }
}