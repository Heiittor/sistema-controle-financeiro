package com.controlefinanceiro.api.presentation.controller;

import com.controlefinanceiro.api.application.TransacaoService;
import com.controlefinanceiro.api.domain.Dinheiro;
import com.controlefinanceiro.api.presentation.dto.RegistrarTransacaoRequest;
import com.controlefinanceiro.api.presentation.dto.TransacaoResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

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
            // TEMPORÁRIO: na Fase 7 o usuário virá do token JWT. NUNCA vai para produção assim.
            @RequestHeader("X-Usuario-Id") Long usuarioId,
            @PathVariable Long contaId,
            @Valid @RequestBody RegistrarTransacaoRequest request) {
        return TransacaoResponse.de(transacaoService.registrarReceita(
                usuarioId, contaId, request.categoriaId(), request.descricao(),
                new Dinheiro(request.valor()), request.data()));
    }

    @PostMapping("/despesas")
    @ResponseStatus(HttpStatus.CREATED)
    public TransacaoResponse registrarDespesa(
            // TEMPORÁRIO: mesma ponte provisória.
            @RequestHeader("X-Usuario-Id") Long usuarioId,
            @PathVariable Long contaId,
            @Valid @RequestBody RegistrarTransacaoRequest request) {
        // O Controller não decide saldo. Quem recusa "Saldo insuficiente" é Conta.debitar (422).
        return TransacaoResponse.de(transacaoService.registrarDespesa(
                usuarioId, contaId, request.categoriaId(), request.descricao(),
                new Dinheiro(request.valor()), request.data()));
    }
}