package com.controlefinanceiro.api.infrastructure;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

@Configuration
public class RelogioConfig {

    // IMPORTANTE: "hoje" e "mês atual" são decisões de negócio. Usam o fuso do sistema (Brasil),
    // e não o do servidor, que em container costuma ser UTC.
    @Bean
    public Clock relogio(@Value("${app.fuso-horario}") String fuso) {
        return Clock.system(ZoneId.of(fuso));
    }
}