package com.noctua.backend.service.twoFactor;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import com.warrenstrange.googleauth.GoogleAuthenticator;

class TwoFactorServiceTest {

    private final TwoFactorService twoFactorService = new TwoFactorService();

    // Teste 1: garante que o serviço gera um secret preenchido para configurar o 2FA.
    @Test
    void generateSecretDeveRetornarSecretPreenchido() {
        String secret = twoFactorService.generateSecret();

        assertNotNull(secret);
        assertFalse(secret.isBlank());
    }

    // Teste 2: garante que duas chamadas geram secrets diferentes, evitando reutilização.
    @Test
    void generateSecretDeveRetornarSecretsDiferentes() {
        String primeiroSecret = twoFactorService.generateSecret();
        String segundoSecret = twoFactorService.generateSecret();

        assertNotEquals(primeiroSecret, segundoSecret);
    }

    // Teste 3: garante que a URL otpauth pode ser lida pelo Google Authenticator.
    @Test
    void buildOtpAuthUrlDeveRetornarUrlParaAplicativoAutenticador() {
        String secret = "ABCDEF123456";

        String url = twoFactorService.buildOtpAuthUrl("usuario@email.com", secret);

        assertTrue(url.startsWith("otpauth://totp/"));
        assertTrue(url.contains("Noctua"));
        assertTrue(url.contains("usuario@email.com") || url.contains("usuario%40email.com"));
        assertTrue(url.contains("secret=" + secret));
    }

    // Teste 4: gera um código TOTP real para o secret atual e valida que ele é aceito.
    @Test
    void verifyCodeDeveRetornarTrueParaCodigoValido() {
        String secret = twoFactorService.generateSecret();
        int codigoAtual = new GoogleAuthenticator().getTotpPassword(secret);

        boolean valid = twoFactorService.verifyCode(secret, codigoAtual);

        assertTrue(valid);
    }

    // Teste 5: garante que um código incorreto não passe na validação do 2FA.
    @Test
    void verifyCodeDeveRetornarFalseParaCodigoInvalido() {
        String secret = twoFactorService.generateSecret();

        boolean valid = twoFactorService.verifyCode(secret, 0);

        assertFalse(valid);
    }
}
