package com.controlefinanceiro.api.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "contas")
public class Conta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nome;

    private BigDecimal saldo;

    @ManyToOne //(Muitos para um ) 1 usuario pode ter varias contas mas uma conta so pode pertencer a 1 usuario
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    // O JPA exige o construtor vazio
    public Conta() {
    }

    public Conta(Long id, String nome, BigDecimal saldo, Usuario usuario) {
        this.id = id;
        this.nome = nome;
        this.saldo = saldo;
        this.usuario = usuario;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public BigDecimal getSaldo() {
        return saldo;
    }

    public void setSaldo(BigDecimal saldo) {
        this.saldo = saldo;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }
}