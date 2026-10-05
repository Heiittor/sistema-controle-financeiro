package com.controlefinanceiro.api.infrastructure;

import com.controlefinanceiro.api.domain.Conta;
import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Optional;

// IMPORTANTE: sem findById nem findAll de propósito, para não vazar contas de outros usuários.
public interface ContaRepository extends Repository<Conta, Long> {

    // Serve para inserir e para atualizar (o saldo novo é gravado por aqui).
    Conta save(Conta conta);

    // IMPORTANTE: toda busca individual traz o dono junto.
    // É esta regra que impede o usuário A de mexer na conta do usuário B.
    Optional<Conta> findByIdAndUsuarioId(Long id, Long usuarioId);

    List<Conta> findByUsuarioIdOrderByNomeAsc(Long usuarioId);

    // Checagem amigável de nome repetido; o UNIQUE do banco é a garantia final.
    boolean existsByUsuarioIdAndNomeIgnoreCase(Long usuarioId, String nome);
}