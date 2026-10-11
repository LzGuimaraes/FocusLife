package dev.LzGuimaraes.FocusLifeHub.Admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import dev.LzGuimaraes.FocusLifeHub.Admin.dto.AdminUserDetailDTO;
import dev.LzGuimaraes.FocusLifeHub.Ativo.AtivoRepository;
import dev.LzGuimaraes.FocusLifeHub.Carteira.CarteiraDividasRepository;
import dev.LzGuimaraes.FocusLifeHub.Carteira.CarteiraInvestimentoRepository;
import dev.LzGuimaraes.FocusLifeHub.Despesa.DespesaRepository;
import dev.LzGuimaraes.FocusLifeHub.Estudos.EstudosRepository;
import dev.LzGuimaraes.FocusLifeHub.Exceptions.ResourceNotFoundException;
import dev.LzGuimaraes.FocusLifeHub.Investimento.ComprasVendas.AtivoPrecoAlvoRepository;
import dev.LzGuimaraes.FocusLifeHub.Investimento.ComprasVendas.OperacaoCompraVendaRepository;
import dev.LzGuimaraes.FocusLifeHub.Materia.MateriaRepository;
import dev.LzGuimaraes.FocusLifeHub.Materia.SessaoEstudoRepository;
import dev.LzGuimaraes.FocusLifeHub.Metas.MetasRepository;
import dev.LzGuimaraes.FocusLifeHub.Planejamento.Aporte.AporteConfigRepository;
import dev.LzGuimaraes.FocusLifeHub.Planejamento.Avaliacao.ChecklistAtivoRepository;
import dev.LzGuimaraes.FocusLifeHub.Planejamento.Avaliacao.ChecklistModeloRepository;
import dev.LzGuimaraes.FocusLifeHub.Planejamento.Estrategia.EstrategiaRepository;
import dev.LzGuimaraes.FocusLifeHub.Planejamento.Historico.AporteRegistroRepository;
import dev.LzGuimaraes.FocusLifeHub.Planejamento.Historico.AtivoScoreHistoricoRepository;
import dev.LzGuimaraes.FocusLifeHub.Planejamento.MetaAtivo.MetaAtivoRepository;
import dev.LzGuimaraes.FocusLifeHub.Tarefas.TarefasRepository;
import dev.LzGuimaraes.FocusLifeHub.Treino.WorkoutRepository;
import dev.LzGuimaraes.FocusLifeHub.Treino.WorkoutSessionRepository;
import dev.LzGuimaraes.FocusLifeHub.User.Role;
import dev.LzGuimaraes.FocusLifeHub.User.UserModel;
import dev.LzGuimaraes.FocusLifeHub.User.UserRepository;

/**
 * Montagem do resumo administrativo, sem banco: os repositórios são mocks.
 *
 * Garante que (a) cada contagem do resumo vem da relação CORRETA com o usuário e
 * (b) o detalhe NUNCA carrega credenciais/segredos do {@code UserModel}.
 */
class AdminUserDetailServiceTest {

    private UserRepository userRepository;
    private CarteiraInvestimentoRepository carteiraInvestimentoRepository;
    private CarteiraDividasRepository carteiraDividasRepository;
    private AtivoRepository ativoRepository;
    private DespesaRepository despesaRepository;
    private OperacaoCompraVendaRepository operacaoCompraVendaRepository;
    private AtivoPrecoAlvoRepository ativoPrecoAlvoRepository;
    private EstrategiaRepository estrategiaRepository;
    private MetaAtivoRepository metaAtivoRepository;
    private ChecklistModeloRepository checklistModeloRepository;
    private ChecklistAtivoRepository checklistAtivoRepository;
    private AporteConfigRepository aporteConfigRepository;
    private AporteRegistroRepository aporteRegistroRepository;
    private AtivoScoreHistoricoRepository ativoScoreHistoricoRepository;
    private MateriaRepository materiaRepository;
    private EstudosRepository estudosRepository;
    private SessaoEstudoRepository sessaoEstudoRepository;
    private TarefasRepository tarefasRepository;
    private WorkoutRepository workoutRepository;
    private WorkoutSessionRepository workoutSessionRepository;

