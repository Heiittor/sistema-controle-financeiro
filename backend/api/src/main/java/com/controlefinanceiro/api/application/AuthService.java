package com.controlefinanceiro.api.application;

import com.controlefinanceiro.api.application.TokenService.TokenEmitido;
import com.controlefinanceiro.api.domain.Usuario;
import com.controlefinanceiro.api.domain.exception.CredenciaisInvalidasException;
import com.controlefinanceiro.api.domain.exception.TentativasExcedidasException;
import com.controlefinanceiro.api.infrastructure.UsuarioRepository;
import com.controlefinanceiro.api.infrastructure.seguranca.LimitadorTentativasLogin;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
public class AuthService {

    private static final String MENSAGEM = "E-mail ou senha inválidos.";

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final LimitadorTentativasLogin limitador;

    // Hash de uma senha qualquer: gasta o mesmo tempo quando o e-mail não existe.
    private final String hashFalso;

    public AuthService(UsuarioRepository usuarioRepository,
                       PasswordEncoder passwordEncoder,
                       TokenService tokenService,
                       LimitadorTentativasLogin limitador) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.limitador = limitador;
        this.hashFalso = passwordEncoder.encode("senha-falsa-para-igualar-o-tempo");
    }

    @Transactional(readOnly = true)
    public TokenEmitido autenticar(String email, String senha, String ip) {
        String emailNormalizado = email.trim().toLowerCase(Locale.ROOT);
        String chave = emailNormalizado + "|" + ip;

        // IMPORTANTE: checa o bloqueio ANTES do BCrypt, para o atacante não gastar nossa CPU.
        limitador.tempoDeBloqueio(chave).ifPresent(espera -> {
            throw new TentativasExcedidasException(espera.toSeconds());
        });

        Usuario usuario = usuarioRepository.findByEmail(emailNormalizado).orElse(null);

        String hashParaComparar = usuario != null ? usuario.getSenha() : hashFalso;
        boolean senhaConfere = passwordEncoder.matches(senha, hashParaComparar);

        if (usuario == null || !senhaConfere) {
            limitador.registrarFalha(chave);
            throw new CredenciaisInvalidasException(MENSAGEM);
        }

        limitador.limpar(chave);
        return tokenService.gerar(usuario);
    }
}