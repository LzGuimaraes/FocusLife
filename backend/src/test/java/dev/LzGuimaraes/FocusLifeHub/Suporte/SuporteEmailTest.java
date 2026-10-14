package dev.LzGuimaraes.FocusLifeHub.Suporte;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import jakarta.mail.MessagingException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.MailSendException;
import dev.LzGuimaraes.FocusLifeHub.Auth.MailService;

class SuporteEmailTest {
    private SuporteChamadoModel chamado() {
        var c = new SuporteChamadoModel(); c.setId(7L); c.setTipo(TipoChamado.NOVA_MUSICA); c.setTitulo("Adicionar <script>rock</script>");
        c.setDescricao("Uma música & outra\n<iframe>teste</iframe>"); c.setUsuarioNome("Maria <teste>"); c.setUsuarioEmail("maria@example.test"); c.setCriadoEm(Instant.parse("2026-10-14T02:00:00Z"));
        return c;
    }
    @Test void enviaNoDestinoInformadoComIdentificacaoEConteudoEscapado() throws Exception {
        var repo = mock(SuporteRepository.class); var mail = mock(MailService.class); var c = chamado();
        when(repo.buscarParaEnviar(7L)).thenReturn(Optional.of(c));
        new SuporteEmailService(repo, mail, "luizsantosleventi19901@gmail.com").enviar(7L);
        var html = ArgumentCaptor.forClass(String.class);
        verify(mail).sendHtml(eq("luizsantosleventi19901@gmail.com"), eq("FocusLife — SUP-000007 — Nova música"), html.capture(), eq("maria@example.test"));
        assertThat(html.getValue()).contains("SUP-000007", "Maria &lt;teste&gt;", "maria@example.test", "&lt;script&gt;", "&amp;", "<br>", "13/10/2026 22:00").doesNotContain("<script>", "<iframe>");
        assertThat(c.isEmailEnviado()).isTrue(); assertThat(c.getEmailEnviadoEm()).isNotNull(); assertThat(c.getTentativas()).isEqualTo(1);
    }
    @Test void smtpIndisponivelMantemAvisoPendenteParaNovaTentativa() throws Exception {
        var repo = mock(SuporteRepository.class); var mail = mock(MailService.class); var c = chamado();
        when(repo.buscarParaEnviar(7L)).thenReturn(Optional.of(c));
        doThrow(new MailSendException("SMTP indisponível")).when(mail).sendHtml(anyString(), anyString(), anyString(), anyString());
        var service = new SuporteEmailService(repo, mail, "suporte@example.test"); service.enviar(7L);
        assertThat(c.isEmailEnviado()).isFalse(); assertThat(c.getTentativas()).isEqualTo(1); assertThat(c.getUltimaTentativaEm()).isNotNull();
        doNothing().when(mail).sendHtml(anyString(), anyString(), anyString(), anyString()); service.enviar(7L);
        assertThat(c.isEmailEnviado()).isTrue(); assertThat(c.getTentativas()).isEqualTo(2);
    }
    @Test void erroDeFormatoOuConfiguracaoTambemFicaPendente() throws Exception {
        for (Exception erro : List.of(new MessagingException("Formato"), new IllegalStateException("Configuração"))) {
            var repo = mock(SuporteRepository.class); var mail = mock(MailService.class); var c = chamado();
            when(repo.buscarParaEnviar(7L)).thenReturn(Optional.of(c));
            doThrow(erro).when(mail).sendHtml(anyString(), anyString(), anyString(), anyString());
            new SuporteEmailService(repo, mail, "suporte@example.test").enviar(7L);
            assertThat(c.isEmailEnviado()).isFalse(); assertThat(c.getTentativas()).isEqualTo(1);
        }
    }
    @Test void avisoJaEnviadoOuChamadoRemovidoNaoEnviaNovamente() {
        var repo = mock(SuporteRepository.class); var mail = mock(MailService.class); var c = chamado(); c.setEmailEnviado(true);
        when(repo.buscarParaEnviar(7L)).thenReturn(Optional.of(c)); when(repo.buscarParaEnviar(99L)).thenReturn(Optional.empty());
        var service = new SuporteEmailService(repo, mail, "suporte@example.test"); service.enviar(7L); service.enviar(99L);
        verifyNoInteractions(mail); assertThat(c.getTentativas()).isZero();
    }
    @Test void jobContinuaMesmoSeUmChamadoFalhar() {
        var repo = mock(SuporteRepository.class); var service = mock(SuporteEmailService.class);
        when(repo.buscarPendentes(any())).thenReturn(List.of(1L, 2L)); doThrow(new IllegalStateException("DB")).when(service).enviar(1L);
        new SuporteEmailJob(repo, service).reenviarPendentes();
        verify(service).enviar(1L); verify(service).enviar(2L);
    }
}
