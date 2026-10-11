package dev.LzGuimaraes.FocusLifeHub.Admin;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
import dev.LzGuimaraes.FocusLifeHub.User.UserModel;
import dev.LzGuimaraes.FocusLifeHub.User.UserRepository;

/**
 * Consulta administrativa dos detalhes de UM usuário específico.
 *
 * Monta os dados cadastrais permitidos + um resumo (contagens) dos dados do
 * usuário nos módulos. TODAS as contagens são COUNT no banco: nenhuma entidade é
 * carregada e nenhuma coleção é percorrida — sem N+1 e sem trazer grandes volumes.
 *
 * A camada de autorização (ADMIN apenas) fica no {@code SecurityConfig} e no
 * {@code SecurityFilter}; este serviço só lê. Quem chama já foi autorizado.
 */
@Service
public class AdminUserDetailService {

    private final UserRepository userRepository;
    private final CarteiraInvestimentoRepository carteiraInvestimentoRepository;
    private final CarteiraDividasRepository carteiraDividasRepository;
    private final AtivoRepository ativoRepository;
    private final DespesaRepository despesaRepository;
    private final OperacaoCompraVendaRepository operacaoCompraVendaRepository;
    private final AtivoPrecoAlvoRepository ativoPrecoAlvoRepository;
    private final EstrategiaRepository estrategiaRepository;
    private final MetaAtivoRepository metaAtivoRepository;
    private final ChecklistModeloRepository checklistModeloRepository;
    private final ChecklistAtivoRepository checklistAtivoRepository;
    private final AporteConfigRepository aporteConfigRepository;
    private final AporteRegistroRepository aporteRegistroRepository;
    private final AtivoScoreHistoricoRepository ativoScoreHistoricoRepository;
    private final MateriaRepository materiaRepository;
    private final EstudosRepository estudosRepository;
    private final SessaoEstudoRepository sessaoEstudoRepository;
    private final TarefasRepository tarefasRepository;
    private final WorkoutRepository workoutRepository;
    private final WorkoutSessionRepository workoutSessionRepository;

    public AdminUserDetailService(UserRepository userRepository,
                                  CarteiraInvestimentoRepository carteiraInvestimentoRepository,
                                  CarteiraDividasRepository carteiraDividasRepository,
                                  AtivoRepository ativoRepository,
                                  DespesaRepository despesaRepository,
                                  OperacaoCompraVendaRepository operacaoCompraVendaRepository,
                                  AtivoPrecoAlvoRepository ativoPrecoAlvoRepository,
                                  EstrategiaRepository estrategiaRepository,
                                  MetaAtivoRepository metaAtivoRepository,
                                  ChecklistModeloRepository checklistModeloRepository,
                                  ChecklistAtivoRepository checklistAtivoRepository,
                                  AporteConfigRepository aporteConfigRepository,
                                  AporteRegistroRepository aporteRegistroRepository,
                                  AtivoScoreHistoricoRepository ativoScoreHistoricoRepository,
                                  MateriaRepository materiaRepository,
                                  EstudosRepository estudosRepository,
                                  SessaoEstudoRepository sessaoEstudoRepository,
                                  TarefasRepository tarefasRepository,
                                  WorkoutRepository workoutRepository,
                                  WorkoutSessionRepository workoutSessionRepository) {
        this.userRepository = userRepository;
        this.carteiraInvestimentoRepository = carteiraInvestimentoRepository;
        this.carteiraDividasRepository = carteiraDividasRepository;
        this.ativoRepository = ativoRepository;
        this.despesaRepository = despesaRepository;
        this.operacaoCompraVendaRepository = operacaoCompraVendaRepository;
        this.ativoPrecoAlvoRepository = ativoPrecoAlvoRepository;
        this.estrategiaRepository = estrategiaRepository;
        this.metaAtivoRepository = metaAtivoRepository;
        this.checklistModeloRepository = checklistModeloRepository;
        this.checklistAtivoRepository = checklistAtivoRepository;
        this.aporteConfigRepository = aporteConfigRepository;
        this.aporteRegistroRepository = aporteRegistroRepository;
        this.ativoScoreHistoricoRepository = ativoScoreHistoricoRepository;
        this.materiaRepository = materiaRepository;
        this.estudosRepository = estudosRepository;
        this.sessaoEstudoRepository = sessaoEstudoRepository;
        this.tarefasRepository = tarefasRepository;
        this.workoutRepository = workoutRepository;
        this.workoutSessionRepository = workoutSessionRepository;
    }

    @Transactional(readOnly = true)
    public AdminUserDetailDTO getUserDetail(Long userId) {
        UserModel user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário com ID " + userId + " não encontrado"));

        var financas = new AdminUserDetailDTO.FinancasDTO(
                carteiraInvestimentoRepository.countByUserId(userId),
                carteiraDividasRepository.countByUserId(userId),
                ativoRepository.countByCarteiraInvestimento_UserId(userId),
                despesaRepository.countByCarteiraDividas_UserId(userId),
                operacaoCompraVendaRepository.countByUserId(userId),
                ativoPrecoAlvoRepository.countByUserId(userId));

        var planejamento = new AdminUserDetailDTO.PlanejamentoDTO(
                estrategiaRepository.countByUserId(userId),
                metaAtivoRepository.countByCarteiraInvestimento_UserId(userId),
                checklistModeloRepository.countByUserId(userId),
                checklistAtivoRepository.countByUserId(userId),
                aporteConfigRepository.existsByUserId(userId),
                aporteRegistroRepository.countByUserId(userId),
                ativoScoreHistoricoRepository.countByUserId(userId));

        var estudos = new AdminUserDetailDTO.EstudosDTO(
                materiaRepository.countByUserId(userId),
                estudosRepository.countByMateria_UserId(userId),
                sessaoEstudoRepository.countDoUsuario(userId));

        var treino = new AdminUserDetailDTO.TreinoDTO(
                workoutRepository.countByUserId(userId),
                workoutSessionRepository.countByUserId(userId));

        var resumo = new AdminUserDetailDTO.ResumoDTO(
                financas,
                planejamento,
                estudos,
                tarefasRepository.countByUserId(userId),
                treino);

        String role = user.getRole() != null ? user.getRole().name() : "USER";

        return new AdminUserDetailDTO(
                user.getId(),
                user.getNome(),
                user.getEmail(),
                role,
                user.isEnabled(),
                resumo);
    }
}
