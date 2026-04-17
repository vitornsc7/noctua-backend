package com.noctua.backend.service.usuario;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.noctua.backend.entity.Usuario.UsuarioEntity;
import com.noctua.backend.repository.usuario.UsuarioRepository;

@Service
public class PasswordResetService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    public PasswordResetService(
            UsuarioRepository usuarioRepository,
            PasswordEncoder passwordEncoder,
            EmailService emailService) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

    public void solicitarResetSenha(String email) {
        Optional<UsuarioEntity> usuarioOpt = usuarioRepository.findByEmail(email);

        if (usuarioOpt.isEmpty()) {
            return;
        }

        UsuarioEntity usuario = usuarioOpt.get();

        String token = UUID.randomUUID().toString();
        LocalDateTime expiracao = LocalDateTime.now().plusMinutes(30);

        usuario.setTokenSenhaReset(token);
        usuario.setTokenSenhaExpiracao(expiracao);
        usuario.setTokenSenhaUtilizado(false);

        usuarioRepository.save(usuario);

        String link = frontendUrl + "/reset-password?token=" + token;

        String assunto = "Redefinição de senha - Noctua";
        String conteudo = """
                Olá,

                Recebemos uma solicitação para redefinir a sua senha no Noctua.

                Clique no link abaixo para cadastrar uma nova senha:
                %s

                Este link expira em 30 minutos.

                Se você não fez essa solicitação, ignore este e-mail.
                """.formatted(link);

        emailService.enviarEmail(usuario.getEmail(), assunto, conteudo);
    }

    public void redefinirSenha(String token, String novaSenha, String confirmacaoSenha) {
        if (!novaSenha.equals(confirmacaoSenha)) {
            throw new IllegalArgumentException("As senhas não coincidem.");
        }

        if (novaSenha.length() < 8) {
            throw new IllegalArgumentException("A nova senha deve ter pelo menos 8 caracteres.");
        }

        UsuarioEntity usuario = usuarioRepository.findByTokenSenhaReset(token)
                .orElseThrow(() -> new IllegalArgumentException("Token inválido."));

        if (usuario.getTokenSenhaExpiracao() == null ||
                usuario.getTokenSenhaExpiracao().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Token expirado.");
        }

        if (Boolean.TRUE.equals(usuario.getTokenSenhaUtilizado())) {
            throw new IllegalArgumentException("Token já utilizado.");
        }

        usuario.setSenhaHash(passwordEncoder.encode(novaSenha));
        usuario.setTokenSenhaReset(null);
        usuario.setTokenSenhaExpiracao(null);
        usuario.setTokenSenhaUtilizado(true);

        usuarioRepository.save(usuario);
    }
}
