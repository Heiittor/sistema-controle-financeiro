package com.controlefinanceiro.api.application;

import com.controlefinanceiro.api.infrastructure.TransacaoRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Service
public class HistoricoTransferenciasService {
    private final TransacaoRepository transacoes;
    private final ContaDoUsuario contaDoUsuario;

    public HistoricoTransferenciasService(TransacaoRepository transacoes, ContaDoUsuario contaDoUsuario) {
        this.transacoes = transacoes;
        this.contaDoUsuario = contaDoUsuario;
    }

    public record Item(UUID transferenciaId, Long contaOrigemId, String contaOrigemNome,
                       Long contaDestinoId, String contaDestinoNome,
                       String descricao, BigDecimal valor, LocalDate data) {}

    @Transactional(readOnly = true)
    public Page<Item> listar(Long usuarioId, Long contaId, int pagina, int tamanho) {
        if (contaId != null) contaDoUsuario.buscar(usuarioId, contaId);
        // Mesmo comportamento do histórico de transações: página negativa vira 0,
        // tamanho não positivo usa 20 e pedidos acima de 100 são limitados.
        int tamanhoSeguro = tamanho <= 0 ? 20 : Math.min(tamanho, 100);
        var pageable = PageRequest.of(Math.max(pagina, 0), tamanhoSeguro);
        return transacoes.historicoTransferencias(usuarioId, contaId, pageable)
                .map(t -> new Item(t.getTransferenciaId(), t.getContaOrigemId(), t.getContaOrigemNome(),
                        t.getContaDestinoId(), t.getContaDestinoNome(), t.getDescricao(), t.getValor(), t.getData()));
    }
}
