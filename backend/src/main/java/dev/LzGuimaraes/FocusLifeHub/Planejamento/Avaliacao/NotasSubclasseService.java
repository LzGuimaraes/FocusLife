package dev.LzGuimaraes.FocusLifeHub.Planejamento.Avaliacao;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.LzGuimaraes.FocusLifeHub.Exceptions.BusinessRuleException;
import dev.LzGuimaraes.FocusLifeHub.Planejamento.Avaliacao.dto.ChecklistAtivoDTO;
import dev.LzGuimaraes.FocusLifeHub.Planejamento.Avaliacao.dto.ChecklistModeloDTO;
import dev.LzGuimaraes.FocusLifeHub.Planejamento.Avaliacao.dto.NotasSubclasseDTO;
import dev.LzGuimaraes.FocusLifeHub.Planejamento.Avaliacao.dto.PerguntaDTO;
import dev.LzGuimaraes.FocusLifeHub.Planejamento.comum.ContextoUsuario;
import dev.LzGuimaraes.FocusLifeHub.User.UserModel;
import dev.LzGuimaraes.FocusLifeHub.User.UserRepository;

/**
 * NOTAS POR SUBCLASSE — a avaliação no formato curto.
 *
 * O usuário pediu o oposto do checklist por empresa: UM conjunto de perguntas por
 * SUBCLASSE ("Financeiro", "Bens Industriais") e uma página única para dar a nota
 * de cada empresa que cai nessa subclasse. Várias empresas compartilham as mesmas
 * perguntas; a NOTA é de cada uma (é o que permite comparar duas empresas do
 * mesmo setor) e é ela que ordena o aporte.
 *
 * Como a subclasse da Carteira Ideal é recriada a cada save, o vínculo é pelo
 * NOME NORMALIZADO (slug): "Financeiro", "financeiro" e "FINANCEIRO" caem no
 * mesmo checklist padrão.
 *
 * O checklist padrão (5 perguntas de nota 0–10) é criado na primeira vez que a
 * subclasse é aberta — ninguém precisa montar pergunta para começar.
 */
@Service
public class NotasSubclasseService {

    /** Perguntas do checklist padrão: simples, de propósito. */
    private static final List<String> PADRAO = List.of(
            "A empresa dá lucro de forma consistente?",
            "A dívida está sob controle?",
            "Paga dividendos com regularidade?",
            "Você entende bem o negócio dela?",
            "O preço está atrativo hoje?");

    private static final BigDecimal NOTA_MAXIMA = new BigDecimal("10");

    private final ChecklistModeloRepository modeloRepository;
    private final ChecklistAtivoRepository checklistRepository;
    private final ChecklistAtivoService checklistService;
    private final UserRepository userRepository;
    private final ContextoUsuario contextoUsuario;

    public NotasSubclasseService(ChecklistModeloRepository modeloRepository,
                                 ChecklistAtivoRepository checklistRepository,
                                 ChecklistAtivoService checklistService,
                                 UserRepository userRepository,
                                 ContextoUsuario contextoUsuario) {
        this.modeloRepository = modeloRepository;
        this.checklistRepository = checklistRepository;
        this.checklistService = checklistService;
        this.userRepository = userRepository;
        this.contextoUsuario = contextoUsuario;
    }

    /** Tudo o que a página precisa: as perguntas e as notas já dadas. */
    @Transactional
    public NotasSubclasseDTO.Painel painel(String nome, String slugBruto) {
        String slug = slug(slugBruto, nome);
        ChecklistModeloModel modelo = modeloPadrao(nome, slug);

        List<ChecklistModeloPerguntaModel> perguntas = modelo.getPerguntas().stream()
                .sorted(java.util.Comparator.comparing(p -> p.getOrdem() == null ? 0 : p.getOrdem()))
                .toList();

        return new NotasSubclasseDTO.Painel(
                slug,
                nome,
                modelo.getId(),
                modelo.getNome(),
                perguntas.stream()
                        .map(p -> new NotasSubclasseDTO.Pergunta(p.getId(), p.getTitulo(), p.getNotaMaxima(),
                                p.getOrdem() == null ? 0 : p.getOrdem()))
                        .toList(),
                itensDaSubclasse(slug, perguntas.size()));
    }

