package dev.LzGuimaraes.FocusLifeHub.Planejamento.Avaliacao;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.LzGuimaraes.FocusLifeHub.Ativo.AtivoModel;
import dev.LzGuimaraes.FocusLifeHub.Ativo.AtivoRepository;
import dev.LzGuimaraes.FocusLifeHub.Ativo.CategoriaInvestimento;
import dev.LzGuimaraes.FocusLifeHub.Carteira.CarteiraInvestimentoModel;
import dev.LzGuimaraes.FocusLifeHub.Carteira.CarteiraInvestimentoRepository;
import dev.LzGuimaraes.FocusLifeHub.Exceptions.BusinessRuleException;
import dev.LzGuimaraes.FocusLifeHub.Planejamento.Avaliacao.dto.ChecklistAtivoDTO;
import dev.LzGuimaraes.FocusLifeHub.Planejamento.Avaliacao.dto.NotasSubclasseDTO;
import dev.LzGuimaraes.FocusLifeHub.Planejamento.CarteiraIdeal.CarteiraIdealClasseModel;
import dev.LzGuimaraes.FocusLifeHub.Planejamento.CarteiraIdeal.CarteiraIdealClasseRepository;
import dev.LzGuimaraes.FocusLifeHub.Planejamento.CarteiraIdeal.CarteiraIdealSubclasseModel;
import dev.LzGuimaraes.FocusLifeHub.Planejamento.CarteiraIdeal.CarteiraIdealSubclasseRepository;
import dev.LzGuimaraes.FocusLifeHub.Planejamento.MetaAtivo.MetaAtivoModel;
import dev.LzGuimaraes.FocusLifeHub.Planejamento.MetaAtivo.MetaAtivoRepository;
import dev.LzGuimaraes.FocusLifeHub.Planejamento.comum.CarteiraLookup;
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
 * DUAS COISAS QUE NÃO SÃO "EMPRESA" e precisavam do mesmo tratamento:
 *
 *   1. CLASSE SEM SUBCLASSE — cripto, renda fixa, Tesouro e caixinhas não se
 *      dividem em setores, então o balde da avaliação é a PRÓPRIA CLASSE
 *      (`classe_inteira = true`, slug = a classe). Sem isso, um BTC ou uma
 *      caixinha não tinham onde ser avaliados.
 *   2. CHECKLIST PADRÃO POR TIPO — perguntar "paga dividendos?" para um título
 *      público, ou "qual o índice replicado?" para uma ação, não faz sentido.
 *      Cada classe tem o seu conjunto padrão (`PADROES`), com as MESMAS regras
 *      (5 perguntas de nota 0–10), o que mantém a grade da tela igual para todos.
 *
 * As POSIÇÕES SEM TICKER entram na lista como qualquer ativo: o balde é montado a
 * partir das metas (ticker) E das posições (renda fixa/caixinha), não só dos
 * checklists que já existem — antes, quem nunca foi avaliado não aparecia.
 *
 * Como a subclasse da Carteira Ideal é recriada a cada save, o vínculo é pelo
 * NOME NORMALIZADO (slug): "Financeiro", "financeiro" e "FINANCEIRO" caem no
 * mesmo checklist padrão.
 */
@Service
public class NotasSubclasseService {

    private static final BigDecimal NOTA_MAXIMA = new BigDecimal("10");

