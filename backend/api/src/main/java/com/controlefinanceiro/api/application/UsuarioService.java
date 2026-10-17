package com.controlefinanceiro.api.application;

import com.controlefinanceiro.api.domain.AcaoAuditoria;
import com.controlefinanceiro.api.domain.EntidadeAuditada;
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
    private final AuditoriaService auditoriaService;

    public UsuarioService(UsuarioRepository usuarioRepository,
                          PasswordEncoder passwordEncoder,
                          AuditoriaService auditoriaService) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditoriaService = auditoriaService;
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

        Usuario salvo = usuarioRepository.save(usuario);

        // Sem detalhes: nada de e-mail ou dado pessoal na auditoria.
        auditoriaService.registrar(salvo.getId(), AcaoAuditoria.CADASTRAR_USUARIO,
                EntidadeAuditada.USUARIO, salvo.getId(), null);
        return salvo;
    }
}