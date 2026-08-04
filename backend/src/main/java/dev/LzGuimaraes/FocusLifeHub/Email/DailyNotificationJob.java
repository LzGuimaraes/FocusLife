package dev.LzGuimaraes.FocusLifeHub.Email;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import dev.LzGuimaraes.FocusLifeHub.Auth.MailService;
import dev.LzGuimaraes.FocusLifeHub.Contas.ContasModel;
import dev.LzGuimaraes.FocusLifeHub.Contas.ContasRepository;
import dev.LzGuimaraes.FocusLifeHub.User.UserModel;

@Component
public class DailyNotificationJob {

    private static final Logger log = LoggerFactory.getLogger(DailyNotificationJob.class);

    private final ContasRepository contasRepository;
    private final EmailLogRepository emailLogRepository;
    private final MailService mailService;

    public DailyNotificationJob(ContasRepository contasRepository,
                                EmailLogRepository emailLogRepository,
                                MailService mailService) {
        this.contasRepository = contasRepository;
        this.emailLogRepository = emailLogRepository;
        this.mailService = mailService;
    }

    @Scheduled(cron = "0 0 8 * * *", zone = "America/Cuiaba")
    public void run() {
        LocalDate hoje = LocalDate.now();
        log.info("Job diário executado - {}", hoje);
        enviarEmailsDeVencimento(hoje);
    }

    /**
     * Seleciona as contas a notificar na data: não pagas, com dataVencimento
     * na data e que ainda NÃO possuem email_log (CONTA_VENCIMENTO) para a
     * mesma conta/data (evita e-mail duplicado).
     */
    public List<ContasModel> selecionarContasParaNotificar(LocalDate data) {
        return contasRepository.findByPagoFalseAndDataVencimento(data)
                .stream()
                .filter(conta -> !emailLogRepository.existsByTipoAndReferenciaIdAndDataReferencia(
                        TipoEmailLog.CONTA_VENCIMENTO, conta.getId(), data))
                .collect(Collectors.toList());
    }

    private void enviarEmailsDeVencimento(LocalDate data) {
        List<ContasModel> contas = selecionarContasParaNotificar(data);

        Map<UserModel, List<ContasModel>> porUsuario = contas.stream()
                .filter(c -> c.getFinancas() != null && c.getFinancas().getUser() != null)
                .collect(Collectors.groupingBy(c -> c.getFinancas().getUser()));

        for (Map.Entry<UserModel, List<ContasModel>> entry : porUsuario.entrySet()) {
            try {
                UserModel usuario = entry.getKey();
                List<ContasModel> contasDoUsuario = entry.getValue();
                log.info("Enviando e-mail de vencimento para {} ({} contas)", usuario.getEmail(), contasDoUsuario.size());
                enviarEmailParaUsuario(usuario, contasDoUsuario, data);
            } catch (Exception ex) {
                // Falha em um usuário não pode impedir os demais
                log.error("Falha ao enviar e-mail de vencimento para o usuário {} (contas: {})",
                        entry.getKey().getEmail(),
                        entry.getValue().stream().map(ContasModel::getId).collect(Collectors.toList()),
                        ex);
            }
        }
    }

    private void enviarEmailParaUsuario(UserModel usuario, List<ContasModel> contas, LocalDate data) throws Exception {
        StringBuilder html = new StringBuilder();
        html.append("<h2>FocusLife Hub — Contas vencendo hoje (").append(data).append(")</h2>");
        html.append("<ul>");
        for (ContasModel conta : contas) {
            html.append("<li><b>").append(conta.getNome()).append("</b> — R$ ")
                .append(String.format("%.2f", conta.getSaldo() != null ? conta.getSaldo() : 0f))
                .append("</li>");
        }
        html.append("</ul>");

        String subject = "FocusLife Hub — Contas vencendo hoje (" + data + ")";
        mailService.sendHtml(usuario.getEmail(), subject, html.toString());

        for (ContasModel conta : contas) {
            EmailLogModel emailLog = new EmailLogModel();
            emailLog.setUsuario_id(usuario.getId());
            emailLog.setTipo(TipoEmailLog.CONTA_VENCIMENTO);
            emailLog.setReferencia_id(conta.getId());
            emailLog.setData_referencia(data);
            emailLog.setEnviado_em(LocalDateTime.now());
            emailLogRepository.save(emailLog);
            log.info("email_log gravado para conta {}", conta.getId());
        }
    }
}
