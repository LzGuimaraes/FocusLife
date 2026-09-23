package dev.LzGuimaraes.FocusLifeHub.Planejamento.Avaliacao;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.LzGuimaraes.FocusLifeHub.Ativo.AtivoModel;
import dev.LzGuimaraes.FocusLifeHub.Ativo.AtivoRepository;
import dev.LzGuimaraes.FocusLifeHub.AtivoCadastro.AtivoCadastroModel;
import dev.LzGuimaraes.FocusLifeHub.AtivoCadastro.AtivoCadastroRepository;
import dev.LzGuimaraes.FocusLifeHub.Exceptions.BusinessRuleException;
import dev.LzGuimaraes.FocusLifeHub.Exceptions.ResourceNotFoundException;
import dev.LzGuimaraes.FocusLifeHub.Planejamento.Avaliacao.dto.ChecklistAtivoDTO;
import dev.LzGuimaraes.FocusLifeHub.Planejamento.Avaliacao.dto.PerguntaDTO;
import dev.LzGuimaraes.FocusLifeHub.Planejamento.comum.ContextoUsuario;
import dev.LzGuimaraes.FocusLifeHub.User.UserModel;
import dev.LzGuimaraes.FocusLifeHub.User.UserRepository;

/**
 * Checklist do ativo (Módulos 2, 3 e 5).
 *
 * • Um ativo pode ter N checklists, cada um com quantas perguntas quiser.
 * • Criar a partir de um modelo copia as perguntas/regras (SNAPSHOT): editar o
 *   modelo depois não muda nada aqui, e editar aqui não muda o modelo.
 * • O Quality Score é sempre a soma ponderada DAS NOTAS DO USUÁRIO — o sistema
 *   não infere nota. Sem nada pontuado respondido, o score é `null`.
 */
@Service
public class ChecklistAtivoService {

    private final ChecklistAtivoRepository checklistRepository;
    private final ChecklistModeloRepository modeloRepository;
    private final AtivoCadastroRepository ativoCadastroRepository;
    private final AtivoRepository ativoRepository;
    private final UserRepository userRepository;
    private final ContextoUsuario contextoUsuario;
    private final PerguntaValidador validador;
    private final ScoreCalculator scoreCalculator;
    private final AvaliacaoMapper mapper;

    public ChecklistAtivoService(ChecklistAtivoRepository checklistRepository,
                                 ChecklistModeloRepository modeloRepository,
                                 AtivoCadastroRepository ativoCadastroRepository,
                                 AtivoRepository ativoRepository,
                                 UserRepository userRepository,
                                 ContextoUsuario contextoUsuario,
                                 PerguntaValidador validador,
                                 ScoreCalculator scoreCalculator,
                                 AvaliacaoMapper mapper) {
        this.checklistRepository = checklistRepository;
        this.modeloRepository = modeloRepository;
        this.ativoCadastroRepository = ativoCadastroRepository;
        this.ativoRepository = ativoRepository;
        this.userRepository = userRepository;
        this.contextoUsuario = contextoUsuario;
        this.validador = validador;
        this.scoreCalculator = scoreCalculator;
        this.mapper = mapper;
    }

    /* ══════════════════════════════════════════════════════════════════
       Leitura
       ══════════════════════════════════════════════════════════════════ */

