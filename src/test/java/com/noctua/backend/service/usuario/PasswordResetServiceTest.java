package com.noctua.backend.service.usuario;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.anyString;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import com.noctua.backend.entity.Usuario.UsuarioEntity;
import com.noctua.backend.repository.usuario.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

        @Mock
        private UsuarioRepository usuarioRepository;

        @Mock
        private PasswordEncoder passwordEncoder;

        @Mock
        private EmailService emailService;

        @InjectMocks
        private PasswordResetService passwordResetService;

        @BeforeEach
        void setUp() {
                ReflectionTestUtils.setField(
                                passwordResetService,
                                "frontendUrl",
                                "http://localhost:5173");
        }

        // Teste 1: solicitar redefinição para um e-mail inexistente ---> não deve fazer nada.
        @Test
        void solicitarResetSenhaNaoDeveFazerNadaQuandoEmailNaoExistir() {
                when(usuarioRepository.findByEmail("inexistente@email.com"))
                                .thenReturn(Optional.empty());

                passwordResetService.solicitarResetSenha("inexistente@email.com");

                verify(usuarioRepository).findByEmail("inexistente@email.com");
                verify(usuarioRepository, never()).save(org.mockito.Mockito.any());
                verifyNoInteractions(emailService);
        }

        // Teste 2: solicitar redefinição para usuário existente --> gera token, expiração e envia o e-mail.
        @Test
        void solicitarResetSenhaDeveGerarTokenSalvarUsuarioEEnviarEmail() {
                UsuarioEntity usuario = criarUsuario();

                when(usuarioRepository.findByEmail(usuario.getEmail()))
                                .thenReturn(Optional.of(usuario));

                passwordResetService.solicitarResetSenha(usuario.getEmail());

                assertNotNull(usuario.getTokenSenhaReset());
                assertNotNull(usuario.getTokenSenhaExpiracao());
                assertFalse(usuario.getTokenSenhaUtilizado());
                assertTrue(usuario.getTokenSenhaExpiracao().isAfter(LocalDateTime.now()));

                verify(usuarioRepository).save(usuario);
                verify(emailService).enviarEmailHtmlComImagemInline(
                                org.mockito.Mockito.eq(usuario.getEmail()),
                                org.mockito.Mockito.contains("Redefini"),
                                org.mockito.Mockito.contains("http://localhost:5173/reset-password?token="
                                                + usuario.getTokenSenhaReset()),
                                org.mockito.Mockito.eq("passwordSecurity"),
                                org.mockito.Mockito.eq("static/images/emails/noctua-password-security.png"));
        }

        // Teste 3: senhas diferentes --> bloqueiam a redefinição antes de buscar token.
        @Test
        void redefinirSenhaDeveLancarErroQuandoSenhasNaoCoincidirem() {
                IllegalArgumentException exception = assertThrows(
                                IllegalArgumentException.class,
                                () -> passwordResetService.redefinirSenha("token", "senha123", "outraSenha123"));

        assertEquals("As senhas não coincidem.", exception.getMessage());
                verifyNoInteractions(usuarioRepository, passwordEncoder, emailService);
        }

        // Teste 4: senha curta --> bloqueia a redefinição antes de buscar token.
        @Test
        void redefinirSenhaDeveLancarErroQuandoSenhaForCurta() {
                IllegalArgumentException exception = assertThrows(
                                IllegalArgumentException.class,
                                () -> passwordResetService.redefinirSenha("token", "1234567", "1234567"));

                assertEquals("A nova senha deve ter pelo menos 8 caracteres.", exception.getMessage());
                verifyNoInteractions(usuarioRepository, passwordEncoder, emailService);
        }

        // Teste 5: token inexistente --> deve dar erro e não alterar a senha.
        @Test
        void redefinirSenhaDeveLancarErroQuandoTokenNaoExistir() {
                when(usuarioRepository.findByTokenSenhaReset("token-invalido"))
                                .thenReturn(Optional.empty());

                IllegalArgumentException exception = assertThrows(
                                IllegalArgumentException.class,
                                () -> passwordResetService.redefinirSenha("token-invalido", "novaSenha123",
                                                "novaSenha123"));

        assertEquals("Token inválido.", exception.getMessage());
                verify(passwordEncoder, never()).encode(anyString());
                verify(usuarioRepository, never()).save(org.mockito.Mockito.any());
                verifyNoInteractions(emailService);
        }

        // Teste 6: token expirado --> deve dar erro e não alterar a senha.
        @Test
        void redefinirSenhaDeveLancarErroQuandoTokenEstiverExpirado() {
                UsuarioEntity usuario = criarUsuarioComToken();
                usuario.setTokenSenhaExpiracao(LocalDateTime.now().minusMinutes(1));

                when(usuarioRepository.findByTokenSenhaReset("token-reset"))
                                .thenReturn(Optional.of(usuario));

                IllegalArgumentException exception = assertThrows(
                                IllegalArgumentException.class,
                                () -> passwordResetService.redefinirSenha("token-reset", "novaSenha123",
                                                "novaSenha123"));

                assertEquals("Token expirado.", exception.getMessage());
                verify(passwordEncoder, never()).encode(anyString());
                verify(usuarioRepository, never()).save(org.mockito.Mockito.any());
                verifyNoInteractions(emailService);
        }

        // Teste 7: token já utilizado --> deve dar erro e não alterar a senha.
        @Test
        void redefinirSenhaDeveLancarErroQuandoTokenJaTiverSidoUtilizado() {
                UsuarioEntity usuario = criarUsuarioComToken();
                usuario.setTokenSenhaUtilizado(true);

                when(usuarioRepository.findByTokenSenhaReset("token-reset"))
                                .thenReturn(Optional.of(usuario));

                IllegalArgumentException exception = assertThrows(
                                IllegalArgumentException.class,
                                () -> passwordResetService.redefinirSenha("token-reset", "novaSenha123",
                                                "novaSenha123"));

        assertEquals("Token já utilizado.", exception.getMessage());
                verify(passwordEncoder, never()).encode(anyString());
                verify(usuarioRepository, never()).save(org.mockito.Mockito.any());
                verifyNoInteractions(emailService);
        }

        // Teste 8: token válido --> deve trocar a senha, invalidar o token e enviar a confirmação.
        @Test
        void redefinirSenhaDeveAtualizarSenhaInvalidarTokenEEnviarConfirmacao() {
                UsuarioEntity usuario = criarUsuarioComToken();

                when(usuarioRepository.findByTokenSenhaReset("token-reset"))
                                .thenReturn(Optional.of(usuario));

                when(passwordEncoder.encode("novaSenha123"))
                                .thenReturn("senha-hash-nova");

                passwordResetService.redefinirSenha("token-reset", "novaSenha123", "novaSenha123");

                ArgumentCaptor<UsuarioEntity> usuarioCaptor = ArgumentCaptor.forClass(UsuarioEntity.class);
                verify(usuarioRepository).save(usuarioCaptor.capture());

                UsuarioEntity usuarioSalvo = usuarioCaptor.getValue();
                assertEquals("senha-hash-nova", usuarioSalvo.getSenhaHash());
                assertNull(usuarioSalvo.getTokenSenhaReset());
                assertNull(usuarioSalvo.getTokenSenhaExpiracao());
                assertTrue(usuarioSalvo.getTokenSenhaUtilizado());

                verify(emailService).enviarEmailHtmlComImagemInline(
                                org.mockito.Mockito.eq(usuario.getEmail()),
                                org.mockito.Mockito.contains("Senha redefinida"),
                                org.mockito.Mockito.contains("Senha alterada"),
                                org.mockito.Mockito.eq("passwordSecurity"),
                                org.mockito.Mockito.eq("static/images/emails/noctua-password-security.png"));
        }

        private UsuarioEntity criarUsuario() {
                UsuarioEntity usuario = new UsuarioEntity();
                usuario.setId(1L);
                usuario.setNome("Usuario Teste");
                usuario.setEmail("usuario@email.com");
                usuario.setSenhaHash("senha-hash-antiga");
                usuario.setAtivo(true);
                usuario.setTokenSenhaUtilizado(false);
                usuario.setTwoFactorEnabled(false);
                return usuario;
        }

        private UsuarioEntity criarUsuarioComToken() {
                UsuarioEntity usuario = criarUsuario();
                usuario.setTokenSenhaReset("token-reset");
                usuario.setTokenSenhaExpiracao(LocalDateTime.now().plusMinutes(10));
                usuario.setTokenSenhaUtilizado(false);
                return usuario;
        }
}
