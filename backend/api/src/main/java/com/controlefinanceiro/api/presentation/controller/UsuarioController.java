package com.controlefinanceiro.api.presentation.controller;

import com.controlefinanceiro.api.application.UsuarioService;
import com.controlefinanceiro.api.domain.Usuario;
import com.controlefinanceiro.api.presentation.dto.CadastroUsuarioRequest;
import com.controlefinanceiro.api.presentation.dto.UsuarioResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/usuarios")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    // IMPORTANTE: sem @Valid, as anotações do DTO (@NotBlank, @Email...) seriam ignoradas.
    public UsuarioResponse cadastrar(@Valid @RequestBody CadastroUsuarioRequest request) {
        // O Controller só repassa. Nenhuma regra de negócio mora aqui.
        Usuario usuario = usuarioService.cadastrar(request.nome(), request.email(), request.senha());

        // IMPORTANTE: devolvemos o DTO, nunca a entidade. É isso que impede a senha de sair.
        return UsuarioResponse.de(usuario);
    }
}