package com.controlefinanceiro.api.application;

import com.controlefinanceiro.api.domain.Usuario;
import com.controlefinanceiro.api.infrastructure.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;

    public UsuarioService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional
    public Usuario cadastrar(String nome, String email, String senha) {
        // IMPORTANTE: criar o Usuario primeiro. O domínio valida e normaliza o e-mail
        // (minúsculas, sem espaços) ANTES de checarmos se ele já existe.
        Usuario usuario = new Usuario(nome, email, senha);

        // Checagem amigável. O UNIQUE do banco continua sendo a garantia final.
        if (usuarioRepository.existsByEmail(usuario.getEmail())) {
            throw new IllegalArgumentException("Este e-mail já está em uso.");
        }

        // Fase 7: aqui a senha será trocada pelo HASH antes de salvar.
        return usuarioRepository.save(usuario);
    }
}