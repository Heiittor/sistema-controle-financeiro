package com.controlefinanceiro.api.infrastructure;

import com.controlefinanceiro.api.domain.TipoTransacao;
import com.controlefinanceiro.api.domain.Transacao;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;
import java.util.Collection;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

// IMPORTANTE: sem findById nem findAll. Toda leitura passa pelo dono da conta.
public interface TransacaoRepository extends Repository<Transacao, Long> {

    // Projeção escalar: nenhuma entidade lazy é carregada por linha (sem N+1).
    // O UUID já tem índice. Cada par gera uma única linha, partindo da saída.
    @Query(value = """
            select s.transferenciaId as transferenciaId,
                   origem.id as contaOrigemId, origem.nome as contaOrigemNome,
                   destino.id as contaDestinoId, destino.nome as contaDestinoNome,
                   s.descricao as descricao, s.valor.valor as valor, s.data as data
            from Transacao s
            join Transacao e on e.transferenciaId = s.transferenciaId
            join s.conta origem
            join e.conta destino
            where s.tipo = com.controlefinanceiro.api.domain.TipoTransacao.TRANSFERENCIA_SAIDA
              and e.tipo = com.controlefinanceiro.api.domain.TipoTransacao.TRANSFERENCIA_ENTRADA
              and origem.usuario.id = :usuarioId and destino.usuario.id = :usuarioId
              and (:contaId is null or origem.id = :contaId or destino.id = :contaId)
            order by s.data desc, s.id desc
            """, countQuery = """
            select count(s.id)
            from Transacao s
            join Transacao e on e.transferenciaId = s.transferenciaId
            join s.conta origem
            join e.conta destino
            where s.tipo = com.controlefinanceiro.api.domain.TipoTransacao.TRANSFERENCIA_SAIDA
              and e.tipo = com.controlefinanceiro.api.domain.TipoTransacao.TRANSFERENCIA_ENTRADA
              and origem.usuario.id = :usuarioId and destino.usuario.id = :usuarioId
              and (:contaId is null or origem.id = :contaId or destino.id = :contaId)
            """)
    Page<TransferenciaHistorico> historicoTransferencias(@Param("usuarioId") Long usuarioId,
                                                       @Param("contaId") Long contaId,
                                                       Pageable pageable);

    interface TransferenciaHistorico {
        java.util.UUID getTransferenciaId();
        Long getContaOrigemId();
        String getContaOrigemNome();
        Long getContaDestinoId();
        String getContaDestinoNome();
        String getDescricao();
        BigDecimal getValor();
        LocalDate getData();
    }

    // Só grava o que a Conta já criou e validou. Nunca crie Transacao por fora da Conta.
    Transacao save(Transacao transacao);

    // ContaUsuarioId = conta -> usuario -> id. A transação só aparece se a conta for do usuário.
    Optional<Transacao> findByIdAndContaUsuarioId(Long id, Long usuarioId);

    // IMPORTANTE: além do dono, exige que a transação seja DESTA conta (a da URL).
    Optional<Transacao> findByIdAndContaIdAndContaUsuarioId(Long id, Long contaId, Long usuarioId);

    // Evita estorno duplicado: existe alguma transação que estorna esta?
    boolean existsByEstornoDe_Id(Long estornoDeId);

    // Dentre os ids informados, quais já têm um estorno apontando para eles. Uma consulta por página.
    // Os ids vêm de uma página já filtrada por dono, então esta consulta não abre acesso novo.
    @Query("select t.estornoDe.id from Transacao t where t.estornoDe.id in :ids")
    List<Long> idsEstornados(@Param("ids") Collection<Long> ids);

    // IMPORTANTE: traz a categoria junto, num JOIN só. Evita 1 consulta extra por transação (N+1).
    @EntityGraph(attributePaths = "categoria")
    Page<Transacao> findByContaIdAndContaUsuarioIdAndDataBetween(
            Long contaId, Long usuarioId, LocalDate inicio, LocalDate fim, Pageable pageable);

    // Totais por categoria no período. Só receita e despesa (transferência e estorno ficam de fora).
    // A soma é feita pelo banco: não carregamos as transações na memória.
    // Totais por categoria no período. Só receita e despesa que NÃO foram estornadas.
    // A soma é feita pelo banco: não carregamos as transações na memória.
    @Query("""
            select t.categoria.id as categoriaId, t.categoria.nome as categoriaNome,
                   t.tipo as tipo, sum(t.valor.valor) as total
            from Transacao t
            where t.conta.id = :contaId
              and t.conta.usuario.id = :usuarioId
              and t.data between :inicio and :fim
              and t.tipo in (com.controlefinanceiro.api.domain.TipoTransacao.RECEITA,
                             com.controlefinanceiro.api.domain.TipoTransacao.DESPESA)
              and not exists (select 1 from Transacao e where e.estornoDe.id = t.id)
            group by t.categoria.id, t.categoria.nome, t.tipo
            order by t.categoria.nome
            """)
    List<TotalPorCategoria> totaisPorCategoria(@Param("contaId") Long contaId,
                                               @Param("usuarioId") Long usuarioId,
                                               @Param("inicio") LocalDate inicio,
                                               @Param("fim") LocalDate fim);

    // Dashboard: consolida apenas dados do dono, mantendo a semântica do resumo por conta.
    @Query("""
            select t.categoria.id as categoriaId, t.categoria.nome as categoriaNome,
                   t.tipo as tipo, sum(t.valor.valor) as total
            from Transacao t
            where t.conta.usuario.id = :usuarioId
              and t.data between :inicio and :fim
              and t.tipo in (com.controlefinanceiro.api.domain.TipoTransacao.RECEITA,
                             com.controlefinanceiro.api.domain.TipoTransacao.DESPESA)
              and not exists (select 1 from Transacao e where e.estornoDe.id = t.id)
            group by t.categoria.id, t.categoria.nome, t.tipo
            order by t.categoria.nome, t.categoria.id
            """)
    List<TotalPorCategoria> totaisDoUsuarioPorCategoria(@Param("usuarioId") Long usuarioId,
                                                      @Param("inicio") LocalDate inicio,
                                                      @Param("fim") LocalDate fim);

    @Query("""
            select year(t.data) as ano, month(t.data) as mes, t.tipo as tipo, sum(t.valor.valor) as total
            from Transacao t
            where t.conta.usuario.id = :usuarioId
              and t.data between :inicio and :fim
              and t.tipo in (com.controlefinanceiro.api.domain.TipoTransacao.RECEITA,
                             com.controlefinanceiro.api.domain.TipoTransacao.DESPESA)
              and not exists (select 1 from Transacao e where e.estornoDe.id = t.id)
            group by year(t.data), month(t.data), t.tipo
            order by year(t.data), month(t.data)
            """)
    List<TotalMensal> totaisMensaisDoUsuario(@Param("usuarioId") Long usuarioId,
                                            @Param("inicio") LocalDate inicio,
                                            @Param("fim") LocalDate fim);

    interface TotalMensal {
        Integer getAno();
        Integer getMes();
        TipoTransacao getTipo();
        BigDecimal getTotal();
    }

    // Projeção: o Spring preenche só estes quatro campos.
    interface TotalPorCategoria {
        Long getCategoriaId();
        String getCategoriaNome();
        TipoTransacao getTipo();
        BigDecimal getTotal();
    }
}
