package com.controlefinanceiro.api.presentation.controller;

import com.controlefinanceiro.api.application.OrcamentoService;
import com.controlefinanceiro.api.domain.Dinheiro;
import com.controlefinanceiro.api.infrastructure.seguranca.UsuarioAutenticado;
import com.controlefinanceiro.api.presentation.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import java.time.YearMonth;

@RestController
@RequestMapping("/orcamentos")
public class OrcamentoController {
    private final OrcamentoService service;
    public OrcamentoController(OrcamentoService service) { this.service = service; }
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cria limite mensal para uma categoria de despesa do dono autenticado")
    public OrcamentoResponse criar(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CriarOrcamentoRequest r) {
        return OrcamentoResponse.de(service.criar(UsuarioAutenticado.id(jwt), r.categoriaId(), r.mes(), new Dinheiro(r.limite())));
    }
    @PatchMapping("/{id}")
    @Operation(summary = "Edita somente o limite de um orçamento do dono autenticado")
    public OrcamentoResponse editar(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
                                    @Valid @RequestBody EditarOrcamentoRequest r) {
        return OrcamentoResponse.de(service.editar(UsuarioAutenticado.id(jwt), id, new Dinheiro(r.limite())));
    }
    @GetMapping
    @Operation(summary = "Lista orçamentos do mês com consumo e resumo de todo o mês",
            description = "mes obrigatório no formato AAAA-MM. Ignora transferências, estornos e originais estornadas. "
                    + "ATENCAO desde 80% até 100%; ACIMA_DO_LIMITE acima de 100%. "
                    + "Paginação base zero, size máximo 100. Não altera saldos.")
    public OrcamentosResponse listar(@AuthenticationPrincipal Jwt jwt,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth mes,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
        return OrcamentosResponse.de(service.listar(UsuarioAutenticado.id(jwt), mes, page, size));
    }
}
