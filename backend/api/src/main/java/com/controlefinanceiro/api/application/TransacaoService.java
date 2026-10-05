package com.controlefinanceiro.api.application;

import com.controlefinanceiro.api.domain.Categoria;
import com.controlefinanceiro.api.domain.Conta;
import com.controlefinanceiro.api.domain.Dinheiro;
import com.controlefinanceiro.api.domain.Transacao;
import com.controlefinanceiro.api.infrastructure.CategoriaRepository;
import com.controlefinanceiro.api.infrastructure.ContaRepository;
import com.controlefinanceiro.api.infrastructure.TransacaoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
public class TransacaoService {

    private final ContaRepository contaRepository;
    private final CategoriaRepository categoriaRepository;
    private final TransacaoRepository transacaoRepository;

    public TransacaoService(ContaRepository contaRepository,
                            CategoriaRepository categoriaRepository,
                            TransacaoRepository transacaoRepository) {
        this.contaRepository = contaRepository;
        this.categoriaRepository = categoriaRepository;
        this.transacaoRepository = transacaoRepository;
    }

    // IMPORTANTE: @Transactional faz as duas gravações valerem juntas ou nenhuma.
    @Transactional
    public Transacao registrarReceita(Long usuarioId, Long contaId, Long categoriaId,
                                      String descricao, Dinheiro valor, LocalDate data) {
        Conta conta = buscarConta(usuarioId, contaId);
        Categoria categoria = buscarCategoria(usuarioId, categoriaId);

        // As regras moram na Conta. O Service não decide nada sobre saldo.
        Transacao transacao = conta.registrarReceita(descricao, valor, data, categoria);

        return gravar(conta, transacao);
    }

    @Transactional
    public Transacao registrarDespesa(Long usuarioId, Long contaId, Long categoriaId,
                                      String descricao, Dinheiro valor, LocalDate data) {
        Conta conta = buscarConta(usuarioId, contaId);
        Categoria categoria = buscarCategoria(usuarioId, categoriaId);

        Transacao transacao = conta.registrarDespesa(descricao, valor, data, categoria);

        return gravar(conta, transacao);
    }

    // IMPORTANTE: busca sempre com o dono. Conta de outro usuário = "não encontrada".
    private Conta buscarConta(Long usuarioId, Long contaId) {
        return contaRepository.findByIdAndUsuarioId(contaId, usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Conta não encontrada."));
    }

    private Categoria buscarCategoria(Long usuarioId, Long categoriaId) {
        return categoriaRepository.findByIdAndUsuarioId(categoriaId, usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Categoria não encontrada."));
    }

    // Grava o saldo novo e a transação dentro da mesma transação do banco.
    private Transacao gravar(Conta conta, Transacao transacao) {
        contaRepository.save(conta);
        return transacaoRepository.save(transacao);
    }
}