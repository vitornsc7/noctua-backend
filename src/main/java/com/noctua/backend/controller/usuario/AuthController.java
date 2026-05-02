package com.noctua.backend.controller.usuario;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.noctua.backend.dto.Login.LoginRequestDTO;
import com.noctua.backend.dto.Usuario.AuthenticatedUserResponseDTO;
import com.noctua.backend.dto.Usuario.ForgotPasswordRequestDTO;
import com.noctua.backend.dto.Usuario.ResetPasswordRequestDTO;
import com.noctua.backend.dto.Usuario.UsuarioUpdateRequestDTO;
import com.noctua.backend.dto.twoFactor.TwoFactorVerifyLoginRequestDTO;
import com.noctua.backend.service.usuario.AuthService;
import com.noctua.backend.service.usuario.PasswordResetService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final PasswordResetService passwordResetService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequestDTO dto) {
        try {
            return ResponseEntity.ok(authService.login(dto));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Erro interno no login.");
        }
    }

    @PostMapping("/2fa/verify-login")
    public ResponseEntity<?> verifyLoginTwoFactor(@RequestBody TwoFactorVerifyLoginRequestDTO dto) {
        try {
            return ResponseEntity.ok(authService.verifyLoginTwoFactor(dto));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Erro interno na validação do 2FA.");
        }
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@Valid @RequestBody ForgotPasswordRequestDTO dto) {
        try {
            passwordResetService.solicitarResetSenha(dto.getEmail());

            return ResponseEntity.ok(Map.of(
                    "message",
                    "Se existir uma conta com esse e-mail, enviaremos instruções para redefinição de senha."));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Erro interno ao solicitar redefinição de senha.");
        }
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@Valid @RequestBody ResetPasswordRequestDTO dto) {
        try {
            passwordResetService.redefinirSenha(
                    dto.getToken(),
                    dto.getNovaSenha(),
                    dto.getConfirmacaoSenha());

            return ResponseEntity.ok(Map.of(
                    "message", "Senha redefinida com sucesso."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Erro interno ao redefinir senha.");
        }
    }

    @GetMapping("/me")
    public ResponseEntity<AuthenticatedUserResponseDTO> me(Authentication authentication) {
        return ResponseEntity.ok(authService.buscarUsuarioAutenticado(authentication.getName()));
    }

    @PutMapping("/me")
    public ResponseEntity<?> atualizarMe(
            Authentication authentication,
            @RequestBody UsuarioUpdateRequestDTO dto) {
        try {
            return ResponseEntity.ok(
                    authService.atualizarUsuarioAutenticado(authentication.getName(), dto));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/me")
    public ResponseEntity<?> excluirMe(Authentication authentication) {
        try {
            authService.excluirUsuarioAutenticado(authentication.getName());
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}