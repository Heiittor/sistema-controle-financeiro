package com.controlefinanceiro.api.infrastructure.seguranca;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SegurancaConfig {

    // BCrypt com salt automático: transforma a senha em hash.
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filtros(HttpSecurity http, ErroSegurancaHandler erroHandler)
            throws Exception {
        http
                // CSRF protege sessões com cookie. Nossa API é stateless com token no header.
                .csrf(AbstractHttpConfigurer::disable)
                // IMPORTANTE: sem sessão no servidor. Cada requisição prova quem é pelo token.
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(a -> a
                        .requestMatchers(HttpMethod.POST, "/usuarios", "/auth/login").permitAll()
                        .requestMatchers(HttpMethod.GET, "/actuator/health").permitAll()
                        // IMPORTANTE: todo o resto exige token válido.
                        .anyRequest().authenticated())
                // Lê "Authorization: Bearer <token>" e valida com o JwtDecoder do JwtConfig.
                .oauth2ResourceServer(o -> o
                        .jwt(j -> { })
                        .authenticationEntryPoint(erroHandler)
                        .accessDeniedHandler(erroHandler))
                .exceptionHandling(e -> e
                        .authenticationEntryPoint(erroHandler)
                        .accessDeniedHandler(erroHandler));
        return http.build();
    }
}