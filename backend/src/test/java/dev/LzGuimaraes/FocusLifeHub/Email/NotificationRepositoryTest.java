package dev.LzGuimaraes.FocusLifeHub.Email;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.hibernate.Hibernate;
import org.hibernate.LazyInitializationException;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;

import dev.LzGuimaraes.FocusLifeHub.Carteira.CarteiraDividasModel;
import dev.LzGuimaraes.FocusLifeHub.Despesa.DespesaModel;
import dev.LzGuimaraes.FocusLifeHub.Despesa.DespesaRepository;
import dev.LzGuimaraes.FocusLifeHub.Despesa.TipoLancamento;
import dev.LzGuimaraes.FocusLifeHub.Metas.MetasModel;
import dev.LzGuimaraes.FocusLifeHub.Tarefas.Enum.TarefaStatus;
import dev.LzGuimaraes.FocusLifeHub.Tarefas.TarefasModel;
import dev.LzGuimaraes.FocusLifeHub.Tarefas.TarefasRepository;
import dev.LzGuimaraes.FocusLifeHub.User.UserModel;

/** Hibernate e os repositórios reais, em H2 isolado: nenhuma conexão à base da aplicação. */
class NotificationRepositoryTest {
    private static final LocalDate DIA = LocalDate.of(2026, 10, 13);
    private static SessionFactory banco;

    @BeforeAll
    static void iniciarBancoIsolado() {
        banco = new Configuration()
                .setProperty("hibernate.connection.driver_class", "org.h2.Driver")
                .setProperty("hibernate.connection.url", "jdbc:h2:mem:notification-test;DB_CLOSE_DELAY=-1;MODE=PostgreSQL")
                .setProperty("hibernate.connection.username", "sa")
                .setProperty("hibernate.connection.password", "")
                .setProperty("hibernate.hbm2ddl.auto", "create-drop")
                .addAnnotatedClass(UserModel.class).addAnnotatedClass(MetasModel.class)
                .addAnnotatedClass(CarteiraDividasModel.class).addAnnotatedClass(DespesaModel.class)
                .addAnnotatedClass(TarefasModel.class).addAnnotatedClass(EmailLogModel.class)
                .buildSessionFactory();
    }

    @AfterAll
    static void encerrarBanco() {
        if (banco != null) banco.close();
    }

    @BeforeEach
    void limparDados() {
        try (var em = banco.createEntityManager()) {
            em.getTransaction().begin();
            for (String entidade : List.of("EmailLogModel", "DespesaModel", "TarefasModel", "CarteiraDividasModel", "MetasModel", "UserModel")) {
                em.createQuery("delete from " + entidade).executeUpdate();
            }
            em.getTransaction().commit();
        }
    }

    @Test
    void reproduzFalhaOriginalAoAcessarDestinatariosDepoisDeFecharSessao() {
        inserirConta(false, TipoLancamento.DESPESA, DIA, true);
        inserirTarefa(TarefaStatus.Ativa, DIA, true);
        List<DespesaModel> contas;
        List<TarefasModel> tarefas;
        try (var em = banco.createEntityManager()) {
            var factory = new JpaRepositoryFactory(em);
            contas = factory.getRepository(DespesaRepository.class).findByPagoFalseAndDataVencimento(DIA);
            tarefas = factory.getRepository(TarefasRepository.class).findByPrazoAndStatusNot(DIA, TarefaStatus.Concluida);
        }
        assertThatThrownBy(() -> contas.get(0).getCarteiraDividas().getUser().getEmail())
                .isInstanceOf(LazyInitializationException.class);
        assertThatThrownBy(() -> tarefas.get(0).getUser().getEmail()).isInstanceOf(LazyInitializationException.class);
    }

    @Test
    void novasConsultasMantemOsDestinatariosAcessiveisSemSessaoAberta() {
        inserirConta(false, TipoLancamento.DESPESA, DIA, true);
        inserirTarefa(TarefaStatus.PENDENTE, DIA, true);
        var contas = contas();
        var tarefas = tarefas();
        assertThat(contas).hasSize(1);
        assertThat(Hibernate.isInitialized(contas.get(0).getCarteiraDividas())).isTrue();
        assertThat(contas.get(0).getCarteiraDividas().getUser().getEmail()).isEqualTo("contas@example.test");
        assertThat(tarefas).hasSize(1);
        assertThat(tarefas.get(0).getUser().getEmail()).isEqualTo("tarefas@example.test");
    }

    @Test
    void selecionaSomenteDespesasNaoPagasDoDiaDeUsuariosAtivos() {
        var esperada = inserirConta(false, TipoLancamento.DESPESA, DIA, true);
        var legado = inserirConta(null, TipoLancamento.DESPESA, DIA, true);
        inserirConta(true, TipoLancamento.DESPESA, DIA, true);
        inserirConta(false, TipoLancamento.SALDO, DIA, true);
        inserirConta(false, TipoLancamento.DESPESA, DIA.plusDays(1), true);
        inserirConta(false, TipoLancamento.DESPESA, DIA.minusDays(1), true);
        inserirConta(false, TipoLancamento.DESPESA, DIA, false);
        assertThat(contas()).extracting(DespesaModel::getId).containsExactlyInAnyOrder(esperada.getId(), legado.getId());
    }

