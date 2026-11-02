package com.controlefinanceiro.api.presentation.dto;
import com.controlefinanceiro.api.application.OrcamentoService.Item;
import com.controlefinanceiro.api.domain.ConsumoOrcamento.Situacao;
import java.math.BigDecimal;
import java.time.YearMonth;
public record OrcamentoResponse(Long id, Long categoriaId, String categoriaNome, YearMonth mes,
        BigDecimal limite, BigDecimal gasto, BigDecimal restante, BigDecimal percentualConsumido, Situacao situacao) {
    public static OrcamentoResponse de(Item i) {
        return new OrcamentoResponse(i.id(), i.categoriaId(), i.categoriaNome(), i.mes(), i.limite(),
                i.gasto(), i.restante(), i.percentualConsumido(), i.situacao());
    }
}
