package com.controlefinanceiro.api.application;

import com.controlefinanceiro.api.domain.AcaoAuditoria;
import com.controlefinanceiro.api.domain.Conta;
import com.controlefinanceiro.api.domain.EntidadeAuditada;
import com.controlefinanceiro.api.domain.Usuario;
import com.controlefinanceiro.api.domain.exception.ConflitoException;
import com.controlefinanceiro.api.domain.exception.RecursoNaoEncontradoException;
import com.controlefinanceiro.api.infrastructure.ContaRepository;
import com.controlefinanceiro.api.infrastructure.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ContaService {

    private final ContaRepository contaRepository;
    private final UsuarioRepository usuarioRepository;
    private final AuditoriaService auditoriaService;

    public ContaService(ContaRepository contaRepository,
                        UsuarioRepository usuarioRepository,
                        AuditoriaService auditoriaService) {
        this.contaRepository = contaRepository;
        this.usuarioRepository = usuarioRepository;
        this.auditoriaService = auditoriaService;
    }

    @Transactional
    public Conta criar(Long usuarioId, String nome) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado."));

        // IMPORTANTE: criar primeiro. O domínio limpa o nome e define o saldo em zero.
        Conta conta = new Conta(nome, usuario);

        // Checagem amigável. O UNIQUE do banco continua sendo a garantia final.
        if (contaRepository.existsByUsuarioIdAndNomeIgnoreCase(usuarioId, conta.getNome())) {
            throw new ConflitoException("Você já tem uma conta com esse nome.");
        }

        Conta salva = contaRepository.save(conta);

        auditoriaService.registrar(usuarioId, AcaoAuditoria.CRIAR_CONTA,
                EntidadeAuditada.CONTA, salva.getId(), "nome=" + salva.getNome());
        return salva;
    }

    @Transactional(readOnly = true)
    public List<Conta> listar(Long usuarioId) {
        // IMPORTANTE: sempre filtra pelo dono. Nunca existe "listar todas as contas".
        return contaRepository.findByUsuarioIdOrderByNomeAsc(usuarioId);
    }
}