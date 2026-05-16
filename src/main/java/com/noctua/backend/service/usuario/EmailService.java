package com.noctua.backend.service.usuario;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String remetente;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void enviarEmail(String destinatario, String assunto, String conteudo) {
        enviar(destinatario, assunto, conteudo, false);
    }

    public void enviarEmailHtml(String destinatario, String assunto, String conteudoHtml) {
        enviar(destinatario, assunto, conteudoHtml, true);
    }

    public void enviarEmailHtmlComImagemInline(
            String destinatario,
            String assunto,
            String conteudoHtml,
            String contentId,
            String caminhoImagem) {
        try {
            MimeMessage mensagem = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mensagem, true, "UTF-8");
            helper.setFrom(remetente);
            helper.setTo(destinatario);
            helper.setSubject(assunto);
            helper.setText(conteudoHtml, true);
            helper.addInline(contentId, new ClassPathResource(caminhoImagem));

            mailSender.send(mensagem);
        } catch (MessagingException exception) {
            throw new IllegalStateException("Erro ao montar e-mail com imagem.", exception);
        }
    }

    private void enviar(String destinatario, String assunto, String conteudo, boolean html) {
        try {
            MimeMessage mensagem = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mensagem, "UTF-8");
            helper.setFrom(remetente);
            helper.setTo(destinatario);
            helper.setSubject(assunto);
            helper.setText(conteudo, html);

            mailSender.send(mensagem);
        } catch (MessagingException exception) {
            throw new IllegalStateException("Erro ao montar e-mail.", exception);
        }
    }
}
