package com.controlefinanceiro.api.presentation;

import com.controlefinanceiro.api.IntegracaoBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SegurancaTest extends IntegracaoBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    private String login(String email, String senha) {
        return "{\"email\":\"" + email + "\",\"senha\":\"" + senha + "\"}";
    }

    @Test
    void deveRecusarRotaProtegidaSemToken() throws Exception {
        mockMvc.perform(get("/contas"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void deveRecusarTokenInvalido() throws Exception {
        mockMvc.perform(get("/contas").header("Authorization", "Bearer isto-nao-e-um-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deveRecusarTokenAdulterado() throws Exception {
        Sessao ana = cadastrarELogar(mockMvc, "Ana");

        // Token = cabecalho.payload.assinatura. Trocamos o PRIMEIRO caractere da assinatura:
        // os 6 bits dele são significativos, então os bytes da assinatura mudam de verdade.
        // (O último caractere tem bits de preenchimento ignorados pelo decodificador.)
        String[] partes = ana.token().split("\\.");
        String assinatura = partes[2];
        char primeiro = assinatura.charAt(0);
        String adulterada = (primeiro == 'A' ? 'B' : 'A') + assinatura.substring(1);
        String adulterado = partes[0] + "." + partes[1] + "." + adulterada;

        mockMvc.perform(get("/contas").header("Authorization", "Bearer " + adulterado))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deveIgnorarOHeaderAntigoXUsuarioId() throws Exception {
        Sessao ana = cadastrarELogar(mockMvc, "Ana");

        mockMvc.perform(get("/contas").header("X-Usuario-Id", ana.id()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deveLogarComEmailEmMaiusculas() throws Exception {
        String email = "login-" + java.util.UUID.randomUUID() + "@teste.com";
        mockMvc.perform(post("/usuarios").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Ana\",\"email\":\"" + email + "\",\"senha\":\"senha12345\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(login(email.toUpperCase(), "senha12345")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipo").value("Bearer"))
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    void senhaErradaEEmailInexistenteTemAMesmaResposta() throws Exception {
        Sessao ana = cadastrarELogar(mockMvc, "Ana");
        String email = jdbc.queryForObject("SELECT email FROM usuarios WHERE id = ?", String.class, ana.id());

        mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(login(email, "senha-errada-123")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensagem").value("E-mail ou senha inválidos."));

        mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(login("nao-existe@teste.com", "senha12345")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensagem").value("E-mail ou senha inválidos."));
    }

    @Test
    void deveGravarSenhaComHashEnaoEmTextoPuro() throws Exception {
        Sessao ana = cadastrarELogar(mockMvc, "Ana");
        String gravada = jdbc.queryForObject("SELECT senha FROM usuarios WHERE id = ?", String.class, ana.id());

        assertTrue(gravada.startsWith("$2"), "a senha deveria estar em hash BCrypt");
        assertTrue(!gravada.contains("senha12345"));
    }
}