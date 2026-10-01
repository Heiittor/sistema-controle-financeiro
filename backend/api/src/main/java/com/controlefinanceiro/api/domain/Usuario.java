package com.controlefinanceiro.api.domain;

public class Usuario {

    private Long id;
    private String nome;
    private String email;
    private String senha;

    public Usuario() {
    }

    public Usuario(Long id, String nome, String email, String senha) {
        this.id = id;
        this.nome = nome;
        this.email = email;
        this.senha = senha;
    }
    //pega/ler o id
    public Long getId() {
        return id;
    }
    //altera o id
    public void setId(Long id) {
        this.id = id;
    }
    //pega/ler o nome
    public String getNome() {
        return nome;
    }
    //altera o nome
    public void setNome(String nome) {
        this.nome = nome;
    }
    //pega/ler o email
    public String getEmail() {
        return email;
    }
    //altera o email
    public void setEmail(String email) {
        this.email = email;
    }
    //pega/ler a senha
    public String getSenha() {
        return senha;
    }
    //altera a senha
    public void setSenha(String senha) {
        this.senha = senha;
    }
}