package dev.LzGuimaraes.FocusLifeHub.Admin;

import java.time.LocalDate;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.LzGuimaraes.FocusLifeHub.Admin.dto.AdminAtivoDTO;
import dev.LzGuimaraes.FocusLifeHub.Admin.dto.AdminDespesaDTO;
import dev.LzGuimaraes.FocusLifeHub.Admin.dto.AdminUserRecordsDTO;
import dev.LzGuimaraes.FocusLifeHub.Ativo.AtivoModel;
import dev.LzGuimaraes.FocusLifeHub.Ativo.AtivoRepository;
import dev.LzGuimaraes.FocusLifeHub.Ativo.CategoriaInvestimento;
import dev.LzGuimaraes.FocusLifeHub.Carteira.CarteiraInvestimentoModel;
import dev.LzGuimaraes.FocusLifeHub.Despesa.DespesaModel;
import dev.LzGuimaraes.FocusLifeHub.Despesa.DespesaRepository;
import dev.LzGuimaraes.FocusLifeHub.Despesa.TipoLancamento;
import dev.LzGuimaraes.FocusLifeHub.Estudos.EstudosMapper;
import dev.LzGuimaraes.FocusLifeHub.Estudos.EstudosRepository;
import dev.LzGuimaraes.FocusLifeHub.Estudos.dto.EstudosResponseDTO;
import dev.LzGuimaraes.FocusLifeHub.Exceptions.ResourceNotFoundException;
import dev.LzGuimaraes.FocusLifeHub.Metas.Enum.MetaStatus;
import dev.LzGuimaraes.FocusLifeHub.Metas.MetasMapper;
import dev.LzGuimaraes.FocusLifeHub.Metas.MetasRepository;
import dev.LzGuimaraes.FocusLifeHub.Metas.dto.MetasResponseDTO;
import dev.LzGuimaraes.FocusLifeHub.Tarefas.Enum.Prioridade;
import dev.LzGuimaraes.FocusLifeHub.Tarefas.Enum.TarefaStatus;
import dev.LzGuimaraes.FocusLifeHub.Tarefas.TarefasMapper;
import dev.LzGuimaraes.FocusLifeHub.Tarefas.TarefasRepository;
import dev.LzGuimaraes.FocusLifeHub.Tarefas.dto.TarefasResponseDTO;
import dev.LzGuimaraes.FocusLifeHub.User.UserRepository;

/**
 * Consultas administrativas, SOMENTE LEITURA, dos registros de UM usuário-alvo
 * por módulo de negócio.
 *
 * COMO O ESCOPO É GARANTIDO
 * <ul>
 *   <li>O id do usuário-alvo vem do CAMINHO da URL e é passado como argumento
 *       obrigatório para o repositório. As consultas ({@code buscarDoUsuario})
 *       aplicam esse id com {@code AND} aos filtros, então nenhum parâmetro de
 *       requisição (status, prioridade, matéria, datas) consegue ampliar o
 *       conjunto para registros de terceiros.</li>
 *   <li>O usuário-alvo é validado ({@code existsById}) antes da consulta: alvo
 *       inexistente devolve 404, o que também impede consultas "abertas".</li>
 *   <li>Toda resposta identifica o alvo explicitamente
 *       ({@link AdminUserRecordsDTO#usuarioId()}).</li>
 * </ul>
 *
 * REUSO SEM QUEBRAR AUTORIZAÇÃO
 * Os serviços comuns (ex.: {@code TarefasService}) resolvem o usuário pelo
 * {@code SecurityContext}; reusá-los aqui devolveria os dados do PRÓPRIO admin,
 * não os do alvo, e comprometeria a autorização. Por isso esta camada lê
 * direto dos repositórios por {@code userId} e reusa os MAPPERS existentes ou
 * faz mapeamento local puro (quando o DTO administrativo ganha campo extra,
 * como a carteira dona de investimentos/contas) — sem duplicar contrato de
 * resposta nem reimplementar regras.
 *
 * SEM ESCRITA
 * Todos os métodos são {@code readOnly} e não alteram nada.
 */
@Service
public class AdminUserRecordsService {

    /** Teto de itens por página: impede que a requisição peça o conjunto inteiro. */
    static final int TAMANHO_MAXIMO_PAGINA = 100;
    static final int TAMANHO_PADRAO_PAGINA = 20;

    private static final Sort TAREFAS_ORDEM_PADRAO = Sort.by(Sort.Direction.DESC, "id");
    private static final Sort METAS_ORDEM_PADRAO = Sort.by(Sort.Direction.DESC, "id");
    private static final Sort ESTUDOS_ORDEM_PADRAO = Sort.by(Sort.Direction.DESC, "data").and(Sort.by(Sort.Direction.DESC, "id"));
    private static final Sort FINANCEIRO_ORDEM_PADRAO = Sort.by(Sort.Direction.DESC, "id");

