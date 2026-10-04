package com.controlefinanceiro.api.application;

import com.controlefinanceiro.api.domain.Usuario;
import com.controlefinanceiro.api.infrastructure.UsuarioRepository;
import org.springframework.stereotype.Service;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;

    // Injeção de Dependência via construtor (Boa prática do SOLID)
    public UsuarioService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    public Usuario cadastrar(Usuario usuario) {
        // Regra de negócio 1: Validar e-mail duplicado
        if (usuarioRepository.existsByEmail(usuario.getEmail())) {
            throw new IllegalArgumentException("Este e-mail já está em uso.");
        }

        // Regra de negócio 2 (Futuro): Aqui entraremos com a criptografia da senha antes de salvar

        // Se passou pelas regras, mandamos o repositório salvar no banco
        return usuarioRepository.save(usuario);
    }
}