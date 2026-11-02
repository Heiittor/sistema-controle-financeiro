package com.controlefinanceiro.api.application;

import com.controlefinanceiro.api.domain.exception.RecursoNaoEncontradoException;
import com.controlefinanceiro.api.infrastructure.TransacaoRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.mockito.ArgumentCaptor;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class HistoricoTransferenciasServiceTest {
    private final TransacaoRepository repository = mock(TransacaoRepository.class);
    private final ContaDoUsuario contas = mock(ContaDoUsuario.class);
    private final HistoricoTransferenciasService service = new HistoricoTransferenciasService(repository, contas);

    @Test
    void contaAlheiaNaoDeveChegarAConsultaDoHistorico() {
        when(contas.buscar(1L, 9L)).thenThrow(new RecursoNaoEncontradoException("Conta não encontrada."));
        assertThrows(RecursoNaoEncontradoException.class, () -> service.listar(1L, 9L, 0, 5));
        verifyNoInteractions(repository);
    }

    @Test
    void deveAplicarOsLimitesEConsultarSomenteODono() {
        when(repository.historicoTransferencias(eq(1L), isNull(), any())).thenReturn(Page.empty());
        service.listar(1L, null, -3, 10000);
        service.listar(1L, null, 0, 0);
        var pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(repository, times(2)).historicoTransferencias(eq(1L), isNull(), pageable.capture());
        assertEquals(0, pageable.getAllValues().get(0).getPageNumber());
        assertEquals(100, pageable.getAllValues().get(0).getPageSize());
        assertEquals(20, pageable.getAllValues().get(1).getPageSize());
        verifyNoInteractions(contas);
    }

    @Test
    void deveMapearAProjecaoSemCalcularValores() {
        var linha = mock(TransacaoRepository.TransferenciaHistorico.class);
        UUID id = UUID.randomUUID();
        when(linha.getTransferenciaId()).thenReturn(id);
        when(linha.getContaOrigemId()).thenReturn(7L);
        when(linha.getContaDestinoId()).thenReturn(8L);
        when(linha.getContaOrigemNome()).thenReturn("Origem");
        when(linha.getContaDestinoNome()).thenReturn("Destino");
        when(linha.getDescricao()).thenReturn("Reserva");
        when(linha.getValor()).thenReturn(new BigDecimal("1206.35"));
        when(linha.getData()).thenReturn(LocalDate.of(2020, 1, 1));
        when(repository.historicoTransferencias(eq(1L), eq(7L), any()))
                .thenReturn(new PageImpl<>(List.of(linha)));
        var pagina = service.listar(1L, 7L, 0, 5);
        verify(contas).buscar(1L, 7L);
        assertEquals(new HistoricoTransferenciasService.Item(id, 7L, "Origem", 8L, "Destino",
                "Reserva", new BigDecimal("1206.35"), LocalDate.of(2020, 1, 1)), pagina.getContent().getFirst());
    }
}
