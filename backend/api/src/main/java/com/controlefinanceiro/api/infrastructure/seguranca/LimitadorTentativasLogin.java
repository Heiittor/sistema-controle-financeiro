package com.controlefinanceiro.api.infrastructure.seguranca;

import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class LimitadorTentativasLogin {

    private static final int MAXIMO_FALHAS = 5;
    private static final Duration JANELA = Duration.ofMinutes(15);

    private final Clock relogio;
    // chave (email|ip) -> horários das falhas dentro da janela
    private final Map<String, Deque<Instant>> falhas = new ConcurrentHashMap<>();

    public LimitadorTentativasLogin() {
        this(Clock.systemUTC());
    }

    // Construtor para testes: permite controlar o relógio sem esperar 15 minutos.
    LimitadorTentativasLogin(Clock relogio) {
        this.relogio = relogio;
    }

    // Se a chave está bloqueada, devolve quanto falta esperar. Vazio = pode tentar.
    public Optional<Duration> tempoDeBloqueio(String chave) {
        Deque<Instant> registros = falhas.get(chave);
        if (registros == null) {
            return Optional.empty();
        }
        synchronized (registros) {
            descartarExpiradas(registros);
            if (registros.isEmpty()) {
                falhas.remove(chave, registros);
                return Optional.empty();
            }
            if (registros.size() < MAXIMO_FALHAS) {
                return Optional.empty();
            }
            // IMPORTANTE: libera quando a falha MAIS ANTIGA sair da janela.
            Instant liberaEm = registros.peekFirst().plus(JANELA);
            return Optional.of(Duration.between(relogio.instant(), liberaEm));
        }
    }

    public void registrarFalha(String chave) {
        Deque<Instant> registros = falhas.computeIfAbsent(chave, k -> new ArrayDeque<>());
        synchronized (registros) {
            descartarExpiradas(registros);
            registros.addLast(relogio.instant());
        }
    }

    // Login correto zera o contador daquela chave.
    public void limpar(String chave) {
        falhas.remove(chave);
    }

    private void descartarExpiradas(Deque<Instant> registros) {
        Instant limite = relogio.instant().minus(JANELA);
        while (!registros.isEmpty() && !registros.peekFirst().isAfter(limite)) {
            registros.removeFirst();
        }
    }
}