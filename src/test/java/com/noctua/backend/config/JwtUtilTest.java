package com.noctua.backend.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

class JwtUtilTest {

    private static final String SECRET = "noctua-test-jwt-secret-12345678901234567890";

    // Teste 1: gera token JWT válido e extrai o e-mail usado como subject.
    @Test
    void generateTokenDeveGerarTokenValidoComEmail() {
        JwtUtil jwtUtil = new JwtUtil(SECRET, 3_600_000L, 86_400_000L);

        String token = jwtUtil.generateToken("teste@email.com", false);

        assertNotNull(token);
        assertTrue(jwtUtil.isValid(token));
        assertEquals("teste@email.com", jwtUtil.extractEmail(token));
    }

    // Teste 2: gera token válido também quando rememberMe usa expiração estendida.
    @Test
    void generateTokenDeveGerarTokenValidoComRememberMe() {
        JwtUtil jwtUtil = new JwtUtil(SECRET, 3_600_000L, 86_400_000L);

        String token = jwtUtil.generateToken("remember@email.com", true);

        assertTrue(jwtUtil.isValid(token));
        assertEquals("remember@email.com", jwtUtil.extractEmail(token));
    }

    // Teste 3: retorna falso quando o token está malformado.
    @Test
    void isValidDeveRetornarFalseParaTokenInvalido() {
        JwtUtil jwtUtil = new JwtUtil(SECRET, 3_600_000L, 86_400_000L);

        boolean valid = jwtUtil.isValid("token-invalido");

        assertFalse(valid);
    }

    // Teste 4: retorna falso quando o token está expirado.
    @Test
    void isValidDeveRetornarFalseParaTokenExpirado() {
        JwtUtil jwtUtil = new JwtUtil(SECRET, -1L, 86_400_000L);
        String token = jwtUtil.generateToken("expirado@email.com", false);

        boolean valid = jwtUtil.isValid(token);

        assertFalse(valid);
    }
}
