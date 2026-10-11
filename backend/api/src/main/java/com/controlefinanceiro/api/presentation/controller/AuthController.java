package com.controlefinanceiro.api.presentation.controller;

import com.controlefinanceiro.api.application.AuthService;
import com.controlefinanceiro.api.presentation.dto.LoginRequest;
import com.controlefinanceiro.api.presentation.dto.TokenResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest request) {
        // O Controller só repassa. Quem confere e-mail e senha é o AuthService.
        return TokenResponse.de(authService.autenticar(request.email(), request.senha()));
    }
}