package com.controlefinanceiro.api.domain;

import com.controlefinanceiro.api.domain.exception.RegraDeNegocioException;
import java.math.BigDecimal;
import java.math.RoundingMode;

// Value Object de leitura. Restante negativo não representa saldo de Conta.
public record ConsumoOrcamento(BigDecimal gasto, BigDecimal restante,
                               BigDecimal percentualConsumido, Situacao situacao) {
    public enum Situacao { DENTRO_DO_LIMITE, ATENCAO, ACIMA_DO_LIMITE }
    public static ConsumoOrcamento de(Dinheiro limite, BigDecimal gasto) {
        if (limite == null || limite.ehZero())
            throw new RegraDeNegocioException("O limite deve ser maior que zero.");
        if (gasto == null || gasto.signum() < 0)
            throw new RegraDeNegocioException("O consumo não pode ser negativo.");
        Situacao situacao = gasto.compareTo(limite.valor()) > 0 ? Situacao.ACIMA_DO_LIMITE
                : gasto.multiply(BigDecimal.valueOf(100)).compareTo(limite.valor().multiply(BigDecimal.valueOf(80))) >= 0
                ? Situacao.ATENCAO : Situacao.DENTRO_DO_LIMITE;
        return new ConsumoOrcamento(gasto, limite.valor().subtract(gasto),
                gasto.multiply(BigDecimal.valueOf(100)).divide(limite.valor(), 2, RoundingMode.HALF_UP), situacao);
    }
}
