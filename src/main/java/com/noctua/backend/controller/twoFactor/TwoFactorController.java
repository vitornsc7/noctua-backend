package com.noctua.backend.controller.twoFactor;

import java.time.LocalDateTime;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import com.noctua.backend.dto.twoFactor.TwoFactorSetupResponseDTO;
import com.noctua.backend.entity.Usuario.UsuarioEntity;
import com.noctua.backend.repository.usuario.UsuarioRepository;
import com.noctua.backend.service.twoFactor.TwoFactorService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/2fa")
@RequiredArgsConstructor
@Validated
public class TwoFactorController {

    private final TwoFactorService twoFactorService;
    private final UsuarioRepository usuarioRepository;

    @PostMapping("/setup")
    public ResponseEntity<TwoFactorSetupResponseDTO> setup(Authentication authentication) {
        String email = authentication.getName();

        UsuarioEntity usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado."));

        String secret = twoFactorService.generateSecret();
        String otpauthUrl = twoFactorService.buildOtpAuthUrl(usuario.getEmail(), secret);

        usuario.setTwoFactorTempSecret(secret);
        usuarioRepository.save(usuario);

        return ResponseEntity.ok(new TwoFactorSetupResponseDTO(secret, otpauthUrl));
    }

    @PostMapping("/verify-setup")
    public ResponseEntity<?> verifySetup(
            Authentication authentication,
            @Valid @RequestBody TwoFactorVerifySetupRequestDTO request) {

        String email = authentication.getName();

        UsuarioEntity usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado."));

        String tempSecret = usuario.getTwoFactorTempSecret();

        if (tempSecret == null || tempSecret.isBlank()) {
            return ResponseEntity.badRequest().body("Configuração 2FA não iniciada.");
        }

        boolean valid = twoFactorService.verifyCode(tempSecret, Integer.parseInt(request.getCode()));

        if (!valid) {
            return ResponseEntity.badRequest().body("Código inválido.");
        }

        usuario.setTwoFactorSecret(tempSecret);
        usuario.setTwoFactorTempSecret(null);
        usuario.setTwoFactorEnabled(true);
        usuario.setTwoFactorConfirmedAt(LocalDateTime.now());

        usuarioRepository.save(usuario);

        return ResponseEntity.ok("2FA ativado com sucesso.");
    }
}