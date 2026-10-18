package com.controlefinanceiro.api.application;

import com.controlefinanceiro.api.application.TokenService.TokenEmitido;
import com.controlefinanceiro.api.domain.Usuario;
import com.controlefinanceiro.api.domain.exception.CredenciaisInvalidasException;
import com.controlefinanceiro.api.domain.exception.RegraDeNegocioException;
import com.controlefinanceiro.api.domain.exception.TentativasExcedidasException;
import com.controlefinanceiro.api.infrastructure.UsuarioRepository;
import com.controlefinanceiro.api.infrastructure.seguranca.LimitadorTentativasLogin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;


@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
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
        String emailNormalizado;
        try {
            // Mesma regra do cadastro (Usuario.normalizarEmail): uma só fonte da verdade.
            emailNormalizado = Usuario.normalizarEmail(email);
        } catch (RegraDeNegocioException e) {
            // E-mail malformado nunca existe no banco. Mesma resposta de "senha errada", sem criar oráculo.
            log.warn("Falha de login: formato de e-mail inválido ip={}", ip);
            passwordEncoder.matches(senha, hashFalso); // iguala o tempo de resposta
            throw new CredenciaisInvalidasException(MENSAGEM);
        }
        String chave = emailNormalizado + "|" + ip;

        // IMPORTANTE: checa o bloqueio ANTES do BCrypt, para o atacante não gastar nossa CPU.
        limitador.tempoDeBloqueio(chave).ifPresent(espera -> {
            log.warn("Login bloqueado por excesso de tentativas: alvo={} ip={} liberaEmSegundos={}",
                    identificar(emailNormalizado), ip, espera.toSeconds());
            throw new TentativasExcedidasException(espera.toSeconds());
        });

        Usuario usuario = usuarioRepository.findByEmail(emailNormalizado).orElse(null);

        String hashParaComparar = usuario != null ? usuario.getSenha() : hashFalso;
        boolean senhaConfere = passwordEncoder.matches(senha, hashParaComparar);

        if (usuario == null || !senhaConfere) {
            limitador.registrarFalha(chave);
            // Mesmo texto para e-mail inexistente e senha errada: o log não revela se o e-mail existe.
            log.warn("Falha de login: alvo={} ip={}", identificar(emailNormalizado), ip);
            throw new CredenciaisInvalidasException(MENSAGEM);
        }

        limitador.limpar(chave);
        log.info("Login realizado: usuarioId={} ip={}", usuario.getId(), ip);
        return tokenService.gerar(usuario);
    }

    // IMPORTANTE: nunca logamos o e-mail. Um hash curto permite ver "o mesmo alvo atacado" sem expor quem é.
    private static String identificar(String emailNormalizado) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256")
                    .digest(emailNormalizado.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash).substring(0, 8);
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 existe em toda JVM; se faltar, é erro de ambiente.
            throw new IllegalStateException("SHA-256 indisponível.", e);
        }
    }
}