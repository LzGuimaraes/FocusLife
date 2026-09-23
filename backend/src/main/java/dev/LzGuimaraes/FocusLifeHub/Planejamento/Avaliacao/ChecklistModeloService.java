package dev.LzGuimaraes.FocusLifeHub.Planejamento.Avaliacao;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.LzGuimaraes.FocusLifeHub.Exceptions.ResourceNotFoundException;
import dev.LzGuimaraes.FocusLifeHub.Planejamento.Avaliacao.dto.ChecklistModeloDTO;
import dev.LzGuimaraes.FocusLifeHub.Planejamento.Avaliacao.dto.PerguntaDTO;
import dev.LzGuimaraes.FocusLifeHub.Planejamento.comum.ContextoUsuario;
import dev.LzGuimaraes.FocusLifeHub.User.UserModel;
import dev.LzGuimaraes.FocusLifeHub.User.UserRepository;

/**
 * CRUD dos MODELOS de checklist (Módulo 4).
 *
 * O modelo é um template do usuário: ele não influencia checklists já criados
 * (que guardam um snapshot próprio) e pode ser duplicado à vontade.
 */
@Service
public class ChecklistModeloService {

    private final ChecklistModeloRepository modeloRepository;
    private final UserRepository userRepository;
    private final ContextoUsuario contextoUsuario;
    private final PerguntaValidador validador;
    private final AvaliacaoMapper mapper;

    public ChecklistModeloService(ChecklistModeloRepository modeloRepository,
                                  UserRepository userRepository,
                                  ContextoUsuario contextoUsuario,
                                  PerguntaValidador validador,
                                  AvaliacaoMapper mapper) {
        this.modeloRepository = modeloRepository;
        this.userRepository = userRepository;
        this.contextoUsuario = contextoUsuario;
        this.validador = validador;
        this.mapper = mapper;
    }