    @Test
    void naoNotificaTarefasCanceladasConcluidasOuDeUsuarioDesativado() {
        var ativa = inserirTarefa(TarefaStatus.Ativa, DIA, true);
        var pendente = inserirTarefa(TarefaStatus.PENDENTE, DIA, true);
        inserirTarefa(TarefaStatus.Cancelada, DIA, true);
        inserirTarefa(TarefaStatus.Concluida, DIA, true);
        inserirTarefa(TarefaStatus.Ativa, DIA, false);
        inserirTarefa(TarefaStatus.Ativa, DIA.plusDays(1), true);
        assertThat(tarefas()).extracting(TarefasModel::getId).containsExactlyInAnyOrder(ativa.getId(), pendente.getId());
    }

    @Test
    void novasTentativasExcluemSucessosRegistradosParaMesmoUsuarioTipoEData() {
        var conta = inserirConta(false, TipoLancamento.DESPESA, DIA, true);
        var tarefa = inserirTarefa(TarefaStatus.Ativa, DIA, true);
        assertThat(contas()).hasSize(1);
        assertThat(tarefas()).hasSize(1);
        registrar(TipoEmailLog.CONTA_VENCIMENTO, conta.getId(), conta.getCarteiraDividas().getUser().getId(), DIA);
        registrar(TipoEmailLog.TAREFA_DIA, tarefa.getId(), tarefa.getUser().getId(), DIA);
        assertThat(contas()).isEmpty();
        assertThat(tarefas()).isEmpty();
    }

    @Test
    void registroDeOutraDataOuDestinatarioNaoBloqueiaAvisoAtual() {
        var conta = inserirConta(false, TipoLancamento.DESPESA, DIA, true);
        registrar(TipoEmailLog.CONTA_VENCIMENTO, conta.getId(), conta.getCarteiraDividas().getUser().getId(), DIA.minusDays(1));
        registrar(TipoEmailLog.CONTA_VENCIMENTO, conta.getId(), 9999L, DIA);
        assertThat(contas()).hasSize(1);
    }

    private List<DespesaModel> contas() {
        try (var em = banco.createEntityManager()) {
            return new JpaRepositoryFactory(em).getRepository(DespesaRepository.class)
                    .buscarContasParaNotificar(DIA, TipoLancamento.DESPESA, TipoEmailLog.CONTA_VENCIMENTO);
        }
    }

    private List<TarefasModel> tarefas() {
        try (var em = banco.createEntityManager()) {
            return new JpaRepositoryFactory(em).getRepository(TarefasRepository.class)
                    .buscarTarefasParaNotificar(DIA, List.of(TarefaStatus.Ativa, TarefaStatus.PENDENTE), TipoEmailLog.TAREFA_DIA);
        }
    }

    private DespesaModel inserirConta(Boolean paga, TipoLancamento tipo, LocalDate data, boolean habilitado) {
        try (var em = banco.createEntityManager()) {
            em.getTransaction().begin();
            var usuario = usuario("contas@example.test", habilitado);
            em.persist(usuario);
            var carteira = new CarteiraDividasModel();
            carteira.setUser(usuario);
            em.persist(carteira);
            var conta = new DespesaModel();
            conta.setNome("Conta de teste");
            conta.setPago(paga);
            conta.setTipo(tipo);
            conta.setDataVencimento(data);
            conta.setCarteiraDividas(carteira);
            em.persist(conta);
            em.getTransaction().commit();
            return conta;
        }
    }

    private TarefasModel inserirTarefa(TarefaStatus status, LocalDate data, boolean habilitado) {
        try (var em = banco.createEntityManager()) {
            em.getTransaction().begin();
            var usuario = usuario("tarefas@example.test", habilitado);
            em.persist(usuario);
            var tarefa = new TarefasModel();
            tarefa.setTitulo("Tarefa de teste");
            tarefa.setPrazo(data);
            tarefa.setStatus(status);
            tarefa.setUser(usuario);
            em.persist(tarefa);
            em.getTransaction().commit();
            return tarefa;
        }
    }

    private UserModel usuario(String email, boolean habilitado) {
        var usuario = new UserModel();
        usuario.setEmail(email);
        usuario.setEnabled(habilitado);
        return usuario;
    }

    private void registrar(TipoEmailLog tipo, Long referencia, Long usuario, LocalDate data) {
        try (var em = banco.createEntityManager()) {
            em.getTransaction().begin();
            var registro = new EmailLogModel();
            registro.setUsuario_id(usuario);
            registro.setTipo(tipo);
            registro.setReferencia_id(referencia);
            registro.setData_referencia(data);
            registro.setEnviado_em(LocalDateTime.of(data, java.time.LocalTime.of(8, 0)));
            em.persist(registro);
            em.getTransaction().commit();
        }
    }
}
