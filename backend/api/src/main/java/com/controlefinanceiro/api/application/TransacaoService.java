package com.controlefinanceiro.api.application;

import com.controlefinanceiro.api.domain.Categoria;
import com.controlefinanceiro.api.domain.Conta;
import com.controlefinanceiro.api.domain.Dinheiro;
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

import java.time.LocalDate;

@Service
public class TransacaoService {

    private static final int TAMANHO_MAXIMO_PAGINA = 100;
    private static final int TAMANHO_PADRAO_PAGINA = 20;

    private final ContaRepository contaRepository;
    private final CategoriaRepository categoriaRepository;
    private final TransacaoRepository transacaoRepository;

    public TransacaoService(ContaRepository contaRepository,
                            CategoriaRepository categoriaRepository,
                            TransacaoRepository transacaoRepository) {
        this.contaRepository = contaRepository;
        this.categoriaRepository = categoriaRepository;
        this.transacaoRepository = transacaoRepository;
    }

    // IMPORTANTE: @Transactional faz as duas gravações valerem juntas ou nenhuma.
    @Transactional
    public Transacao registrarReceita(Long usuarioId, Long contaId, Long categoriaId,
                                      String descricao, Dinheiro valor, LocalDate data) {
        Conta conta = buscarConta(usuarioId, contaId);
        Categoria categoria = buscarCategoria(usuarioId, categoriaId);

        // As regras moram na Conta. O Service não decide nada sobre saldo.
        Transacao transacao = conta.registrarReceita(descricao, valor, data, categoria);

        return gravar(conta, transacao);
    }

    @Transactional
    public Transacao registrarDespesa(Long usuarioId, Long contaId, Long categoriaId,
                                      String descricao, Dinheiro valor, LocalDate data) {
        Conta conta = buscarConta(usuarioId, contaId);
        Categoria categoria = buscarCategoria(usuarioId, categoriaId);

        Transacao transacao = conta.registrarDespesa(descricao, valor, data, categoria);

        return gravar(conta, transacao);
    }

    // Desfaz uma receita ou despesa com um lançamento inverso. Nada é apagado.
    @Transactional
    public Transacao estornar(Long usuarioId, Long contaId, Long transacaoId) {
        // IMPORTANTE: a conta vem primeiro. Na mesma sessão, a transação reaproveita essa instância.
        Conta conta = buscarConta(usuarioId, contaId);
        Transacao original = buscarTransacao(usuarioId, contaId, transacaoId);

        if (transacaoRepository.existsByEstornoDeId(transacaoId)) {
            throw new ConflitoException("Esta transação já foi estornada.");
        }

        // Regras (transferência, estorno, saldo) moram na Conta.
        Transacao estorno = conta.estornar(original, LocalDate.now());

        return gravar(conta, estorno);
    }

    // Edita só descrição e categoria. O saldo não muda.
    @Transactional
    public Transacao editar(Long usuarioId, Long contaId, Long transacaoId,
                            Long categoriaId, String descricao) {
        Conta conta = buscarConta(usuarioId, contaId);
        Transacao transacao = buscarTransacao(usuarioId, contaId, transacaoId);
        Categoria categoria = buscarCategoria(usuarioId, categoriaId);

        conta.editarTransacao(transacao, descricao, categoria);

        return transacaoRepository.save(transacao);
    }

    @Transactional(readOnly = true)
    public Page<Transacao> listar(Long usuarioId, Long contaId,
                                  LocalDate inicio, LocalDate fim, int pagina, int tamanho) {
        // IMPORTANTE: confirma que a conta é do usuário. Se não for, 404 (e não lista vazia).
        buscarConta(usuarioId, contaId);

        LocalDate hoje = LocalDate.now();
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

        return transacaoRepository.findByContaIdAndContaUsuarioIdAndDataBetween(
                contaId, usuarioId, de, ate, pageable);
    }

    // IMPORTANTE: busca sempre com o dono. Conta de outro usuário = 404, igual a "não existe".
    private Conta buscarConta(Long usuarioId, Long contaId) {
        return contaRepository.findByIdAndUsuarioId(contaId, usuarioId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Conta não encontrada."));
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
}