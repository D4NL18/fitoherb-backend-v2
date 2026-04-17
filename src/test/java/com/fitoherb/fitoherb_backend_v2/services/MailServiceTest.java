package com.fitoherb.fitoherb_backend_v2.services;

import com.fitoherb.fitoherb_backend_v2.dtos.requests.MailReq;
import com.fitoherb.fitoherb_backend_v2.exceptions.MailSendingException;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MailServiceTest {

    @Mock
    private JavaMailSender javaMailSender;

    @Mock
    private MimeMessage mimeMessage;

    private MailService mailService;

    private static final String SENDER_EMAIL = "suporte@fitoherb.com";

    @BeforeEach
    void setup() {
        mailService = new MailService(javaMailSender);
        ReflectionTestUtils.setField(mailService, "senderEmail", SENDER_EMAIL);
    }

    @Nested
    @DisplayName("Testes de Envio de E-mail")
    class EmailSendingTests {

        @Test
        void sendEmailSuccess() {
            MailReq req = new MailReq();
            req.setEmail("cliente@gmail.com");
            req.setSubject("Teste");
            req.setMessage("Olá,\nTeste.");

            when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);

            assertDoesNotThrow(() -> mailService.sendEmail(req));

            verify(javaMailSender).send(any(MimeMessage.class));
        }

        @Test
        void sendEmailFailureShouldThrowMailSendingException() {
            MailReq req = new MailReq();
            req.setEmail("erro@test.com");

            when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);
            lenient().doThrow(new RuntimeException("SMTP Down")).when(javaMailSender).send(any(MimeMessage.class));

            assertThrows(MailSendingException.class, () -> mailService.sendEmail(req));
        }
    }

    @Nested
    @DisplayName("Testes de Segurança e Robustez")
    class SecurityTests {

        @Test
        void emailInjectionAttemptShouldFail() {
            MailReq req = new MailReq();
            req.setEmail("victim@test.com\nCc: attacker@test.com");
            req.setSubject("Assunto");
            req.setMessage("Conteúdo");

            when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);

            assertThrows(MailSendingException.class, () -> mailService.sendEmail(req));
        }

        @Test
        void xssInEmailBody() {
            MailReq req = new MailReq();
            req.setEmail("user@test.com");
            req.setSubject("Alerta");
            req.setMessage("<script>alert('hack')</script>");

            when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);

            assertDoesNotThrow(() -> mailService.sendEmail(req));
        }

        @Test
        void veryLargeMessageBody() {
            MailReq req = new MailReq();
            req.setEmail("user@test.com");
            req.setSubject("Relatório");
            req.setMessage("A".repeat(10000));

            when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);

            assertDoesNotThrow(() -> mailService.sendEmail(req));
        }

        @Test
        void nullCharactersInMessage() {
            MailReq req = new MailReq();
            req.setEmail("user@test.com");
            req.setSubject("Título\0");
            req.setMessage("Corpo \0");

            when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);

            assertDoesNotThrow(() -> mailService.sendEmail(req));
        }
    }
}