    /**
     * Checklist padrão de cada TIPO de ativo. Todos com 5 perguntas de 0 a 10
     * (mesma grade na tela); o que muda é o que se pergunta.
     */
    private static final Map<CategoriaInvestimento, List<String>> PADROES = Map.of(
            CategoriaInvestimento.ACOES, List.of(
                    "A empresa dá lucro de forma consistente?",
                    "A dívida está sob controle?",
                    "Paga dividendos com regularidade?",
                    "Você entende bem o negócio dela?",
                    "O preço está atrativo hoje?"),
            CategoriaInvestimento.FIIS, List.of(
                    "A renda do fundo é recorrente e previsível?",
                    "A qualidade dos imóveis e a vacância estão boas?",
                    "A gestão é confiável e tem bom histórico?",
                    "Distribui rendimento com regularidade?",
                    "O preço (P/VP) está atrativo hoje?"),
            CategoriaInvestimento.ETFS, List.of(
                    "Você entende exatamente o índice replicado?",
                    "A taxa de administração é baixa?",
                    "Tem liquidez e patrimônio suficientes?",
                    "A carteira é diversificada o bastante para você?",
                    "O preço está atrativo hoje?"),
            CategoriaInvestimento.CRIPTOMOEDAS, List.of(
                    "Você entende a tese e a utilidade desse ativo?",
                    "O ativo tem adoção, liquidez e tempo de mercado?",
                    "Você domina a custódia (exchange ou autocustódia)?",
                    "A volatilidade cabe no tamanho da posição que você tem?",
                    "O preço está atrativo hoje?"),
            CategoriaInvestimento.RENDA_FIXA, List.of(
                    "O emissor é sólido (banco grande ou instituição conhecida)?",
                    "Tem proteção do FGC ou garantia equivalente?",
                    "A liquidez atende o prazo (dá para resgatar quando precisar)?",
                    "O indexador casa com o objetivo do dinheiro?",
                    "A taxa está atrativa para o prazo hoje?"),
            CategoriaInvestimento.TESOURO_DIRETO, List.of(
                    "O título e o indexador casam com o objetivo do dinheiro?",
                    "O vencimento é compatível com o seu prazo?",
                    "A liquidez diária do Tesouro atende o plano?",
                    "Você está confortável com o risco de marcação a mercado?",
                    "A taxa está atrativa para o prazo hoje?"),
            CategoriaInvestimento.OUTROS, List.of(
                    "Você entende exatamente em que está investindo?",
                    "O risco é compatível com o resto da sua carteira?",
                    "Dá para resgatar quando você precisar?",
                    "A custódia e a instituição são confiáveis?",
                    "A condição atual está atrativa?"));

    private static final Map<CategoriaInvestimento, String> ROTULOS = Map.of(
            CategoriaInvestimento.RENDA_FIXA, "Renda Fixa",
            CategoriaInvestimento.TESOURO_DIRETO, "Tesouro Direto",
            CategoriaInvestimento.ACOES, "Ações",
            CategoriaInvestimento.FIIS, "FIIs",
            CategoriaInvestimento.ETFS, "ETFs",
            CategoriaInvestimento.CRIPTOMOEDAS, "Criptomoedas",
            CategoriaInvestimento.OUTROS, "Outros");

    private final ChecklistModeloRepository modeloRepository;
    private final ChecklistAtivoRepository checklistRepository;
    private final ChecklistAtivoService checklistService;
    private final UserRepository userRepository;
    private final ContextoUsuario contextoUsuario;
    private final CarteiraLookup carteiraLookup;
    private final CarteiraInvestimentoRepository carteiraRepository;
    private final CarteiraIdealClasseRepository classeRepository;
    private final CarteiraIdealSubclasseRepository subclasseRepository;
    private final MetaAtivoRepository metaAtivoRepository;
    private final AtivoRepository ativoRepository;

    public NotasSubclasseService(ChecklistModeloRepository modeloRepository,
                                 ChecklistAtivoRepository checklistRepository,
                                 ChecklistAtivoService checklistService,
                                 UserRepository userRepository,
                                 ContextoUsuario contextoUsuario,
                                 CarteiraLookup carteiraLookup,
                                 CarteiraInvestimentoRepository carteiraRepository,
                                 CarteiraIdealClasseRepository classeRepository,
                                 CarteiraIdealSubclasseRepository subclasseRepository,
                                 MetaAtivoRepository metaAtivoRepository,
                                 AtivoRepository ativoRepository) {
        this.modeloRepository = modeloRepository;
        this.checklistRepository = checklistRepository;
        this.checklistService = checklistService;
        this.userRepository = userRepository;
        this.contextoUsuario = contextoUsuario;
        this.carteiraLookup = carteiraLookup;
        this.carteiraRepository = carteiraRepository;
        this.classeRepository = classeRepository;
        this.subclasseRepository = subclasseRepository;
        this.metaAtivoRepository = metaAtivoRepository;
        this.ativoRepository = ativoRepository;
    }

