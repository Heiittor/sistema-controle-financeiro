package com.controlefinanceiro.api.presentation;

import com.controlefinanceiro.api.IntegracaoBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class LimiteLoginTest extends IntegracaoBase {

    @Autowired
    private MockMvc mockMvc;

    private String cadastrar() throws Exception {
        String email = "limite-" + UUID.randomUUID() + "@teste.com";
        mockMvc.perform(post("/usuarios").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Ana\",\"email\":\"" + email + "\",\"senha\":\"senha12345\"}"))
                .andExpect(status().isCreated());
        return email;
    }

    private ResultActions login(String email, String senha) throws Exception {
        return mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"senha\":\"" + senha + "\"}"));
    }

    private void falharCincoVezes(String email) throws Exception {
        for (int i = 0; i < 5; i++) {
            login(email, "senha-errada-123").andExpect(status().isUnauthorized());
        }
    }

    @Test
    void deveBloquearNaSextaTentativaMesmoComSenhaCorreta() throws Exception {
        String email = cadastrar();
        falharCincoVezes(email);

        String retryAfter = login(email, "senha12345")
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.status").value(429))
                .andReturn().getResponse().getHeader("Retry-After");

        long segundos = Long.parseLong(retryAfter);
        assertTrue(segundos > 0 && segundos <= 900, "Retry-After fora do esperado: " + segundos);
    }

    @Test
    void naoDeveBloquearAbaixoDoLimite() throws Exception {
        String email = cadastrar();
        for (int i = 0; i < 4; i++) {
            login(email, "senha-errada-123").andExpect(status().isUnauthorized());
        }

        login(email, "senha12345").andExpect(status().isOk());
    }

    @Test
    void loginCorretoDeveZerarOContador() throws Exception {
        String email = cadastrar();
        for (int i = 0; i < 4; i++) {
            login(email, "senha-errada-123").andExpect(status().isUnauthorized());
        }
        login(email, "senha12345").andExpect(status().isOk());

        // Se o contador não tivesse zerado, a 5ª falha já bloquearia a próxima tentativa.
        for (int i = 0; i < 4; i++) {
            login(email, "senha-errada-123").andExpect(status().isUnauthorized());
        }
        login(email, "senha12345").andExpect(status().isOk());
    }

    @Test
    void bloqueioDeUmEmailNaoAfetaOutro() throws Exception {
        String bloqueado = cadastrar();
        String livre = cadastrar();
        falharCincoVezes(bloqueado);

        login(bloqueado, "senha12345").andExpect(status().isTooManyRequests());
        login(livre, "senha12345").andExpect(status().isOk());
    }

    @Test
    void emailInexistenteTambemBloqueiaSemRevelarQueNaoExiste() throws Exception {
        String inexistente = "fantasma-" + UUID.randomUUID() + "@teste.com";
        falharCincoVezes(inexistente);

        login(inexistente, "qualquer-senha-1").andExpect(status().isTooManyRequests());
    }

    @Test
    void bloqueioIgnoraMaiusculasNoEmail() throws Exception {
        String email = cadastrar();
        falharCincoVezes(email);

        login(email.toUpperCase(), "senha12345").andExpect(status().isTooManyRequests());
    }
}