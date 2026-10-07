package com.controlefinanceiro.api.presentation.controller;

import com.controlefinanceiro.api.application.ContaService;
import com.controlefinanceiro.api.presentation.dto.ContaResponse;
import com.controlefinanceiro.api.presentation.dto.CriarContaRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

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
            // TEMPORÁRIO: na Fase 7 o usuário virá do token JWT. NUNCA vai para produção assim.
            @RequestHeader("X-Usuario-Id") Long usuarioId,
            @Valid @RequestBody CriarContaRequest request) {
        return ContaResponse.de(contaService.criar(usuarioId, request.nome()));
    }

    @GetMapping
    public List<ContaResponse> listar(
            // TEMPORÁRIO: mesma ponte provisória do POST.
            @RequestHeader("X-Usuario-Id") Long usuarioId) {
        // O Service já filtra pelo dono: nunca existe "listar todas as contas".
        return contaService.listar(usuarioId).stream()
                .map(ContaResponse::de)
                .toList();
    }
}