    private final UserRepository userRepository;
    private final TarefasRepository tarefasRepository;
    private final MetasRepository metasRepository;
    private final EstudosRepository estudosRepository;
    private final AtivoRepository ativoRepository;
    private final DespesaRepository despesaRepository;
    private final TarefasMapper tarefasMapper;
    private final MetasMapper metasMapper;
    private final EstudosMapper estudosMapper;

    public AdminUserRecordsService(UserRepository userRepository,
                                   TarefasRepository tarefasRepository,
                                   MetasRepository metasRepository,
                                   EstudosRepository estudosRepository,
                                   AtivoRepository ativoRepository,
                                   DespesaRepository despesaRepository,
                                   TarefasMapper tarefasMapper,
                                   MetasMapper metasMapper,
                                   EstudosMapper estudosMapper) {
        this.userRepository = userRepository;
        this.tarefasRepository = tarefasRepository;
        this.metasRepository = metasRepository;
        this.estudosRepository = estudosRepository;
        this.ativoRepository = ativoRepository;
        this.despesaRepository = despesaRepository;
        this.tarefasMapper = tarefasMapper;
        this.metasMapper = metasMapper;
        this.estudosMapper = estudosMapper;
    }

    /** Tarefas de um usuário-alvo, com filtro opcional de status e prioridade. */
    @Transactional(readOnly = true)
    public AdminUserRecordsDTO<TarefasResponseDTO> tarefasDoUsuario(Long usuarioId,
                                                                    TarefaStatus status,
                                                                    Prioridade prioridade,
                                                                    Pageable pageable) {
        validarUsuarioAlvo(usuarioId);
        Page<TarefasResponseDTO> pagina = tarefasRepository
                .buscarDoUsuario(usuarioId, status != null, status, prioridade != null, prioridade,
                        sanitizar(pageable, TAREFAS_ORDEM_PADRAO))
                .map(tarefasMapper::toResponse);
        return new AdminUserRecordsDTO<>(usuarioId, pagina);
    }

    /** Metas de um usuário-alvo, com filtro opcional de status. */
    @Transactional(readOnly = true)
    public AdminUserRecordsDTO<MetasResponseDTO> metasDoUsuario(Long usuarioId,
                                                                MetaStatus status,
                                                                Pageable pageable) {
        validarUsuarioAlvo(usuarioId);
        Page<MetasResponseDTO> pagina = metasRepository
                .buscarDoUsuario(usuarioId, status != null, status, sanitizar(pageable, METAS_ORDEM_PADRAO))
                .map(metasMapper::toResponse);
        return new AdminUserRecordsDTO<>(usuarioId, pagina);
    }

    /**
     * Registros de estudo de um usuário-alvo, com filtros opcionais de matéria e
     * período. A posse é revalidada no banco pela matéria dona: um
     * {@code materiaId} de terceiro apenas retorna vazio.
     */
    @Transactional(readOnly = true)
    public AdminUserRecordsDTO<EstudosResponseDTO> estudosDoUsuario(Long usuarioId,
                                                                    Long materiaId,
                                                                    LocalDate de,
                                                                    LocalDate ate,
                                                                    Pageable pageable) {
        validarUsuarioAlvo(usuarioId);
        Page<EstudosResponseDTO> pagina = estudosRepository
                .buscarDoUsuario(usuarioId, materiaId != null, materiaId, de != null, de, ate != null, ate,
                        sanitizar(pageable, ESTUDOS_ORDEM_PADRAO))
                .map(estudosMapper::toResponse);
        return new AdminUserRecordsDTO<>(usuarioId, pagina);
    }

    /**
     * Posições de investimento de um usuário-alvo, com filtro opcional de
     * classe ({@link CategoriaInvestimento}).
     *
     * A posse vem da carteira ({@code ativo.carteira_investimento_id ->
     * carteira_investimento.user_id}) e é aplicada no banco junto com o filtro
     * ({@code AND}), então nenhum parâmetro amplia o escopo para terceiros.
     * Aqui não existe mapper compartilhado (o DTO administrativo ganhou a
     * carteira dona), então o mapeamento é local e puro.
     */
    @Transactional(readOnly = true)
    public AdminUserRecordsDTO<AdminAtivoDTO> investimentosDoUsuario(Long usuarioId,
                                                                     CategoriaInvestimento categoria,
                                                                     Pageable pageable) {
        validarUsuarioAlvo(usuarioId);
        Page<AdminAtivoDTO> pagina = ativoRepository
                .buscarDoUsuario(usuarioId, categoria != null, categoria, sanitizar(pageable, FINANCEIRO_ORDEM_PADRAO))
                .map(this::toAdminAtivo);
        return new AdminUserRecordsDTO<>(usuarioId, pagina);
    }

