package dev.LzGuimaraes.FocusLifeHub.Jornadas;
import static org.assertj.core.api.Assertions.*;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.UUID;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import dev.LzGuimaraes.FocusLifeHub.Auth.MailService;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import java.time.*;
/** V40 real em PostgreSQL, sempre num schema aleatório criado e removido pelo teste. */
@EnabledIfEnvironmentVariable(named="JORNADAS_TEST_DB_URL",matches="jdbc:postgresql:.*")
class JornadasMigracaoTest {
    Connection connection; String schema;
    @BeforeEach void iniciar() throws Exception {
        connection=DriverManager.getConnection(System.getenv("JORNADAS_TEST_DB_URL"),System.getenv().getOrDefault("JORNADAS_TEST_DB_USER","jornadas_test"),System.getenv().getOrDefault("JORNADAS_TEST_DB_PASSWORD",""));
        schema="jornadas_"+UUID.randomUUID().toString().replace("-","");sql("CREATE SCHEMA "+schema);sql("SET search_path TO "+schema);
        sql("CREATE TABLE tb_user(id BIGINT PRIMARY KEY,email VARCHAR(255),enabled BOOLEAN)");
        sql("INSERT INTO tb_user VALUES (1,'maria@example.test',true),(2,'joao@example.test',true)");
        migration("V2__create_email_log.sql");migration("V6__add_dias_semana_to_materia.sql");migration("V7__create_sessao_estudo.sql");
    }
    @AfterEach void fechar() throws Exception { if(connection!=null) {sql("DROP SCHEMA "+schema+" CASCADE");connection.close();} }
    void sql(String sql) throws Exception {try(var s=connection.createStatement()){s.execute(sql);}}
    void migration(String nome) throws Exception {try(var in=getClass().getResourceAsStream("/db/migration/"+nome)){sql(new String(in.readAllBytes(),StandardCharsets.UTF_8));}}
    long numero(String query) throws Exception {try(var s=connection.createStatement();var rs=s.executeQuery(query)){rs.next();return rs.getLong(1);}}
    void legado() throws Exception {
        sql("CREATE TABLE tb_tarefas(id BIGINT PRIMARY KEY,user_id BIGINT,titulo VARCHAR(255),prazo DATE,horario TIME,prioridade VARCHAR(20),status VARCHAR(30))");
        sql("INSERT INTO tb_tarefas VALUES (10,1,'Comprar livro','2026-11-05','10:00','alta','PENDENTE'),(11,2,'Feita',null,null,null,'Concluida')");
        sql("CREATE TABLE tb_metas(id BIGINT PRIMARY KEY,user_id BIGINT,titulo VARCHAR(255),descricao VARCHAR(255),prazo DATE,prograsso REAL,status VARCHAR(30))");
        sql("INSERT INTO tb_metas VALUES (10,1,'Objetivo','Descrição da meta','2026-11-15',42.5,'Ativa'),(12,2,'Sem medida',null,null,null,'PENDENTE'),(13,1,'Percentual inválido',null,null,140,'Ativa')");
        sql("CREATE TABLE tb_materia(id BIGINT PRIMARY KEY,user_id BIGINT,nome VARCHAR(255),descricao VARCHAR(255))");
        sql("INSERT INTO tb_materia VALUES (10,1,'Curso','Descrição do curso')");
        sql("INSERT INTO materia_dias_semana VALUES (10,'MONDAY'),(10,'FRIDAY')");
        sql("INSERT INTO sessao_estudo(id,materia_id,inicio,fim,duracao_segundos) VALUES(50,10,'2026-10-20 10:00','2026-10-20 11:00',3600),(51,10,'2026-11-05 10:00',null,0)");
        sql("CREATE TABLE \"tb-estudos\"(id BIGINT PRIMARY KEY,nome VARCHAR(255),duracao_min INT,data DATE,notas VARCHAR(255),materia_id BIGINT)");
        sql("INSERT INTO \"tb-estudos\" VALUES(60,'Revisão',60,'2026-10-20','Anotações importantes',10)");
        sql("INSERT INTO email_log(usuario_id,tipo,referencia_id,data_referencia,enviado_em) VALUES (1,'TAREFA_DIA',10,'2026-11-05','2026-11-05 10:00')");
    }
    @Test void migraTresOrigensSemConfundirIdsPreservaPercentualEDedicacao() throws Exception {
        legado();migration("V40__unificar_jornadas.sql");
        assertThat(numero("SELECT count(*) FROM jornada_atividade")).isEqualTo(6);
        assertThat(numero("SELECT count(*) FROM jornada_atividade WHERE origem_id=10")).isEqualTo(3);
        assertThat(numero("SELECT count(*) FROM jornada_atividade WHERE origem='META' AND origem_id=10 AND total=100 AND saldo_inicial=42.5 AND percentual_legado=42.5 AND unidade='%'")).isEqualTo(1);
        assertThat(numero("SELECT count(*) FROM jornada_atividade WHERE origem='META' AND origem_id=13 AND total IS NULL AND percentual_legado=140")).isEqualTo(1);
        assertThat(numero("SELECT count(*) FROM jornada_atividade WHERE origem='MATERIA' AND total IS NULL AND inicio IS NULL AND prazo IS NULL AND descricao='Descrição do curso'")).isEqualTo(1);
        assertThat(numero("SELECT sum(duracao_segundos) FROM sessao_estudo WHERE atividade_id IS NOT NULL")).isEqualTo(3600);
        assertThat(numero("SELECT count(*) FROM sessao_estudo WHERE fim IS NULL AND atividade_id IS NOT NULL")).isEqualTo(1);
        assertThat(numero("SELECT count(*) FROM \"tb-estudos\" WHERE atividade_id IS NOT NULL AND notas='Anotações importantes' AND duracao_min=60")).isEqualTo(1);
        assertThat(numero("SELECT count(*) FROM jornada_dia")).isEqualTo(2);assertThat(numero("SELECT count(*) FROM jornada_progresso")).isZero();
        assertThat(numero("SELECT count(*) FROM email_log WHERE tipo='ATIVIDADE_DIA'")).isEqualTo(1);
        assertThat(numero("SELECT count(*) FROM tb_metas")).isEqualTo(3);assertThat(numero("SELECT count(*) FROM tb_tarefas")).isEqualTo(2);
    }
    @Test void instalacaoNovaEExclusaoDeUsuarioRespeitamCascatas() throws Exception {
        migration("V40__unificar_jornadas.sql");
        sql("INSERT INTO jornada_atividade(id,user_id,titulo,status) VALUES(100,1,'A','NAO_INICIADA'),(200,2,'B','NAO_INICIADA')");
        sql("INSERT INTO jornada_material(user_id,atividade_id,titulo,url) VALUES(1,100,'Recurso','https://example.test'),(2,200,'Outro','https://example.test')");
        sql("INSERT INTO jornada_progresso(atividade_id,request_id,data,modo,quantidade) VALUES(100,'00000000-0000-0000-0000-000000000001','2026-11-05','INCREMENTO',1)");
        sql("INSERT INTO sessao_estudo(atividade_id,inicio,duracao_segundos) VALUES(100,'2026-11-05 10:00',300)");
        sql("DELETE FROM tb_user WHERE id=1");
        assertThat(numero("SELECT count(*) FROM jornada_atividade")).isEqualTo(1);assertThat(numero("SELECT count(*) FROM jornada_material WHERE user_id=2")).isEqualTo(1);assertThat(numero("SELECT count(*) FROM sessao_estudo")).isZero();assertThat(numero("SELECT count(*) FROM jornada_progresso")).isZero();
    }
    @Test void lembretesRespeitamUsuarioHorarioDiasEEnvioAnterior() throws Exception {
        migration("V40__unificar_jornadas.sql");
        sql("INSERT INTO jornada_atividade(id,user_id,titulo,status,inicio,prazo,horario) VALUES "
            +"(1,1,'A <script>','EM_ANDAMENTO','2026-10-15','2026-11-05','10:00'),"
            +"(2,1,'Somente amanhã','EM_ANDAMENTO','2026-10-15','2026-11-10','18:00'),"
            +"(3,1,'Pausada','PAUSADA',null,'2026-11-05',null),"
            +"(4,2,'Outro usuário','NAO_INICIADA',null,'2026-11-05',null),"
            +"(5,1,'Sexta','EM_ANDAMENTO',null,null,'10:00')");
        sql("INSERT INTO jornada_dia VALUES(5,'FRIDAY')");
        var mail=mock(MailService.class);var jdbc=new JdbcTemplate(new SingleConnectionDataSource(connection,true));
        var clock=Clock.fixed(Instant.parse("2026-11-05T16:00:00Z"),ZoneId.of("UTC"));
        var job=new JornadasNotificationJob(jdbc,mail,clock,"https://focus.example.test");job.run();
        verify(mail).sendHtml(eq("maria@example.test"),anyString(),contains("A &lt;script&gt;"));verify(mail).sendHtml(eq("joao@example.test"),anyString(),contains("/jornadas/4"));
        assertThat(numero("SELECT count(*) FROM email_log WHERE tipo='ATIVIDADE_DIA'")).isEqualTo(2);
        job.run();verifyNoMoreInteractions(mail);
    }
    @Test void falhaSmtpNaoRegistraSucessoENaoBloqueiaOutrosUsuarios() throws Exception {
        migration("V40__unificar_jornadas.sql");
        sql("INSERT INTO jornada_atividade(id,user_id,titulo,status,prazo) VALUES(1,1,'A','EM_ANDAMENTO','2026-11-05'),(2,2,'B','EM_ANDAMENTO','2026-11-05')");
        var mail=mock(MailService.class);doThrow(new org.springframework.mail.MailSendException("SMTP")).when(mail).sendHtml(eq("maria@example.test"),anyString(),anyString());
        var job=new JornadasNotificationJob(new JdbcTemplate(new SingleConnectionDataSource(connection,true)),mail,Clock.fixed(Instant.parse("2026-11-05T16:00:00Z"),ZoneId.of("UTC")),"https://focus.example.test");job.run();
        assertThat(numero("SELECT count(*) FROM email_log WHERE usuario_id=1")).isZero();assertThat(numero("SELECT count(*) FROM email_log WHERE usuario_id=2")).isEqualTo(1);
        doNothing().when(mail).sendHtml(eq("maria@example.test"),anyString(),anyString());job.run();assertThat(numero("SELECT count(*) FROM email_log")).isEqualTo(2);verify(mail,times(1)).sendHtml(eq("joao@example.test"),anyString(),anyString());
    }
}
