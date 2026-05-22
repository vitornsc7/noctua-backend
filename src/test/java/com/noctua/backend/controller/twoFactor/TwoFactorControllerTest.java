package com.noctua.backend.controller.twoFactor;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import com.noctua.backend.dto.twoFactor.TwoFactorSetupResponseDTO;
import com.noctua.backend.dto.twoFactor.TwoFactorVerifySetupRequestDTO;
import com.noctua.backend.entity.Usuario.UsuarioEntity;
import com.noctua.backend.repository.usuario.UsuarioRepository;
import com.noctua.backend.service.twoFactor.TwoFactorService;

@ExtendWith(MockitoExtension.class)
class TwoFactorControllerTest {

    @Mock
    private TwoFactorService twoFactorService;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private TwoFactorController twoFactorController;

    // Teste 1: endpoint POST /2fa/setup gera secret temporário e retorna URL do autenticador.
    @Test
    void setupDeveGerarSecretTemporarioERetornarDadosDeConfiguracao() {
        UsuarioEntity usuario = criarUsuario();

        when(authentication.getName()).thenReturn("prof@email.com");
        when(usuarioRepository.findByEmail("prof@email.com")).thenReturn(Optional.of(usuario));
        when(twoFactorService.generateSecret()).thenReturn("secret-2fa");
        when(twoFactorService.buildOtpAuthUrl("prof@email.com", "secret-2fa"))
                .thenReturn("otpauth://totp/Noctua:prof@email.com");

        ResponseEntity<TwoFactorSetupResponseDTO> response = twoFactorController.setup(authentication);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("secret-2fa", response.getBody().getSecret());
        assertEquals("otpauth://totp/Noctua:prof@email.com", response.getBody().getOtpauthUrl());
        assertEquals("secret-2fa", usuario.getTwoFactorTempSecret());
        verify(usuarioRepository).save(usuario);
    }

    // Teste 2: endpoint POST /2fa/setup lança erro quando usuário autenticado não existe.
    @Test
    void setupDeveLancarErroQuandoUsuarioNaoForEncontrado() {
        when(authentication.getName()).thenReturn("prof@email.com");
        when(usuarioRepository.findByEmail("prof@email.com")).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> twoFactorController.setup(authentication));

        assertEquals("Usuário não encontrado.", exception.getMessage());
        verify(usuarioRepository, never()).save(org.mockito.Mockito.any());
    }

    // Teste 3: endpoint POST /2fa/verify-setup ativa 2FA quando código é válido.
    @Test
    void verifySetupDeveAtivarTwoFactorQuandoCodigoForValido() {
        UsuarioEntity usuario = criarUsuario();
        usuario.setTwoFactorTempSecret("secret-2fa");
        TwoFactorVerifySetupRequestDTO request = criarVerifySetupRequest("123456");

        when(authentication.getName()).thenReturn("prof@email.com");
        when(usuarioRepository.findByEmail("prof@email.com")).thenReturn(Optional.of(usuario));
        when(twoFactorService.verifyCode("secret-2fa", 123456)).thenReturn(true);

        ResponseEntity<?> response = twoFactorController.verifySetup(authentication, request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("2FA ativado com sucesso.", response.getBody());
        assertEquals("secret-2fa", usuario.getTwoFactorSecret());
        assertNull(usuario.getTwoFactorTempSecret());
        assertTrue(usuario.getTwoFactorEnabled());
        assertTrue(usuario.getTwoFactorConfirmedAt() != null);
        verify(usuarioRepository).save(usuario);
    }

    // Teste 4: endpoint POST /2fa/verify-setup retorna 400 quando configuração não foi iniciada.
    @Test
    void verifySetupDeveRetornarBadRequestQuandoConfiguracaoNaoFoiIniciada() {
        UsuarioEntity usuario = criarUsuario();
        usuario.setTwoFactorTempSecret(null);
        TwoFactorVerifySetupRequestDTO request = criarVerifySetupRequest("123456");

        when(authentication.getName()).thenReturn("prof@email.com");
        when(usuarioRepository.findByEmail("prof@email.com")).thenReturn(Optional.of(usuario));

        ResponseEntity<?> response = twoFactorController.verifySetup(authentication, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Configuração 2FA não iniciada.", response.getBody());
        verify(twoFactorService, never()).verifyCode(org.mockito.Mockito.anyString(), org.mockito.Mockito.anyInt());
        verify(usuarioRepository, never()).save(org.mockito.Mockito.any());
    }

    // Teste 5: endpoint POST /2fa/verify-setup retorna 400 quando código é inválido.
    @Test
    void verifySetupDeveRetornarBadRequestQuandoCodigoForInvalido() {
        UsuarioEntity usuario = criarUsuario();
        usuario.setTwoFactorTempSecret("secret-2fa");
        TwoFactorVerifySetupRequestDTO request = criarVerifySetupRequest("123456");

        when(authentication.getName()).thenReturn("prof@email.com");
        when(usuarioRepository.findByEmail("prof@email.com")).thenReturn(Optional.of(usuario));
        when(twoFactorService.verifyCode("secret-2fa", 123456)).thenReturn(false);

        ResponseEntity<?> response = twoFactorController.verifySetup(authentication, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Código inválido.", response.getBody());
        verify(usuarioRepository, never()).save(org.mockito.Mockito.any());
    }

    // Teste 6: endpoint POST /2fa/verify-setup lança erro quando usuário autenticado não existe.
    @Test
    void verifySetupDeveLancarErroQuandoUsuarioNaoForEncontrado() {
        TwoFactorVerifySetupRequestDTO request = criarVerifySetupRequest("123456");

        when(authentication.getName()).thenReturn("prof@email.com");
        when(usuarioRepository.findByEmail("prof@email.com")).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> twoFactorController.verifySetup(authentication, request));

        assertEquals("Usuário não encontrado.", exception.getMessage());
        verify(usuarioRepository, never()).save(org.mockito.Mockito.any());
    }

    private TwoFactorVerifySetupRequestDTO criarVerifySetupRequest(String code) {
        TwoFactorVerifySetupRequestDTO request = new TwoFactorVerifySetupRequestDTO();
        request.setCode(code);
        return request;
    }

    private UsuarioEntity criarUsuario() {
        return UsuarioEntity.builder()
                .id(1L)
                .nome("Professor")
                .email("prof@email.com")
                .senhaHash("hash")
                .ativo(true)
                .twoFactorEnabled(false)
                .build();
    }
}
