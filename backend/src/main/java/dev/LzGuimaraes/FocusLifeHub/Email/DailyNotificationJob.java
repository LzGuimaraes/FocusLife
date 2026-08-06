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
import dev.LzGuimaraes.FocusLifeHub.Tarefas.Enum.TarefaStatus;
import dev.LzGuimaraes.FocusLifeHub.Tarefas.TarefasModel;
import dev.LzGuimaraes.FocusLifeHub.Tarefas.TarefasRepository;
import dev.LzGuimaraes.FocusLifeHub.User.UserModel;

@Component
public class DailyNotificationJob {

    private static final Logger log = LoggerFactory.getLogger(DailyNotificationJob.class);

    private final ContasRepository contasRepository;
    private final TarefasRepository tarefasRepository;
    private final EmailLogRepository emailLogRepository;
    private final MailService mailService;

    public DailyNotificationJob(ContasRepository contasRepository,
                                TarefasRepository tarefasRepository,
                                EmailLogRepository emailLogRepository,
                                MailService mailService) {
        this.contasRepository = contasRepository;
        this.tarefasRepository = tarefasRepository;
        this.emailLogRepository = emailLogRepository;
        this.mailService = mailService;
    }

    @Scheduled(cron = "0 0 8 * * *", zone = "America/Cuiaba")
    public void run() {
        LocalDate hoje = LocalDate.now();
        log.info("Job diário executado - {}", hoje);

        // Blocos independentes: falha em um não impede o outro
        try {
            enviarEmailsDeVencimento(hoje);
        } catch (Exception ex) {
            log.error("Falha no bloco de contas vencendo", ex);
        }
        try {
            enviarEmailsDeTarefas(hoje);
        } catch (Exception ex) {
            log.error("Falha no bloco de tarefas do dia", ex);
        }
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
                .filter(c -> c.getCarteiraAtiva() != null && c.getCarteiraAtiva().getUser() != null)
                .collect(Collectors.groupingBy(c -> c.getCarteiraAtiva().getUser()));

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
        String subject = "FocusLife Hub — Contas vencendo hoje (" + data + ")";
        mailService.sendHtml(usuario.getEmail(), subject, EmailTemplate.contasVencendo(data, contas));

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

    /**
     * Seleciona as tarefas a notificar na data: prazo = data, não concluídas
     * e que ainda NÃO possuem email_log (TAREFA_DIA) para a mesma tarefa/data.
     */
    public List<TarefasModel> selecionarTarefasParaNotificar(LocalDate data) {
        return tarefasRepository.findByPrazoAndStatusNot(data, TarefaStatus.Concluida)
                .stream()
                .filter(tarefa -> !emailLogRepository.existsByTipoAndReferenciaIdAndDataReferencia(
                        TipoEmailLog.TAREFA_DIA, tarefa.getId(), data))
                .collect(Collectors.toList());
    }

    private void enviarEmailsDeTarefas(LocalDate data) {
        List<TarefasModel> tarefas = selecionarTarefasParaNotificar(data);

        Map<UserModel, List<TarefasModel>> porUsuario = tarefas.stream()
                .filter(t -> t.getUser() != null)
                .collect(Collectors.groupingBy(TarefasModel::getUser));

        for (Map.Entry<UserModel, List<TarefasModel>> entry : porUsuario.entrySet()) {
            try {
                UserModel usuario = entry.getKey();
                List<TarefasModel> tarefasDoUsuario = entry.getValue();
                log.info("Enviando e-mail de tarefas do dia para {} ({} tarefas)", usuario.getEmail(), tarefasDoUsuario.size());
                enviarEmailTarefasParaUsuario(usuario, tarefasDoUsuario, data);
            } catch (Exception ex) {
                log.error("Falha ao enviar e-mail de tarefas do dia para o usuário {} (tarefas: {})",
                        entry.getKey().getEmail(),
                        entry.getValue().stream().map(TarefasModel::getId).collect(Collectors.toList()),
                        ex);
            }
        }
    }

    private void enviarEmailTarefasParaUsuario(UserModel usuario, List<TarefasModel> tarefas, LocalDate data) throws Exception {
        String subject = "FocusLife Hub — Tarefas do dia (" + data + ")";
        mailService.sendHtml(usuario.getEmail(), subject, EmailTemplate.tarefasDoDia(data, tarefas));

        for (TarefasModel tarefa : tarefas) {
            EmailLogModel emailLog = new EmailLogModel();
            emailLog.setUsuario_id(usuario.getId());
            emailLog.setTipo(TipoEmailLog.TAREFA_DIA);
            emailLog.setReferencia_id(tarefa.getId());
            emailLog.setData_referencia(data);
            emailLog.setEnviado_em(LocalDateTime.now());
            emailLogRepository.save(emailLog);
            log.info("email_log gravado para tarefa {}", tarefa.getId());
        }
    }
}
