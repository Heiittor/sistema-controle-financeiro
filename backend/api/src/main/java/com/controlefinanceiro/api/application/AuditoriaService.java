package com.controlefinanceiro.api.application;

import com.controlefinanceiro.api.domain.AcaoAuditoria;
import com.controlefinanceiro.api.domain.Auditoria;
import com.controlefinanceiro.api.domain.EntidadeAuditada;
import com.controlefinanceiro.api.infrastructure.AuditoriaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditoriaService {

    private final AuditoriaRepository auditoriaRepository;

    public AuditoriaService(AuditoriaRepository auditoriaRepository) {
        this.auditoriaRepository = auditoriaRepository;
    }

    // IMPORTANTE: MANDATORY exige uma transação já aberta. Assim o registro só existe junto da
    // operação que ele descreve: commit grava os dois, rollback desfaz os dois.
    // Nunca coloque senha, token ou e-mail em "detalhes".
    @Transactional(propagation = Propagation.MANDATORY)
    public void registrar(Long usuarioId, AcaoAuditoria acao, EntidadeAuditada entidade,
                          Long entidadeId, String detalhes) {
        auditoriaRepository.save(new Auditoria(usuarioId, acao, entidade, entidadeId, detalhes));
    }
}