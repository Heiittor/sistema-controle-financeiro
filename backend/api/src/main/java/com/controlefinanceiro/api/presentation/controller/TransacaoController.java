package com.controlefinanceiro.api.presentation.controller;

import com.controlefinanceiro.api.application.TransacaoService;
import com.controlefinanceiro.api.domain.Dinheiro;
import com.controlefinanceiro.api.infrastructure.seguranca.UsuarioAutenticado;
import com.controlefinanceiro.api.presentation.dto.EditarTransacaoRequest;
import com.controlefinanceiro.api.presentation.dto.PaginaResponse;
import com.controlefinanceiro.api.presentation.dto.RegistrarTransacaoRequest;
import com.controlefinanceiro.api.presentation.dto.TransacaoResponse;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/contas/{contaId}")
public class TransacaoController {

    private final TransacaoService transacaoService;

    public TransacaoController(TransacaoService transacaoService) {
        this.transacaoService = transacaoService;
    }

    @PostMapping("/receitas")
    @ResponseStatus(HttpStatus.CREATED)
    public TransacaoResponse registrarReceita(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long contaId,
            @Valid @RequestBody RegistrarTransacaoRequest request) {
        return TransacaoResponse.de(transacaoService.registrarReceita(
                UsuarioAutenticado.id(jwt), contaId, request.categoriaId(), request.descricao(),
                new Dinheiro(request.valor()), request.data()));
    }

    @PostMapping("/despesas")
    @ResponseStatus(HttpStatus.CREATED)
    public TransacaoResponse registrarDespesa(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long contaId,
            @Valid @RequestBody RegistrarTransacaoRequest request) {
        return TransacaoResponse.de(transacaoService.registrarDespesa(
                UsuarioAutenticado.id(jwt), contaId, request.categoriaId(), request.descricao(),
                new Dinheiro(request.valor()), request.data()));
    }

    @GetMapping("/transacoes")
    public PaginaResponse<TransacaoResponse> listar(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long contaId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return PaginaResponse.de(
                transacaoService.listar(UsuarioAutenticado.id(jwt), contaId, inicio, fim, page, size),
                TransacaoResponse::de);
    }

    // Sem corpo: o estorno é sempre o inverso exato da original, com a data de hoje.
    @PostMapping("/transacoes/{transacaoId}/estorno")
    @ResponseStatus(HttpStatus.CREATED)
    public TransacaoResponse estornar(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long contaId,
            @PathVariable Long transacaoId) {
        return TransacaoResponse.de(
                transacaoService.estornar(UsuarioAutenticado.id(jwt), contaId, transacaoId));
    }

    @PatchMapping("/transacoes/{transacaoId}")
    public TransacaoResponse editar(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long contaId,
            @PathVariable Long transacaoId,
            @Valid @RequestBody EditarTransacaoRequest request) {
        return TransacaoResponse.de(transacaoService.editar(
                UsuarioAutenticado.id(jwt), contaId, transacaoId,
                request.categoriaId(), request.descricao()));
    }
}