    /**
     * Lista paginada de modelos (sem carregar as perguntas, apenas a contagem).
     * A página é montada em memória a partir de uma única query com fetch join,
     * evitando N+1 e o problema de paginação com fetch de coleção.
     */
    @Transactional(readOnly = true)
    public Page<ChecklistModeloDTO.Resumo> getAll(Pageable pageable) {
        List<ChecklistModeloModel> todos = modeloRepository.findAllComPerguntas(contextoUsuario.id());
        todos.sort(Comparator.comparing(ChecklistModeloModel::getNome, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(ChecklistModeloModel::getId));

        int inicio = (int) Math.min(pageable.getOffset(), todos.size());
        int fim = Math.min(inicio + pageable.getPageSize(), todos.size());
        List<ChecklistModeloDTO.Resumo> pagina = todos.subList(inicio, fim).stream()
                .map(m -> new ChecklistModeloDTO.Resumo(
                        m.getId(), m.getNome(), m.getDescricao(), m.getTipoAlvo(), m.getTipo(), m.getAtiva(),
                        m.getPerguntas().size()))
                .toList();

        return new PageImpl<>(pagina, pageable, todos.size());
    }

    @Transactional(readOnly = true)
    public ChecklistModeloDTO.Response getById(Long id) {
        return toResponse(exigirDoUsuario(id));
    }

    @Transactional
    public ChecklistModeloDTO.Response create(ChecklistModeloDTO.Request dto) {
        Long userId = contextoUsuario.id();
        UserModel user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário com ID " + userId + " não encontrado"));

        validador.validar(dto.perguntas());

        ChecklistModeloModel modelo = new ChecklistModeloModel();
        modelo.setNome(dto.nome().trim());
        modelo.setDescricao(dto.descricao());
        modelo.setTipoAlvo(dto.tipo_alvo());
        modelo.setTipo(dto.tipo() != null ? dto.tipo() : dev.LzGuimaraes.FocusLifeHub.Planejamento.Avaliacao.TipoChecklist.QUALIDADE);
        modelo.setAtiva(dto.ativa() == null || dto.ativa());
        modelo.setUser(user);
        modelo.setCreatedAt(LocalDateTime.now());
        modelo.setUpdatedAt(LocalDateTime.now());
        aplicarPerguntas(modelo, dto.perguntas());

        return toResponse(modeloRepository.save(modelo));
    }

    @Transactional
    public ChecklistModeloDTO.Response update(Long id, ChecklistModeloDTO.Request dto) {
        ChecklistModeloModel modelo = exigirDoUsuario(id);
        validador.validar(dto.perguntas());

        if (dto.nome() != null && !dto.nome().isBlank()) {
            modelo.setNome(dto.nome().trim());
        }
        if (dto.descricao() != null) {
            modelo.setDescricao(dto.descricao());
        }
        if (dto.tipo_alvo() != null) {
            modelo.setTipoAlvo(dto.tipo_alvo());
        }
        if (dto.tipo() != null) {
            modelo.setTipo(dto.tipo());
        }
        if (dto.ativa() != null) {
            modelo.setAtiva(dto.ativa());
        }
        // Replace-all das perguntas (o modelo não guarda respostas).
        modelo.getPerguntas().clear();
        aplicarPerguntas(modelo, dto.perguntas());
        modelo.setUpdatedAt(LocalDateTime.now());

        return toResponse(modeloRepository.save(modelo));
    }

    @Transactional
    public void delete(Long id) {
        modeloRepository.delete(exigirDoUsuario(id));
    }

    /** Cria uma cópia independente do modelo (perguntas e regras incluídas). */
    @Transactional
    public ChecklistModeloDTO.Response duplicar(Long id) {
        ChecklistModeloModel origem = exigirDoUsuario(id);

        ChecklistModeloModel copia = new ChecklistModeloModel();
        String base = origem.getNome().replaceAll("\\s*\\(cópia\\)\\s*$", "");
        copia.setNome(base + " (cópia)");
        copia.setDescricao(origem.getDescricao());
        copia.setTipoAlvo(origem.getTipoAlvo());
        copia.setTipo(origem.getTipo());
        copia.setAtiva(origem.getAtiva());
        copia.setUser(origem.getUser());
        copia.setCreatedAt(LocalDateTime.now());
        copia.setUpdatedAt(LocalDateTime.now());

        int ordem = 0;
        for (ChecklistModeloPerguntaModel pergunta : origem.getPerguntas()) {
            ChecklistModeloPerguntaModel nova = new ChecklistModeloPerguntaModel();
            mapper.copiarDefinicao(nova, pergunta);
            nova.setObrigatoria(pergunta.getObrigatoria());
            nova.setModelo(copia);
            int ordemRegra = 0;
            for (ChecklistModeloPerguntaRegraModel regra : pergunta.getRegras()) {
                ChecklistModeloPerguntaRegraModel copiaRegra = new ChecklistModeloPerguntaRegraModel();
                mapper.copiarRegra(copiaRegra, regra, ordemRegra++);
                copiaRegra.setPergunta(nova);
                nova.getRegras().add(copiaRegra);
            }
            copia.getPerguntas().add(nova);
            ordem++;
        }

        return toResponse(modeloRepository.save(copia));
    }

    /* ── Helpers ── */

    private ChecklistModeloModel exigirDoUsuario(Long id) {
        return modeloRepository.findDetalheByIdAndUserId(id, contextoUsuario.id())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Modelo de checklist com ID " + id + " não encontrado"));
    }

    private void aplicarPerguntas(ChecklistModeloModel modelo, List<PerguntaDTO.PerguntaRequest> perguntas) {
        if (perguntas == null) {
            return;
        }
        int ordem = 0;
        for (PerguntaDTO.PerguntaRequest req : perguntas) {
            boolean pontua = validador.pontua(req.tipo(), req.conta_no_score());

            ChecklistModeloPerguntaModel pergunta = new ChecklistModeloPerguntaModel();
            mapper.copiarDefinicao(pergunta, req, pontua, ordem++);
            pergunta.setObrigatoria(req.obrigatoria() != null && req.obrigatoria());
            pergunta.setModelo(modelo);

            int ordemRegra = 0;
            for (PerguntaDTO.RegraRequest regra : regrasDe(req)) {
                ChecklistModeloPerguntaRegraModel nova = mapper.novaRegra(ChecklistModeloPerguntaRegraModel::new, regra, ordemRegra++);
                nova.setPergunta(pergunta);
                pergunta.getRegras().add(nova);
            }
            modelo.getPerguntas().add(pergunta);
        }
    }

    private List<PerguntaDTO.RegraRequest> regrasDe(PerguntaDTO.PerguntaRequest req) {
        return (req.regras() == null) ? new ArrayList<>() : req.regras();
    }

    private ChecklistModeloDTO.Response toResponse(ChecklistModeloModel modelo) {
        List<PerguntaDTO.PerguntaResponse> perguntas = mapper.ordenar(modelo.getPerguntas()).stream()
                .map(p -> mapper.toPerguntaResponse(p, p.getObrigatoria(), p.getRegras(), null, null, null, null, null))
                .toList();

        return new ChecklistModeloDTO.Response(
                modelo.getId(),
                modelo.getNome(),
                modelo.getDescricao(),
                modelo.getTipoAlvo(),
                modelo.getTipo(),
                modelo.getAtiva(),
                (modelo.getUser() != null) ? modelo.getUser().getId() : null,
                perguntas);
    }
}