    /**
     * Salva as notas de todos os ativos da subclasse de uma vez (a página manda
     * tudo junto). Para cada ativo: aproveita o checklist que já existe ou cria
     * um a partir do padrão da subclasse, e grava as notas na ORDEM das perguntas.
     */
    @Transactional
    public NotasSubclasseDTO.SalvarResponse salvar(String nome, String slugBruto,
                                                   NotasSubclasseDTO.SalvarRequest req) {
        String slug = slug(slugBruto, nome);
        ChecklistModeloModel modelo = modeloPadrao(nome, slug);
        List<ChecklistModeloPerguntaModel> perguntas = modelo.getPerguntas().stream()
                .sorted(java.util.Comparator.comparing(p -> p.getOrdem() == null ? 0 : p.getOrdem()))
                .toList();
        if (perguntas.isEmpty()) {
            throw new BusinessRuleException("O checklist desta subclasse não tem perguntas.");
        }
        if (req == null || req.itens() == null || req.itens().isEmpty()) {
            throw new BusinessRuleException("Informe as notas dos ativos.");
        }

        int avaliados = 0;
        for (NotasSubclasseDTO.NotaItem item : req.itens()) {
            if (item.notas() == null || item.notas().stream().allMatch(java.util.Objects::isNull)) {
                continue;   // linha em branco: nada a gravar
            }
            ChecklistAtivoModel checklist = checklistDoAtivo(item, modelo, slug);
            checklistService.responder(checklist.getId(), respostas(checklist, perguntas, item.notas()));
            avaliados++;
        }

        return new NotasSubclasseDTO.SalvarResponse(avaliados, itensDaSubclasse(slug, perguntas.size()));
    }

    /* ── Internos ── */

    /** Notas já gravadas, alinhadas com as perguntas do padrão (null = sem nota). */
    private List<NotasSubclasseDTO.Item> itensDaSubclasse(String slug, int totalPerguntas) {
        List<ChecklistAtivoModel> checklists = checklistRepository
                .findBySubclasseComPerguntas(contextoUsuario.id(), slug);

        List<NotasSubclasseDTO.Item> itens = new ArrayList<>();
        for (ChecklistAtivoModel c : checklists) {
            List<ChecklistAtivoPerguntaModel> perguntas = c.getPerguntas().stream()
                    .sorted(java.util.Comparator.comparing(p -> p.getOrdem() == null ? 0 : p.getOrdem()))
                    .toList();
            List<BigDecimal> notas = new ArrayList<>();
            for (int i = 0; i < totalPerguntas; i++) {
                notas.add((i < perguntas.size()) ? perguntas.get(i).getNotaAtribuida() : null);
            }
            itens.add(new NotasSubclasseDTO.Item(
                    (c.getAtivoCadastro() != null) ? c.getAtivoCadastro().getId() : null,
                    (c.getAtivo() != null) ? c.getAtivo().getId() : null,
                    tickerDe(c),
                    c.getId(),
                    notas,
                    checklistService.scoreDoChecklist(c)));
        }
        return itens;
    }

    private ChecklistAtivoDTO.RespostasRequest respostas(ChecklistAtivoModel checklist,
                                                         List<ChecklistModeloPerguntaModel> modelo,
                                                         List<BigDecimal> notas) {
        List<ChecklistAtivoPerguntaModel> daInstancia = checklist.getPerguntas().stream()
                .sorted(java.util.Comparator.comparing(p -> p.getOrdem() == null ? 0 : p.getOrdem()))
                .toList();
        List<ChecklistAtivoDTO.RespostaItem> respostas = new ArrayList<>();
        for (int i = 0; i < modelo.size() && i < notas.size(); i++) {
            if (i >= daInstancia.size()) {
                break;   // o padrão tem mais perguntas que o snapshot deste ativo
            }
            BigDecimal nota = notas.get(i);
            respostas.add(new ChecklistAtivoDTO.RespostaItem(
                    daInstancia.get(i).getId(), nota, null, null, null));
        }
        return new ChecklistAtivoDTO.RespostasRequest(respostas);
    }