    /* ── Onde dá para dar notas ── */

    /**
     * Baldes de avaliação da carteira: as subclasses que existem e, quando a
     * classe tem ativos sem subclasse (cripto, renda fixa, caixinhas), a CLASSE
     * inteira. A ordem é a da Carteira Ideal.
     */
    @Transactional(readOnly = true)
    public List<NotasSubclasseDTO.Bucket> buckets(Long carteiraId) {
        Long carteira = carteiraDoUsuario(carteiraId);
        if (carteira == null) {
            return List.of();
        }
        return montarBuckets(carteira).stream()
                .map(b -> new NotasSubclasseDTO.Bucket(b.slug(), b.nome(), b.classe(),
                        rotulo(b.classe()), b.classeInteira(), b.ativos().size()))
                .toList();
    }

    /* ── A página ── */

    /** Tudo o que a página precisa: as perguntas e as notas já dadas. */
    @Transactional
    public NotasSubclasseDTO.Painel painel(String nome, String slugBruto, Long carteiraId) {
        Long carteira = carteiraDoUsuario(carteiraId);
        String slug = slugDoBalde(slugBruto, nome);
        Bucket balde = balde(carteira, slug, nome);
        ChecklistModeloModel modelo = modeloPadrao(balde, slug);
        List<ChecklistModeloPerguntaModel> perguntas = ordenadas(modelo.getPerguntas());

        return new NotasSubclasseDTO.Painel(
                slug,
                balde.nome(),
                balde.classe(),
                rotulo(balde.classe()),
                balde.classeInteira(),
                modelo.getId(),
                modelo.getNome(),
                perguntas.stream()
                        .map(p -> new NotasSubclasseDTO.Pergunta(p.getId(), p.getTitulo(), p.getNotaMaxima(),
                                p.getOrdem() == null ? 0 : p.getOrdem()))
                        .toList(),
                itensDoBalde(balde, slug, perguntas.size()));
    }

    /**
     * Salva as notas de todos os ativos do balde de uma vez (a página manda tudo
     * junto). Para cada ativo: aproveita o checklist que já existe ou cria um a
     * partir do padrão do balde, e grava as notas na ORDEM das perguntas.
     */
    @Transactional
    public NotasSubclasseDTO.SalvarResponse salvar(String nome, String slugBruto, Long carteiraId,
                                                   NotasSubclasseDTO.SalvarRequest req) {
        Long carteira = carteiraDoUsuario(carteiraId);
        String slug = slugDoBalde(slugBruto, nome);
        Bucket balde = balde(carteira, slug, nome);
        ChecklistModeloModel modelo = modeloPadrao(balde, slug);

        List<ChecklistModeloPerguntaModel> perguntas = ordenadas(modelo.getPerguntas());
        if (perguntas.isEmpty()) {
            throw new BusinessRuleException("O checklist deste tipo de ativo não tem perguntas.");
        }
        if (req == null || req.itens() == null || req.itens().isEmpty()) {
            throw new BusinessRuleException("Informe as notas dos ativos.");
        }

        int avaliados = 0;
        for (NotasSubclasseDTO.NotaItem item : req.itens()) {
            if (item.notas() == null || item.notas().stream().allMatch(Objects::isNull)) {
                continue;   // linha em branco: nada a gravar
            }
            ChecklistAtivoModel checklist = checklistDoAtivo(item, modelo, slug);
            checklistService.responder(checklist.getId(), respostas(checklist, perguntas, item.notas()));
            avaliados++;
        }

        return new NotasSubclasseDTO.SalvarResponse(avaliados, itensDoBalde(balde, slug, perguntas.size()));
    }

