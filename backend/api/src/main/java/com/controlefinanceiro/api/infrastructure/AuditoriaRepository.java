package com.controlefinanceiro.api.infrastructure;

import com.controlefinanceiro.api.domain.Auditoria;
import org.springframework.data.repository.Repository;

// IMPORTANTE: só save. Sem busca, update ou delete: a auditoria é um registro imutável.
public interface AuditoriaRepository extends Repository<Auditoria, Long> {

    Auditoria save(Auditoria auditoria);
}