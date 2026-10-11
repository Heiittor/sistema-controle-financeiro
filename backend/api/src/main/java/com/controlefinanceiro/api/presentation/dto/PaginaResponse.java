package com.controlefinanceiro.api.presentation.dto;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

// IMPORTANTE: formato próprio de página. A API não depende do JSON interno do Page do Spring.
public record PaginaResponse<T>(
        List<T> conteudo,
        int pagina,
        int tamanho,
        long totalElementos,
        int totalPaginas
) {

    // Converte cada item (ex.: Transacao -> TransacaoResponse) e copia os números da página.
    public static <E, T> PaginaResponse<T> de(Page<E> page, Function<E, T> conversor) {
        return new PaginaResponse<>(
                page.getContent().stream().map(conversor).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages());
    }
}