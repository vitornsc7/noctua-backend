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

    private static final String PASSWORD_SECURITY_CONTENT_ID = "passwordSecurity";
    private static final String PASSWORD_SECURITY_IMAGE_PATH = "static/images/emails/noctua-password-security.png";

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
        String conteudo = construirEmailRedefinicaoSenha(usuario.getNome(), link);

        enviarEmailHtmlNoctua(usuario.getEmail(), assunto, conteudo);
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

        enviarConfirmacaoResetSenha(usuario);
    }

    private void enviarConfirmacaoResetSenha(UsuarioEntity usuario) {
        String assunto = "Senha redefinida - Noctua";
        String conteudo = construirEmailConfirmacaoSenhaAlterada(usuario.getNome());

        enviarEmailHtmlNoctua(usuario.getEmail(), assunto, conteudo);
    }

    private void enviarEmailHtmlNoctua(String destinatario, String assunto, String conteudo) {
        emailService.enviarEmailHtmlComImagemInline(
                destinatario,
                assunto,
                conteudo,
                PASSWORD_SECURITY_CONTENT_ID,
                PASSWORD_SECURITY_IMAGE_PATH);
    }

    private String construirEmailRedefinicaoSenha(String nome, String link) {
        String nomeSeguro = escapeHtml(nome == null || nome.isBlank() ? "usuário" : nome);
        String linkSeguro = escapeHtml(link);

        return construirTemplateNoctua(
                "Redefinição de senha",
                """
                        <h2 style="margin:0 0 16px 0;font-size:18px;font-weight:600;color:#1E293B;">Redefinir senha</h2>
                        <p style="margin:0 0 12px 0;font-size:15px;line-height:24px;color:#334155;">Olá, <strong>{{nome}}</strong>.</p>
                        <p style="margin:0 0 24px 0;font-size:15px;line-height:24px;color:#475569;">
                            Recebemos uma solicitação para redefinir a sua senha. Se você reconhece essa ação, basta clicar no botão abaixo para escolher uma nova credencial:
                        </p>
                        <table role="presentation" border="0" cellpadding="0" cellspacing="0" width="100%" style="margin-bottom:28px;">
                            <tr>
                                <td align="center">
                                    <a href="{{link}}" target="_blank" style="display:inline-block;background-color:#2D4356;color:#FFFFFF;font-size:15px;font-weight:600;text-decoration:none;padding:14px 32px;border-radius:8px;box-shadow:0 2px 4px rgba(45,67,86,0.2);">Redefinir senha</a>
                                </td>
                            </tr>
                        </table>
                        <p style="margin:0;font-size:14px;line-height:22px;color:#64748B;background-color:#F8FAFC;padding:12px 16px;border-radius:6px;border-left:3px solid #CBD5E1;">
                            <strong>Nota:</strong> Se você não fez essa solicitação, pode ignorar esta mensagem com segurança. Sua senha atual permanecerá a mesma.
                        </p>
                        """
                        .replace("{{nome}}", nomeSeguro)
                        .replace("{{link}}", linkSeguro));
    }

    private String construirEmailConfirmacaoSenhaAlterada(String nome) {
        String nomeSeguro = escapeHtml(nome == null || nome.isBlank() ? "usuário" : nome);

        return construirTemplateNoctua(
                "Senha alterada",
                """
                        <h2 style="margin:0 0 16px 0;font-size:18px;font-weight:600;color:#1E293B;">Senha alterada</h2>
                        <p style="margin:0 0 12px 0;font-size:15px;line-height:24px;color:#334155;">Olá, <strong>{{nome}}</strong>.</p>
                        <p style="margin:0 0 24px 0;font-size:15px;line-height:24px;color:#475569;">
                            Sua senha foi alterada com sucesso. Se foi você quem realizou essa ação, nenhuma etapa adicional é necessária.
                        </p>
                        <p style="margin:0;font-size:14px;line-height:22px;color:#64748B;background-color:#F8FAFC;padding:12px 16px;border-radius:6px;border-left:3px solid #CBD5E1;">
                            <strong>Atenção:</strong> Se você não reconhece essa alteração, redefina sua senha novamente e entre em contato com o suporte imediatamente.
                        </p>
                        """
                        .replace("{{nome}}", nomeSeguro));
    }

    private String construirTemplateNoctua(String titulo, String conteudo) {
        String tituloSeguro = escapeHtml(titulo);

        return """
                <!doctype html>
                <html lang="pt-BR">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>{{titulo}} - Noctua</title>
                </head>
                <body style="margin:0;padding:0;background-color:#F8FAFC;font-family:Inter,-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,Helvetica,Arial,sans-serif;-webkit-font-smoothing:antialiased;width:100% !important;">
                    <table role="presentation" border="0" cellpadding="0" cellspacing="0" width="100%" style="background-color:#F8FAFC;padding:48px 16px;">
                        <tr>
                            <td align="center">
                                <table role="presentation" border="0" cellpadding="0" cellspacing="0" width="100%" style="max-width:520px;background-color:#FFFFFF;border-radius:16px;border:1px solid #E2E8F0;box-shadow:0 4px 6px -1px rgba(0,0,0,0.05),0 2px 4px -1px rgba(0,0,0,0.03);overflow:hidden;">
                                    <tr>
                                        <td align="center" style="padding:40px 40px 24px 40px;">
                                            <img src="cid:passwordSecurity" alt="Coruja Noctua protegendo uma senha" width="220" style="display:block;width:100%;max-width:220px;height:auto;margin:0 auto 20px;border:0;">
                                            <h1 style="margin:0;font-size:26px;font-weight:700;color:#0F172A;letter-spacing:-0.02em;">Noctua</h1>
                                            <p style="margin:4px 0 0 0;font-size:13px;font-weight:400;color:#64748B;font-style:italic;">Insights poderosos que mudam a educação.</p>
                                        </td>
                                    </tr>
                                    <tr>
                                        <td style="padding:0 40px;">
                                            <div style="height:1px;background-color:#E2E8F0;line-height:1px;font-size:1px;">&nbsp;</div>
                                        </td>
                                    </tr>
                                    <tr>
                                        <td style="padding:32px 40px 40px 40px;">
                                            {{conteudo}}
                                        </td>
                                    </tr>
                                    <tr>
                                        <td style="padding:0 40px 32px 40px;">
                                            <p style="margin:0;font-size:14px;line-height:20px;color:#475569;">
                                                Obrigado,<br>
                                                <span style="font-weight:600;color:#2D4356;">Equipe Noctua</span>
                                            </p>
                                        </td>
                                    </tr>
                                </table>
                                <table role="presentation" border="0" cellpadding="0" cellspacing="0" width="100%" style="max-width:520px;margin-top:24px;">
                                    <tr>
                                        <td align="center" style="font-size:12px;color:#94A3B8;line-height:18px;">
                                            Este é um e-mail automático enviado pelo sistema Noctua.<br>
                                            Por favor, não responda a esta mensagem.
                                        </td>
                                    </tr>
                                </table>
                            </td>
                        </tr>
                    </table>
                </body>
                </html>
                """
                .replace("{{titulo}}", tituloSeguro)
                .replace("{{conteudo}}", conteudo);
    }

    private String escapeHtml(String valor) {
        return valor
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