    private AdminUserDetailService service;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        carteiraInvestimentoRepository = mock(CarteiraInvestimentoRepository.class);
        carteiraDividasRepository = mock(CarteiraDividasRepository.class);
        ativoRepository = mock(AtivoRepository.class);
        despesaRepository = mock(DespesaRepository.class);
        operacaoCompraVendaRepository = mock(OperacaoCompraVendaRepository.class);
        ativoPrecoAlvoRepository = mock(AtivoPrecoAlvoRepository.class);
        estrategiaRepository = mock(EstrategiaRepository.class);
        metaAtivoRepository = mock(MetaAtivoRepository.class);
        checklistModeloRepository = mock(ChecklistModeloRepository.class);
        checklistAtivoRepository = mock(ChecklistAtivoRepository.class);
        aporteConfigRepository = mock(AporteConfigRepository.class);
        aporteRegistroRepository = mock(AporteRegistroRepository.class);
        ativoScoreHistoricoRepository = mock(AtivoScoreHistoricoRepository.class);
        materiaRepository = mock(MateriaRepository.class);
        estudosRepository = mock(EstudosRepository.class);
        sessaoEstudoRepository = mock(SessaoEstudoRepository.class);
        tarefasRepository = mock(TarefasRepository.class);
        workoutRepository = mock(WorkoutRepository.class);
        workoutSessionRepository = mock(WorkoutSessionRepository.class);

