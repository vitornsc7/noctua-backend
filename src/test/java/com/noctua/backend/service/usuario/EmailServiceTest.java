package com.noctua.backend.service.usuario;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import jakarta.mail.Message;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    private EmailService emailService;

    @BeforeEach
    void setUp() {
        emailService = new EmailService(mailSender);
        ReflectionTestUtils.setField(emailService, "remetente", "no-reply@noctua.com");
    }

    // Teste 1: email simples deve montar remetente, destinatario, assunto e conteudo.
    @Test
    void enviarEmailDeveMontarEEnviarMensagemDeTexto() throws Exception {
        MimeMessage mensagem = criarMimeMessage();

        when(mailSender.createMimeMessage()).thenReturn(mensagem);

        emailService.enviarEmail(
                "usuario@email.com",
                "Assunto simples",
                "Conteudo simples");

        MimeMessage mensagemEnviada = capturarMensagemEnviada();

        assertEquals("no-reply@noctua.com", mensagemEnviada.getFrom()[0].toString());
        assertEquals("usuario@email.com", mensagemEnviada.getRecipients(Message.RecipientType.TO)[0].toString());
        assertEquals("Assunto simples", mensagemEnviada.getSubject());
        assertEquals("Conteudo simples", mensagemEnviada.getContent().toString());
    }

    // Teste 2: email HTML deve preservar o conteudo HTML informado.
    @Test
    void enviarEmailHtmlDeveMontarEEnviarMensagemHtml() throws Exception {
        MimeMessage mensagem = criarMimeMessage();
        String conteudoHtml = "<strong>Conteudo HTML</strong>";

        when(mailSender.createMimeMessage()).thenReturn(mensagem);

        emailService.enviarEmailHtml(
                "usuario@email.com",
                "Assunto HTML",
                conteudoHtml);

        MimeMessage mensagemEnviada = capturarMensagemEnviada();

        assertEquals("no-reply@noctua.com", mensagemEnviada.getFrom()[0].toString());
        assertEquals("usuario@email.com", mensagemEnviada.getRecipients(Message.RecipientType.TO)[0].toString());
        assertEquals("Assunto HTML", mensagemEnviada.getSubject());
        assertEquals(conteudoHtml, mensagemEnviada.getContent().toString());
    }

    // Teste 3: email HTML com imagem inline deve ser multipart e tambem ser enviado.
    @Test
    void enviarEmailHtmlComImagemInlineDeveMontarMensagemMultipart() throws Exception {
        MimeMessage mensagem = criarMimeMessage();

        when(mailSender.createMimeMessage()).thenReturn(mensagem);

        emailService.enviarEmailHtmlComImagemInline(
                "usuario@email.com",
                "Assunto com imagem",
                "<p>Conteudo com imagem</p>",
                "noctuaKey",
                "static/images/emails/noctua-key.png");

        MimeMessage mensagemEnviada = capturarMensagemEnviada();

        assertEquals("no-reply@noctua.com", mensagemEnviada.getFrom()[0].toString());
        assertEquals("usuario@email.com", mensagemEnviada.getRecipients(Message.RecipientType.TO)[0].toString());
        assertEquals("Assunto com imagem", mensagemEnviada.getSubject());
        assertNotNull(mensagemEnviada.getContent());
        assertEquals("multipart", mensagemEnviada.getContentType().substring(0, "multipart".length()).toLowerCase());
    }

    private MimeMessage criarMimeMessage() {
        return new MimeMessage(Session.getInstance(new Properties()));
    }

    private MimeMessage capturarMensagemEnviada() throws Exception {
        ArgumentCaptor<MimeMessage> mensagemCaptor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(mensagemCaptor.capture());
        MimeMessage mensagem = mensagemCaptor.getValue();
        mensagem.saveChanges();
        return mensagem;
    }
}
