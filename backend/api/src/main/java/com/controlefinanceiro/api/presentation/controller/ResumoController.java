package com.controlefinanceiro.api.presentation.controller;

import com.controlefinanceiro.api.application.ResumoService;
import com.controlefinanceiro.api.infrastructure.seguranca.UsuarioAutenticado;
import com.controlefinanceiro.api.presentation.dto.ResumoResponse;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/contas/{contaId}/resumo")
public class ResumoController {

    private final ResumoService resumoService;

    public ResumoController(ResumoService resumoService) {
        this.resumoService = resumoService;
    }

    @GetMapping
    public ResumoResponse resumir(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long contaId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim) {
        return ResumoResponse.de(
                resumoService.resumir(UsuarioAutenticado.id(jwt), contaId, inicio, fim));
    }
}