package com.controlefinanceiro.api.presentation.controller;

import com.controlefinanceiro.api.application.CategoriaService;
import com.controlefinanceiro.api.domain.TipoTransacao;
import com.controlefinanceiro.api.presentation.dto.CategoriaResponse;
import com.controlefinanceiro.api.presentation.dto.CriarCategoriaRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import com.controlefinanceiro.api.infrastructure.seguranca.UsuarioAutenticado;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;

@RestController
@RequestMapping("/categorias")
public class CategoriaController {

    private final CategoriaService categoriaService;

    public CategoriaController(CategoriaService categoriaService) {
        this.categoriaService = categoriaService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CategoriaResponse criar(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CriarCategoriaRequest request) {
        return CategoriaResponse.de(
                categoriaService.criar(UsuarioAutenticado.id(jwt), request.nome(), request.tipo()));
    }

    @GetMapping
    public List<CategoriaResponse> listar(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) TipoTransacao tipo) {
        return categoriaService.listar(UsuarioAutenticado.id(jwt), tipo).stream()
                .map(CategoriaResponse::de)
                .toList();
    }
}