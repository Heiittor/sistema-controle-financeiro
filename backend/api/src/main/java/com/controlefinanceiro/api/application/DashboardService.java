package com.controlefinanceiro.api.application;

import com.controlefinanceiro.api.domain.TipoTransacao;
import com.controlefinanceiro.api.domain.exception.RegraDeNegocioException;
import com.controlefinanceiro.api.infrastructure.ContaRepository;
import com.controlefinanceiro.api.infrastructure.TransacaoRepository;
import com.controlefinanceiro.api.infrastructure.TransacaoRepository.TotalPorCategoria;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
public class DashboardService {
    private static final BigDecimal ZERO = new BigDecimal("0.00");
    private final ContaRepository contas;
    private final TransacaoRepository transacoes;

    public DashboardService(ContaRepository contas, TransacaoRepository transacoes) {
        this.contas = contas;
        this.transacoes = transacoes;
    }

    public record Resumo(LocalDate inicio, LocalDate fim, BigDecimal saldoTotal,
                         BigDecimal totalReceitas, BigDecimal totalDespesas,
                         List<TotalPorCategoria> porCategoria) {
        public BigDecimal saldoDoPeriodo() {
            return totalReceitas.subtract(totalDespesas);
        }
    }

    public record Mes(String mes, BigDecimal receitas, BigDecimal despesas) {}

    // As duas consultas enxergam o mesmo estado do banco, mesmo com movimentações concorrentes.
    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public Resumo resumir(Long usuarioId, LocalDate inicio, LocalDate fim) {
        validarPeriodo(inicio, fim);
        var totais = transacoes.totaisDoUsuarioPorCategoria(usuarioId, inicio, fim);
        return new Resumo(inicio, fim, contas.saldoTotalDoUsuario(usuarioId).setScale(2),
                somar(totais, TipoTransacao.RECEITA), somar(totais, TipoTransacao.DESPESA),
                totais.stream().filter(t -> t.getTipo() == TipoTransacao.DESPESA).toList());
    }

    @Transactional(readOnly = true)
    public List<Mes> serieMensal(Long usuarioId, LocalDate inicio, LocalDate fim) {
        validarPeriodo(inicio, fim);
        var totais = transacoes.totaisMensaisDoUsuario(usuarioId, inicio, fim);
        List<Mes> serie = new ArrayList<>();
        YearMonth ultimo = YearMonth.from(fim);
        for (YearMonth mes = YearMonth.from(inicio); !mes.isAfter(ultimo); mes = mes.plusMonths(1)) {
            BigDecimal receitas = ZERO;
            BigDecimal despesas = ZERO;
            for (var total : totais) {
                if (total.getAno() == mes.getYear() && total.getMes() == mes.getMonthValue()) {
                    if (total.getTipo() == TipoTransacao.RECEITA) {
                        receitas = total.getTotal().setScale(2);
                    } else {
                        despesas = total.getTotal().setScale(2);
                    }
                }
            }
            serie.add(new Mes(mes.toString(), receitas, despesas));
        }
        return List.copyOf(serie);
    }

    private static BigDecimal somar(List<TotalPorCategoria> totais, TipoTransacao tipo) {
        return totais.stream().filter(t -> t.getTipo() == tipo).map(TotalPorCategoria::getTotal)
                .reduce(ZERO, BigDecimal::add).setScale(2);
    }

    private static void validarPeriodo(LocalDate inicio, LocalDate fim) {
        if (inicio == null || fim == null || inicio.isAfter(fim)) {
            throw new RegraDeNegocioException("Informe um período com início não posterior ao fim.");
        }
        if (ChronoUnit.MONTHS.between(YearMonth.from(inicio), YearMonth.from(fim)) >= 24) {
            throw new RegraDeNegocioException("O período deve abranger no máximo 24 meses.");
        }
    }
}
