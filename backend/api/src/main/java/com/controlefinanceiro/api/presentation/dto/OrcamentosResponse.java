package com.controlefinanceiro.api.presentation.dto;
import com.controlefinanceiro.api.application.OrcamentoService.Listagem;
import java.util.List;
public record OrcamentosResponse(List<OrcamentoResponse> conteudo, int pagina, int tamanho,
                                 long totalElementos, int totalPaginas, Resumo resumo) {
    public record Resumo(long ativos, long dentroDoPlanejado, long acimaDoLimite) {}
    public static OrcamentosResponse de(Listagem l) {
        var p = l.pagina();
        return new OrcamentosResponse(p.getContent().stream().map(OrcamentoResponse::de).toList(),
                p.getNumber(), p.getSize(), p.getTotalElements(), p.getTotalPages(),
                new Resumo(l.resumo().ativos(), l.resumo().dentroDoPlanejado(), l.resumo().acimaDoLimite()));
    }
}
