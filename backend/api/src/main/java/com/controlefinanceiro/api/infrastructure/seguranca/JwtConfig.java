package com.controlefinanceiro.api.infrastructure.seguranca;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

@Configuration
public class JwtConfig {

    private static final int TAMANHO_MINIMO_BYTES = 32;

    // IMPORTANTE: encoder e decoder usam a MESMA chave. Só quem tem o segredo emite token válido.
    private static SecretKey chave(String segredo) {
        byte[] bytes = segredo.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < TAMANHO_MINIMO_BYTES) {
            throw new IllegalStateException(
                    "JWT_SECRET deve ter pelo menos 32 caracteres (exigência do HS256).");
        }
        return new SecretKeySpec(bytes, "HmacSHA256");
    }

    // Assina tokens (será usado no login).
    @Bean
    public JwtEncoder jwtEncoder(@Value("${app.jwt.secret}") String segredo) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(chave(segredo)));
    }

    // Confere assinatura e validade (será usado pelo filtro, no passo 4).
    @Bean
    public JwtDecoder jwtDecoder(@Value("${app.jwt.secret}") String segredo) {
        return NimbusJwtDecoder.withSecretKey(chave(segredo))
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
    }
}