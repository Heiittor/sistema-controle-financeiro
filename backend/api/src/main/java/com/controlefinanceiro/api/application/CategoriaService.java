package com.controlefinanceiro.api.application;

import com.controlefinanceiro.api.domain.Categoria;
import com.controlefinanceiro.api.domain.TipoCategoria;
import com.controlefinanceiro.api.domain.Usuario;
import com.controlefinanceiro.api.domain.exception.ConflitoException;
import com.controlefinanceiro.api.domain.exception.RecursoNaoEncontradoException;
import com.controlefinanceiro.api.infrastructure.CategoriaRepository;
import com.controlefinanceiro.api.infrastructure.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CategoriaService {

    private final CategoriaRepository categoriaRepository;
    private final UsuarioRepository usuarioRepository;

    public CategoriaService(CategoriaRepository categoriaRepository,
                            UsuarioRepository usuarioRepository) {
        this.categoriaRepository = categoriaRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional
    public Categoria criar(Long usuarioId, String nome, TipoCategoria tipo) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado."));

        // IMPORTANTE: criar primeiro. O domínio limpa o nome (trim) ANTES da checagem de duplicidade.
        Categoria categoria = new Categoria(nome, tipo, usuario);

        // Checagem amigável. O UNIQUE do banco continua sendo a garantia final.
        if (categoriaRepository.existsByUsuarioIdAndTipoAndNomeIgnoreCase(
                usuarioId, categoria.getTipo(), categoria.getNome())) {
            throw new ConflitoException("Você já tem uma categoria com esse nome e tipo.");
        }

        return categoriaRepository.save(categoria);
    }

    // readOnly: avisa ao Spring que não vamos gravar nada, e ele pode otimizar.
    @Transactional(readOnly = true)
    public List<Categoria> listar(Long usuarioId, TipoCategoria tipo) {
        // IMPORTANTE: sempre filtra pelo dono. Nunca existe "listar todas".
        if (tipo == null) {
            return categoriaRepository.findByUsuarioIdOrderByNomeAsc(usuarioId);
        }
        return categoriaRepository.findByUsuarioIdAndTipoOrderByNomeAsc(usuarioId, tipo);
    }
}