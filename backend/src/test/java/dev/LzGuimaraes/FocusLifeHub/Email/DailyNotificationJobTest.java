package dev.LzGuimaraes.FocusLifeHub.Email;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.MailSendException;

import dev.LzGuimaraes.FocusLifeHub.Auth.MailService;
import dev.LzGuimaraes.FocusLifeHub.Carteira.CarteiraDividasModel;
import dev.LzGuimaraes.FocusLifeHub.Despesa.DespesaModel;
import dev.LzGuimaraes.FocusLifeHub.Despesa.DespesaRepository;
import dev.LzGuimaraes.FocusLifeHub.Despesa.TipoLancamento;
import dev.LzGuimaraes.FocusLifeHub.Tarefas.Enum.TarefaStatus;
import dev.LzGuimaraes.FocusLifeHub.Tarefas.TarefasModel;
import dev.LzGuimaraes.FocusLifeHub.Tarefas.TarefasRepository;
import dev.LzGuimaraes.FocusLifeHub.User.UserModel;

class DailyNotificationJobTest {
    private static final LocalDate DIA = LocalDate.of(2026, 10, 13);
    private static final List<TarefaStatus> STATUS = List.of(TarefaStatus.Ativa, TarefaStatus.PENDENTE);
    private DespesaRepository despesas;
    private TarefasRepository tarefas;
    private EmailLogRepository registros;
    private MailService mail;
    private DailyNotificationJob job;

    @BeforeEach
    void setUp() {
        despesas = mock(DespesaRepository.class);
        tarefas = mock(TarefasRepository.class);
        registros = mock(EmailLogRepository.class);
        mail = mock(MailService.class);
        job = novoJob("2026-10-13T12:00:00Z"); // 08h em Cuiabá
    }

    @Test
    void agrupaContasDoMesmoUsuarioERegistraSomenteAposEnviar() throws Exception {
        var usuario = usuario(1L);
        when(despesas.buscarContasParaNotificar(DIA, TipoLancamento.DESPESA, TipoEmailLog.CONTA_VENCIMENTO))
                .thenReturn(List.of(conta(1L, usuario), conta(2L, usuario)));
        job.run();
        verify(mail).sendHtml(eq(usuario.getEmail()), eq("FocusLife Hub — Contas vencendo hoje (2026-10-13)"),
                org.mockito.ArgumentMatchers.contains("Conta 1"));
        var ordem = inOrder(mail, registros);
        ordem.verify(mail).sendHtml(anyString(), anyString(), anyString());
        ordem.verify(registros).saveAll(any());
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<EmailLogModel>> captor = ArgumentCaptor.forClass(List.class);
        verify(registros).saveAll(captor.capture());
        assertThat(captor.getValue()).hasSize(2).allSatisfy(r -> {
            assertThat(r.getData_referencia()).isEqualTo(DIA);
            assertThat(r.getUsuario_id()).isEqualTo(1L);
            assertThat(r.getEnviado_em().getHour()).isEqualTo(8);
        });
    }

    @Test
    void falhaDoSmtpNaoGravaSucessoEPermiteNovaTentativa() throws Exception {
        var usuario = usuario(1L);
        when(despesas.buscarContasParaNotificar(DIA, TipoLancamento.DESPESA, TipoEmailLog.CONTA_VENCIMENTO))
                .thenReturn(List.of(conta(1L, usuario)));
        doThrow(new MailSendException("SMTP temporariamente indisponível"))
                .doNothing().when(mail).sendHtml(anyString(), anyString(), anyString());
        job.run();
        verifyNoInteractions(registros);
        job.run();
        verify(mail, times(2)).sendHtml(anyString(), anyString(), anyString());
        verify(registros).saveAll(any());
    }

