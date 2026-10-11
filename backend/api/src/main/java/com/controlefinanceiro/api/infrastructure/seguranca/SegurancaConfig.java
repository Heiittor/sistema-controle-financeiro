package com.controlefinanceiro.api.infrastructure.seguranca;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SegurancaConfig {

    // IMPORTANTE: BCrypt com salt automático. É ele que transforma a senha em hash.
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // TEMPORÁRIO: libera tudo para o Spring Security não bloquear a API enquanto migramos.
    // No passo 4 isto vira a regra real (só /usuarios e /auth/login públicos).
    @Bean
    public SecurityFilterChain filtros(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(a -> a.anyRequest().permitAll())
                .httpBasic(Customizer.withDefaults());
        return http.build();
    }
}