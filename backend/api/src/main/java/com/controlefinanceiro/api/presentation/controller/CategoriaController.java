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
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

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
            // TEMPORÁRIO: na Fase 7 o usuário virá do token JWT. NUNCA vai para produção assim.
            @RequestHeader("X-Usuario-Id") Long usuarioId,
            @Valid @RequestBody CriarCategoriaRequest request) {
        return CategoriaResponse.de(
                categoriaService.criar(usuarioId, request.nome(), request.tipo()));
    }

    @GetMapping
    public List<CategoriaResponse> listar(
            // TEMPORÁRIO: mesma ponte provisória do POST.
            @RequestHeader("X-Usuario-Id") Long usuarioId,
            // IMPORTANTE: filtro opcional. Sem o parâmetro, o Service devolve todas as categorias do usuário.
            @RequestParam(required = false) TipoTransacao tipo) {
        return categoriaService.listar(usuarioId, tipo).stream()
                .map(CategoriaResponse::de)
                .toList();
    }
}