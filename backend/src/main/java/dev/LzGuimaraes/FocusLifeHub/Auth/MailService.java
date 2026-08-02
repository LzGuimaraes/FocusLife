package dev.LzGuimaraes.FocusLifeHub.Auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
public class MailService {

    private static final Logger log = LoggerFactory.getLogger(MailService.class);
    private final JavaMailSender mailSender;
    private final String fromAddress;

    public MailService(JavaMailSender mailSender,
                       @Value("${spring.mail.from:${spring.mail.username}}") String fromAddress) {
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
    }

    public void sendHtml(String to, String subject, String htmlBody) throws MessagingException {
        if (fromAddress == null || fromAddress.isBlank()) {
            throw new IllegalStateException("spring.mail.from or spring.mail.username must be configured");
        }

        log.info("Enviando email '{}' para {} usando remetente {}", subject, to, fromAddress);
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");
        helper.setFrom(fromAddress);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(htmlBody, true);
        mailSender.send(message);
        log.info("Email '{}' enviado para {}", subject, to);
    }
}
