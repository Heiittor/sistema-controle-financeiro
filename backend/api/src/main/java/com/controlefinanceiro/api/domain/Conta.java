package com.controlefinanceiro.api.domain;

import java.math.BigDecimal;

public class Conta {

    private Long id;
    private String nome;
    private BigDecimal saldo; // Usamos BigDecimal para calculos mais aproximados
    private Usuario usuario; // Aqui criamos o vínculo! A conta pertence a um Usuário.

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