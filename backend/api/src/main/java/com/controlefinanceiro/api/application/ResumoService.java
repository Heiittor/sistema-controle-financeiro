package com.controlefinanceiro.api.application;

import com.controlefinanceiro.api.domain.TipoTransacao;
import com.controlefinanceiro.api.domain.exception.RecursoNaoEncontradoException;
import com.controlefinanceiro.api.domain.exception.RegraDeNegocioException;
import com.controlefinanceiro.api.infrastructure.ContaRepository;
import com.controlefinanceiro.api.infrastructure.TransacaoRepository;
import com.controlefinanceiro.api.infrastructure.TransacaoRepository.TotalPorCategoria;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public class ResumoService {

    private final ContaRepository contaRepository;
    private final TransacaoRepository transacaoRepository;

    public ResumoService(ContaRepository contaRepository, TransacaoRepository transacaoRepository) {
        this.contaRepository = contaRepository;
        this.transacaoRepository = transacaoRepository;
    }

    public record Resumo(LocalDate inicio, LocalDate fim,
                         BigDecimal totalReceitas, BigDecimal totalDespesas,
                         List<TotalPorCategoria> porCategoria) {

        public BigDecimal saldoDoPeriodo() {
            return totalReceitas.subtract(totalDespesas);
        }
    }

    @Transactional(readOnly = true)
    public Resumo resumir(Long usuarioId, Long contaId, LocalDate inicio, LocalDate fim) {
        // IMPORTANTE: confirma que a conta é do usuário. Se não for, 404.
        contaRepository.findByIdAndUsuarioId(contaId, usuarioId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Conta não encontrada."));

        LocalDate hoje = LocalDate.now();
        LocalDate de = inicio != null ? inicio : hoje.withDayOfMonth(1);
        LocalDate ate = fim != null ? fim : hoje.withDayOfMonth(hoje.lengthOfMonth());
        if (de.isAfter(ate)) {
            throw new RegraDeNegocioException("A data inicial não pode ser posterior à data final.");
        }

        List<TotalPorCategoria> totais = transacaoRepository.totaisPorCategoria(contaId, usuarioId, de, ate);

        BigDecimal receitas = somar(totais, TipoTransacao.RECEITA);
        BigDecimal despesas = somar(totais, TipoTransacao.DESPESA);
        return new Resumo(de, ate, receitas, despesas, totais);
    }

    private static BigDecimal somar(List<TotalPorCategoria> totais, TipoTransacao tipo) {
        return totais.stream()
                .filter(t -> t.getTipo() == tipo)
                .map(TotalPorCategoria::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2);
    }
}