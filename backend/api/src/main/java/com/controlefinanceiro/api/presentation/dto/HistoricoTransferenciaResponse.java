package com.controlefinanceiro.api.presentation.dto;

import com.controlefinanceiro.api.application.HistoricoTransferenciasService.Item;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record HistoricoTransferenciaResponse(
        UUID transferenciaId, Long contaOrigemId, String contaOrigemNome,
        Long contaDestinoId, String contaDestinoNome,
        String descricao, BigDecimal valor, LocalDate data) {
    public static HistoricoTransferenciaResponse de(Item item) {
        return new HistoricoTransferenciaResponse(item.transferenciaId(), item.contaOrigemId(),
                item.contaOrigemNome(), item.contaDestinoId(), item.contaDestinoNome(),
                item.descricao(), item.valor(), item.data());
    }
}