    /** Checklists de um ativo — por ticker do catálogo ou por posição. */
    @Transactional(readOnly = true)
    public List<ChecklistAtivoDTO.Response> getByAtivo(UUID ativoCadastroId, Long ativoId) {
        Long userId = contextoUsuario.id();
        List<ChecklistAtivoModel> checklists;

        if (ativoCadastroId != null) {
            checklists = checklistRepository.findByAtivoCadastroComPerguntas(userId, ativoCadastroId);
        } else if (ativoId != null) {
            checklists = checklistRepository.findByAtivoComPerguntas(userId, ativoId);
        } else {
            throw new BusinessRuleException("Informe o ativo do catálogo (ativo_cadastro_id) ou a posição (ativo_id).");
        }

        return checklists.stream()
                .sorted(Comparator.comparing((ChecklistAtivoModel c) -> nz(c.getOrdem())).thenComparing(ChecklistAtivoModel::getId))
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ChecklistAtivoDTO.Response getById(Long id) {
        return toResponse(exigirDoUsuario(id));
    }

    /** Resumo dos ativos já avaliados, com o Quality Score consolidado (Módulo 5). */
    @Transactional(readOnly = true)
    public List<ChecklistAtivoDTO.AtivoAvaliado> resumoPorAtivo() {
        List<ChecklistAtivoModel> todos = checklistRepository.findAllComPerguntas(contextoUsuario.id());

        // Agrupa preservando a ordem de primeira aparição.
        Map<String, List<ChecklistAtivoModel>> porAtivo = new LinkedHashMap<>();
        for (ChecklistAtivoModel c : todos) {
            porAtivo.computeIfAbsent(chaveDoAtivo(c), k -> new ArrayList<>()).add(c);
        }

        List<ChecklistAtivoDTO.AtivoAvaliado> resumo = new ArrayList<>();
        for (List<ChecklistAtivoModel> grupo : porAtivo.values()) {
            ChecklistAtivoModel primeiro = grupo.get(0);

            List<BigDecimal> scores = new ArrayList<>();
            List<BigDecimal> pesos = new ArrayList<>();
            int totalPerguntas = 0;
            int totalRespondidas = 0;

            for (ChecklistAtivoModel c : grupo) {
                List<ChecklistAtivoPerguntaModel> perguntas = ordenar(c);
                scores.add(scoreDe(perguntas));
                pesos.add((c.getPeso() != null) ? c.getPeso() : BigDecimal.ONE);
                totalPerguntas += perguntas.size();
                totalRespondidas += (int) perguntas.stream().filter(this::respondida).count();
            }

            resumo.add(new ChecklistAtivoDTO.AtivoAvaliado(
                    (primeiro.getAtivoCadastro() != null) ? primeiro.getAtivoCadastro().getId() : null,
                    (primeiro.getAtivo() != null) ? primeiro.getAtivo().getId() : null,
                    tickerDe(primeiro),
                    grupo.size(),
                    totalPerguntas,
                    totalRespondidas,
                    scoreCalculator.qualityScoreDoAtivo(scores, pesos)));
        }

        resumo.sort(Comparator.comparing(ChecklistAtivoDTO.AtivoAvaliado::ticker,
                Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));
        return resumo;
    }

    /* ══════════════════════════════════════════════════════════════════
       Escrita
       ══════════════════════════════════════════════════════════════════ */

    @Transactional
    public ChecklistAtivoDTO.Response create(ChecklistAtivoDTO.Request dto) {
        Long userId = contextoUsuario.id();
        UserModel user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário com ID " + userId + " não encontrado"));

        ChecklistAtivoModel checklist = new ChecklistAtivoModel();
        checklist.setUser(user);
        aplicarAncora(checklist, dto.ativo_cadastro_id(), dto.ativo_id());
        checklist.setPeso((dto.peso() != null) ? dto.peso() : BigDecimal.ONE);
        checklist.setOrdem((dto.ordem() != null) ? dto.ordem() : proximaOrdem(userId, dto));
        checklist.setAtiva(true);
        checklist.setCreatedAt(LocalDateTime.now());
        checklist.setUpdatedAt(LocalDateTime.now());

        if (dto.modelo_id() != null) {
            // SNAPSHOT do modelo: copia perguntas e regras para este checklist.
            ChecklistModeloModel modelo = modeloRepository
                    .findDetalheByIdAndUserId(dto.modelo_id(), userId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Modelo de checklist com ID " + dto.modelo_id() + " não encontrado"));
            checklist.setModeloOrigemId(modelo.getId());
            checklist.setNome((dto.nome() != null && !dto.nome().isBlank()) ? dto.nome().trim() : modelo.getNome());
            for (ChecklistModeloPerguntaModel pergunta : mapper.ordenar(modelo.getPerguntas())) {
                ChecklistAtivoPerguntaModel copia = mapper.snapshot(pergunta);
                copia.setChecklist(checklist);
                checklist.getPerguntas().add(copia);
            }
        } else {
            if (dto.nome() == null || dto.nome().isBlank()) {
                throw new BusinessRuleException("Informe o nome do checklist.");
            }
            checklist.setNome(dto.nome().trim());
            validador.validar(dto.perguntas());
            aplicarPerguntas(checklist, dto.perguntas());
        }

        return toResponse(checklistRepository.save(checklist));
    }

    @Transactional
    public ChecklistAtivoDTO.Response update(Long id, ChecklistAtivoDTO.UpdateRequest dto) {
        ChecklistAtivoModel checklist = exigirDoUsuario(id);
        if (dto.nome() != null && !dto.nome().isBlank()) {
            checklist.setNome(dto.nome().trim());
        }
        if (dto.peso() != null) {
            checklist.setPeso(dto.peso());
        }
        if (dto.ordem() != null) {
            checklist.setOrdem(dto.ordem());
        }
        if (dto.ativa() != null) {
            checklist.setAtiva(dto.ativa());
        }
        checklist.setUpdatedAt(LocalDateTime.now());
        return toResponse(checklistRepository.save(checklist));
    }

    @Transactional
    public void delete(Long id) {
        checklistRepository.delete(exigirDoUsuario(id));
    }

    /**
     * Duplica um checklist. A ESTRUTURA (perguntas/regras) é copiada, mas as
     * RESPOSTAS não: a cópia nasce sem avaliação, pronta para um novo ativo.
     * Sem destino informado, a cópia fica no mesmo ativo.
     */
    @Transactional
    public ChecklistAtivoDTO.Response duplicar(Long id, ChecklistAtivoDTO.Request destino) {
        Long userId = contextoUsuario.id();
        ChecklistAtivoModel origem = exigirDoUsuario(id);

        ChecklistAtivoModel copia = new ChecklistAtivoModel();
        copia.setUser(origem.getUser());
        copia.setModeloOrigemId(origem.getModeloOrigemId());
        copia.setPeso(origem.getPeso());

        boolean temDestino = destino != null && (destino.ativo_cadastro_id() != null || destino.ativo_id() != null);
        if (temDestino) {
            aplicarAncora(copia, destino.ativo_cadastro_id(), destino.ativo_id());
            copia.setOrdem(proximaOrdem(userId, destino));
        } else {
            copia.setAtivoCadastro(origem.getAtivoCadastro());
            copia.setAtivo(origem.getAtivo());
            copia.setOrdem(origem.getOrdem());
        }

        String nomeBase = origem.getNome().replaceAll("\\s*\\(cópia\\)\\s*$", "");
        copia.setNome((destino != null && destino.nome() != null && !destino.nome().isBlank())
                ? destino.nome().trim()
                : nomeBase + " (cópia)");
        copia.setAtiva(true);
        copia.setCreatedAt(LocalDateTime.now());
        copia.setUpdatedAt(LocalDateTime.now());

        for (ChecklistAtivoPerguntaModel pergunta : ordenar(origem)) {
            ChecklistAtivoPerguntaModel nova = mapper.duplicarEstrutura(pergunta);
            nova.setChecklist(copia);
            copia.getPerguntas().add(nova);
        }

        return toResponse(checklistRepository.save(copia));
    }

    /* ── Perguntas ── */

    @Transactional
    public ChecklistAtivoDTO.Response adicionarPergunta(Long id, PerguntaDTO.PerguntaRequest dto) {
        ChecklistAtivoModel checklist = exigirDoUsuario(id);
        validador.validarUma(dto);

        boolean pontua = validador.pontua(dto.tipo(), dto.conta_no_score());
        ChecklistAtivoPerguntaModel pergunta = new ChecklistAtivoPerguntaModel();
        mapper.copiarDefinicao(pergunta, dto, pontua, checklist.getPerguntas().size());
        pergunta.setChecklist(checklist);

        int ordemRegra = 0;
        for (PerguntaDTO.RegraRequest regra : regrasDe(dto)) {
            ChecklistAtivoPerguntaRegraModel nova = mapper.novaRegra(ChecklistAtivoPerguntaRegraModel::new, regra, ordemRegra++);
            nova.setPergunta(pergunta);
            pergunta.getRegras().add(nova);
        }
        checklist.getPerguntas().add(pergunta);
        checklist.setUpdatedAt(LocalDateTime.now());

        return toResponse(checklistRepository.save(checklist));
    }

    /**
     * Altera a definição de uma pergunta da instância (o que o usuário pode
     * ajustar sem mexer no modelo) e/ou a resposta dela.
     */
    @Transactional
    public ChecklistAtivoDTO.Response alterarPergunta(Long id, Long perguntaId, PerguntaDTO.PerguntaRequest dto) {
        ChecklistAtivoModel checklist = exigirDoUsuario(id);
        ChecklistAtivoPerguntaModel pergunta = exigirPergunta(checklist, perguntaId);
        validador.validarUma(dto);

        boolean pontua = validador.pontua(dto.tipo(), dto.conta_no_score());
        mapper.copiarDefinicao(pergunta, dto, pontua, nz(pergunta.getOrdem()));

        pergunta.getRegras().clear();
        int ordemRegra = 0;
        for (PerguntaDTO.RegraRequest regra : regrasDe(dto)) {
            ChecklistAtivoPerguntaRegraModel nova = mapper.novaRegra(ChecklistAtivoPerguntaRegraModel::new, regra, ordemRegra++);
            nova.setPergunta(pergunta);
            pergunta.getRegras().add(nova);
        }
        checklist.setUpdatedAt(LocalDateTime.now());

        return toResponse(checklistRepository.save(checklist));
    }

    @Transactional
    public ChecklistAtivoDTO.Response removerPergunta(Long id, Long perguntaId) {
        ChecklistAtivoModel checklist = exigirDoUsuario(id);
        checklist.getPerguntas().remove(exigirPergunta(checklist, perguntaId));
        checklist.setUpdatedAt(LocalDateTime.now());
        return toResponse(checklistRepository.save(checklist));
    }

    /** Reordena as perguntas do checklist conforme a lista de IDs enviada. */
    @Transactional
    public ChecklistAtivoDTO.Response reordenarPerguntas(Long id, List<Long> perguntaIds) {
        ChecklistAtivoModel checklist = exigirDoUsuario(id);
        if (perguntaIds == null || perguntaIds.isEmpty()) {
            throw new BusinessRuleException("Informe a ordem das perguntas.");
        }
        int ordem = 0;
        for (Long perguntaId : perguntaIds) {
            ChecklistAtivoPerguntaModel pergunta = exigirPergunta(checklist, perguntaId);
            pergunta.setOrdem(ordem++);
        }
        checklist.setUpdatedAt(LocalDateTime.now());
        return toResponse(checklistRepository.save(checklist));
    }

    /**
     * Grava as respostas do usuário (Módulos 3 e 5).
     *
     * Por tipo:
     *   • SIM_NAO/NOTA ...... nota digitada (0..nota máxima)
     *   • NUMERO/PERCENTUAL . valor bruto; a NOTA vem da faixa cadastrada
     *   • MULTIPLA_ESCOLHA .. a opção escolhida; a NOTA vem da opção
     *   • TEXTO/LISTA ....... texto livre (informativo, não pontua)
     * Enviar todos os campos nulos LIMPA a resposta.
     */
    @Transactional
    public ChecklistAtivoDTO.Response responder(Long id, ChecklistAtivoDTO.RespostasRequest dto) {
        ChecklistAtivoModel checklist = exigirDoUsuario(id);

        for (ChecklistAtivoDTO.RespostaItem item : dto.respostas()) {
            ChecklistAtivoPerguntaModel pergunta = exigirPergunta(checklist, item.pergunta_id());

            boolean limpar = item.nota() == null && item.valor() == null
                    && (item.texto() == null || item.texto().isBlank());
            if (limpar) {
                limparResposta(pergunta);
                continue;
            }

            BigDecimal notaMaxima = (pergunta.getNotaMaxima() != null) ? pergunta.getNotaMaxima() : BigDecimal.TEN;
            boolean pontua = Boolean.TRUE.equals(pergunta.getContaNoScore());
            TipoPergunta tipo = pergunta.getTipo();

            pergunta.setValorNumerico(null);
            pergunta.setRespostaTexto(null);
            pergunta.setNotaAtribuida(null);

            if (tipo.usaFaixas()) {
                if (item.valor() == null) {
                    throw new BusinessRuleException("Informe o valor da pergunta \"" + pergunta.getTitulo() + "\".");
                }
                pergunta.setValorNumerico(item.valor());
                if (pontua) {
                    List<ScoreCalculator.FaixaCalculavel> faixas = pergunta.getRegras().stream()
                            .map(r -> new ScoreCalculator.FaixaCalculavel(r.getValorMin(), r.getValorMax(), r.getNota(), r.getTexto()))
                            .toList();
                    BigDecimal nota = scoreCalculator.notaDaFaixa(faixas, item.valor());
                    if (nota == null) {
                        throw new BusinessRuleException("Nenhuma faixa da pergunta \"" + pergunta.getTitulo()
                                + "\" cobre o valor informado. Ajuste as faixas ou cadastre uma faixa aberta.");
                    }
                    pergunta.setNotaAtribuida(scoreCalculator.normalizar(nota));
                }
            } else if (tipo.usaOpcoes()) {
                if (item.texto() == null || item.texto().isBlank()) {
                    throw new BusinessRuleException("Escolha uma opção em \"" + pergunta.getTitulo() + "\".");
                }
                List<ScoreCalculator.FaixaCalculavel> opcoes = pergunta.getRegras().stream()
                        .map(r -> new ScoreCalculator.FaixaCalculavel(r.getValorMin(), r.getValorMax(), r.getNota(), r.getTexto()))
                        .toList();
                BigDecimal nota = scoreCalculator.notaDaOpcao(opcoes, item.texto());
                if (nota == null) {
                    throw new BusinessRuleException("A opção \"" + item.texto().trim()
                            + "\" não existe na pergunta \"" + pergunta.getTitulo() + "\".");
                }
                pergunta.setRespostaTexto(item.texto().trim());
                if (pontua) {
                    pergunta.setNotaAtribuida(scoreCalculator.normalizar(nota));
                }
            } else if (tipo == TipoPergunta.SIM_NAO || tipo == TipoPergunta.NOTA) {
                if (item.nota() == null) {
                    throw new BusinessRuleException("Informe a nota da pergunta \"" + pergunta.getTitulo() + "\".");
                }
                validarNota(pergunta, item.nota(), notaMaxima);
                pergunta.setNotaAtribuida(pontua ? scoreCalculator.normalizar(item.nota()) : null);
            } else {
                // TEXTO/LISTA: informativo
                if (item.texto() == null || item.texto().isBlank()) {
                    throw new BusinessRuleException("Informe a resposta da pergunta \"" + pergunta.getTitulo() + "\".");
                }
                pergunta.setRespostaTexto(item.texto().trim());
            }

            pergunta.setObservacao(item.observacao());
            pergunta.setRespondidoEm(LocalDateTime.now());
        }

        checklist.setUpdatedAt(LocalDateTime.now());
        return toResponse(checklistRepository.save(checklist));
    }

    /* ══════════════════════════════════════════════════════════════════
       Helpers
       ══════════════════════════════════════════════════════════════════ */

    private ChecklistAtivoModel exigirDoUsuario(Long id) {
        return checklistRepository.findByIdAndUserId(id, contextoUsuario.id())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Checklist com ID " + id + " não encontrado"));
    }

    private ChecklistAtivoPerguntaModel exigirPergunta(ChecklistAtivoModel checklist, Long perguntaId) {
        return checklist.getPerguntas().stream()
                .filter(p -> p.getId() != null && p.getId().equals(perguntaId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Pergunta com ID " + perguntaId + " não encontrada neste checklist"));
    }

    /** Exatamente um dos dois alvos deve ser informado. */
    private void aplicarAncora(ChecklistAtivoModel checklist, UUID ativoCadastroId, Long ativoId) {
        if ((ativoCadastroId == null) == (ativoId == null)) {
            throw new BusinessRuleException(
                    "Informe o ativo do catálogo (ativo_cadastro_id) OU a posição (ativo_id) — exatamente um dos dois.");
        }
        if (ativoCadastroId != null) {
            AtivoCadastroModel ativo = ativoCadastroRepository.findById(ativoCadastroId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Ativo do catálogo com ID " + ativoCadastroId + " não encontrado"));
            checklist.setAtivoCadastro(ativo);
        } else {
            Long userId = contextoUsuario.id();
            AtivoModel posicao = ativoRepository.findById(ativoId)
                    .orElseThrow(() -> new ResourceNotFoundException("Posição com ID " + ativoId + " não encontrada"));
            boolean dono = posicao.getCarteiraInvestimento() != null
                    && posicao.getCarteiraInvestimento().getUser() != null
                    && posicao.getCarteiraInvestimento().getUser().getId().equals(userId);
            if (!dono) {
                throw new ResourceNotFoundException("Posição com ID " + ativoId + " não encontrada");
            }
            checklist.setAtivo(posicao);
        }
    }

    private int proximaOrdem(Long userId, ChecklistAtivoDTO.Request dto) {
        if (dto != null && dto.ativo_cadastro_id() != null) {
            return (int) checklistRepository.countByUserIdAndAtivoCadastroId(userId, dto.ativo_cadastro_id());
        }
        return 0;
    }

    private void aplicarPerguntas(ChecklistAtivoModel checklist, List<PerguntaDTO.PerguntaRequest> perguntas) {
        if (perguntas == null) {
            return;
        }
        int ordem = 0;
        for (PerguntaDTO.PerguntaRequest req : perguntas) {
            boolean pontua = validador.pontua(req.tipo(), req.conta_no_score());
            ChecklistAtivoPerguntaModel pergunta = new ChecklistAtivoPerguntaModel();
            mapper.copiarDefinicao(pergunta, req, pontua, ordem++);
            pergunta.setChecklist(checklist);

            int ordemRegra = 0;
            for (PerguntaDTO.RegraRequest regra : regrasDe(req)) {
                ChecklistAtivoPerguntaRegraModel nova = mapper.novaRegra(ChecklistAtivoPerguntaRegraModel::new, regra, ordemRegra++);
                nova.setPergunta(pergunta);
                pergunta.getRegras().add(nova);
            }
            checklist.getPerguntas().add(pergunta);
        }
    }

    private List<PerguntaDTO.RegraRequest> regrasDe(PerguntaDTO.PerguntaRequest req) {
        return (req.regras() == null) ? new ArrayList<>() : req.regras();
    }

    private void validarNota(ChecklistAtivoPerguntaModel pergunta, BigDecimal nota, BigDecimal notaMaxima) {
        if (nota.compareTo(BigDecimal.ZERO) < 0 || nota.compareTo(notaMaxima) > 0) {
            throw new BusinessRuleException("A nota de \"" + pergunta.getTitulo() + "\" deve ficar entre 0 e "
                    + notaMaxima.stripTrailingZeros().toPlainString() + ".");
        }
    }

    private void limparResposta(ChecklistAtivoPerguntaModel pergunta) {
        pergunta.setNotaAtribuida(null);
        pergunta.setValorNumerico(null);
        pergunta.setRespostaTexto(null);
        pergunta.setObservacao(null);
        pergunta.setRespondidoEm(null);
    }

    private boolean respondida(ChecklistAtivoPerguntaModel p) {
        return p.getNotaAtribuida() != null
                || p.getValorNumerico() != null
                || (p.getRespostaTexto() != null && !p.getRespostaTexto().isBlank());
    }

    private List<ChecklistAtivoPerguntaModel> ordenar(ChecklistAtivoModel checklist) {
        return mapper.ordenar(checklist.getPerguntas());
    }

    private BigDecimal scoreDe(List<ChecklistAtivoPerguntaModel> perguntas) {
        List<ScoreCalculator.PerguntaCalculavel> calculaveis = perguntas.stream()
                .map(p -> new ScoreCalculator.PerguntaCalculavel(
                        p.getPeso(), p.getNotaMaxima(), Boolean.TRUE.equals(p.getContaNoScore()), p.getNotaAtribuida()))
                .toList();
        return scoreCalculator.score(calculaveis);
    }

    private String chaveDoAtivo(ChecklistAtivoModel c) {
        if (c.getAtivoCadastro() != null) {
            return "cat:" + c.getAtivoCadastro().getId();
        }
        return "pos:" + ((c.getAtivo() != null) ? c.getAtivo().getId() : "?");
    }

    private String tickerDe(ChecklistAtivoModel c) {
        if (c.getAtivoCadastro() != null) {
            return c.getAtivoCadastro().getNome();
        }
        return (c.getAtivo() != null) ? c.getAtivo().getNome() : null;
    }

    private int nz(Integer valor) {
        return (valor != null) ? valor : 0;
    }

    private ChecklistAtivoDTO.Response toResponse(ChecklistAtivoModel checklist) {
        List<ChecklistAtivoPerguntaModel> perguntas = ordenar(checklist);

        List<PerguntaDTO.PerguntaResponse> perguntasDto = perguntas.stream()
                .map(p -> mapper.toPerguntaResponse(p, null, p.getRegras(),
                        p.getNotaAtribuida(), p.getValorNumerico(), p.getRespostaTexto(),
                        p.getObservacao(), p.getRespondidoEm()))
                .toList();

        int respondidas = (int) perguntas.stream().filter(this::respondida).count();
        int pontuadasRespondidas = (int) perguntas.stream()
                .filter(p -> Boolean.TRUE.equals(p.getContaNoScore()) && p.getNotaAtribuida() != null)
                .count();

        return new ChecklistAtivoDTO.Response(
                checklist.getId(),
                (checklist.getAtivoCadastro() != null) ? checklist.getAtivoCadastro().getId() : null,
                (checklist.getAtivo() != null) ? checklist.getAtivo().getId() : null,
                tickerDe(checklist),
                checklist.getNome(),
                checklist.getModeloOrigemId(),
                checklist.getPeso(),
                checklist.getOrdem(),
                checklist.getAtiva(),
                scoreDe(perguntas),
                perguntas.size(),
                respondidas,
                pontuadasRespondidas,
                perguntasDto);
    }
}
