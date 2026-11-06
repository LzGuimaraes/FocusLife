package dev.LzGuimaraes.FocusLifeHub.Jornadas;
import static org.assertj.core.api.Assertions.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.function.Function;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.junit.jupiter.api.*;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import dev.LzGuimaraes.FocusLifeHub.User.*;
import dev.LzGuimaraes.FocusLifeHub.Materia.*;
import dev.LzGuimaraes.FocusLifeHub.Metas.MetasModel;
import dev.LzGuimaraes.FocusLifeHub.Estudos.*;
import dev.LzGuimaraes.FocusLifeHub.Exceptions.*;
import org.springframework.web.server.ResponseStatusException;
import static dev.LzGuimaraes.FocusLifeHub.Jornadas.JornadasDTO.*;
class JornadasPersistenciaTest {
    static SessionFactory banco;
    static Clock clock=Clock.fixed(Instant.parse("2026-11-06T02:00:00Z"),ZoneId.of("UTC")); // ainda 05/11 em Cuiabá
    Long maria,joao;
    @BeforeAll static void abrir() {
        banco=new Configuration().setProperty("hibernate.connection.url","jdbc:h2:mem:jornadas;DB_CLOSE_DELAY=-1;MODE=PostgreSQL")
            .setProperty("hibernate.physical_naming_strategy","org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy")
            .setProperty("hibernate.connection.driver_class","org.h2.Driver").setProperty("hibernate.hbm2ddl.auto","create-drop")
            .addAnnotatedClass(UserModel.class).addAnnotatedClass(MetasModel.class).addAnnotatedClass(MateriaModel.class)
            .addAnnotatedClass(EstudosModel.class).addAnnotatedClass(SessaoEstudo.class).addAnnotatedClass(Atividade.class)
            .addAnnotatedClass(Progresso.class).addAnnotatedClass(Material.class).buildSessionFactory();
    }
    @AfterAll static void fechar() { banco.close(); }
    @BeforeEach void usuarios() { maria=usuario();joao=usuario(); }
    Long usuario() { try(var em=banco.createEntityManager()) { em.getTransaction().begin();var u=new UserModel();u.setNome("Teste");em.persist(u);em.getTransaction().commit();return u.getId(); } }
    <T> T executar(Function<JornadasService,T> acao) {
        try(var em=banco.createEntityManager()) {
            em.getTransaction().begin();
            try {
                var f=new JpaRepositoryFactory(em);
                var s=new JornadasService(f.getRepository(AtividadeRepository.class),f.getRepository(ProgressoRepository.class),f.getRepository(MaterialRepository.class),f.getRepository(SessaoEstudoRepository.class),f.getRepository(EstudosRepository.class),f.getRepository(UserRepository.class),new CalculadoraJornada(),clock);
                T result=acao.apply(s);em.getTransaction().commit();return result;
            } catch(RuntimeException e) {em.getTransaction().rollback();throw e;}
        }
    }
    Pedido pedido(String unidade,String total,String saldo,LocalDate ref,Long version,StatusAtividade status) {
        return new Pedido(version,"Minha atividade","Descrição",LocalDate.of(2026,10,15),LocalDate.of(2026,11,15),unidade,"horas".equals(unidade),new BigDecimal(total),new BigDecimal(saldo),ref,status,"media",null,Set.of(),"Curso");
    }
    Resposta criar(String unidade,String total) { return executar(s->s.salvar(maria,null,pedido(unidade,total,"0",null,null,StatusAtividade.NAO_INICIADA))); }
    Lancamento lancamento(Resposta a,String data,String qtd,Progresso.Modo modo,UUID uuid) {return new Lancamento(a.version(),uuid,LocalDate.parse(data),modo,new BigDecimal(qtd),"Observação",4);}
    Resposta lancar(Resposta a,String data,String qtd,Progresso.Modo modo) {return executar(s->s.lancar(maria,a.id(),null,lancamento(a,data,qtd,modo,UUID.randomUUID())));}
    @Test void livroPorPaginaAtualRetroativosCorrecaoEExclusao() {
        var a=criar("páginas","350");a=lancar(a,"2026-10-20","100",Progresso.Modo.ACUMULADO);a=lancar(a,"2026-11-01","120",Progresso.Modo.ACUMULADO);a=lancar(a,"2026-10-25","5",Progresso.Modo.INCREMENTO);
        assertThat(a.indicadores().realizado()).isEqualTo(120);assertThat(a.historico()).hasSize(3);
        final var antes=a;long primeiro=a.historico().get(0).getId();
        a=executar(s->s.lancar(maria,antes.id(),primeiro,lancamento(antes,"2026-10-20","90",Progresso.Modo.ACUMULADO,UUID.randomUUID())));
        assertThat(a.indicadores().realizado()).isEqualTo(120);
        final var corrigida=a;long ultimo=a.historico().get(2).getId();
        a=executar(s->s.excluirProgresso(maria,corrigida.id(),ultimo,corrigida.version()));assertThat(a.indicadores().realizado()).isEqualTo(95);
        assertThat(executar(s->s.buscar(maria,corrigida.id())).indicadores().realizado()).isEqualTo(95);
    }
    @Test void incrementosIdempotenciaVersaoERejeicaoDeRetroativoInvalidoRevertemTudo() {
        var a=criar("exercícios","200");UUID uuid=UUID.randomUUID();var p=lancamento(a,"2026-11-01","5",Progresso.Modo.INCREMENTO,uuid);
        var primeira=executar(s->s.lancar(maria,a.id(),null,p));var repetida=executar(s->s.lancar(maria,a.id(),null,p));
        assertThat(repetida.indicadores().realizado()).isEqualTo(5);assertThat(repetida.historico()).hasSize(1);
        assertThatThrownBy(()->executar(s->s.lancar(maria,a.id(),null,lancamento(a,"2026-11-02","5",Progresso.Modo.INCREMENTO,UUID.randomUUID())))).isInstanceOf(ResponseStatusException.class);
        var acumulada=lancar(primeira,"2026-11-03","10",Progresso.Modo.ACUMULADO);
        assertThatThrownBy(()->lancar(acumulada,"2026-11-02","20",Progresso.Modo.INCREMENTO)).isInstanceOf(BusinessRuleException.class);
        var depois=executar(s->s.buscar(maria,a.id()));assertThat(depois.historico()).hasSize(2);assertThat(depois.indicadores().realizado()).isEqualTo(10);
    }
    @Test void atividadeSimplesConcluiEExclusaoReabre() {
        var a=lancar(criar("unidades","1"),"2026-11-05","1",Progresso.Modo.ACUMULADO);assertThat(a.status()).isEqualTo(StatusAtividade.CONCLUIDA);assertThat(a.referencia()).isEqualTo(LocalDate.of(2026,11,5));
        var reaberta=executar(s->s.excluirProgresso(maria,a.id(),a.historico().get(0).getId(),a.version()));assertThat(reaberta.status()).isEqualTo(StatusAtividade.NAO_INICIADA);assertThat(reaberta.indicadores().realizado()).isZero();
    }
    @Test void saldoInicialSemHistoricoEFracoes() {
        var a=executar(s->s.salvar(maria,null,pedido("horas","40","12.5",LocalDate.of(2026,11,4),null,StatusAtividade.EM_ANDAMENTO)));
        assertThat(a.indicadores().ritmoObservado()).isNull();assertThat(a.historico()).isEmpty();
        a=lancar(a,"2026-11-05","1.25",Progresso.Modo.INCREMENTO);assertThat(a.indicadores().realizado()).isEqualTo(13.75);assertThat(a.indicadores().ritmoObservado()).isEqualTo(1.25);
    }
    @Test void linksPersistemEditamExcluemEIsolamUsuario() {
        var a=criar("unidades","1");
        var geral=executar(s->s.salvarLink(maria,null,new LinkPedido(null,"Documentação","https://example.test/doc",null,null)));
        var vinculado=executar(s->s.salvarLink(maria,null,new LinkPedido(null,"Livro","http://example.test/livro","Capítulo 1",a.id())));
        assertThat(this.<List<Material>>executar(s->s.links(maria))).hasSize(2);assertThat(this.<List<Material>>executar(s->s.links(joao))).isEmpty();assertThat(executar(s->s.buscar(maria,a.id())).materiais()).hasSize(1);
        assertThatThrownBy(()->executar(s->s.salvarLink(joao,null,new LinkPedido(null,"Invasão","https://example.test",null,a.id())))).isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(()->executar(s->s.salvarLink(joao,vinculado.getId(),new LinkPedido(vinculado.getVersion(),"X","https://example.test",null,null)))).isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(()->executar(s->{s.excluirLink(joao,geral.getId(),geral.getVersion());return null;})).isInstanceOf(ResourceNotFoundException.class);
        var editado=executar(s->s.salvarLink(maria,geral.getId(),new LinkPedido(geral.getVersion(),"Novo título","https://example.test/novo",null,null)));
        assertThat(editado.getTitulo()).isEqualTo("Novo título");executar(s->{s.excluirLink(maria,editado.getId(),editado.getVersion());return null;});assertThat(this.<List<Material>>executar(s->s.links(maria))).hasSize(1);
        executar(s->{s.excluir(maria,a.id(),a.version());return null;});assertThat(executar(s->s.links(maria)).get(0).getAtividadeId()).isNull();
    }
    @Test void recusaUrlsInvalidasEProgressoDeOutroUsuario() {
        var a=criar("exercícios","200");var atual=lancar(a,"2026-11-01","5",Progresso.Modo.INCREMENTO);long progresso=atual.historico().get(0).getId();
        for(String url:List.of("javascript:alert(1)","file:///etc/passwd","https://","https://user:pass@example.test","ftp://example.test")) assertThatThrownBy(()->executar(s->s.salvarLink(maria,null,new LinkPedido(null,"X",url,null,null)))).isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(()->executar(s->s.buscar(joao,a.id()))).isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(()->executar(s->s.excluirProgresso(joao,a.id(),progresso,atual.version()))).isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(()->executar(s->s.lancar(joao,a.id(),progresso,lancamento(atual,"2026-11-01","10",Progresso.Modo.INCREMENTO,UUID.randomUUID())))).isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(()->executar(s->{s.excluir(joao,a.id(),atual.version());return null;})).isInstanceOf(ResourceNotFoundException.class);
    }
    @Test void sessoesRecuperamAbertasEAnotacoesNaoGeramProgresso() {
        var a=criar("aulas","30");var aberta=executar(s->s.iniciar(maria,a.id()));var mesma=executar(s->s.iniciar(maria,a.id()));assertThat(aberta.sessoes().get(0).inicio().getOffset()).isEqualTo(ZoneOffset.ofHours(-4));assertThat(mesma.sessoes()).hasSize(1);assertThat(mesma.sessoes().get(0).id()).isEqualTo(aberta.sessoes().get(0).id());
        assertThatThrownBy(()->executar(s->s.pausar(joao,a.id(),aberta.sessoes().get(0).id()))).isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(()->executar(s->{s.excluir(maria,a.id(),a.version());return null;})).isInstanceOf(BusinessRuleException.class);
        var pausada=executar(s->s.pausar(maria,a.id(),aberta.sessoes().get(0).id()));assertThat(pausada.sessoes().get(0).fim()).isNotNull();
        var anotada=executar(s->s.anotar(maria,a.id(),null,new EstudoPedido("Revisão",LocalDate.of(2026,11,5),60,"Anotações preservadas")));
        assertThat(anotada.estudos()).hasSize(1);assertThat(anotada.indicadores().realizado()).isZero();
        var editada=executar(s->s.anotar(maria,a.id(),anotada.estudos().get(0).id(),new EstudoPedido("Revisão editada",LocalDate.of(2026,11,5),90,"Novas notas")));
        assertThat(editada.estudos().get(0).duracaoMin()).isEqualTo(90);
        var excluida=executar(s->s.excluirEstudo(maria,a.id(),editada.estudos().get(0).id()));assertThat(excluida.estudos()).isEmpty();
    }
    @Test void validaDatasQuantidadesECoerenciaDeConclusao() {
        var a=criar("unidades","1");
        assertThatThrownBy(()->lancar(a,"2026-11-06","1",Progresso.Modo.INCREMENTO)).isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(()->lancar(a,"2026-10-14","1",Progresso.Modo.INCREMENTO)).isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(()->lancar(a,"2026-11-01","2",Progresso.Modo.INCREMENTO)).isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(()->lancar(a,"2026-11-01","0.5",Progresso.Modo.INCREMENTO)).isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(()->executar(s->s.salvar(maria,a.id(),pedido("unidades","1","0",null,a.version(),StatusAtividade.CONCLUIDA)))).isInstanceOf(BusinessRuleException.class);
        assertThat(executar(s->s.buscar(maria,a.id())).historico()).isEmpty();
    }
    @Test void escritasConcorrentesNaoPerdemProgresso() throws Exception {
        var a=criar("exercícios","200");
        var pool=java.util.concurrent.Executors.newFixedThreadPool(2);
        var inicio=new java.util.concurrent.CountDownLatch(1);
        var sucessos=new java.util.concurrent.atomic.AtomicInteger();
        var conflitos=new java.util.concurrent.atomic.AtomicInteger();
        try {
            java.util.concurrent.Callable<Void> acao=()->{
                inicio.await();
                try { lancar(a,"2026-11-05","5",Progresso.Modo.INCREMENTO);sucessos.incrementAndGet(); }
                catch(ResponseStatusException ex) { assertThat(ex.getStatusCode().value()).isEqualTo(409);conflitos.incrementAndGet(); }
                return null;
            };
            var primeira=pool.submit(acao);var segunda=pool.submit(acao);inicio.countDown();
            primeira.get(10,java.util.concurrent.TimeUnit.SECONDS);segunda.get(10,java.util.concurrent.TimeUnit.SECONDS);
            assertThat(sucessos.get()).isEqualTo(1);assertThat(conflitos.get()).isEqualTo(1);
            var recarregada=executar(service->service.buscar(maria,a.id()));
            assertThat(recarregada.indicadores().realizado()).isEqualTo(5);
            var repetida=lancar(recarregada,"2026-11-05","5",Progresso.Modo.INCREMENTO);
            assertThat(repetida.indicadores().realizado()).isEqualTo(10);assertThat(repetida.historico()).hasSize(2);
        } finally {pool.shutdownNow();}
    }

}
