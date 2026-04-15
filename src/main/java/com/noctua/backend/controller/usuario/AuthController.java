package com.noctua.backend.controller.usuario;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.noctua.backend.dto.Login.LoginRequestDTO;
import com.noctua.backend.dto.twoFactor.TwoFactorVerifyLoginRequestDTO;
import com.noctua.backend.service.usuario.AuthService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

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
}