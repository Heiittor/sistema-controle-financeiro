package com.controlefinanceiro.api.application;

import com.controlefinanceiro.api.domain.Usuario;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

@Service
public class TokenService {

    private static final String EMISSOR = "controle-financeiro-api";

    private final JwtEncoder jwtEncoder;
    private final Duration validade;

    public TokenService(JwtEncoder jwtEncoder,
                        @Value("${app.jwt.expiracao-minutos}") long expiracaoMinutos) {
        this.jwtEncoder = jwtEncoder;
        this.validade = Duration.ofMinutes(expiracaoMinutos);
    }

    // O token e quanto tempo ele vale, em segundos (o frontend usa para saber quando renovar o login).
    public record TokenEmitido(String token, long expiraEmSegundos) {
    }

    public TokenEmitido gerar(Usuario usuario) {
        Instant agora = Instant.now();

        // IMPORTANTE: o payload é legível por qualquer pessoa. Só id e datas, nada sensível.
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(EMISSOR)
                .subject(String.valueOf(usuario.getId()))
                .issuedAt(agora)
                .expiresAt(agora.plus(validade))
                .build();

        // O cabeçalho precisa declarar HS256, o mesmo algoritmo do JwtDecoder.
        JwsHeader cabecalho = JwsHeader.with(MacAlgorithm.HS256).build();

        String token = jwtEncoder.encode(JwtEncoderParameters.from(cabecalho, claims)).getTokenValue();
        return new TokenEmitido(token, validade.toSeconds());
    }
}