        service = new AdminUserDetailService(
                userRepository,
                carteiraInvestimentoRepository,
                carteiraDividasRepository,
                ativoRepository,
                despesaRepository,
                operacaoCompraVendaRepository,
                ativoPrecoAlvoRepository,
                estrategiaRepository,
                metaAtivoRepository,
                checklistModeloRepository,
                checklistAtivoRepository,
                aporteConfigRepository,
                aporteRegistroRepository,
                ativoScoreHistoricoRepository,
                materiaRepository,
                estudosRepository,
                sessaoEstudoRepository,
                tarefasRepository,
                workoutRepository,
                workoutSessionRepository);
    }

    private UserModel usuario(Long id) {
        UserModel u = new UserModel();
        u.setId(id);
        u.setNome("Maria");
        u.setEmail("maria@focuslife.test");
        u.setRole(Role.USER);
        u.setEnabled(true);
        return u;
    }

    @Test
    @DisplayName("monta o resumo com a contagem de cada relacionamento confirmado")
    void montaResumoComContagensDeCadaModulo() {
        Long id = 9L;
        when(userRepository.findById(id)).thenReturn(Optional.of(usuario(id)));

        when(carteiraInvestimentoRepository.countByUserId(id)).thenReturn(2L);
        when(carteiraDividasRepository.countByUserId(id)).thenReturn(1L);
        when(ativoRepository.countByCarteiraInvestimento_UserId(id)).thenReturn(15L);
        when(despesaRepository.countByCarteiraDividas_UserId(id)).thenReturn(8L);
        when(operacaoCompraVendaRepository.countByUserId(id)).thenReturn(6L);
        when(ativoPrecoAlvoRepository.countByUserId(id)).thenReturn(4L);

        when(estrategiaRepository.countByUserId(id)).thenReturn(3L);
        when(metaAtivoRepository.countByCarteiraInvestimento_UserId(id)).thenReturn(5L);
        when(checklistModeloRepository.countByUserId(id)).thenReturn(2L);
        when(checklistAtivoRepository.countByUserId(id)).thenReturn(1L);
        when(aporteConfigRepository.existsByUserId(id)).thenReturn(true);
        when(aporteRegistroRepository.countByUserId(id)).thenReturn(10L);
        when(ativoScoreHistoricoRepository.countByUserId(id)).thenReturn(7L);

        when(materiaRepository.countByUserId(id)).thenReturn(4L);
        when(estudosRepository.countByMateria_UserId(id)).thenReturn(20L);
        when(sessaoEstudoRepository.countDoUsuario(id)).thenReturn(30L);

        when(tarefasRepository.countByUserId(id)).thenReturn(12L);
        when(workoutRepository.countByUserId(id)).thenReturn(5L);
        when(workoutSessionRepository.countByUserId(id)).thenReturn(40L);

        AdminUserDetailDTO dto = service.getUserDetail(id);

        assertThat(dto.id()).isEqualTo(9L);
        assertThat(dto.nome()).isEqualTo("Maria");
        assertThat(dto.email()).isEqualTo("maria@focuslife.test");
        assertThat(dto.role()).isEqualTo("USER");
        assertThat(dto.ativo()).isTrue();

        assertThat(dto.resumo().financas().carteirasInvestimento()).isEqualTo(2L);
        assertThat(dto.resumo().financas().carteirasDividas()).isEqualTo(1L);
        assertThat(dto.resumo().financas().ativos()).isEqualTo(15L);
        assertThat(dto.resumo().financas().despesas()).isEqualTo(8L);
        assertThat(dto.resumo().financas().operacoesCompraVenda()).isEqualTo(6L);
        assertThat(dto.resumo().financas().precosAlvo()).isEqualTo(4L);

        assertThat(dto.resumo().planejamento().estrategias()).isEqualTo(3L);
        assertThat(dto.resumo().planejamento().metasDeAtivos()).isEqualTo(5L);
        assertThat(dto.resumo().planejamento().checklistsModelo()).isEqualTo(2L);
        assertThat(dto.resumo().planejamento().checklistsAtivo()).isEqualTo(1L);
        assertThat(dto.resumo().planejamento().aporteConfigurado()).isTrue();
        assertThat(dto.resumo().planejamento().aportesRegistrados()).isEqualTo(10L);
        assertThat(dto.resumo().planejamento().registrosHistorico()).isEqualTo(7L);

        assertThat(dto.resumo().estudos().materias()).isEqualTo(4L);
        assertThat(dto.resumo().estudos().estudos()).isEqualTo(20L);
        assertThat(dto.resumo().estudos().sessoesEstudo()).isEqualTo(30L);

        assertThat(dto.resumo().tarefas()).isEqualTo(12L);
        assertThat(dto.resumo().treino().treinos()).isEqualTo(5L);
        assertThat(dto.resumo().treino().sessoes()).isEqualTo(40L);
    }

    @Test
    @DisplayName("usuário inexistente lança ResourceNotFoundException (-> 404)")
    void usuarioInexistenteLancaNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getUserDetail(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    @DisplayName("o detalhe nunca serializa credenciais nem segredos do usuário")
    void naoSerializaCredenciaisNemSegredos() throws Exception {
        Long id = 9L;
        UserModel u = usuario(id);
        // Preenche TUDO o que é sensível — nada disso pode chegar ao JSON.
        u.setPassword("$2a$10$segredo-hash-bcrypt");
        u.setActivationCode("codigo-de-ativacao-secreto");
        u.setResetPasswordToken("token-de-reset-secreto");
        u.setTokenVersion(7);
        when(userRepository.findById(id)).thenReturn(Optional.of(u));

        AdminUserDetailDTO dto = service.getUserDetail(id);
        String json = new ObjectMapper().writeValueAsString(dto);

        assertThat(json)
                .doesNotContain("password")
                .doesNotContain("activationCode")
                .doesNotContain("resetPasswordToken")
                .doesNotContain("tokenVersion")
                .doesNotContain("segredo");
    }

    @Test
    @DisplayName("contrato do DTO: campos fixos — incluir um segredo exige mudar o record de propósito")
    void contratoDoDtoNaoTemCamposSensiveis() {
        List<String> componentes = Arrays.stream(AdminUserDetailDTO.class.getRecordComponents())
                .map(RecordComponent::getName)
                .toList();

        assertThat(componentes).containsExactly("id", "nome", "email", "role", "ativo", "resumo");
    }
}
