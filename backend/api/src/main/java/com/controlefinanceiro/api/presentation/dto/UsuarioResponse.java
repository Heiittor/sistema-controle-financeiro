package com.controlefinanceiro.api.presentation.dto;

import com.controlefinanceiro.api.domain.Usuario;

// IMPORTANTE: só os campos declarados aqui saem na API. A senha não está na lista, então não vaza.
public record UsuarioResponse(Long id, String nome, String email) {

    // Único ponto de conversão Usuario -> JSON. Se a entidade ganhar campos novos, eles NÃO saem sozinhos.
    public static UsuarioResponse de(Usuario usuario) {
        return new UsuarioResponse(usuario.getId(), usuario.getNome(), usuario.getEmail());
    }
}