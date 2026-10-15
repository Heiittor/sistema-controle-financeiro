package com.controlefinanceiro.api.application;

import com.controlefinanceiro.api.domain.Usuario;
import com.controlefinanceiro.api.domain.exception.ConflitoException;
import com.controlefinanceiro.api.domain.exception.RegraDeNegocioException;
import com.controlefinanceiro.api.infrastructure.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public Usuario cadastrar(String nome, String email, String senha) {
        // A senha crua é validada ANTES do hash, porque o hash nunca é vazio.
        if (senha == null || senha.isBlank()) {
            throw new RegraDeNegocioException("A senha é obrigatória.");
        }

        // IMPORTANTE: o que vai para o banco é o HASH. A senha crua nunca é gravada.
        Usuario usuario = new Usuario(nome, email, passwordEncoder.encode(senha));

        if (usuarioRepository.existsByEmail(usuario.getEmail())) {
            throw new ConflitoException("Este e-mail já está em uso.");
        }

        return usuarioRepository.save(usuario);
    }
}