    /* ── Modelo padrão por TIPO ── */

    private List<String> perguntasPadrao(CategoriaInvestimento classe) {
        return PADROES.getOrDefault(classe, PADROES.get(CategoriaInvestimento.OUTROS));
    }

    /**
     * Checklist padrão do balde (cria na primeira vez). Um por usuário + slug: o
     * índice único do banco garante que não existam dois padrões. O conjunto de
     * perguntas vem do TIPO da classe do balde.
     */
    private ChecklistModeloModel modeloPadrao(Bucket balde, String slug) {
        var existente = modeloRepository.findBySubclasseComPerguntas(contextoUsuario.id(), slug);
        if (existente.isPresent()) {
            return existente.get();
        }

        Long userId = contextoUsuario.id();
        UserModel user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessRuleException("Usuário não encontrado."));

        ChecklistModeloModel modelo = new ChecklistModeloModel();
        modelo.setUser(user);
        modelo.setNome(nomeExibicao(balde, slug));
        modelo.setDescricao("Checklist padrão de " + rotulo(balde.classe())
                + ". Dê uma nota de 0 a 10 para cada pergunta.");
        modelo.setTipo(TipoChecklist.QUALIDADE);
        modelo.setAtiva(true);
        modelo.setSubclasseSlug(slug);

