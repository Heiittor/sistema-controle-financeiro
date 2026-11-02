package com.controlefinanceiro.api.presentation.controller;

import com.controlefinanceiro.api.application.TransferenciaService;
import com.controlefinanceiro.api.application.HistoricoTransferenciasService;
import com.controlefinanceiro.api.presentation.dto.HistoricoTransferenciaResponse;
import com.controlefinanceiro.api.presentation.dto.PaginaResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import io.swagger.v3.oas.annotations.Operation;
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
    private final HistoricoTransferenciasService historico;

    public TransferenciaController(TransferenciaService transferenciaService, HistoricoTransferenciasService historico) {
        this.transferenciaService = transferenciaService;
        this.historico = historico;
    }

    @GetMapping
    @Operation(summary = "Histórico paginado de transferências do usuário",
            description = "Uma linha por operação, da mais recente para a mais antiga. Sem corte de período. "
                    + "contaId opcional inclui entradas e saídas da conta. page começa em zero; "
                    + "size máximo 100, não positivo usa 20; page negativo usa zero.")
    public PaginaResponse<HistoricoTransferenciaResponse> listar(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) Long contaId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size) {
        return PaginaResponse.de(historico.listar(UsuarioAutenticado.id(jwt), contaId, page, size),
                HistoricoTransferenciaResponse::de);
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
