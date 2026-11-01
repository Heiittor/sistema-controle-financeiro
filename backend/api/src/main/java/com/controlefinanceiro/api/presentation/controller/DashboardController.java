package com.controlefinanceiro.api.presentation.controller;

import com.controlefinanceiro.api.application.DashboardService;
import com.controlefinanceiro.api.infrastructure.seguranca.UsuarioAutenticado;
import com.controlefinanceiro.api.presentation.dto.DashboardResumoResponse;
import com.controlefinanceiro.api.presentation.dto.DashboardMesResponse;
import com.controlefinanceiro.api.presentation.dto.ErroResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/dashboard")
@ApiResponses({
        @ApiResponse(responseCode = "400", description = "Parâmetro ausente ou malformado",
                content = @Content(schema = @Schema(implementation = ErroResponse.class))),
        @ApiResponse(responseCode = "401", description = "Autenticação necessária",
                content = @Content(schema = @Schema(implementation = ErroResponse.class))),
        @ApiResponse(responseCode = "422", description = "Período invertido ou acima de 24 meses",
                content = @Content(schema = @Schema(implementation = ErroResponse.class)))
})
public class DashboardController {
    private final DashboardService dashboard;

    public DashboardController(DashboardService dashboard) {
        this.dashboard = dashboard;
    }

    @GetMapping("/resumo")
    @Operation(summary = "Resumo consolidado do usuário autenticado",
            description = "Período inclusivo de até 24 meses civis. Ignora transferências, estornos e originais estornadas. Saldo total é atual.")
    @ApiResponse(responseCode = "200", description = "Resumo financeiro consolidado",
            content = @Content(schema = @Schema(implementation = DashboardResumoResponse.class)))
    public DashboardResumoResponse resumo(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim) {
        return DashboardResumoResponse.de(dashboard.resumir(UsuarioAutenticado.id(jwt), inicio, fim));
    }

    @GetMapping("/serie-mensal")
    @ApiResponse(responseCode = "200", description = "Série mensal em ordem crescente",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = DashboardMesResponse.class))))
    @Operation(summary = "Receitas e despesas mensais do usuário autenticado",
            description = "Até 24 meses em ordem crescente, incluindo meses zerados. Respeita as datas exatas, inclusive meses parciais. Exclui transferências, estornos e originais estornadas.")
    public List<DashboardMesResponse> serieMensal(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim) {
        return dashboard.serieMensal(UsuarioAutenticado.id(jwt), inicio, fim).stream()
                .map(DashboardMesResponse::de).toList();
    }
}
