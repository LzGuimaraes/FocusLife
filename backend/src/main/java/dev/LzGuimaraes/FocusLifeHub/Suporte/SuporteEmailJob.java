package dev.LzGuimaraes.FocusLifeHub.Suporte;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class SuporteEmailJob {
    private static final Logger log = LoggerFactory.getLogger(SuporteEmailJob.class);
    private final SuporteRepository chamados;
    private final SuporteEmailService emails;
    public SuporteEmailJob(SuporteRepository chamados, SuporteEmailService emails) { this.chamados = chamados; this.emails = emails; }
    @Scheduled(fixedDelay = 600_000, initialDelay = 600_000)
    public void reenviarPendentes() {
        for (Long id : chamados.buscarPendentes(PageRequest.of(0, 20))) {
            try { emails.enviar(id); }
            catch (RuntimeException ex) { log.warn("Não foi possível processar o aviso do chamado {}: {}", id, ex.getClass().getSimpleName()); }
        }
    }
}
