package com.controlefinanceiro.api.infrastructure;

import com.controlefinanceiro.api.domain.Transacao;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.Repository;

import java.time.LocalDate;
import java.util.Optional;

// IMPORTANTE: sem findById nem findAll. Toda leitura passa pelo dono da conta.
public interface TransacaoRepository extends Repository<Transacao, Long> {

    // Só grava o que a Conta já criou e validou. Nunca crie Transacao por fora da Conta.
    Transacao save(Transacao transacao);

    // IMPORTANTE: ContaUsuarioId = conta -> usuario -> id.
    // A transação só aparece se a conta dela pertencer ao usuário que pediu.
    Optional<Transacao> findByIdAndContaUsuarioId(Long id, Long usuarioId);

    // Histórico de UMA conta, num período, paginado.
    // Exige conta e dono: se a conta for de outro usuário, a lista vem vazia.
    Page<Transacao> findByContaIdAndContaUsuarioIdAndDataBetween(
            Long contaId, Long usuarioId, LocalDate inicio, LocalDate fim, Pageable pageable);
}