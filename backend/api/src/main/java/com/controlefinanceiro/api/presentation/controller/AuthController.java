package com.controlefinanceiro.api.presentation.controller;

import com.controlefinanceiro.api.application.AuthService;
import com.controlefinanceiro.api.presentation.dto.LoginRequest;
import com.controlefinanceiro.api.presentation.dto.TokenResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest request,
                               HttpServletRequest http) {
        // IMPORTANTE: getRemoteAddr, e não X-Forwarded-For (que o cliente pode forjar para fugir do limite).
        return TokenResponse.de(
                authService.autenticar(request.email(), request.senha(), http.getRemoteAddr()));
    }
}