package com.controlefinanceiro.api.presentation.dto;

import com.controlefinanceiro.api.domain.Categoria;
import com.controlefinanceiro.api.domain.TipoTransacao;

// IMPORTANTE: só os campos declarados aqui saem na API. O dono (usuario) não está na lista.
public record CategoriaResponse(Long id, String nome, TipoTransacao tipo) {

    // Único ponto de conversão Categoria -> JSON.
    public static CategoriaResponse de(Categoria categoria) {
        return new CategoriaResponse(categoria.getId(), categoria.getNome(), categoria.getTipo());
    }
}