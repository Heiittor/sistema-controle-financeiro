package com.controlefinanceiro.api.presentation.controller;

import com.controlefinanceiro.api.application.TransferenciaService;
import com.controlefinanceiro.api.domain.Dinheiro;
import com.controlefinanceiro.api.infrastructure.seguranca.UsuarioAutenticado;
import com.controlefinanceiro.api.presentation.dto.TransferenciaResponse;
import com.controlefinanceiro.api.presentation.dto.TransferirRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/transferencias")
public class TransferenciaController {

    private final TransferenciaService transferenciaService;

    public TransferenciaController(TransferenciaService transferenciaService) {
        this.transferenciaService = transferenciaService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TransferenciaResponse transferir(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody TransferirRequest request) {
        return TransferenciaResponse.de(transferenciaService.transferir(
                UsuarioAutenticado.id(jwt),
                request.contaOrigemId(), request.contaDestinoId(),
                request.descricao(), new Dinheiro(request.valor()), request.data()));
    }
}