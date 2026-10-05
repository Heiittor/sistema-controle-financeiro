package com.controlefinanceiro.api.application;

import com.controlefinanceiro.api.domain.Conta;
import com.controlefinanceiro.api.domain.Usuario;
import com.controlefinanceiro.api.infrastructure.ContaRepository;
import com.controlefinanceiro.api.infrastructure.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ContaService {

    private final ContaRepository contaRepository;
    private final UsuarioRepository usuarioRepository;

    public ContaService(ContaRepository contaRepository,
                        UsuarioRepository usuarioRepository) {
        this.contaRepository = contaRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional
    public Conta criar(Long usuarioId, String nome) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado."));

        // IMPORTANTE: criar primeiro. O domínio limpa o nome e define o saldo em zero.
        Conta conta = new Conta(nome, usuario);

        // Checagem amigável. O UNIQUE do banco continua sendo a garantia final.
        if (contaRepository.existsByUsuarioIdAndNomeIgnoreCase(usuarioId, conta.getNome())) {
            throw new IllegalArgumentException("Você já tem uma conta com esse nome.");
        }

        return contaRepository.save(conta);
    }

    @Transactional(readOnly = true)
    public List<Conta> listar(Long usuarioId) {
        // IMPORTANTE: sempre filtra pelo dono. Nunca existe "listar todas as contas".
        return contaRepository.findByUsuarioIdOrderByNomeAsc(usuarioId);
    }
}