    /**
     * Lançamentos (contas a pagar e saldos) de um usuário-alvo, com filtros
     * opcionais de tipo e de pagamento.
     *
     * A posse vem da carteira ({@code despesa.carteira_dividas_id ->
     * carteira_dividas.user_id}) e é aplicada no banco junto com os filtros
     * ({@code AND}), então nenhum parâmetro amplia o escopo para terceiros.
     * Um {@code pago=false} devolve só despesas não pagas: SALDOS têm
     * {@code pago} nulo e ficam de fora (use o filtro de tipo para vê-los).
     */
    @Transactional(readOnly = true)
    public AdminUserRecordsDTO<AdminDespesaDTO> contasDoUsuario(Long usuarioId,
                                                                TipoLancamento tipo,
                                                                Boolean pago,
                                                                Pageable pageable) {
        validarUsuarioAlvo(usuarioId);
        Page<AdminDespesaDTO> pagina = despesaRepository
                .buscarDoUsuario(usuarioId, tipo != null, tipo, pago != null, pago, sanitizar(pageable, FINANCEIRO_ORDEM_PADRAO))
                .map(this::toAdminDespesa);
        return new AdminUserRecordsDTO<>(usuarioId, pagina);
    }

    /**
     * Mapeamento puro posição → DTO administrativo. O preço atual segue a MESMA
     * regra do endpoint comum: o preço do catálogo vinculado prevalece sobre o
     * guardado na posição (a carteira e o catálogo vêm por join fetch).
     */
    private AdminAtivoDTO toAdminAtivo(AtivoModel ativo) {
        CarteiraInvestimentoModel carteira = ativo.getCarteiraInvestimento();

        Float precoAtual = ativo.getPrecoAtual();
        if (ativo.getAtivoCadastro() != null && ativo.getAtivoCadastro().getPrecoAtual() != null) {
            precoAtual = ativo.getAtivoCadastro().getPrecoAtual();
        }

        return new AdminAtivoDTO(
                ativo.getId(),
                ativo.getNome(),
                ativo.getCategoriaInvestimento(),
                ativo.getQuantidade(),
                ativo.getValorUnitario(),
                precoAtual,
                ativo.getSaldo(),
                ativo.getInstituicao(),
                ativo.getDataAplicacao(),
                ativo.getVencimento(),
                ativo.getDataVencimento(),
                ativo.getRentabilidade(),
                carteira != null ? carteira.getId() : null,
                carteira != null ? carteira.getNome() : null,
                carteira != null ? carteira.getMoeda() : null);
    }

    /** Mapeamento puro lançamento → DTO administrativo (mesmos campos que o usuário vê). */
    private AdminDespesaDTO toAdminDespesa(DespesaModel despesa) {
        var carteira = despesa.getCarteiraDividas();
        return new AdminDespesaDTO(
                despesa.getId(),
                despesa.getNome(),
                despesa.getSaldo(),
                despesa.getDataVencimento(),
                despesa.getPago(),
                despesa.getTipo() != null ? despesa.getTipo() : TipoLancamento.DESPESA,
                despesa.getCategoria(),
                carteira != null ? carteira.getId() : null,
                carteira != null ? carteira.getNome() : null,
                carteira != null ? carteira.getMoeda() : null);
    }

    private void validarUsuarioAlvo(Long usuarioId) {
        if (usuarioId == null || !userRepository.existsById(usuarioId)) {
            throw new ResourceNotFoundException("Usuário com ID " + usuarioId + " não encontrado");
        }
    }

    /**
     * Normaliza a paginação vinda da requisição: página e tamanho nunca negativos,
     * tamanho limitado ao teto e ordenação padrão quando o cliente não informa
     * nenhuma (mantém o resultado determinístico entre páginas).
     */
    private Pageable sanitizar(Pageable pageable, Sort ordemPadrao) {
        int numero = pageable == null ? 0 : Math.max(pageable.getPageNumber(), 0);
        int tamanho = pageable == null ? TAMANHO_PADRAO_PAGINA : pageable.getPageSize();
        if (tamanho <= 0) {
            tamanho = TAMANHO_PADRAO_PAGINA;
        }
        if (tamanho > TAMANHO_MAXIMO_PAGINA) {
            tamanho = TAMANHO_MAXIMO_PAGINA;
        }
        Sort ordem = (pageable == null || pageable.getSort().isUnsorted()) ? ordemPadrao : pageable.getSort();
        return PageRequest.of(numero, tamanho, ordem);
    }
}
