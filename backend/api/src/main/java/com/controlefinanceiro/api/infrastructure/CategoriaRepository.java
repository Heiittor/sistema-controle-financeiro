package com.controlefinanceiro.api.infrastructure;

import com.controlefinanceiro.api.domain.Categoria;
import com.controlefinanceiro.api.domain.TipoCategoria;
import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Optional;

// IMPORTANTE: estendemos Repository (vazio) e não JpaRepository.
// Assim não existe findById nem findAll, e ninguém consegue buscar dado de outro usuário por engano.
public interface CategoriaRepository extends Repository<Categoria, Long> {

    Categoria save(Categoria categoria);

    // IMPORTANTE: a busca individual sempre exige o dono.
    // Se o id for de outro usuário, o resultado é vazio, como se não existisse.
    Optional<Categoria> findByIdAndUsuarioId(Long id, Long usuarioId);

    List<Categoria> findByUsuarioIdOrderByNomeAsc(Long usuarioId);

    List<Categoria> findByUsuarioIdAndTipoOrderByNomeAsc(Long usuarioId, TipoCategoria tipo);

    // IMPORTANTE: esta é a checagem amigável (ignora maiúsculas).
    // O UNIQUE do banco continua sendo a garantia final se duas requisições chegarem juntas.
    boolean existsByUsuarioIdAndTipoAndNomeIgnoreCase(Long usuarioId, TipoCategoria tipo, String nome);
}