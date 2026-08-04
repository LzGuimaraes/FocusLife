package dev.LzGuimaraes.FocusLifeHub.Email;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.LzGuimaraes.FocusLifeHub.Auth.MailService;
import dev.LzGuimaraes.FocusLifeHub.Contas.ContasModel;
import dev.LzGuimaraes.FocusLifeHub.Contas.ContasRepository;

class DailyNotificationJobTest {

    private ContasRepository contasRepository;
    private EmailLogRepository emailLogRepository;
    private MailService mailService;
    private DailyNotificationJob job;

    @BeforeEach
    void setUp() {
        contasRepository = mock(ContasRepository.class);
        emailLogRepository = mock(EmailLogRepository.class);
        mailService = mock(MailService.class);
        job = new DailyNotificationJob(contasRepository, emailLogRepository, mailService);
    }

    @Test
    void selecionaSomenteContasSemEmailLogJaEnviado() {
        LocalDate hoje = LocalDate.of(2026, 8, 3);

        ContasModel jaNotificada = new ContasModel();
        jaNotificada.setId(1L);

        ContasModel pendente = new ContasModel();
        pendente.setId(2L);

        when(contasRepository.findByPagoFalseAndDataVencimento(hoje))
                .thenReturn(List.of(jaNotificada, pendente));
        when(emailLogRepository.existsByTipoAndReferenciaIdAndDataReferencia(
                TipoEmailLog.CONTA_VENCIMENTO, 1L, hoje)).thenReturn(true);
        when(emailLogRepository.existsByTipoAndReferenciaIdAndDataReferencia(
                TipoEmailLog.CONTA_VENCIMENTO, 2L, hoje)).thenReturn(false);

        List<ContasModel> resultado = job.selecionarContasParaNotificar(hoje);

        assertEquals(1, resultado.size());
        assertEquals(2L, resultado.get(0).getId());
    }
}
