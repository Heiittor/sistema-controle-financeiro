package com.controlefinanceiro.api.presentation.controller;

import com.controlefinanceiro.api.application.ContaService;
import com.controlefinanceiro.api.presentation.dto.ContaResponse;
import com.controlefinanceiro.api.presentation.dto.CriarContaRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import com.controlefinanceiro.api.infrastructure.seguranca.UsuarioAutenticado;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;

@RestController
@RequestMapping("/contas")
public class ContaController {

    private final ContaService contaService;

    public ContaController(ContaService contaService) {
        this.contaService = contaService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ContaResponse criar(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CriarContaRequest request) {
        return ContaResponse.de(contaService.criar(UsuarioAutenticado.id(jwt), request.nome()));
    }

    @GetMapping
    public List<ContaResponse> listar(@AuthenticationPrincipal Jwt jwt) {
        return contaService.listar(UsuarioAutenticado.id(jwt)).stream()
                .map(ContaResponse::de)
                .toList();
    }
}