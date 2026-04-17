package com.fitoherb.fitoherb_backend_v2.services;

import com.fitoherb.fitoherb_backend_v2.dtos.requests.MailReq;
import com.fitoherb.fitoherb_backend_v2.exceptions.MailSendingException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MailService {

    private final JavaMailSender javaMailSender;

    @Value("${spring.mail.username}")
    private String senderEmail;

    public void sendEmail(MailReq mailReq) {
        try {
            var message = javaMailSender.createMimeMessage();
            var helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom("Suporte Fitoherb <" + senderEmail + ">");
            helper.setTo(mailReq.getEmail());
            helper.setSubject(mailReq.getSubject());

            String htmlContent = """
                <div style="background-color: #f4f4f4; padding: 20px; font-family: sans-serif;">
                    <div style="background-color: #ffffff; padding: 40px; border-radius: 8px; max-width: 600px; margin: 0 auto;">
                        <h1 style="color: #2e7d32; margin-top: 0;">Fitoherb</h1>
                        <div style="font-size: 16px; line-height: 1.5; color: #333;">
                            %s
                        </div>
                        <hr style="border: 0; border-top: 1px solid #eee; margin: 30px 0;">
                        <p style="font-size: 12px; color: #999; text-align: center;">
                            Esta é uma mensagem automática do sistema Fitoherb.<br>
                            Salvador, Bahia - Brasil.
                        </p>
                    </div>
                </div>
                """.formatted(mailReq.getMessage().replace("\n", "<br>"));

            helper.setText(htmlContent, true);
            javaMailSender.send(message);

        } catch (Exception e) {
            throw new MailSendingException("Falha ao enviar e-mail de boas-vindas", e);
        }
    }
}