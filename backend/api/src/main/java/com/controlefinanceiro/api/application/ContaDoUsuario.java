package com.controlefinanceiro.api.application;

import com.controlefinanceiro.api.domain.Conta;
import com.controlefinanceiro.api.domain.exception.RecursoNaoEncontradoException;
import com.controlefinanceiro.api.infrastructure.ContaRepository;
import org.springframework.stereotype.Component;

// IMPORTANTE: único lugar que busca conta para um usuário. Conta de outro usuário = 404,
// igual a "não existe". Todo service que precisa de conta passa por aqui (BOLA).
@Component
public class ContaDoUsuario {

    private final ContaRepository contaRepository;

    public ContaDoUsuario(ContaRepository contaRepository) {
        this.contaRepository = contaRepository;
    }

    public Conta buscar(Long usuarioId, Long contaId) {
        return contaRepository.findByIdAndUsuarioId(contaId, usuarioId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Conta não encontrada."));
    }
}