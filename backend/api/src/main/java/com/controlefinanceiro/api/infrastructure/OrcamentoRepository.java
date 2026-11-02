package com.controlefinanceiro.api.infrastructure;

import com.controlefinanceiro.api.domain.Orcamento;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

public interface OrcamentoRepository extends Repository<Orcamento, Long> {
    Orcamento save(Orcamento orcamento);
    @EntityGraph(attributePaths = "categoria")
    Optional<Orcamento> findByIdAndUsuarioId(Long id, Long usuarioId);
    boolean existsByUsuarioIdAndCategoriaIdAndMes(Long usuarioId, Long categoriaId, LocalDate mes);

    // Agregação no banco, restrita ao dono. O estorno não tem corte de data,
    // preservando a semântica dos resumos. Sem consultas adicionais por linha.
    String CONSUMO = """
            with gastos as (
                select t.categoria_id, sum(t.valor) as gasto
                from transacoes t join contas c on c.id = t.conta_id
                where c.usuario_id = :usuarioId and t.tipo = 'DESPESA'
                  and t.data between :inicio and :fim
                  and not exists (select 1 from transacoes e where e.estorno_de_id = t.id)
                group by t.categoria_id
            ), visao as (
                select o.id, o.categoria_id, c.nome, o.mes, o.limite, coalesce(g.gasto, 0) as gasto
                from orcamentos o join categorias c on c.id = o.categoria_id
                left join gastos g on g.categoria_id = o.categoria_id
                where o.usuario_id = :usuarioId and c.usuario_id = :usuarioId and o.mes = :inicio
            )
            """;
    @Query(value = CONSUMO + """
            select id, categoria_id as "categoriaId", nome as "categoriaNome", mes, limite, gasto
            from visao order by nome, id
            """, countQuery = """
            select count(*) from orcamentos o join categorias c on c.id = o.categoria_id
            where o.usuario_id = :usuarioId and c.usuario_id = :usuarioId and o.mes = :inicio
            """, nativeQuery = true)
    Page<Linha> listarComConsumo(@Param("usuarioId") Long usuarioId, @Param("inicio") LocalDate inicio,
                                @Param("fim") LocalDate fim, Pageable pageable);

    @Query(value = CONSUMO + """
            select count(*) as ativos,
                   count(*) filter (where gasto <= limite) as "dentroDoPlanejado",
                   count(*) filter (where gasto > limite) as "acimaDoLimite"
            from visao
            """, nativeQuery = true)
    Resumo resumo(@Param("usuarioId") Long usuarioId, @Param("inicio") LocalDate inicio, @Param("fim") LocalDate fim);

    @Query(value = """
            select coalesce(sum(t.valor), 0) from transacoes t join contas c on c.id = t.conta_id
            where c.usuario_id = :usuarioId and t.categoria_id = :categoriaId and t.tipo = 'DESPESA'
              and t.data between :inicio and :fim
              and not exists (select 1 from transacoes e where e.estorno_de_id = t.id)
            """, nativeQuery = true)
    BigDecimal gastoDaCategoria(@Param("usuarioId") Long usuarioId, @Param("categoriaId") Long categoriaId,
                                @Param("inicio") LocalDate inicio, @Param("fim") LocalDate fim);

    interface Linha {
        Long getId();
        Long getCategoriaId();
        String getCategoriaNome();
        LocalDate getMes();
        BigDecimal getLimite();
        BigDecimal getGasto();
    }
    interface Resumo {
        long getAtivos();
        long getDentroDoPlanejado();
        long getAcimaDoLimite();
    }
}
