package dev.LzGuimaraes.FocusLifeHub.Email;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Clock;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

import dev.LzGuimaraes.FocusLifeHub.Auth.MailService;
import dev.LzGuimaraes.FocusLifeHub.Despesa.DespesaModel;
import dev.LzGuimaraes.FocusLifeHub.Despesa.DespesaRepository;
import dev.LzGuimaraes.FocusLifeHub.Despesa.TipoLancamento;
import dev.LzGuimaraes.FocusLifeHub.Tarefas.Enum.TarefaStatus;
import dev.LzGuimaraes.FocusLifeHub.Tarefas.TarefasModel;
import dev.LzGuimaraes.FocusLifeHub.Tarefas.TarefasRepository;
import dev.LzGuimaraes.FocusLifeHub.User.UserModel;

@Component
public class DailyNotificationJob {

    private static final Logger log = LoggerFactory.getLogger(DailyNotificationJob.class);

    private final DespesaRepository despesaRepository;
    private final TarefasRepository tarefasRepository;
    private final EmailLogRepository emailLogRepository;
    private final MailService mailService;
    private final Clock clock;
    private final AtomicBoolean executando = new AtomicBoolean();
    @Value("${app.frontend.url:https://focus.lzguimaraes.com.br}")
    private String frontendUrl = "https://focus.lzguimaraes.com.br";

    @Autowired
    public DailyNotificationJob(DespesaRepository despesaRepository,
                                TarefasRepository tarefasRepository,
                                EmailLogRepository emailLogRepository,
                                MailService mailService) {
        this(despesaRepository, tarefasRepository, emailLogRepository, mailService,
                Clock.system(ZoneId.of("America/Cuiaba")));
    }

    DailyNotificationJob(DespesaRepository despesaRepository, TarefasRepository tarefasRepository,
                         EmailLogRepository emailLogRepository, MailService mailService, Clock clock) {
        this.despesaRepository = despesaRepository;
        this.tarefasRepository = tarefasRepository;
        this.emailLogRepository = emailLogRepository;
        this.mailService = mailService;
        this.clock = clock;
    }

    // As consultas excluem os registros já enviados: a repetição recupera
    // falhas temporárias e lançamentos feitos depois da primeira execução.
    @Scheduled(cron = "${app.notifications.cron:0 0 8-23 * * *}", zone = "America/Cuiaba")
    public void run() {
        if (!executando.compareAndSet(false, true)) {
            log.info("Job de notificações já em execução; chamada simultânea ignorada");
            return;
        }
        try {
            executar(LocalDate.now(clock));
        } finally {
            executando.set(false);
        }
    }

    @EventListener(ApplicationReadyEvent.class)
    public void recuperarAoIniciar() {
        if (!LocalTime.now(clock).isBefore(LocalTime.of(8, 0))) {
            run();
        }
    }

    private void executar(LocalDate hoje) {
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
     * até a data e que ainda NÃO possuem email_log (CONTA_VENCIMENTO) para a
     * mesma conta/data do aviso. Vencidas são lembradas uma vez por dia até o pagamento.
     */
    public List<DespesaModel> selecionarContasParaNotificar(LocalDate data) {
        return despesaRepository.buscarContasParaNotificar(data, TipoLancamento.DESPESA, TipoEmailLog.CONTA_VENCIMENTO);
    }

    private void enviarEmailsDeVencimento(LocalDate data) {
        List<DespesaModel> contas = selecionarContasParaNotificar(data);
        log.info("Notificações de contas: {} lançamento(s) pendente(s) para {}", contas.size(), data);

        Map<UserModel, List<DespesaModel>> porUsuario = contas.stream()
                .filter(c -> c.getCarteiraDividas() != null && c.getCarteiraDividas().getUser() != null)
                .collect(Collectors.groupingBy(c -> c.getCarteiraDividas().getUser()));

        for (Map.Entry<UserModel, List<DespesaModel>> entry : porUsuario.entrySet()) {
            try {
                UserModel usuario = entry.getKey();
                List<DespesaModel> contasDoUsuario = entry.getValue();
                log.info("Enviando e-mail de vencimento para {} ({} contas)", usuario.getEmail(), contasDoUsuario.size());
                enviarEmailParaUsuario(usuario, contasDoUsuario, data);
            } catch (Exception ex) {
                // Falha em um usuário não pode impedir os demais
                log.error("Falha ao enviar e-mail de vencimento para o usuário {} (contas: {})",
                        entry.getKey().getEmail(),
                        entry.getValue().stream().map(DespesaModel::getId).collect(Collectors.toList()),
                        ex);
            }
        }
    }

    private void enviarEmailParaUsuario(UserModel usuario, List<DespesaModel> contas, LocalDate data) throws Exception {
        String subject = EmailTemplate.assuntoContas(data, contas);
        mailService.sendHtml(usuario.getEmail(), subject, EmailTemplate.contasVencendo(data, contas, frontendUrl));

        List<EmailLogModel> registros = contas.stream().map(conta -> {
            EmailLogModel emailLog = new EmailLogModel();
            emailLog.setUsuario_id(usuario.getId());
            emailLog.setTipo(TipoEmailLog.CONTA_VENCIMENTO);
            emailLog.setReferencia_id(conta.getId());
            emailLog.setData_referencia(data);
            emailLog.setEnviado_em(LocalDateTime.now(clock));
            return emailLog;
        }).toList();
        emailLogRepository.saveAll(registros);
    }

    /**
     * Seleciona as tarefas a notificar na data: prazo = data, ativas/pendentes
     * e que ainda NÃO possuem email_log (TAREFA_DIA) para a mesma tarefa/data.
     */
    public List<TarefasModel> selecionarTarefasParaNotificar(LocalDate data) {
        return tarefasRepository.buscarTarefasParaNotificar(data,
                List.of(TarefaStatus.Ativa, TarefaStatus.PENDENTE), TipoEmailLog.TAREFA_DIA);
    }

    private void enviarEmailsDeTarefas(LocalDate data) {
        List<TarefasModel> tarefas = selecionarTarefasParaNotificar(data);
        log.info("Notificações de tarefas: {} tarefa(s) pendente(s) para {}", tarefas.size(), data);

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

        List<EmailLogModel> registros = tarefas.stream().map(tarefa -> {
            EmailLogModel emailLog = new EmailLogModel();
            emailLog.setUsuario_id(usuario.getId());
            emailLog.setTipo(TipoEmailLog.TAREFA_DIA);
            emailLog.setReferencia_id(tarefa.getId());
            emailLog.setData_referencia(data);
            emailLog.setEnviado_em(LocalDateTime.now(clock));
            return emailLog;
        }).toList();
        emailLogRepository.saveAll(registros);
    }
}
