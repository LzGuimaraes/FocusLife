package dev.LzGuimaraes.FocusLifeHub.Admin;

import java.time.LocalDate;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.LzGuimaraes.FocusLifeHub.Admin.dto.AdminUserRecordsDTO;
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
 * direto dos repositórios por {@code userId} e reusa apenas os MAPPERS
 * existentes (mapeamento puro, sem regra de negócio) — sem duplicar contrato de
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

    private final UserRepository userRepository;
    private final TarefasRepository tarefasRepository;
    private final MetasRepository metasRepository;
    private final EstudosRepository estudosRepository;
    private final TarefasMapper tarefasMapper;
    private final MetasMapper metasMapper;
    private final EstudosMapper estudosMapper;

    public AdminUserRecordsService(UserRepository userRepository,
                                   TarefasRepository tarefasRepository,
                                   MetasRepository metasRepository,
                                   EstudosRepository estudosRepository,
                                   TarefasMapper tarefasMapper,
                                   MetasMapper metasMapper,
                                   EstudosMapper estudosMapper) {
        this.userRepository = userRepository;
        this.tarefasRepository = tarefasRepository;
        this.metasRepository = metasRepository;
        this.estudosRepository = estudosRepository;
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
