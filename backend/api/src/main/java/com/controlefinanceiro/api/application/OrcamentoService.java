package com.controlefinanceiro.api.application;

import com.controlefinanceiro.api.domain.*;
import com.controlefinanceiro.api.domain.exception.*;
import com.controlefinanceiro.api.infrastructure.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.YearMonth;

@Service
public class OrcamentoService {
    private final OrcamentoRepository orcamentos;
    private final CategoriaRepository categorias;
    private final UsuarioRepository usuarios;
    private final AuditoriaService auditoria;
    public OrcamentoService(OrcamentoRepository orcamentos, CategoriaRepository categorias,
                            UsuarioRepository usuarios, AuditoriaService auditoria) {
        this.orcamentos = orcamentos;
        this.categorias = categorias;
        this.usuarios = usuarios;
        this.auditoria = auditoria;
    }
    public record Item(Long id, Long categoriaId, String categoriaNome, YearMonth mes, BigDecimal limite,
                       BigDecimal gasto, BigDecimal restante, BigDecimal percentualConsumido,
                       ConsumoOrcamento.Situacao situacao) {}
    public record Resumo(long ativos, long dentroDoPlanejado, long acimaDoLimite) {}
    public record Listagem(Page<Item> pagina, Resumo resumo) {}

    @Transactional
    public Item criar(Long usuarioId, Long categoriaId, YearMonth mes, Dinheiro limite) {
        var categoria = categorias.findByIdAndUsuarioId(categoriaId, usuarioId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Categoria não encontrada."));
        var usuario = usuarios.findById(usuarioId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado."));
        var orcamento = new Orcamento(usuario, categoria, mes, limite);
        if (orcamentos.existsByUsuarioIdAndCategoriaIdAndMes(usuarioId, categoriaId, mes.atDay(1)))
            throw new ConflitoException("Já existe orçamento para esta categoria e mês.");
        var salvo = orcamentos.save(orcamento);
        auditoria.registrar(usuarioId, AcaoAuditoria.CRIAR_ORCAMENTO, EntidadeAuditada.ORCAMENTO,
                salvo.getId(), "categoria=" + categoriaId + "; mes=" + mes);
        return item(salvo, usuarioId);
    }

    @Transactional
    public Item editar(Long usuarioId, Long id, Dinheiro limite) {
        var orcamento = orcamentos.findByIdAndUsuarioId(id, usuarioId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Orçamento não encontrado."));
        orcamento.alterarLimite(limite);
        orcamentos.save(orcamento);
        auditoria.registrar(usuarioId, AcaoAuditoria.EDITAR_ORCAMENTO, EntidadeAuditada.ORCAMENTO,
                id, "mes=" + orcamento.getMes());
        return item(orcamento, usuarioId);
    }

    @Transactional(readOnly = true)
    public Listagem listar(Long usuarioId, YearMonth mes, int pagina, int tamanho) {
        Orcamento.validarMes(mes);
        var de = mes.atDay(1);
        var ate = mes.atEndOfMonth();
        var pageable = PageRequest.of(Math.max(0, pagina), tamanho <= 0 ? 20 : Math.min(100, tamanho));
        var itens = orcamentos.listarComConsumo(usuarioId, de, ate, pageable)
                .map(l -> item(l.getId(), l.getCategoriaId(), l.getCategoriaNome(), YearMonth.from(l.getMes()),
                        l.getLimite(), l.getGasto()));
        var r = orcamentos.resumo(usuarioId, de, ate);
        return new Listagem(itens, new Resumo(r.getAtivos(), r.getDentroDoPlanejado(), r.getAcimaDoLimite()));
    }

    private Item item(Orcamento o, Long usuarioId) {
        var gasto = orcamentos.gastoDaCategoria(usuarioId, o.getCategoria().getId(),
                o.getMes().atDay(1), o.getMes().atEndOfMonth());
        return item(o.getId(), o.getCategoria().getId(), o.getCategoria().getNome(), o.getMes(),
                o.getLimite().valor(), gasto);
    }
    private Item item(Long id, Long categoriaId, String nome, YearMonth mes, BigDecimal limite, BigDecimal gasto) {
        var consumo = ConsumoOrcamento.de(new Dinheiro(limite), gasto);
        return new Item(id, categoriaId, nome, mes, limite, consumo.gasto(), consumo.restante(),
                consumo.percentualConsumido(), consumo.situacao());
    }
}
