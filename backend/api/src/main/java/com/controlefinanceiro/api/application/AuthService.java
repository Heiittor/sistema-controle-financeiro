package com.controlefinanceiro.api.application;

import com.controlefinanceiro.api.application.TokenService.TokenEmitido;
import com.controlefinanceiro.api.domain.Usuario;
import com.controlefinanceiro.api.domain.exception.CredenciaisInvalidasException;
import com.controlefinanceiro.api.infrastructure.UsuarioRepository;
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

    // Hash de uma senha qualquer: usado só para gastar o mesmo tempo quando o e-mail não existe.
    private final String hashFalso;

    public AuthService(UsuarioRepository usuarioRepository,
                       PasswordEncoder passwordEncoder,
                       TokenService tokenService) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.hashFalso = passwordEncoder.encode("senha-falsa-para-igualar-o-tempo");
    }

    @Transactional(readOnly = true)
    public TokenEmitido autenticar(String email, String senha) {
        String emailNormalizado = email.trim().toLowerCase(Locale.ROOT);

        Usuario usuario = usuarioRepository.findByEmail(emailNormalizado).orElse(null);

        // IMPORTANTE: e-mail inexistente e senha errada têm a MESMA resposta e o MESMO custo de tempo.
        String hashParaComparar = usuario != null ? usuario.getSenha() : hashFalso;
        boolean senhaConfere = passwordEncoder.matches(senha, hashParaComparar);

        if (usuario == null || !senhaConfere) {
            throw new CredenciaisInvalidasException(MENSAGEM);
        }

        return tokenService.gerar(usuario);
    }
}