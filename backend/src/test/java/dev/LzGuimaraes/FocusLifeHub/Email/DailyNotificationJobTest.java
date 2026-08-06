package dev.LzGuimaraes.FocusLifeHub.Email;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.LzGuimaraes.FocusLifeHub.Auth.MailService;
import dev.LzGuimaraes.FocusLifeHub.Despesa.DespesaModel;
import dev.LzGuimaraes.FocusLifeHub.Despesa.DespesaRepository;
import dev.LzGuimaraes.FocusLifeHub.Tarefas.TarefasRepository;

class DailyNotificationJobTest {

    private DespesaRepository despesaRepository;
    private TarefasRepository tarefasRepository;
    private EmailLogRepository emailLogRepository;
    private MailService mailService;
    private DailyNotificationJob job;

    @BeforeEach
    void setUp() {
        despesaRepository = mock(DespesaRepository.class);
        tarefasRepository = mock(TarefasRepository.class);
        emailLogRepository = mock(EmailLogRepository.class);
        mailService = mock(MailService.class);
        job = new DailyNotificationJob(despesaRepository, tarefasRepository, emailLogRepository, mailService);
    }

    @Test
    void selecionaSomenteContasSemEmailLogJaEnviado() {
        LocalDate hoje = LocalDate.of(2026, 8, 3);

        DespesaModel jaNotificada = new DespesaModel();
        jaNotificada.setId(1L);

        DespesaModel pendente = new DespesaModel();
        pendente.setId(2L);

        when(despesaRepository.findByPagoFalseAndDataVencimento(hoje))
                .thenReturn(List.of(jaNotificada, pendente));
        when(emailLogRepository.existsByTipoAndReferenciaIdAndDataReferencia(
                TipoEmailLog.CONTA_VENCIMENTO, 1L, hoje)).thenReturn(true);
        when(emailLogRepository.existsByTipoAndReferenciaIdAndDataReferencia(
                TipoEmailLog.CONTA_VENCIMENTO, 2L, hoje)).thenReturn(false);

        List<DespesaModel> resultado = job.selecionarContasParaNotificar(hoje);

        assertEquals(1, resultado.size());
        assertEquals(2L, resultado.get(0).getId());
    }
}
