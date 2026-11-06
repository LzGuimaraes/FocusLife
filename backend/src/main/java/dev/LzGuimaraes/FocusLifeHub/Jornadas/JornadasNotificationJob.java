package dev.LzGuimaraes.FocusLifeHub.Jornadas;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.*;
import org.springframework.stereotype.Component;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.util.HtmlUtils;
import dev.LzGuimaraes.FocusLifeHub.Auth.MailService;
@Component
public class JornadasNotificationJob {
    private static final Logger log=LoggerFactory.getLogger(JornadasNotificationJob.class);
    private final JdbcTemplate jdbc;
    private final MailService mail;
    private final Clock clock;
    private final String frontend;
    private final AtomicBoolean executando=new AtomicBoolean();
    public JornadasNotificationJob(JdbcTemplate jdbc,MailService mail,Clock clock,@Value("${app.frontend.url:https://focus.lzguimaraes.com.br}") String frontend) {
        this.jdbc=jdbc;this.mail=mail;this.clock=clock.withZone(ZoneId.of("America/Cuiaba"));this.frontend=frontend;
    }
    record Aviso(long id,long userId,String titulo,String email) {}
    @Scheduled(cron="${app.jornadas.notifications.cron:0 */15 * * * *}",zone="America/Cuiaba")
    public void run() {
        if(!executando.compareAndSet(false,true)) return;
        try {
            LocalDate hoje=LocalDate.now(clock);LocalTime agora=LocalTime.now(clock);
            List<Aviso> avisos=jdbc.query("""
                SELECT a.id,a.user_id,a.titulo,u.email FROM jornada_atividade a JOIN tb_user u ON u.id=a.user_id
                WHERE u.enabled=true AND a.status IN ('NAO_INICIADA','EM_ANDAMENTO')
                AND (a.inicio IS NULL OR a.inicio<=?)
                AND ((a.horario IS NULL AND a.prazo=? AND ?>=CAST('08:00' AS TIME))
                  OR (a.horario IS NOT NULL AND a.horario<=? AND (a.prazo IS NULL OR a.prazo>=?)))
                AND (NOT EXISTS(SELECT 1 FROM jornada_dia d WHERE d.atividade_id=a.id)
                  OR EXISTS(SELECT 1 FROM jornada_dia d WHERE d.atividade_id=a.id AND d.dia=?))
                AND NOT EXISTS(SELECT 1 FROM email_log l WHERE l.usuario_id=a.user_id AND l.tipo='ATIVIDADE_DIA'
                  AND l.referencia_id=a.id AND l.data_referencia=?)
                ORDER BY a.user_id,a.id
                """,(rs,n)->new Aviso(rs.getLong(1),rs.getLong(2),rs.getString(3),rs.getString(4)),hoje,hoje,agora,agora,hoje,hoje.getDayOfWeek().name(),hoje);
            Map<Long,List<Aviso>> grupos=new LinkedHashMap<>();avisos.forEach(a->grupos.computeIfAbsent(a.userId(),k->new ArrayList<>()).add(a));
            for(List<Aviso> grupo:grupos.values()) {
                try {
                    StringBuilder html=new StringBuilder("<h2>Suas atividades em Jornadas</h2><ul>");
                    for(Aviso a:grupo) html.append("<li><a href=\"").append(HtmlUtils.htmlEscape(frontend.replaceAll("/+$","")+"/jornadas/"+a.id())).append("\">").append(HtmlUtils.htmlEscape(a.titulo())).append("</a></li>");
                    mail.sendHtml(grupo.get(0).email(),"FocusLife Hub — Atividades do dia ("+hoje+")",html.append("</ul>").toString());
                    for(Aviso a:grupo) jdbc.update("INSERT INTO email_log(usuario_id,tipo,referencia_id,data_referencia,enviado_em) VALUES (?,'ATIVIDADE_DIA',?,?,?) ON CONFLICT DO NOTHING",a.userId(),a.id(),hoje,LocalDateTime.now(clock));
                } catch(Exception ex) { log.error("Falha no lembrete de Jornadas do usuário {}",grupo.get(0).userId(),ex); }
            }
        } catch(Exception ex) { log.error("Falha ao consultar lembretes de Jornadas",ex); }
        finally { executando.set(false); }
    }
}
