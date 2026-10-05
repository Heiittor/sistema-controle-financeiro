package com.controlefinanceiro.api.infrastructure;

import com.controlefinanceiro.api.domain.Categoria;
import com.controlefinanceiro.api.domain.TipoTransacao;
import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Optional;

public interface CategoriaRepository extends Repository<Categoria, Long> {

    Categoria save(Categoria categoria);

    // Busca individual SEMPRE com o dono: protege contra acesso a dados de outro usuário.
    Optional<Categoria> findByIdAndUsuarioId(Long id, Long usuarioId);

    List<Categoria> findByUsuarioIdOrderByNomeAsc(Long usuarioId);

    List<Categoria> findByUsuarioIdAndTipoOrderByNomeAsc(Long usuarioId, TipoTransacao tipo);

    // Verificação de duplicidade sem diferenciar maiúsculas de minúsculas.
    boolean existsByUsuarioIdAndTipoAndNomeIgnoreCase(Long usuarioId, TipoTransacao tipo, String nome);
}