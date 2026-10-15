package com.controlefinanceiro.api.application;

import com.controlefinanceiro.api.domain.Conta;
import com.controlefinanceiro.api.domain.Conta.ParTransferencia;
import com.controlefinanceiro.api.domain.Dinheiro;
import com.controlefinanceiro.api.domain.exception.RecursoNaoEncontradoException;
import com.controlefinanceiro.api.domain.exception.RegraDeNegocioException;
import com.controlefinanceiro.api.infrastructure.ContaRepository;
import com.controlefinanceiro.api.infrastructure.TransacaoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Objects;

@Service
public class TransferenciaService {

    private final ContaRepository contaRepository;
    private final TransacaoRepository transacaoRepository;

    public TransferenciaService(ContaRepository contaRepository,
                                TransacaoRepository transacaoRepository) {
        this.contaRepository = contaRepository;
        this.transacaoRepository = transacaoRepository;
    }

    // IMPORTANTE: tudo ou nada. Falhou em qualquer ponto, nada é gravado.
    @Transactional
    public ParTransferencia transferir(Long usuarioId, Long contaOrigemId, Long contaDestinoId,
                                       String descricao, Dinheiro valor, LocalDate data) {
        if (Objects.equals(contaOrigemId, contaDestinoId)) {
            throw new RegraDeNegocioException("A conta de origem e a de destino devem ser diferentes.");
        }

        // Carrega sempre na mesma ordem (menor id primeiro), independente de quem é origem.
        Conta origem;
        Conta destino;
        if (contaOrigemId < contaDestinoId) {
            origem = buscarConta(usuarioId, contaOrigemId);
            destino = buscarConta(usuarioId, contaDestinoId);
        } else {
            destino = buscarConta(usuarioId, contaDestinoId);
            origem = buscarConta(usuarioId, contaOrigemId);
        }

        // As regras (saldo, mesmo dono) moram na Conta. O Service só conduz.
        ParTransferencia par = origem.transferirPara(destino, descricao, valor, data);

        contaRepository.save(origem);
        contaRepository.save(destino);
        transacaoRepository.save(par.saida());
        transacaoRepository.save(par.entrada());
        return par;
    }

    // IMPORTANTE: busca com o dono. Conta de outro usuário = "não encontrada" (404).
    private Conta buscarConta(Long usuarioId, Long contaId) {
        return contaRepository.findByIdAndUsuarioId(contaId, usuarioId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Conta não encontrada."));
    }
}