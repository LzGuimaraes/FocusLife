package dev.LzGuimaraes.FocusLifeHub.Suporte;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import java.nio.charset.StandardCharsets;
import java.sql.DriverManager;
import java.util.UUID;
import java.util.function.BiFunction;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.junit.jupiter.api.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import org.springframework.mail.MailSendException;
import dev.LzGuimaraes.FocusLifeHub.Auth.MailService;
import dev.LzGuimaraes.FocusLifeHub.Exceptions.ResourceNotFoundException;
import dev.LzGuimaraes.FocusLifeHub.Metas.MetasModel;
import dev.LzGuimaraes.FocusLifeHub.User.*;

/** Entidades/repositórios reais e V39 em H2; envio SMTP sempre simulado. */
class SuportePersistenciaTest {
    private static final String URL = "jdbc:h2:mem:suporte-test;DB_CLOSE_DELAY=-1;MODE=PostgreSQL";
    private static SessionFactory banco;
    @BeforeAll static void iniciar() throws Exception {
        try (var base = configuracao("create").addAnnotatedClass(UserModel.class).addAnnotatedClass(MetasModel.class).buildSessionFactory()) {}
        try (var connection = DriverManager.getConnection(URL, "sa", ""); var statement = connection.createStatement();
             var resource = SuportePersistenciaTest.class.getResourceAsStream("/db/migration/V39__suporte_chamados.sql")) {
            for (String sql : new String(resource.readAllBytes(), StandardCharsets.UTF_8).split(";")) if (!sql.isBlank()) statement.execute(sql);
        }
        banco = configuracao("validate").addAnnotatedClass(UserModel.class).addAnnotatedClass(MetasModel.class).addAnnotatedClass(SuporteChamadoModel.class).buildSessionFactory();
    }
    private static Configuration configuracao(String ddl) {
        return new Configuration().setProperty("hibernate.connection.driver_class", "org.h2.Driver").setProperty("hibernate.connection.url", URL)
                .setProperty("hibernate.connection.username", "sa").setProperty("hibernate.connection.password", "").setProperty("hibernate.hbm2ddl.auto", ddl);
    }
    @AfterAll static void fechar() { if (banco != null) banco.close(); }
    @BeforeEach void limpar() {
        try (var em = banco.createEntityManager()) { em.getTransaction().begin(); em.createQuery("delete from SuporteChamadoModel").executeUpdate(); em.createQuery("delete from UserModel").executeUpdate(); em.getTransaction().commit(); }
    }
    private <T> T executar(BiFunction<SuporteService, SuporteRepository, T> acao) {
        try (var em = banco.createEntityManager()) {
            em.getTransaction().begin();
            try {
                var factory = new JpaRepositoryFactory(em); var repo = factory.getRepository(SuporteRepository.class);
                T resultado = acao.apply(new SuporteService(repo, factory.getRepository(UserRepository.class)), repo); em.getTransaction().commit(); return resultado;
            } catch (RuntimeException ex) { em.getTransaction().rollback(); throw ex; }
        }
    }
    private Long usuario(String nome) {
        try (var em = banco.createEntityManager()) { em.getTransaction().begin(); var u = new UserModel(); u.setNome(nome); u.setEmail(nome.toLowerCase()+"@example.test"); u.setEnabled(true); em.persist(u); em.getTransaction().commit(); return u.getId(); }
    }
    private SuporteDTO.Request pedido(UUID id) { return new SuporteDTO.Request(id, TipoChamado.NOVO_ATIVO, " Adicionar ativo ", " Cadastrar TEST3 na categoria Ações. "); }
    @Test void salvaChamadoComIdentidadeDaContaEProtocolo() {
        Long user = usuario("Maria"); var result = executar((s,r) -> s.abrir(user, pedido(UUID.randomUUID())));
        assertThat(result.protocolo()).startsWith("SUP-"); assertThat(result.titulo()).isEqualTo("Adicionar ativo"); assertThat(result.emailEnviado()).isFalse();
        executar((s,r) -> { var c = r.findById(result.id()).orElseThrow(); assertThat(c.getUsuarioNome()).isEqualTo("Maria"); assertThat(c.getUsuarioEmail()).isEqualTo("maria@example.test"); return null; });
        assertThat(executar((s,r) -> s.buscarMeu(user, result.id())).descricao()).contains("TEST3");
    }
    @Test void repeticaoDaMesmaSolicitacaoNaoCriaOutroChamado() {
        Long user = usuario("Maria"); var request = pedido(UUID.randomUUID());
        var first = executar((s,r) -> s.abrir(user, request)); var second = executar((s,r) -> s.abrir(user, request));
        assertThat(second.id()).isEqualTo(first.id()); assertThat(executar((s,r) -> r.count()).longValue()).isEqualTo(1L);
    }
    @Test void historicoEDetalheNuncaExpoemChamadoDeOutroUsuario() {
        Long maria = usuario("Maria"); Long joao = usuario("Joao"); var uuid = UUID.randomUUID();
        var first = executar((s,r) -> s.abrir(maria, pedido(uuid))); var second = executar((s,r) -> s.abrir(joao, pedido(uuid)));
        assertThat(first.id()).isNotEqualTo(second.id());
        assertThat(executar((s,r) -> s.listarMeus(maria,0)).getContent()).extracting(SuporteDTO.Response::id).containsExactly(first.id());
        assertThatThrownBy(() -> executar((s,r) -> s.buscarMeu(maria, second.id()))).isInstanceOf(ResourceNotFoundException.class);
    }
    @Test void smtpFalhaNaoApagaPedidoEReenvioAtualizaEstadoNoBanco() throws Exception {
        Long user = usuario("Maria"); var result = executar((s,r) -> s.abrir(user, pedido(UUID.randomUUID())));
        var mail = mock(MailService.class); doThrow(new MailSendException("SMTP")).when(mail).sendHtml(anyString(), anyString(), anyString(), anyString());
        executar((s,r) -> { new SuporteEmailService(r,mail,"suporte@example.test").enviar(result.id()); return null; });
        assertThat(executar((s,r) -> s.buscarMeu(user,result.id())).emailEnviado()).isFalse();
        assertThat(executar((s,r) -> r.buscarPendentes(PageRequest.of(0,20))).contains(result.id())).isTrue();
        doNothing().when(mail).sendHtml(anyString(), anyString(), anyString(), anyString());
        executar((s,r) -> { new SuporteEmailService(r,mail,"suporte@example.test").enviar(result.id()); return null; });
        assertThat(executar((s,r) -> s.buscarMeu(user,result.id())).emailEnviado()).isTrue();
        assertThat(executar((s,r) -> r.buscarPendentes(PageRequest.of(0,20))).isEmpty()).isTrue();
        executar((s,r) -> { new SuporteEmailService(r,mail,"suporte@example.test").enviar(result.id()); return null; });
        verify(mail,times(2)).sendHtml(anyString(), anyString(), anyString(), anyString());
    }
    @Test void historicoTemPaginacaoDeOitoPedidos() {
        Long user = usuario("Maria"); for (int i=0;i<9;i++) executar((s,r) -> s.abrir(user,pedido(UUID.randomUUID())));
        var page = executar((s,r) -> s.listarMeus(user,0)); assertThat(page.getContent()).hasSize(8); assertThat(page.getTotalPages()).isEqualTo(2);
        assertThat(executar((s,r) -> s.listarMeus(user,1)).getContent()).hasSize(1);
    }
}
