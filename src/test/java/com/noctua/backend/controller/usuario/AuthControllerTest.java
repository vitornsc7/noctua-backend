package com.noctua.backend.controller.usuario;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import com.noctua.backend.dto.Login.LoginRequestDTO;
import com.noctua.backend.dto.Login.LoginResponseDTO;
import com.noctua.backend.dto.Usuario.AuthenticatedUserResponseDTO;
import com.noctua.backend.dto.Usuario.ForgotPasswordRequestDTO;
import com.noctua.backend.dto.Usuario.ResetPasswordRequestDTO;
import com.noctua.backend.dto.Usuario.UsuarioUpdateRequestDTO;
import com.noctua.backend.dto.twoFactor.TwoFactorVerifyLoginRequestDTO;
import com.noctua.backend.service.usuario.AuthService;
import com.noctua.backend.service.usuario.PasswordResetService;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;

    @Mock
    private PasswordResetService passwordResetService;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private AuthController authController;

    // Teste 1: endpoint de login retorna 200 com token quando a service autentica.
    @Test
    void loginDeveRetornarOkQuandoCredenciaisForemValidas() {
        LoginRequestDTO request = criarLoginRequest();
        LoginResponseDTO serviceResponse = new LoginResponseDTO("token", false, null);

        when(authService.login(request)).thenReturn(serviceResponse);

        ResponseEntity<?> response = authController.login(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(serviceResponse, response.getBody());
    }

    // Teste 2: endpoint de login retorna 400 quando a service lança erro de validação.
    @Test
    void loginDeveRetornarBadRequestQuandoServiceLancarIllegalArgumentException() {
        LoginRequestDTO request = criarLoginRequest();
        when(authService.login(request)).thenThrow(new IllegalArgumentException("Senha inválida!"));

        ResponseEntity<?> response = authController.login(request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Senha inválida!", response.getBody());
    }

    // Teste 3: endpoint de login retorna 500 para erro inesperado.
    @Test
    void loginDeveRetornarInternalServerErrorQuandoServiceLancarErroInesperado() {
        LoginRequestDTO request = criarLoginRequest();
        when(authService.login(request)).thenThrow(new RuntimeException("erro"));

        ResponseEntity<?> response = authController.login(request);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("Erro interno no login.", response.getBody());
    }

    // Teste 4: endpoint de verificação 2FA retorna 200 quando o código é válido.
    @Test
    void verifyLoginTwoFactorDeveRetornarOkQuandoCodigoForValido() {
        TwoFactorVerifyLoginRequestDTO request = criarTwoFactorRequest();
        LoginResponseDTO serviceResponse = new LoginResponseDTO("token-2fa", false, null);

        when(authService.verifyLoginTwoFactor(request)).thenReturn(serviceResponse);

        ResponseEntity<?> response = authController.verifyLoginTwoFactor(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(serviceResponse, response.getBody());
    }

    // Teste 5: endpoint de forgot-password retorna mensagem genérica de sucesso.
    @Test
    void forgotPasswordDeveRetornarOkComMensagemGenerica() {
        ForgotPasswordRequestDTO request = new ForgotPasswordRequestDTO();
        request.setEmail("prof@email.com");

        ResponseEntity<?> response = authController.forgotPassword(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(
                "Se existir uma conta com esse e-mail, enviaremos instruções para redefinição de senha.",
                ((Map<?, ?>) response.getBody()).get("message"));
        verify(passwordResetService).solicitarResetSenha("prof@email.com");
    }

    // Teste 6: endpoint de reset-password retorna 200 quando senha é redefinida.
    @Test
    void resetPasswordDeveRetornarOkQuandoSenhaForRedefinida() {
        ResetPasswordRequestDTO request = criarResetPasswordRequest();

        ResponseEntity<?> response = authController.resetPassword(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Senha redefinida com sucesso.", ((Map<?, ?>) response.getBody()).get("message"));
        verify(passwordResetService).redefinirSenha("token", "NovaSenha123", "NovaSenha123");
    }

    // Teste 7: endpoint de reset-password retorna 400 quando a service valida erro.
    @Test
    void resetPasswordDeveRetornarBadRequestQuandoServiceLancarIllegalArgumentException() {
        ResetPasswordRequestDTO request = criarResetPasswordRequest();
        org.mockito.Mockito.doThrow(new IllegalArgumentException("Token inválido."))
                .when(passwordResetService)
                .redefinirSenha("token", "NovaSenha123", "NovaSenha123");

        ResponseEntity<?> response = authController.resetPassword(request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Token inválido.", response.getBody());
    }

    // Teste 8: endpoint /me busca usuário pelo e-mail autenticado.
    @Test
    void meDeveRetornarUsuarioAutenticado() {
        AuthenticatedUserResponseDTO serviceResponse = new AuthenticatedUserResponseDTO(
                1L,
                "Professor",
                "prof@email.com",
                "PROFESSOR",
                false);

        when(authentication.getName()).thenReturn("prof@email.com");
        when(authService.buscarUsuarioAutenticado("prof@email.com")).thenReturn(serviceResponse);

        ResponseEntity<AuthenticatedUserResponseDTO> response = authController.me(authentication);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(serviceResponse, response.getBody());
    }

    // Teste 9: endpoint PUT /me atualiza dados do usuário autenticado.
    @Test
    void atualizarMeDeveRetornarUsuarioAtualizado() {
        UsuarioUpdateRequestDTO request = new UsuarioUpdateRequestDTO();
        request.setNome("Novo Nome");
        AuthenticatedUserResponseDTO serviceResponse = new AuthenticatedUserResponseDTO(
                1L,
                "Novo Nome",
                "prof@email.com",
                "PROFESSOR",
                false);

        when(authentication.getName()).thenReturn("prof@email.com");
        when(authService.atualizarUsuarioAutenticado("prof@email.com", request)).thenReturn(serviceResponse);

        ResponseEntity<?> response = authController.atualizarMe(authentication, request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(serviceResponse, response.getBody());
    }

    // Teste 10: endpoint DELETE /me exclui usuário autenticado e retorna 204.
    @Test
    void excluirMeDeveRetornarNoContentQuandoUsuarioForExcluido() {
        when(authentication.getName()).thenReturn("prof@email.com");

        ResponseEntity<?> response = authController.excluirMe(authentication);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        assertNull(response.getBody());
        verify(authService).excluirUsuarioAutenticado("prof@email.com");
    }

    private LoginRequestDTO criarLoginRequest() {
        LoginRequestDTO request = new LoginRequestDTO();
        request.setEmail("prof@email.com");
        request.setSenha("123456");
        request.setRememberMe(true);
        return request;
    }

    private TwoFactorVerifyLoginRequestDTO criarTwoFactorRequest() {
        TwoFactorVerifyLoginRequestDTO request = new TwoFactorVerifyLoginRequestDTO();
        request.setEmail("prof@email.com");
        request.setSenha("123456");
        request.setCode("123456");
        request.setRememberMe(true);
        return request;
    }

    private ResetPasswordRequestDTO criarResetPasswordRequest() {
        ResetPasswordRequestDTO request = new ResetPasswordRequestDTO();
        request.setToken("token");
        request.setNovaSenha("NovaSenha123");
        request.setConfirmacaoSenha("NovaSenha123");
        return request;
    }
}
