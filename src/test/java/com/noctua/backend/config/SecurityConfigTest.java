package com.noctua.backend.config;

import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@WebMvcTest(controllers = SecurityConfigTest.TestController.class)
@Import({ SecurityConfig.class, JwtFilter.class, SecurityConfigTest.TestController.class })
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtUtil jwtUtil;

    // Teste 1: o bean PasswordEncoder usa BCrypt para codificar e validar senhas.
    @Test
    void passwordEncoderDeveCodificarSenhaComBCrypt() {
        SecurityConfig securityConfig = new SecurityConfig(Mockito.mock(JwtFilter.class));
        PasswordEncoder passwordEncoder = securityConfig.passwordEncoder();

        String senhaCodificada = passwordEncoder.encode("123");

        assertTrue(passwordEncoder.matches("123", senhaCodificada));
    }

    // Teste 2: endpoints públicos configurados no SecurityConfig podem ser acessados sem autenticação.
    @Test
    void securityFilterChainDevePermitirEndpointsPublicosSemAutenticacao() throws Exception {
        mockMvc.perform(post("/auth/login"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/professores"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }

    // Teste 3: endpoint protegido deve retornar 401 quando não houver autenticação.
    @Test
    void securityFilterChainDeveBloquearEndpointProtegidoSemAutenticacao() throws Exception {
        mockMvc.perform(get("/protegido"))
                .andExpect(status().isUnauthorized());
    }

    // Teste 4: endpoint protegido deve permitir acesso com usuário autenticado.
    @Test
    @WithMockUser
    void securityFilterChainDevePermitirEndpointProtegidoComAutenticacao() throws Exception {
        mockMvc.perform(get("/protegido"))
                .andExpect(status().isOk());
    }

    @RestController
    public static class TestController {

        @PostMapping("/auth/login")
        ResponseEntity<String> login() {
            return ResponseEntity.ok("login");
        }

        @PostMapping("/professores")
        ResponseEntity<String> professores() {
            return ResponseEntity.ok("professores");
        }

        @GetMapping("/actuator/health")
        ResponseEntity<String> health() {
            return ResponseEntity.ok("up");
        }

        @GetMapping("/protegido")
        ResponseEntity<String> protegido() {
            return ResponseEntity.ok("protegido");
        }
    }
}