    @Test
    void falhaDeUmDestinatarioNaoImpedeOutrosOuAsTarefas() throws Exception {
        var primeiro = usuario(1L);
        var segundo = usuario(2L);
        when(despesas.buscarContasParaNotificar(DIA, TipoLancamento.DESPESA, TipoEmailLog.CONTA_VENCIMENTO))
                .thenReturn(List.of(conta(1L, primeiro), conta(2L, segundo)));
        var tarefa = new TarefasModel();
        tarefa.setId(3L);
        tarefa.setTitulo("Entregar trabalho");
        tarefa.setUser(segundo);
        when(tarefas.buscarTarefasParaNotificar(DIA, STATUS, TipoEmailLog.TAREFA_DIA)).thenReturn(List.of(tarefa));
        doThrow(new MailSendException("Falha do destinatário"))
                .when(mail).sendHtml(eq(primeiro.getEmail()), anyString(), anyString());
        job.run();
        verify(mail, times(2)).sendHtml(eq(segundo.getEmail()), anyString(), anyString());
        verify(registros, times(2)).saveAll(any());
    }

    @Test
    void falhaNaConsultaDeContasNaoImpedeBlocoDeTarefas() {
        when(despesas.buscarContasParaNotificar(DIA, TipoLancamento.DESPESA, TipoEmailLog.CONTA_VENCIMENTO))
                .thenThrow(new IllegalStateException("Falha de consulta"));
        job.run();
        verify(tarefas).buscarTarefasParaNotificar(DIA, STATUS, TipoEmailLog.TAREFA_DIA);
    }

    @Test
    void usaDataDeCuiabaMesmoQuandoUtcJaEstaNoDiaSeguinte() {
        novoJob("2026-10-14T02:00:00Z").run(); // 22h de 13/10 em Cuiabá
        verify(despesas).buscarContasParaNotificar(DIA, TipoLancamento.DESPESA, TipoEmailLog.CONTA_VENCIMENTO);
        verify(tarefas).buscarTarefasParaNotificar(DIA, STATUS, TipoEmailLog.TAREFA_DIA);
    }

    @Test
    void inicializacaoAntesDasOitoNaoDisparaAvisos() {
        novoJob("2026-10-13T11:59:59Z").recuperarAoIniciar();
        verifyNoInteractions(despesas, tarefas, mail, registros);
    }

    @Test
    void inicializacaoDepoisDasOitoRecuperaAvisosDoDia() {
        novoJob("2026-10-13T18:00:00Z").recuperarAoIniciar();
        verify(despesas).buscarContasParaNotificar(DIA, TipoLancamento.DESPESA, TipoEmailLog.CONTA_VENCIMENTO);
    }

    @Test
    void chamadasSimultaneasNaoEnviamOMesmoAvisoNaMesmaInstancia() throws Exception {
        var iniciou = new CountDownLatch(1);
        var liberar = new CountDownLatch(1);
        when(despesas.buscarContasParaNotificar(DIA, TipoLancamento.DESPESA, TipoEmailLog.CONTA_VENCIMENTO))
                .thenReturn(List.of(conta(1L, usuario(1L))));
        doAnswer(invocacao -> {
            iniciou.countDown();
            assertThat(liberar.await(5, TimeUnit.SECONDS)).isTrue();
            return null;
        }).when(mail).sendHtml(anyString(), anyString(), anyString());
        var executor = Executors.newSingleThreadExecutor();
        try {
            var execucao = executor.submit(job::run);
            assertThat(iniciou.await(5, TimeUnit.SECONDS)).isTrue();
            job.run();
            liberar.countDown();
            execucao.get(5, TimeUnit.SECONDS);
            verify(mail).sendHtml(anyString(), anyString(), anyString());
        } finally {
            liberar.countDown();
            executor.shutdownNow();
        }
    }

    private DailyNotificationJob novoJob(String instante) {
        return new DailyNotificationJob(despesas, tarefas, registros, mail,
                Clock.fixed(Instant.parse(instante), ZoneId.of("America/Cuiaba")));
    }

    private UserModel usuario(Long id) {
        var usuario = new UserModel();
        usuario.setId(id);
        usuario.setEnabled(true);
        usuario.setEmail("usuario" + id + "@example.test");
        return usuario;
    }

    private DespesaModel conta(Long id, UserModel usuario) {
        var carteira = new CarteiraDividasModel();
        carteira.setUser(usuario);
        var conta = new DespesaModel();
        conta.setId(id);
        conta.setNome("Conta " + id);
        conta.setCarteiraDividas(carteira);
        return conta;
    }
}
