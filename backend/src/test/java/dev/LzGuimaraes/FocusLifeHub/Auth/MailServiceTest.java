package dev.LzGuimaraes.FocusLifeHub.Auth;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.Properties;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSender;

class MailServiceTest {
    @Test void mensagemDeSuporteUsaRemetenteConfiguradoEDestinoEReplyToCorretos() throws Exception {
        var sender = mock(JavaMailSender.class);
        var message = new MimeMessage(Session.getInstance(new Properties()));
        when(sender.createMimeMessage()).thenReturn(message);
        new MailService(sender, "sistema@example.test").sendHtml("luizsantosleventi19901@gmail.com", "Chamado SUP-000001", "<p>Pedido</p>", "usuario@example.test");
        message.saveChanges(); // O JavaMailSender real prepara os cabeçalhos antes de transmitir.
        assertThat(message.getFrom()[0].toString()).isEqualTo("sistema@example.test");
        assertThat(message.getAllRecipients()[0].toString()).isEqualTo("luizsantosleventi19901@gmail.com");
        assertThat(message.getHeader("Reply-To", null)).isEqualTo("usuario@example.test");
        assertThat(message.getContentType()).contains("text/html");
        verify(sender).send(message);
    }
    @Test void chamadaOriginalDeTresArgumentosMantemComportamento() throws Exception {
        var sender = mock(JavaMailSender.class);
        var message = new MimeMessage(Session.getInstance(new Properties()));
        when(sender.createMimeMessage()).thenReturn(message);
        new MailService(sender, "sistema@example.test").sendHtml("usuario@example.test", "Ativação", "<p>Ative sua conta</p>");
        assertThat(message.getHeader("Reply-To", null)).isNull();
        assertThat(message.getAllRecipients()[0].toString()).isEqualTo("usuario@example.test");
        verify(sender).send(message);
    }
}