    /** Checklist do ativo na subclasse: o que já existe ou um novo do padrão. */
    private ChecklistAtivoModel checklistDoAtivo(NotasSubclasseDTO.NotaItem item,
                                                 ChecklistModeloModel modelo, String slug) {
        UUID catalogoId = item.ativo_cadastro_id();
        Long ativoId = item.ativo_id();
        if (catalogoId == null && ativoId == null) {
            throw new BusinessRuleException("A nota precisa de um ativo (catálogo ou posição).");
        }

        for (ChecklistAtivoModel existente : checklistRepository
                .findBySubclasseComPerguntas(contextoUsuario.id(), slug)) {
            boolean mesmoCatalogo = catalogoId != null && existente.getAtivoCadastro() != null
                    && catalogoId.equals(existente.getAtivoCadastro().getId());
            boolean mesmaPosicao = ativoId != null && existente.getAtivo() != null
                    && ativoId.equals(existente.getAtivo().getId());
            if (mesmoCatalogo || mesmaPosicao) {
                return existente;
            }
        }

        ChecklistAtivoDTO.Response criado = checklistService.create(new ChecklistAtivoDTO.Request(
                catalogoId, ativoId, modelo.getNome(), modelo.getId(), slug, null, null, null, null));
        return checklistRepository.findById(criado.id())
                .orElseThrow(() -> new BusinessRuleException("Não foi possível abrir o checklist do ativo."));
    }

    /**
     * Checklist padrão da subclasse (cria na primeira vez). Um por usuário +
     * subclasse: o índice único do banco garante que não existam dois padrões.
     */
    private ChecklistModeloModel modeloPadrao(String nome, String slug) {
        var existente = modeloRepository.findBySubclasseComPerguntas(contextoUsuario.id(), slug);
        if (existente.isPresent()) {
            return existente.get();
        }

        Long userId = contextoUsuario.id();
        UserModel user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessRuleException("Usuário não encontrado."));

        ChecklistModeloModel modelo = new ChecklistModeloModel();
        modelo.setUser(user);
        modelo.setNome(nomeExibicao(nome, slug));
        modelo.setDescricao("Checklist padrão da subclasse. Dê uma nota de 0 a 10 para cada pergunta.");
        modelo.setTipo(TipoChecklist.QUALIDADE);
        modelo.setAtiva(true);
        modelo.setSubclasseSlug(slug);

        int ordem = 0;
        for (String titulo : PADRAO) {
            ChecklistModeloPerguntaModel pergunta = new ChecklistModeloPerguntaModel();
            pergunta.setTitulo(titulo);
            pergunta.setTipo(TipoPergunta.NOTA);
            pergunta.setPeso(BigDecimal.ONE);
            pergunta.setNotaMaxima(NOTA_MAXIMA);
            pergunta.setContaNoScore(true);
            pergunta.setObrigatoria(false);
            pergunta.setBloqueadora(false);
            pergunta.setOrdem(ordem++);
            pergunta.setModelo(modelo);
            modelo.getPerguntas().add(pergunta);
        }
        modelo = modeloRepository.save(modelo);
        return modeloRepository.findBySubclasseComPerguntas(userId, slug).orElse(modelo);
    }

    private String nomeExibicao(String nome, String slug) {
        if (nome != null && !nome.isBlank()) {
            return nome.trim();
        }
        return slug;
    }

    /** Nome normalizado da subclasse: sem acento, minúsculo, com hífen. */
    public static String slug(String bruto, String alternativa) {
        String base = (bruto != null && !bruto.isBlank()) ? bruto : alternativa;
        if (base == null) {
            return "";
        }
        return java.text.Normalizer.normalize(base.trim().toLowerCase(), java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
    }

    private String tickerDe(ChecklistAtivoModel c) {
        if (c.getAtivoCadastro() != null) {
            return c.getAtivoCadastro().getNome();
        }
        return (c.getAtivo() != null) ? c.getAtivo().getNome() : null;
    }
}