        int ordem = 0;
        for (String titulo : perguntasPadrao(balde.classe())) {
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

    private String nomeExibicao(Bucket balde, String slug) {
        if (balde.nome() != null && !balde.nome().isBlank()) {
            return balde.nome().trim();
        }
        return slug;
    }

    /* ── Baldes ── */

    /** Um balde já resolvido para o banco: subclasse (id) ou classe inteira. */
    private record Bucket(String slug, String nome, CategoriaInvestimento classe,
                          boolean classeInteira, Long subclasseId, List<Alvo> ativos) {}

    /** Um ativo do balde: do catálogo (ticker) ou uma posição sem ticker. */
    private record Alvo(UUID ativoCadastroId, Long ativoId, String ticker, String nome) {}

    /**
     * Monta a lista de baldes da carteira. Cada SUBCLASSE configurada vira um
     * balde; se a classe tiver ativos SEM subclasse (cripto, renda fixa,
     * caixinhas), a CLASSE inteira vira o balde deles. Assim ninguém fica sem
     * lugar para receber nota.
     */
    private List<Bucket> montarBuckets(Long carteiraId) {
        List<CarteiraIdealClasseModel> classes =
                classeRepository.findByCarteiraInvestimentoIdOrderByOrdemAscIdAsc(carteiraId);
        List<Long> classeIds = classes.stream().map(CarteiraIdealClasseModel::getId).toList();
        List<CarteiraIdealSubclasseModel> subclasses = classeIds.isEmpty()
                ? List.of()
                : subclasseRepository.findByClasseIdInOrderByOrdemAscIdAsc(classeIds);

        List<MetaAtivoModel> metas = metaAtivoRepository
                .findByCarteiraInvestimentoIdOrderByOrdemAscIdAsc(carteiraId);
        List<AtivoModel> posicoes = ativoRepository.findByCarteiraInvestimentoId(carteiraId);

        // Ticker que já tem META numa subclasse: a posição dele pertence àquela
        // subclasse, então não pode reaparecer no balde da classe inteira (o
        // mesmo ativo em duas grades confunde na hora de dar a nota).
        Set<UUID> emSubclasse = metas.stream()
                .filter(m -> m.getSubclasse() != null && m.getAtivoCadastro() != null)
                .map(m -> m.getAtivoCadastro().getId())
                .collect(Collectors.toSet());

        List<Bucket> baldes = new ArrayList<>();
        for (CarteiraIdealClasseModel classe : classes) {
            List<CarteiraIdealSubclasseModel> subs = subclasses.stream()
                    .filter(s -> s.getClasse() != null && s.getClasse().getId().equals(classe.getId()))
                    .toList();

            for (CarteiraIdealSubclasseModel sub : subs) {
                baldes.add(new Bucket(
                        slug(sub.getNome(), null),
                        sub.getNome(),
                        classe.getClasse(),
                        false,
                        sub.getId(),
                        alvosDaSubclasse(sub.getId(), metas, posicoes)));
            }

            // Classe inteira: só quando existe ativo dela FORA de qualquer
            // subclasse (cripto, renda fixa, caixinha). Sem isso o balde da
            // classe duplicaria a avaliação de quem já está numa subclasse.
            List<Alvo> semSubclasse = alvosDaClasse(classe.getClasse(), metas, posicoes, emSubclasse);
            if (!semSubclasse.isEmpty()) {
                baldes.add(new Bucket(
                        slugDaClasse(classe.getClasse()),
                        rotulo(classe.getClasse()),
                        classe.getClasse(),
                        true,
                        null,
                        semSubclasse));
            }
        }
        return baldes;
    }

    /** Ativos (tickers e posições) classificados numa subclasse. */
    private List<Alvo> alvosDaSubclasse(Long subclasseId, List<MetaAtivoModel> metas,
                                        List<AtivoModel> posicoes) {
        List<Alvo> alvos = new ArrayList<>();
        for (MetaAtivoModel meta : metas) {
            if (meta.getSubclasse() == null || !subclasseId.equals(meta.getSubclasse().getId())) {
                continue;
            }
            if (meta.getAtivoCadastro() != null) {
                alvos.add(new Alvo(meta.getAtivoCadastro().getId(), null, meta.getAtivoCadastro().getNome(),
                        nomeDaPosicao(meta.getAtivoCadastro().getId(), posicoes)));
            }
        }
        for (AtivoModel posicao : posicoes) {
            if (posicao.getSubclasse() == null || !subclasseId.equals(posicao.getSubclasse().getId())) {
                continue;
            }
            alvos.add(alvoDaPosicao(posicao, posicoes));
        }
        return semDuplicatas(alvos);
    }

    /** Ativos da classe que NÃO estão em nenhuma subclasse. */
    private List<Alvo> alvosDaClasse(CategoriaInvestimento classe, List<MetaAtivoModel> metas,
                                     List<AtivoModel> posicoes, Set<UUID> emSubclasse) {
        List<Alvo> alvos = new ArrayList<>();
        for (MetaAtivoModel meta : metas) {
            if (meta.getClasse() != classe || meta.getSubclasse() != null || meta.getAtivoCadastro() == null) {
                continue;
            }
            alvos.add(new Alvo(meta.getAtivoCadastro().getId(), null, meta.getAtivoCadastro().getNome(),
                    nomeDaPosicao(meta.getAtivoCadastro().getId(), posicoes)));
        }
        for (AtivoModel posicao : posicoes) {
            if (posicao.getCategoriaInvestimento() != classe || posicao.getSubclasse() != null) {
                continue;
            }
            if (posicao.getAtivoCadastro() != null && emSubclasse.contains(posicao.getAtivoCadastro().getId())) {
                continue;   // o ticker já está numa subclasse (via meta)
            }
            alvos.add(alvoDaPosicao(posicao, posicoes));
        }
        return semDuplicatas(alvos);
    }

    /**
     * A posição vira um alvo: com ticker de catálogo, o alvo é o ativo do
     * catálogo (onde a meta e o checklist já vivem); sem ticker, o alvo é a
     * PRÓPRIA posição — é assim que renda fixa, Tesouro e caixinhas entram na
     * avaliação.
     */
    private Alvo alvoDaPosicao(AtivoModel posicao, List<AtivoModel> posicoes) {
        if (posicao.getAtivoCadastro() != null) {
            return new Alvo(posicao.getAtivoCadastro().getId(), null, posicao.getAtivoCadastro().getNome(),
                    nomeDaPosicao(posicao.getAtivoCadastro().getId(), posicoes));
        }
        return new Alvo(null, posicao.getId(), posicao.getNome(), posicao.getNome());
    }

    /** Nome que o usuário deu à posição (mostrado abaixo do ticker na tela). */
    private String nomeDaPosicao(UUID catalogoId, List<AtivoModel> posicoes) {
        if (catalogoId == null) {
            return null;
        }
        return posicoes.stream()
                .filter(p -> p.getAtivoCadastro() != null && catalogoId.equals(p.getAtivoCadastro().getId()))
                .map(AtivoModel::getNome)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    /** O mesmo ticker em duas posições não pode virar duas linhas na grade. */
    private List<Alvo> semDuplicatas(List<Alvo> alvos) {
        Map<String, Alvo> unicos = new LinkedHashMap<>();
        for (Alvo alvo : alvos) {
            String chave = (alvo.ativoCadastroId() != null)
                    ? "cat:" + alvo.ativoCadastroId()
                    : "pos:" + alvo.ativoId();
            unicos.putIfAbsent(chave, alvo);
        }
        return List.copyOf(unicos.values());
    }

    /** Balde do slug pedido; se ele não existir na config, devolve um avulso. */
    private Bucket balde(Long carteiraId, String slug, String nome) {
        if (carteiraId != null) {
            for (Bucket b : montarBuckets(carteiraId)) {
                if (b.slug().equals(slug)) {
                    return b;
                }
            }
        }
        // Subclasse citada na URL mas que não está (mais) na config: mantém a
        // tela funcionando e o checklist já gravado continua acessível.
        return new Bucket(slug, nome != null && !nome.isBlank() ? nome.trim() : slug,
                classeDoSlug(carteiraId, slug), false, null, List.of());
    }

    /**
     * Tipo do ativo a partir do slug: primeiro tenta a CLASSE (baldes de classe
     * inteira: "criptomoedas", "renda-fixa"...) e, se não bater, procura a
     * subclasse na carteira para descobrir a classe dela.
     */
    private CategoriaInvestimento classeDoSlug(Long carteiraId, String slug) {
        for (CategoriaInvestimento c : CategoriaInvestimento.values()) {
            if (slugDaClasse(c).equals(slug)) {
                return c;
            }
        }
        if (carteiraId == null) {
            return CategoriaInvestimento.OUTROS;
        }
        return montarBuckets(carteiraId).stream()
                .filter(b -> b.slug().equals(slug))
                .map(Bucket::classe)
                .findFirst()
                .orElse(CategoriaInvestimento.OUTROS);
    }

    /* ── Itens ── */

    /**
     * Os ativos do balde com as notas já dadas. A lista vem da CARTEIRA (metas +
     * posições), não dos checklists que existem: quem ainda não foi avaliado
     * precisa aparecer na grade para poder receber a nota.
     */
    private List<NotasSubclasseDTO.Item> itensDoBalde(Bucket balde, String slug, int totalPerguntas) {
        Map<String, ChecklistAtivoModel> porChave = new LinkedHashMap<>();
        for (ChecklistAtivoModel c : checklistRepository.findBySubclasseComPerguntas(contextoUsuario.id(), slug)) {
            porChave.put(chaveDe(c.getAtivoCadastro() != null ? c.getAtivoCadastro().getId() : null,
                    c.getAtivo() != null ? c.getAtivo().getId() : null), c);
        }

        List<NotasSubclasseDTO.Item> itens = new ArrayList<>();
        for (Alvo alvo : balde.ativos()) {
            ChecklistAtivoModel checklist = porChave.get(chaveDe(alvo.ativoCadastroId(), alvo.ativoId()));
            List<BigDecimal> notas = null;
            BigDecimal score = null;
            if (checklist != null) {
                List<ChecklistAtivoPerguntaModel> perguntas = ordenadasAtivo(checklist.getPerguntas());
                notas = new ArrayList<>();
                for (int i = 0; i < totalPerguntas; i++) {
                    notas.add((i < perguntas.size()) ? perguntas.get(i).getNotaAtribuida() : null);
                }
                score = checklistService.scoreDoChecklist(checklist);
            }
            itens.add(new NotasSubclasseDTO.Item(
                    alvo.ativoCadastroId(),
                    alvo.ativoId(),
                    alvo.ticker(),
                    alvo.nome(),
                    (checklist != null) ? checklist.getId() : null,
                    notas,
                    score));
        }

        // Quem já tem nota vem primeiro (maior nota na frente); sem nota depois.
        // A nota é a informação útil aqui — o balde é exatamente o conjunto
        // avaliado, então não há "ordem da carteira" a preservar.
        itens.sort(Comparator.comparing((NotasSubclasseDTO.Item i) -> i.score() == null)
                .thenComparing(i -> i.score() == null ? BigDecimal.ZERO : i.score(), Comparator.reverseOrder())
                .thenComparing(i -> i.ticker() == null ? "" : i.ticker()));
        return itens;
    }

    private String chaveDe(UUID catalogoId, Long ativoId) {
        return (catalogoId != null) ? "cat:" + catalogoId : "pos:" + ativoId;
    }

    private List<ChecklistModeloPerguntaModel> ordenadas(List<ChecklistModeloPerguntaModel> perguntas) {
        return perguntas.stream()
                .sorted(Comparator.comparing(p -> p.getOrdem() == null ? 0 : p.getOrdem()))
                .toList();
    }

    private List<ChecklistAtivoPerguntaModel> ordenadasAtivo(List<ChecklistAtivoPerguntaModel> perguntas) {
        return perguntas.stream()
                .sorted(Comparator.comparing(p -> p.getOrdem() == null ? 0 : p.getOrdem()))
                .toList();
    }

    private ChecklistAtivoDTO.RespostasRequest respostas(ChecklistAtivoModel checklist,
                                                         List<ChecklistModeloPerguntaModel> modelo,
                                                         List<BigDecimal> notas) {
        List<ChecklistAtivoPerguntaModel> daInstancia = ordenadasAtivo(checklist.getPerguntas());
        List<ChecklistAtivoDTO.RespostaItem> respostas = new ArrayList<>();
        for (int i = 0; i < modelo.size() && i < notas.size(); i++) {
            if (i >= daInstancia.size()) {
                break;   // o padrão tem mais perguntas que o snapshot deste ativo
            }
            respostas.add(new ChecklistAtivoDTO.RespostaItem(
                    daInstancia.get(i).getId(), notas.get(i), null, null, null));
        }
        return new ChecklistAtivoDTO.RespostasRequest(respostas);
    }

    /** Checklist do ativo no balde: o que já existe ou um novo do padrão. */
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

    /* ── Utilitários ── */

    /** Carteira do usuário (a pedida ou, sem ela, a primeira dele). */
    private Long carteiraDoUsuario(Long carteiraId) {
        if (carteiraId != null) {
            return carteiraLookup.exigirCarteiraDoUsuario(carteiraId).getId();
        }
        Optional<CarteiraInvestimentoModel> primeira = carteiraRepository
                .findByUserId(contextoUsuario.id(), Pageable.ofSize(1)).stream().findFirst();
        return primeira.map(CarteiraInvestimentoModel::getId).orElse(null);
    }

    private static String rotulo(CategoriaInvestimento classe) {
        return ROTULOS.getOrDefault(classe, "Outros");
    }

    /** Slug de um balde de CLASSE INTEIRA (a classe é o balde). */
    public static String slugDaClasse(CategoriaInvestimento classe) {
        return slug(classe.name(), null);
    }

    private String slugDoBalde(String bruto, String alternativa) {
        String slug = slug(bruto, alternativa);
        return slug.isBlank() ? slug("geral", null) : slug;
    }

    /** Nome normalizado: sem acento, minúsculo, com hífen. */
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
}
