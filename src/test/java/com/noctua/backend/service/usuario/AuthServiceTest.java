package com.noctua.backend.service.usuario;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.noctua.backend.config.JwtUtil;
import com.noctua.backend.dto.Login.LoginRequestDTO;
import com.noctua.backend.dto.Login.LoginResponseDTO;
import com.noctua.backend.dto.twoFactor.TwoFactorVerifyLoginRequestDTO;
import com.noctua.backend.entity.Usuario.UsuarioEntity;
import com.noctua.backend.repository.usuario.AdminRepository;
import com.noctua.backend.repository.usuario.ProfessorRepository;
import com.noctua.backend.repository.usuario.UsuarioRepository;
import com.noctua.backend.service.twoFactor.TwoFactorService;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private AdminRepository adminRepository;

    @Mock
    private ProfessorRepository professorRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private TwoFactorService twoFactorService;

    @InjectMocks
    private AuthService authService;

    // Teste 1: login comum com email e senha corretos deve gerar token.
    @Test
    void loginDeveRetornarTokenQuandoCredenciaisForemValidas() {
        LoginRequestDTO request = criarLoginRequest(false);
        UsuarioEntity usuario = criarUsuarioAtivo(false);

        when(usuarioRepository.findByEmail(request.getEmail()))
                .thenReturn(Optional.of(usuario));

        when(passwordEncoder.matches(request.getSenha(), usuario.getSenhaHash()))
                .thenReturn(true);

        when(jwtUtil.generateToken(request.getEmail(), request.isRememberMe()))
                .thenReturn("token-jwt");

        LoginResponseDTO response = authService.login(request);

        assertEquals("token-jwt", response.getToken());
        assertFalse(response.isRequiresTwoFactor());
        assertNull(response.getMessage());

        verify(usuarioRepository).findByEmail(request.getEmail());
        verify(passwordEncoder).matches(request.getSenha(), usuario.getSenhaHash());
        verify(jwtUtil).generateToken(request.getEmail(), request.isRememberMe());
    }

    // Teste 2: login com email inexistente deve dar erro antes de validar a senha.
    @Test
    void loginDeveLancarErroQuandoUsuarioNaoForEncontrado() {
        LoginRequestDTO request = criarLoginRequest(false);

        when(usuarioRepository.findByEmail(request.getEmail()))
                .thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> authService.login(request));

        assertEquals("Usuário não encontrado!", exception.getMessage());
        verify(passwordEncoder, never()).matches(anyString(), anyString());
        verify(jwtUtil, never()).generateToken(anyString(), anyBoolean());
    }

    // Teste 3: login com senha errada deve dar erro e não gerar token.
    @Test
    void loginDeveLancarErroQuandoSenhaForInvalida() {
        LoginRequestDTO request = criarLoginRequest(false);
        UsuarioEntity usuario = criarUsuarioAtivo(false);

        when(usuarioRepository.findByEmail(request.getEmail()))
                .thenReturn(Optional.of(usuario));

        when(passwordEncoder.matches(request.getSenha(), usuario.getSenhaHash()))
                .thenReturn(false);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> authService.login(request));

        assertEquals("Senha inválida!", exception.getMessage());
        verify(jwtUtil, never()).generateToken(anyString(), anyBoolean());
    }

    // Teste 4: login de usuário inativo deve dar erro e não gerar token.
    @Test
    void loginDeveLancarErroQuandoUsuarioEstiverInativo() {
        LoginRequestDTO request = criarLoginRequest(false);
        UsuarioEntity usuario = criarUsuarioAtivo(false);
        usuario.setAtivo(false);

        when(usuarioRepository.findByEmail(request.getEmail()))
                .thenReturn(Optional.of(usuario));

        when(passwordEncoder.matches(request.getSenha(), usuario.getSenhaHash()))
                .thenReturn(true);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> authService.login(request));

        assertEquals("Usuário inativo!", exception.getMessage());
        verify(jwtUtil, never()).generateToken(anyString(), anyBoolean());
    }

    // Teste 5: login de usuário com 2FA ativo deve pedir código, sem gerar token
    // ainda.
    @Test
    void loginDeveSolicitar2FAQuandoUsuarioTiver2FAAtivo() {
        LoginRequestDTO request = criarLoginRequest(true);
        UsuarioEntity usuario = criarUsuarioAtivo(true);

        when(usuarioRepository.findByEmail(request.getEmail()))
                .thenReturn(Optional.of(usuario));

        when(passwordEncoder.matches(request.getSenha(), usuario.getSenhaHash()))
                .thenReturn(true);

        LoginResponseDTO response = authService.login(request);

        assertNull(response.getToken());
        assertTrue(response.isRequiresTwoFactor());
        assertEquals("Código 2FA necessário.", response.getMessage());
        verify(jwtUtil, never()).generateToken(anyString(), anyBoolean());
    }

    // Teste 6: validação de 2FA com código correto deve gerar token.
    @Test
    void verifyLoginTwoFactorDeveRetornarTokenQuandoCodigoForValido() {
        TwoFactorVerifyLoginRequestDTO request = criarTwoFactorRequest("123456", true);
        UsuarioEntity usuario = criarUsuarioAtivo(true);
        usuario.setTwoFactorSecret("secret-2fa");

        when(usuarioRepository.findByEmail(request.getEmail()))
                .thenReturn(Optional.of(usuario));

        when(passwordEncoder.matches(request.getSenha(), usuario.getSenhaHash()))
                .thenReturn(true);

        when(twoFactorService.verifyCode("secret-2fa", 123456))
                .thenReturn(true);

        when(jwtUtil.generateToken(request.getEmail(), request.isRememberMe()))
                .thenReturn("token-2fa");

        LoginResponseDTO response = authService.verifyLoginTwoFactor(request);

        assertEquals("token-2fa", response.getToken());
        assertFalse(response.isRequiresTwoFactor());
        assertNull(response.getMessage());
        verify(jwtUtil).generateToken(request.getEmail(), request.isRememberMe());
    }

    // Teste 7: codigo 2FA não numérico deve dar erro antes de chamar o serviço
    // TOTP.
    @Test
    void verifyLoginTwoFactorDeveLancarErroQuandoCodigoNaoForNumerico() {
        TwoFactorVerifyLoginRequestDTO request = criarTwoFactorRequest("abc123", false);
        UsuarioEntity usuario = criarUsuarioAtivo(true);
        usuario.setTwoFactorSecret("secret-2fa");

        when(usuarioRepository.findByEmail(request.getEmail()))
                .thenReturn(Optional.of(usuario));

        when(passwordEncoder.matches(request.getSenha(), usuario.getSenhaHash()))
                .thenReturn(true);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> authService.verifyLoginTwoFactor(request));

        assertEquals("Código 2FA inválido.", exception.getMessage());
        verify(twoFactorService, never()).verifyCode(anyString(), anyInt());
        verify(jwtUtil, never()).generateToken(anyString(), anyBoolean());
    }

    // Teste 8: código 2FA numérico, mas inválido, deve dar erro e não gerar token.
    @Test
    void verifyLoginTwoFactorDeveLancarErroQuandoCodigoForInvalido() {
        TwoFactorVerifyLoginRequestDTO request = criarTwoFactorRequest("123456", false);
        UsuarioEntity usuario = criarUsuarioAtivo(true);
        usuario.setTwoFactorSecret("secret-2fa");

        when(usuarioRepository.findByEmail(request.getEmail()))
                .thenReturn(Optional.of(usuario));

        when(passwordEncoder.matches(request.getSenha(), usuario.getSenhaHash()))
                .thenReturn(true);

        when(twoFactorService.verifyCode("secret-2fa", 123456))
                .thenReturn(false);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> authService.verifyLoginTwoFactor(request));

        assertEquals("Código 2FA inválido.", exception.getMessage());
        verify(jwtUtil, never()).generateToken(anyString(), anyBoolean());
    }

    private LoginRequestDTO criarLoginRequest(boolean rememberMe) {
        LoginRequestDTO request = new LoginRequestDTO();
        request.setEmail("teste@email.com");
        request.setSenha("123456");
        request.setRememberMe(rememberMe);
        return request;
    }

    // DTO de validacao do segundo fator.
    private TwoFactorVerifyLoginRequestDTO criarTwoFactorRequest(String code, boolean rememberMe) {
        TwoFactorVerifyLoginRequestDTO request = new TwoFactorVerifyLoginRequestDTO();
        request.setEmail("teste@email.com");
        request.setSenha("123456");
        request.setCode(code);
        request.setRememberMe(rememberMe);
        return request;
    }

    // Usuário base, variando se o 2FA esta ativo ou não.
    private UsuarioEntity criarUsuarioAtivo(boolean twoFactorEnabled) {
        UsuarioEntity usuario = new UsuarioEntity();
        usuario.setEmail("teste@email.com");
        usuario.setSenhaHash("senha-hash");
        usuario.setAtivo(true);
        usuario.setTwoFactorEnabled(twoFactorEnabled);
        return usuario;
    }
}
