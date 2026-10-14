package dev.LzGuimaraes.FocusLifeHub.Suporte;

import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import dev.LzGuimaraes.FocusLifeHub.Auth.MailService;
import jakarta.mail.MessagingException;

@Service
public class SuporteEmailService {
    private static final Logger log = LoggerFactory.getLogger(SuporteEmailService.class);
    private final SuporteRepository chamados;
    private final MailService mail;
    private final String destinatario;
    public SuporteEmailService(SuporteRepository chamados, MailService mail,
            @Value("${app.suporte.email:luizsantosleventi19901@gmail.com}") String destinatario) {
        this.chamados = chamados; this.mail = mail; this.destinatario = destinatario;
    }

    @Transactional
    public void enviar(Long id) {
        var chamado = chamados.buscarParaEnviar(id).orElse(null);
        if (chamado == null || chamado.isEmailEnviado()) return;
        chamado.setTentativas(chamado.getTentativas() + 1);
        chamado.setUltimaTentativaEm(Instant.now());
        try {
            String assunto = "FocusLife — " + SuporteDTO.protocolo(id) + " — " + chamado.getTipo().descricao();
            mail.sendHtml(destinatario, assunto, SuporteEmailTemplate.html(chamado), chamado.getUsuarioEmail());
            chamado.setEmailEnviado(true);
            chamado.setEmailEnviadoEm(Instant.now());
        } catch (MessagingException | MailException | IllegalStateException ex) {
            log.warn("Aviso do chamado {} pendente após tentativa {}: {}", id, chamado.getTentativas(), ex.getClass().getSimpleName());
        }
    }
}
