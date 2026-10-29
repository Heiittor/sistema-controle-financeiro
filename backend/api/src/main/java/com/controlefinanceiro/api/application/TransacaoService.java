package com.controlefinanceiro.api.application;

import com.controlefinanceiro.api.domain.AcaoAuditoria;
import com.controlefinanceiro.api.domain.Categoria;
import com.controlefinanceiro.api.domain.Conta;
import com.controlefinanceiro.api.domain.Dinheiro;
import com.controlefinanceiro.api.domain.EntidadeAuditada;
import com.controlefinanceiro.api.domain.Transacao;
import com.controlefinanceiro.api.domain.exception.ConflitoException;
import com.controlefinanceiro.api.domain.exception.RecursoNaoEncontradoException;
import com.controlefinanceiro.api.domain.exception.RegraDeNegocioException;
import com.controlefinanceiro.api.infrastructure.CategoriaRepository;
import com.controlefinanceiro.api.infrastructure.ContaRepository;
import com.controlefinanceiro.api.infrastructure.TransacaoRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class TransacaoService {

    private static final int TAMANHO_MAXIMO_PAGINA = 100;
    private static final int TAMANHO_PADRAO_PAGINA = 20;

    private final ContaRepository contaRepository;
    private final CategoriaRepository categoriaRepository;
    private final TransacaoRepository transacaoRepository;
    private final AuditoriaService auditoriaService;
    private final ContaDoUsuario contaDoUsuario;
    private final Clock relogio;

    public TransacaoService(ContaRepository contaRepository,
                            CategoriaRepository categoriaRepository,
                            TransacaoRepository transacaoRepository,
                            AuditoriaService auditoriaService,
                            ContaDoUsuario contaDoUsuario,
                            Clock relogio) {
        this.contaRepository = contaRepository;
        this.categoriaRepository = categoriaRepository;
        this.transacaoRepository = transacaoRepository;
        this.auditoriaService = auditoriaService;
        this.contaDoUsuario = contaDoUsuario;
        this.relogio = relogio;
    }

    // IMPORTANTE: @Transactional faz saldo, transação e auditoria valerem juntos ou nenhum.
    @Transactional
    public TransacaoComEstorno registrarReceita(Long usuarioId, Long contaId, Long categoriaId,
                                                String descricao, Dinheiro valor, LocalDate data) {
        Conta conta = contaDoUsuario.buscar(usuarioId, contaId);
        Categoria categoria = buscarCategoria(usuarioId, categoriaId);

        // As regras moram na Conta. O Service não decide nada sobre saldo.
        Transacao transacao = conta.registrarReceita(descricao, valor, data, categoria);

        Transacao salva = gravar(conta, transacao);
        auditarMovimento(usuarioId, AcaoAuditoria.REGISTRAR_RECEITA, salva);
        // Uma transação recém-criada não pode ter sido estornada.
        return new TransacaoComEstorno(salva, false);
    }

    @Transactional
    public TransacaoComEstorno registrarDespesa(Long usuarioId, Long contaId, Long categoriaId,
                                                String descricao, Dinheiro valor, LocalDate data) {
        Conta conta = contaDoUsuario.buscar(usuarioId, contaId);
        Categoria categoria = buscarCategoria(usuarioId, categoriaId);

        Transacao transacao = conta.registrarDespesa(descricao, valor, data, categoria);

        Transacao salva = gravar(conta, transacao);
        auditarMovimento(usuarioId, AcaoAuditoria.REGISTRAR_DESPESA, salva);
        return new TransacaoComEstorno(salva, false);
    }

    // Desfaz uma receita ou despesa com um lançamento inverso. Nada é apagado.
    @Transactional
    public TransacaoComEstorno estornar(Long usuarioId, Long contaId, Long transacaoId) {
        // IMPORTANTE: a conta vem primeiro. Na mesma sessão, a transação reaproveita essa instância.
        Conta conta = contaDoUsuario.buscar(usuarioId, contaId);
        Transacao original = buscarTransacao(usuarioId, contaId, transacaoId);

        if (transacaoRepository.existsByEstornoDe_Id(transacaoId)) {
            throw new ConflitoException("Esta transação já foi estornada.");
        }

        // Regras (transferência, estorno, saldo) moram na Conta.
        Transacao estorno = conta.estornar(original, LocalDate.now(relogio));

        Transacao salvo = gravar(conta, estorno);

        // O registro fica na transação ORIGINAL, com o id do estorno nos detalhes.
        auditoriaService.registrar(usuarioId, AcaoAuditoria.ESTORNAR, EntidadeAuditada.TRANSACAO,
                transacaoId, "conta=" + contaId + "; estornoId=" + salvo.getId()
                        + "; valor=" + salvo.getValor().valor().toPlainString());
        // O estorno em si nunca é estornável, então devolve false.
        return new TransacaoComEstorno(salvo, false);
    }

    // Edita só descrição e categoria. O saldo não muda.
    @Transactional
    public TransacaoComEstorno editar(Long usuarioId, Long contaId, Long transacaoId,
                                      Long categoriaId, String descricao) {
        Conta conta = contaDoUsuario.buscar(usuarioId, contaId);
        Transacao transacao = buscarTransacao(usuarioId, contaId, transacaoId);
        Categoria categoria = buscarCategoria(usuarioId, categoriaId);

        conta.editarTransacao(transacao, descricao, categoria);

        Transacao salva = transacaoRepository.save(transacao);

        // Só ids: a descrição digitada pelo usuário não vai para a auditoria.
        auditoriaService.registrar(usuarioId, AcaoAuditoria.EDITAR_TRANSACAO, EntidadeAuditada.TRANSACAO,
                transacaoId, "conta=" + contaId + "; categoriaId=" + categoriaId);
        // Editar uma transação já estornada é permitido, então o estado precisa vir do banco.
        return new TransacaoComEstorno(salva, transacaoRepository.existsByEstornoDe_Id(transacaoId));
    }

    @Transactional(readOnly = true)
    public Page<TransacaoComEstorno> listar(Long usuarioId, Long contaId,
                                            LocalDate inicio, LocalDate fim, int pagina, int tamanho) {
        // IMPORTANTE: confirma que a conta é do usuário. Se não for, 404 (e não lista vazia).
        contaDoUsuario.buscar(usuarioId, contaId);

        LocalDate hoje = LocalDate.now(relogio);
        LocalDate de = inicio != null ? inicio : hoje.withDayOfMonth(1);
        LocalDate ate = fim != null ? fim : hoje.withDayOfMonth(hoje.lengthOfMonth());
        if (de.isAfter(ate)) {
            throw new RegraDeNegocioException("A data inicial não pode ser posterior à data final.");
        }

        // IMPORTANTE: limita o tamanho da página para ninguém pedir milhões de linhas de uma vez.
        int tamanhoSeguro = tamanho <= 0 ? TAMANHO_PADRAO_PAGINA : Math.min(tamanho, TAMANHO_MAXIMO_PAGINA);
        int paginaSegura = Math.max(pagina, 0);

        Pageable pageable = PageRequest.of(paginaSegura, tamanhoSeguro,
                Sort.by(Sort.Order.desc("data"), Sort.Order.desc("id")));

        Page<Transacao> pagina1 = transacaoRepository.findByContaIdAndContaUsuarioIdAndDataBetween(
                contaId, usuarioId, de, ate, pageable);
        return comEstado(pagina1);
    }

    // Uma única consulta por página responde "quais destas já foram estornadas?" (sem N+1).
    // O estorno costuma estar em outra página ou período, por isso não dá para deduzi-lo só da página.
    private Page<TransacaoComEstorno> comEstado(Page<Transacao> pagina) {
        List<Long> ids = pagina.getContent().stream().map(Transacao::getId).toList();
        Set<Long> estornadas = ids.isEmpty()
                ? Set.of()
                : new HashSet<>(transacaoRepository.idsEstornados(ids));
        return pagina.map(t -> new TransacaoComEstorno(t, estornadas.contains(t.getId())));
    }

    private Categoria buscarCategoria(Long usuarioId, Long categoriaId) {
        return categoriaRepository.findByIdAndUsuarioId(categoriaId, usuarioId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Categoria não encontrada."));
    }

    // IMPORTANTE: a transação precisa ser da conta da URL E do usuário. Senão, 404.
    private Transacao buscarTransacao(Long usuarioId, Long contaId, Long transacaoId) {
        return transacaoRepository.findByIdAndContaIdAndContaUsuarioId(transacaoId, contaId, usuarioId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Transação não encontrada."));
    }

    // Grava o saldo novo e a transação dentro da mesma transação do banco.
    private Transacao gravar(Conta conta, Transacao transacao) {
        contaRepository.save(conta);
        return transacaoRepository.save(transacao);
    }

    // Receita e despesa: conta, categoria e valor (sem a descrição digitada).
    private void auditarMovimento(Long usuarioId, AcaoAuditoria acao, Transacao salva) {
        auditoriaService.registrar(usuarioId, acao, EntidadeAuditada.TRANSACAO, salva.getId(),
                "conta=" + salva.getConta().getId()
                        + "; categoriaId=" + salva.getCategoria().getId()
                        + "; valor=" + salva.getValor().valor().toPlainString());
    }
}