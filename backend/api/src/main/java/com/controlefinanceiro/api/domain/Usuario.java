package com.controlefinanceiro.api.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import com.controlefinanceiro.api.domain.exception.RegraDeNegocioException;

import java.util.Locale;

@Entity
@Table(name = "usuarios")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String nome;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(nullable = false, length = 255)
    private String senha;

    // Exigido pelo JPA. protected: o resto do código não cria usuário vazio.
    protected Usuario() {
    }

    // IMPORTANTE: única porta de criação. Dado inválido = usuário não nasce.
    public Usuario(String nome, String email, String senha) {
        this.nome = normalizarNome(nome);
        this.email = normalizarEmail(email);
        this.senha = exigirSenha(senha);
    }

    private static String normalizarNome(String nome) {
        if (nome == null || nome.isBlank()) {
            throw new RegraDeNegocioException("O nome do usuário é obrigatório.");
        }
        String limpo = nome.trim();
        if (limpo.length() > 255) {
            throw new RegraDeNegocioException("O nome deve ter no máximo 255 caracteres.");
        }
        return limpo;
    }

    // IMPORTANTE: minúsculas e sem espaços, para "A@x.com" e "a@x.com" serem o mesmo e-mail.
    public static String normalizarEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new RegraDeNegocioException("O e-mail é obrigatório.");
        }
        String limpo = email.trim().toLowerCase(Locale.ROOT);
        int arroba = limpo.indexOf('@');
        if (arroba < 1 || arroba == limpo.length() - 1 || limpo.indexOf('@', arroba + 1) != -1) {
            throw new RegraDeNegocioException("O e-mail é inválido.");
        }
        if (limpo.length() > 255) {
            throw new RegraDeNegocioException("O e-mail deve ter no máximo 255 caracteres.");
        }
        return limpo;
    }

    // Por enquanto texto puro. Na Fase 7 este campo passa a guardar o HASH da senha.
    private static String exigirSenha(String senha) {
        if (senha == null || senha.isBlank()) {
            throw new RegraDeNegocioException("A senha é obrigatória.");
        }
        return senha;
    }
    public Long getId() { return id; }
    public String getNome() { return nome; }
    public String getEmail() { return email; }
    public String getSenha() { return senha; }
}