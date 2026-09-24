package dev.LzGuimaraes.FocusLifeHub.Planejamento.Aporte;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.LzGuimaraes.FocusLifeHub.Ativo.CategoriaInvestimento;
import dev.LzGuimaraes.FocusLifeHub.Carteira.CarteiraInvestimentoModel;
import dev.LzGuimaraes.FocusLifeHub.Planejamento.Aporte.dto.RankingAportesDTO;
import dev.LzGuimaraes.FocusLifeHub.Planejamento.Aporte.dto.ScoreConfigDTO;
import dev.LzGuimaraes.FocusLifeHub.Planejamento.Avaliacao.ChecklistAtivoService;
import dev.LzGuimaraes.FocusLifeHub.Planejamento.Avaliacao.ScoreCalculator;
import dev.LzGuimaraes.FocusLifeHub.Planejamento.Avaliacao.dto.ChecklistAtivoDTO;
import dev.LzGuimaraes.FocusLifeHub.Planejamento.CarteiraIdeal.CarteiraIdealService;
import dev.LzGuimaraes.FocusLifeHub.Planejamento.CarteiraIdeal.dto.ComparativoResponseDTO;
import dev.LzGuimaraes.FocusLifeHub.Planejamento.CarteiraIdeal.dto.MeusAtivosResponseDTO;
import dev.LzGuimaraes.FocusLifeHub.Planejamento.comum.CarteiraLookup;

/**
 * Motor de aporte — ORQUESTRADOR.
 *
 * Cada pergunta do processo tem UMA fase responsável (§26), nesta ordem:
 *
 *   1. CARTEIRA ATUAL × IDEAL   → {@link CarteiraIdealService#comparativo}   (déficit/excesso por nível)
 *   2. PREÇO                    → {@link PrecoService}                      (atual, médio, oportunidade)
 *   3. ELEGIBILIDADE            → {@link ElegibilidadeService}              (pode receber? por quê não?)
 *   4. PRIORITY SCORE           → {@link PrioridadeService}                 (entre os elegíveis, qual primeiro?)
 *   5. ALOCAÇÃO                 → {@link AlocacaoService}                   (quanto cabe, sem violar tetos)
 *
 * Este service NÃO decide nada por conta própria: ele monta os candidatos com
 * os dados das fases, manda elegibilidade julgar, o ranking ordenar e a
 * alocação distribuir. Assim a tela consegue mostrar exatamente o que o motor
 * usou — e um ativo descartado nunca é "salvo" por um score alto.
 */
@Service
public class AporteService {

    private final CarteiraLookup carteiraLookup;
    private final CarteiraIdealService carteiraIdealService;
    private final ChecklistAtivoService checklistAtivoService;
    private final ScoreConfigService scoreConfigService;
    private final ScoreCalculator scoreCalculator;
    private final ElegibilidadeService elegibilidadeService;
    private final PrioridadeService prioridadeService;
    private final AlocacaoService alocacaoService;
    private final PrecoService precoService;
    private final dev.LzGuimaraes.FocusLifeHub.Planejamento.Historico.AporteRegistroService aporteRegistroService;

    public AporteService(CarteiraLookup carteiraLookup,
                         CarteiraIdealService carteiraIdealService,
                         ChecklistAtivoService checklistAtivoService,
                         ScoreConfigService scoreConfigService,
                         ScoreCalculator scoreCalculator,
                         ElegibilidadeService elegibilidadeService,
                         PrioridadeService prioridadeService,
                         AlocacaoService alocacaoService,
                         PrecoService precoService,
                         dev.LzGuimaraes.FocusLifeHub.Planejamento.Historico.AporteRegistroService aporteRegistroService) {
        this.carteiraLookup = carteiraLookup;
        this.carteiraIdealService = carteiraIdealService;
        this.checklistAtivoService = checklistAtivoService;
        this.scoreConfigService = scoreConfigService;
        this.scoreCalculator = scoreCalculator;
        this.elegibilidadeService = elegibilidadeService;
        this.prioridadeService = prioridadeService;
        this.alocacaoService = alocacaoService;
        this.precoService = precoService;
        this.aporteRegistroService = aporteRegistroService;
    }

    /**
     * Ranking de aporte da carteira.
     *
     * @param valorAporte quando informado, calcula também a distribuição do
     *                    aporte (e os cenários comparativos).
     */
    @Transactional(readOnly = true)
    public RankingAportesDTO.Response ranking(Long carteiraId, BigDecimal valorAporte) {
        CarteiraInvestimentoModel carteira = carteiraLookup.exigirCarteiraDoUsuario(carteiraId);

        ComparativoResponseDTO comparativo = carteiraIdealService.comparativo(carteiraId);
        MeusAtivosResponseDTO meus = carteiraIdealService.meusAtivos(carteiraId);
        ScoreConfigModel config = scoreConfigService.obterOuPadrao();
        List<String> precedencia = precedenciaDaConfig(config);

        // ORÇAMENTO: o aporte informado + as vendas sugeridas (quando o
        // rebalanceamento está ligado). Sem rebalancear, é só o aporte.
        BigDecimal orcamentoTotal = (valorAporte != null) ? valorAporte : BigDecimal.ZERO;

        Avaliacoes avaliacoes = avaliacoes(config);
        AportesRecentes recentes = aportesRecentes(carteiraId);
        PrecoService.IndicePrecoMedio precos = precoService.precoMedio(carteiraId);

        List<AporteCandidato> calculados = candidatos(meus, comparativo, avaliacoes, precos, config,
                recentes, precedencia);

        // ── RANKING (§9, §10) ──
        // Elegível primeiro (o descartado NÃO concorre), depois Priority Score,
        // depois prioridade manual e nome. Um déficit enorme num ativo
        // descartado não muda nada aqui.
        calculados.sort(Comparator
                .comparing(AporteCandidato::elegivel, Comparator.reverseOrder())
                .thenComparing(AporteCandidato::priorityScore, Comparator.reverseOrder())
                // prioridade pode ser null (posição sem meta) — sem nullsLast, o
                // Comparator.sort estoura NullPointerException.
                .thenComparing(AporteCandidato::prioridade, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(AporteCandidato::nome, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));

        // ── ALOCAÇÃO ──
        OrcamentoAporte orcamento = alocacaoService.ratear(calculados, comparativo, orcamentoTotal, config);

        // Sugestões de redução (§19/§21): o que passou do alvo + tolerância.
        List<RankingAportesDTO.RebalanceamentoDTO> rebalanceamento = rebalanceamento(comparativo, calculados, config);
        BigDecimal valorVendas = rebalanceamento.stream()
                .map(RankingAportesDTO.RebalanceamentoDTO::sugerido_vender)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Com o rebalanceamento ligado, a venda sugerida FINANCIA os déficits:
        // o plano é recalculado com o orçamento ampliado. Sem valor de aporte
        // informado não há plano, então o orçamento continua vazio.
        if (valorAporte != null && valorVendas.signum() > 0) {
            orcamentoTotal = orcamentoTotal.add(valorVendas);
            orcamento = alocacaoService.ratear(calculados, comparativo, orcamentoTotal, config);
        }

        List<RankingAportesDTO.Item> itens = new ArrayList<>();
        int posicao = 0;
        for (int i = 0; i < calculados.size(); i++) {
            AporteCandidato c = calculados.get(i);
            BigDecimal sugestao = (orcamento != null) ? orcamento.deCandidato(i) : null;
            // A posição no ranking é só dos ELEGÍVEIS: o descartado aparece na
            // lista (para explicar), mas não ocupa colocação.
            if (c.elegivel()) {
                posicao++;
            }
            itens.add(new RankingAportesDTO.Item(
                    c.elegivel() ? posicao : 0,
                    c.ativoCadastroId(),
                    c.metaId(),
                    c.nome(),
                    c.classe(),
                    c.vinculado(),
                    c.subclasseId(),
                    c.subclasseNome(),
                    c.setorId(),
                    c.setorNome(),
                    c.quality(),
                    c.quality() != null,
                    (c.quality() != null) ? config.pesoDe(TermoScore.QUALITY) : BigDecimal.ZERO,
                    c.momento(),
                    c.momento() != null,
                    c.fator(),
                    c.elegivel(),
                    c.status(),
                    c.motivos(),
                    c.bloqueios(),
                    c.limiteMaximo(),
                    c.limiteAtingido(),
                    c.capacidade(),
                    c.precoAtual(),
                    c.precoMedio(),
                    c.precoMaximoCompra(),
                    c.oportunidadePreco(),
                    c.priorityScore(),
                    c.percentualAtual(),
                    c.percentualIdeal(),
                    c.valorAtual(),
                    c.valorIdeal(),
                    c.deficit(),
                    c.excesso(),
                    c.tolerancia(),
                    c.prioridade(),
                    sugestao,
                    motivo(c, sugestao),
                    c.aportesRecentes(),
                    c.aportesRecentesQtd(),
                    prioridadeService.formula(c.priorityScore(), termosDe(c), config),
                    acaoDe(c, sugestao)));
        }

        List<RankingAportesDTO.ClasseAporteDTO> classes = classesDto(
                comparativo,
                (orcamento != null) ? orcamento.porClasse() : Map.of(),
                (orcamento != null) ? orcamento.porSubclasse() : Map.of(),
                (orcamento != null) ? orcamento.porSetor() : Map.of());

        BigDecimal alocado = (orcamento != null) ? orcamento.alocado() : null;
        BigDecimal naoAlocado = (orcamento != null) ? orcamentoTotal.subtract(alocado) : null;
        List<RankingAportesDTO.Alerta> alertas = alertas(comparativo, calculados, itens, naoAlocado, config);

        int totalElegiveis = (int) calculados.stream().filter(AporteCandidato::elegivel).count();

        return new RankingAportesDTO.Response(
                carteira.getId(),
                carteira.getMoeda(),
                comparativo.valor_total(),
                (valorAporte != null) ? valorAporte : null,
                alocado,
                naoAlocado,
                moeda(valorVendas.doubleValue()),
                (valorAporte != null) ? moeda(orcamentoTotal.doubleValue()) : null,
                config.getRebalancear(),
                config.getTetoAtivoModo(),
                totalElegiveis,
                calculados.size() - totalElegiveis,
                explicacaoNaoAlocado(naoAlocado, calculados, comparativo),
                config.getRedistribuir(),
                config.getEstrategiaAporte(),
                termosDaConfig(config),
                avisos(comparativo, itens, meus, naoAlocado),
                alertas,
                precedenciaLegivel(config),
                cenarios(meus, comparativo, avaliacoes, precos, recentes, valorAporte, config),
                rebalanceamento,
                classes,
                itens);
    }

    /* ── Cálculo dos candidatos (junta as fases) ── */

    /** Aportes dos últimos 30 dias de um item (§24). */
    private record Recente(BigDecimal valor, int quantidade) {}

    /** Índice dos aportes recentes por ticker e por posição. */
    private record AportesRecentes(Map<UUID, Recente> porCatalogo, Map<Long, Recente> porPosicao) {
        Recente de(UUID catalogoId, List<Long> ativoIds) {
            if (catalogoId != null) {
                Recente r = porCatalogo.get(catalogoId);
                if (r != null) {
                    return r;
                }
            }
            for (Long ativoId : ativoIds) {
                Recente r = porPosicao.get(ativoId);
                if (r != null) {
                    return r;
                }
            }
            return new Recente(BigDecimal.ZERO, 0);
        }
    }

    /**
     * Aportes executados nos últimos 30 dias, indexados por ticker e por posição.
     * É o sinal de CONCENTRAÇÃO RECENTE: informa, não decide sozinho.
     */
    private AportesRecentes aportesRecentes(Long carteiraId) {
        Map<UUID, List<BigDecimal>> catValores = new HashMap<>();
        Map<UUID, Integer> catQtd = new HashMap<>();
        Map<Long, List<BigDecimal>> posValores = new HashMap<>();
        Map<Long, Integer> posQtd = new HashMap<>();

        for (dev.LzGuimaraes.FocusLifeHub.Planejamento.Historico.AporteRegistroModel r
                : aporteRegistroService.recentes(carteiraId)) {
            BigDecimal valor = (r.getValor() != null) ? r.getValor() : BigDecimal.ZERO;
            if (r.getAtivoCadastroId() != null) {
                catValores.computeIfAbsent(r.getAtivoCadastroId(), k -> new ArrayList<>()).add(valor);
                catQtd.merge(r.getAtivoCadastroId(), 1, Integer::sum);
            } else if (r.getAtivoId() != null) {
                posValores.computeIfAbsent(r.getAtivoId(), k -> new ArrayList<>()).add(valor);
                posQtd.merge(r.getAtivoId(), 1, Integer::sum);
            }
        }

        Map<UUID, Recente> porCatalogo = new HashMap<>();
        catValores.forEach((id, valores) -> porCatalogo.put(id,
                new Recente(somar(valores), catQtd.getOrDefault(id, 0))));
        Map<Long, Recente> porPosicao = new HashMap<>();
        posValores.forEach((id, valores) -> porPosicao.put(id,
                new Recente(somar(valores), posQtd.getOrDefault(id, 0))));
        return new AportesRecentes(porCatalogo, porPosicao);
    }

    private BigDecimal somar(List<BigDecimal> valores) {
        return valores.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Avaliação consolidada de um ativo: os DOIS eixos (qualidade e momento),
     * o fator de momento (0 a 1) e os critérios eliminatórios reprovados.
     */
    private record Avaliacao(BigDecimal quality, BigDecimal momento, BigDecimal fator, List<String> bloqueios) {
        static Avaliacao vazia() {
            return new Avaliacao(null, null, BigDecimal.ONE, List.of());
        }
    }

    /** Índice das avaliações: por ativo do catálogo (UUID) e por posição (id). */
    private record Avaliacoes(Map<UUID, Avaliacao> porCatalogo, Map<Long, Avaliacao> porPosicao) {

        /** Busca a avaliação do candidato: primeiro pelo ticker, depois pelas posições. */
        Avaliacao de(UUID catalogoId, List<Long> ativoIds) {
            if (catalogoId != null) {
                Avaliacao porTicker = porCatalogo.get(catalogoId);
                if (porTicker != null) {
                    return porTicker;
                }
            }
            for (Long ativoId : ativoIds) {
                Avaliacao porPosicaoId = porPosicao.get(ativoId);
                if (porPosicaoId != null) {
                    return porPosicaoId;
                }
            }
            return Avaliacao.vazia();
        }
    }

    /**
     * Monta os candidatos aplicando, nesta ordem: situação na carteira →
     * preço → ELEGIBILIDADE → PRIORITY SCORE.
     *
     * Nada de score antes da elegibilidade: o Priority Score é calculado para
     * TODOS (para a tela poder comparar), mas quem decide se o ativo concorre é
     * o veredito de elegibilidade.
     */
    private List<AporteCandidato> candidatos(MeusAtivosResponseDTO meus, ComparativoResponseDTO comparativo,
                                             Avaliacoes avaliacoes, PrecoService.IndicePrecoMedio precos,
                                             ScoreConfigModel config, AportesRecentes recentes,
                                             List<String> precedencia) {
        double totalValor = nz(comparativo.valor_total());
        double tol = AlocacaoService.tolerancia(totalValor);

        // 1ª passada: maior déficit/excesso entre os itens COM meta, para
        // normalizar os termos do Priority Score (0..1).
        double maiorDeficit = 0d;
        double maiorExcesso = 0d;
        for (MeusAtivosResponseDTO.MeuAtivoDTO a : meus.ativos()) {
            if (a.meta_id() == null) {
                continue;
            }
            double vAtual = nz(a.valor_atual());
            double vIdeal = valorIdealDe(a.percentual_ideal(), totalValor);
            maiorDeficit = Math.max(maiorDeficit, Math.max(0d, vIdeal - vAtual));
            maiorExcesso = Math.max(maiorExcesso, Math.max(0d, vAtual - vIdeal));
        }
        if (maiorDeficit <= tol) {
            maiorDeficit = 0d;
        }
        if (maiorExcesso <= tol) {
            maiorExcesso = 0d;
        }

        List<AporteCandidato> lista = new ArrayList<>();
        for (MeusAtivosResponseDTO.MeuAtivoDTO a : meus.ativos()) {
            boolean temMeta = a.meta_id() != null;
            double vAtual = nz(a.valor_atual());
            double vIdeal = temMeta ? valorIdealDe(a.percentual_ideal(), totalValor) : 0d;
            double deficit = (temMeta && vIdeal - vAtual > tol) ? vIdeal - vAtual : 0d;
            double excesso = (temMeta && vAtual - vIdeal > tol) ? vAtual - vIdeal : 0d;

            Avaliacao avaliacao = avaliacoes.de(a.ativo_cadastro_id(), a.ativo_ids());
            Recente recente = recentes.de(a.ativo_cadastro_id(), a.ativo_ids());
            BigDecimal tolerancia = (a.tolerancia() != null) ? a.tolerancia() : BigDecimal.ZERO;

            // CAPACIDADE do ativo = déficit + tolerância, limitada pelo limite de
            // concentração. Posição SEM meta própria herda a capacidade do bucket
            // (subclasse/classe) — é o que faz a renda fixa participar.
            double capacidade = temMeta
                    ? tetoDe(vIdeal, tolerancia, vAtual, totalValor, a.limite_maximo())
                    : tetoHerdado(a.subclasse_id(), a.classe(), comparativo, totalValor);
            if ("TETO_ATE_A_CLASSE".equalsIgnoreCase(config.getTetoAtivoModo())) {
                capacidade = Math.max(capacidade, tetoHerdado(a.subclasse_id(), a.classe(), comparativo, totalValor));
            }
            BigDecimal capacidadeMoeda = alocacaoService.moeda(capacidade);

            boolean limiteAtingido = limiteAtingido(vAtual, totalValor, a.limite_maximo());

            // ── PREÇO ──
            // Só ativos COM ticker têm cotação/regra de preço: renda fixa,
            // Tesouro e caixinhas seguem sem essa trava.
            BigDecimal precoAtual = null;
            BigDecimal precoMedio = null;
            BigDecimal precoMaximo = null;
            BigDecimal oportunidade = null;
            if (a.vinculado()) {
                precoAtual = (a.preco_atual() != null) ? a.preco_atual() : null;
                precoMedio = precos.de(a.ativo_cadastro_id(), a.ativo_ids());
                precoMaximo = (a.preco_maximo_compra() != null && a.preco_maximo_compra().signum() > 0)
                        ? a.preco_maximo_compra()
                        : null;
                oportunidade = PrecoService.oportunidade(precoAtual, precoMaximo);
            }
            boolean precoAcimaDoLimite = precoAtual != null && precoMaximo != null
                    && precoAtual.compareTo(precoMaximo) > 0;

            // ── ELEGIBILIDADE ──
            boolean semAvaliacao = avaliacao.quality() == null && avaliacao.momento() == null;
            boolean momentoZero = avaliacao.momento() != null && avaliacao.fator().signum() <= 0;
            boolean nivelSemCapacidade = temMeta
                    ? !temDeficitNoNivel(a, comparativo, tol)
                    : classeSemCapacidade(a.classe(), comparativo, tol);

            ElegibilidadeService.Veredito veredito = elegibilidadeService.avaliar(
                    new ElegibilidadeService.Entrada(
                            avaliacao.bloqueios(), semAvaliacao, limiteAtingido, precoAcimaDoLimite,
                            momentoZero, nivelSemCapacidade, capacidadeMoeda),
                    precedencia);

            // ── PRIORITY SCORE (0..100) ──
            PrioridadeService.Termos termos = new PrioridadeService.Termos(
                    (avaliacao.quality() != null) ? avaliacao.quality().doubleValue() / 100d : null,
                    scoreCalculator.normalizar(deficit, maiorDeficit),
                    scoreCalculator.normalizar(excesso, maiorExcesso),
                    scoreCalculator.normalizarPrioridade(a.prioridade_manual()),
                    (avaliacao.momento() == null) ? null : avaliacao.fator().doubleValue(),
                    (oportunidade != null) ? oportunidade.doubleValue() : null);
            BigDecimal priorityScore = prioridadeService.calcular(termos, config);

            List<String> motivos = new ArrayList<>(veredito.explicacoes());
            if (limiteAtingido) {
                motivos.add("Limite de concentração atingido (" + formatar(a.limite_maximo())
                        + "% da carteira)");
            }

            lista.add(new AporteCandidato(
                    a.ativo_cadastro_id(), a.meta_id(), a.ticker(), a.vinculado(),
                    a.subclasse_id(), a.subclasse_nome(), a.setor_id(), a.setor_nome(),
                    (a.classe() != null) ? a.classe() : CategoriaInvestimento.OUTROS,
                    avaliacao.quality(), avaliacao.momento(), avaliacao.fator(),
                    List.copyOf(avaliacao.bloqueios()),
                    veredito.elegivel(), veredito.status(), List.copyOf(motivos),
                    a.limite_maximo(), limiteAtingido,
                    capacidadeMoeda,
                    precoAtual, precoMedio, precoMaximo, oportunidade,
                    priorityScore,
                    a.percentual_atual(), a.percentual_ideal(),
                    moeda(vAtual), moeda(vIdeal),
                    moeda(deficit), moeda(excesso), tolerancia,
                    a.prioridade_manual(),
                    recente.valor(), recente.quantidade(),
                    termos.qualidade(), termos.deficit(), termos.excesso(),
                    termos.prioridadeManual(), termos.momento(), termos.preco()));
        }
        return lista;
    }

    /**
     * true = a CLASSE (e a subclasse/setor, quando o ativo tem meta) tem
     * déficit: é o que dá capacidade ao nível (§12 — classe acima do alvo não
     * recebe aporte, mesmo com ativos de score alto).
     */
    private boolean temDeficitNoNivel(MeusAtivosResponseDTO.MeuAtivoDTO a,
                                      ComparativoResponseDTO comparativo, double tol) {
        for (ComparativoResponseDTO.ClasseComparativoDTO c : comparativo.classes()) {
            if (c.classe() != a.classe()) {
                continue;
            }
            if (!temDeficit(c.percentual_ideal(), c.tolerancia(), c.percentual_atual(), comparativo, tol)) {
                return false;
            }
            if (a.subclasse_id() == null) {
                return true;
            }
            for (ComparativoResponseDTO.SubclasseComparativoDTO s : c.subclasses()) {
                if (s.id().equals(a.subclasse_id())) {
                    BigDecimal capacidade = alocacaoService.deficitComTolerancia(
                            s.percentual_ideal(), s.tolerancia(), s.percentual_atual(), nz(c.valor_ideal()));
                    if (capacidade.doubleValue() <= tol) {
                        return false;
                    }
                    if (a.setor_id() == null) {
                        return true;
                    }
                    return s.setores().stream()
                            .filter(st -> st.id().equals(a.setor_id()))
                            .findFirst()
                            .map(st -> alocacaoService.deficitComTolerancia(
                                            st.percentual_ideal(), st.tolerancia(), st.percentual_atual(),
                                            nz(s.valor_ideal()))
                                    .doubleValue() > tol)
                            .orElse(true);
                }
            }
            return true;
        }
        return false;
    }

    private boolean classeSemCapacidade(CategoriaInvestimento classe, ComparativoResponseDTO comparativo, double tol) {
        for (ComparativoResponseDTO.ClasseComparativoDTO c : comparativo.classes()) {
            if (c.classe() == classe) {
                return !temDeficit(c.percentual_ideal(), c.tolerancia(), c.percentual_atual(), comparativo, tol);
            }
        }
        return true;   // classe fora da Carteira Ideal: sem alvo, sem capacidade
    }

    private boolean temDeficit(BigDecimal percentualIdeal, BigDecimal tolerancia, BigDecimal percentualAtual,
                               ComparativoResponseDTO comparativo, double tol) {
        double total = nz(comparativo.valor_total());
        return alocacaoService.deficitComTolerancia(percentualIdeal, tolerancia, percentualAtual, total)
                .doubleValue() > tol;
    }

    /** Valor devido de um item: % ideal × total (0 quando o item não tem meta). */
    private double valorIdealDe(BigDecimal percentualIdeal, double total) {
        return (percentualIdeal != null) ? percentualIdeal.doubleValue() / 100d * total : 0d;
    }

    /**
     * Capacidade do ativo: (ideal% + tolerância%) × total − atual, sem nunca
     * passar do limite de concentração (limite% × total − atual), se existir.
     */
    private double tetoDe(double valorIdeal, BigDecimal tolerancia, double valorAtual,
                          double total, BigDecimal limiteMaximo) {
        double alvoComTolerancia = valorIdeal + nz(tolerancia) / 100d * total;
        double capacidade = Math.max(0d, alvoComTolerancia - valorAtual);
        if (limiteMaximo != null && limiteMaximo.signum() > 0) {
            double espacoAteLimite = Math.max(0d, limiteMaximo.doubleValue() / 100d * total - valorAtual);
            capacidade = Math.min(capacidade, espacoAteLimite);
        }
        return capacidade;
    }

    /** true = a posição já chegou ao limite máximo de concentração configurado. */
    private boolean limiteAtingido(double valorAtual, double total, BigDecimal limiteMaximo) {
        if (limiteMaximo == null || limiteMaximo.signum() <= 0 || total <= 0d) {
            return false;
        }
        return valorAtual / total * 100d >= limiteMaximo.doubleValue() - AlocacaoService.tolerancia(total);
    }

    /**
     * Capacidade de uma posição SEM meta própria: o déficit da SUBCLASSE a que ela
     * pertence (ou da classe, quando não está em nenhuma subclasse com alvo).
     *
     * É o que faz a renda fixa/caixinha participar: o alvo não é dela, é da
     * subclasse — e a capacidade é quanto ainda falta para a subclasse.
     */
    private double tetoHerdado(Long subclasseId, CategoriaInvestimento classe,
                               ComparativoResponseDTO comparativo, double total) {
        for (ComparativoResponseDTO.ClasseComparativoDTO c : comparativo.classes()) {
            if (subclasseId != null) {
                for (ComparativoResponseDTO.SubclasseComparativoDTO s : c.subclasses()) {
                    if (subclasseId.equals(s.id())) {
                        // O percentual da subclasse é uma fatia da CLASSE, então a
                        // tolerância dela também é medida sobre o valor da classe.
                        double alvo = nz(s.valor_ideal()) + nz(s.tolerancia()) / 100d * nz(c.valor_ideal());
                        return Math.max(0d, alvo - nz(s.valor_atual()));
                    }
                }
            }
            if (c.classe() == classe) {
                return alocacaoService.deficitComTolerancia(c.percentual_ideal(), c.tolerancia(),
                        c.percentual_atual(), total).doubleValue();
            }
        }
        return 0d;
    }

    /** Termos normalizados do candidato (para a fórmula aberta). */
    private PrioridadeService.Termos termosDe(AporteCandidato c) {
        return new PrioridadeService.Termos(c.qualityNorm(), c.deficitNorm(), c.excessoNorm(),
                c.prioridadeNorm(), c.momentoNorm(), c.oportunidadeNorm());
    }

    /**
     * Ação recomendada: separa "manter" de "aportar".
     *
     * Descartado na elegibilidade → NÃO APORTAR (o déficit continua existindo).
     * Elegível com sugestão > 0 → APORTAR. Elegível sem sugestão: MANTER (já no
     * alvo) ou AVALIAR (ainda sem avaliação cadastrada).
     */
    private RankingAportesDTO.AcaoAtivo acaoDe(AporteCandidato c, BigDecimal sugestao) {
        if (!c.elegivel()) {
            return RankingAportesDTO.AcaoAtivo.NAO_APORTAR;
        }
        if (sugestao != null) {
            return (sugestao.signum() > 0)
                    ? RankingAportesDTO.AcaoAtivo.APORTAR
                    : RankingAportesDTO.AcaoAtivo.MANTER;
        }
        if (c.status() == StatusElegibilidade.SEM_AVALIACAO) {
            return RankingAportesDTO.AcaoAtivo.AVALIAR;
        }
        return (c.capacidade().signum() > 0)
                ? RankingAportesDTO.AcaoAtivo.AVALIAR
                : RankingAportesDTO.AcaoAtivo.MANTER;
    }

    /** Explicação objetiva da decisão, para cada ativo (§18). */
    private String motivo(AporteCandidato c, BigDecimal sugestao) {
        if (!c.elegivel()) {
            List<String> razoes = c.motivos().isEmpty()
                    ? List.of(c.status().getLabel() + ": " + c.status().getDescricao())
                    : c.motivos();
            return "Não aportar: " + String.join(" ", razoes)
                    + " O déficit continua existindo na carteira — o ativo volta a concorrer "
                    + "quando a regra for atendida.";
        }
        if (sugestao == null) {
            return (c.status() == StatusElegibilidade.SEM_AVALIACAO)
                    ? "Elegível, mas sem avaliação cadastrada: a nota de qualidade não entrou no Priority Score."
                    : "Elegível. Informe o valor do aporte para ver quanto caberia a este ativo.";
        }
        if (sugestao.signum() <= 0) {
            if (c.capacidade().signum() <= 0) {
                return "Nada: já está no alvo (capacidade esgotada).";
            }
            return "Nada: não sobrou valor elegível para " + c.classe()
                    + " neste aporte (a classe recebeu o que o déficit dela permitia).";
        }
        List<String> partes = new ArrayList<>();
        partes.add("Recebe " + formatar(sugestao) + " de uma capacidade de " + formatar(c.capacidade()));
        partes.add("déficit " + formatar(c.deficit()) + " + tolerância " + formatar(c.tolerancia()) + "%");
        partes.add("Priority Score " + formatar(c.priorityScore()));
        if (c.precoMaximoCompra() != null) {
            partes.add("preço " + formatar(c.precoAtual()) + " até o limite de " + formatar(c.precoMaximoCompra())
                    + " (oportunidade " + percentualDe(c.oportunidadePreco()) + ")");
        }
        if (c.precoMedio() != null) {
            partes.add("preço médio " + formatar(c.precoMedio()));
        }
        if (c.aportesRecentesQtd() > 0) {
            partes.add("já recebeu " + formatar(c.aportesRecentes()) + " em " + c.aportesRecentesQtd()
                    + " aporte(s) nos últimos 30 dias");
        }
        return String.join(" · ", partes) + ".";
    }

    /** Onde entra o dinheiro: uma linha por classe (e suas subclasses). */
    private List<RankingAportesDTO.ClasseAporteDTO> classesDto(
            ComparativoResponseDTO comparativo,
            Map<CategoriaInvestimento, BigDecimal> sugeridoPorClasse,
            Map<Long, BigDecimal> sugeridoPorSubclasse,
            Map<Long, BigDecimal> sugeridoPorSetor) {

        double total = nz(comparativo.valor_total());
        List<RankingAportesDTO.ClasseAporteDTO> classes = new ArrayList<>();
        for (ComparativoResponseDTO.ClasseComparativoDTO c : comparativo.classes()) {
            BigDecimal sugerido = sugeridoPorClasse.getOrDefault(c.classe(), moeda(0d));
            List<RankingAportesDTO.SubclasseAporteDTO> subs = c.subclasses().stream()
                    .map(s -> {
                        BigDecimal sugeridoSub = sugeridoPorSubclasse.getOrDefault(s.id(), moeda(0d));
                        List<RankingAportesDTO.SetorAporteDTO> setores = s.setores().stream()
                                .map(st -> new RankingAportesDTO.SetorAporteDTO(
                                        st.id(), st.nome(), st.percentual_atual(), st.percentual_ideal(),
                                        st.valor_atual(), st.valor_ideal(), st.deficit(), st.excesso(),
                                        st.tolerancia(), st.limite_maximo(),
                                        statusDe(st.percentual_atual(), st.percentual_ideal(),
                                                st.tolerancia(), st.limite_maximo()),
                                        sugeridoPorSetor.getOrDefault(st.id(), moeda(0d))))
                                .toList();
                        return new RankingAportesDTO.SubclasseAporteDTO(
                                s.id(), s.nome(), s.percentual_atual(), s.percentual_ideal(),
                                s.valor_atual(), s.valor_ideal(), s.deficit(), s.excesso(),
                                s.tolerancia(), s.limite_maximo(),
                                statusDe(s.percentual_atual(), s.percentual_ideal(), s.tolerancia(), s.limite_maximo()),
                                sugeridoSub,
                                // Nada foi para esta subclasse: ou a verba da classe foi
                                // para as outras subclasses, ou as posições dela ainda
                                // não estão atribuídas a ela (o alerta SEM_SUBCLASSE avisa).
                                (sugeridoSub.signum() <= 0 && sugerido.signum() > 0)
                                        ? "Neste aporte nada foi direcionado a posições desta subclasse."
                                        : null,
                                setores);
                    })
                    .toList();
            classes.add(new RankingAportesDTO.ClasseAporteDTO(
                    c.classe(), c.percentual_atual(), c.percentual_ideal(),
                    c.valor_atual(), c.valor_ideal(), c.deficit(), c.excesso(),
                    c.tolerancia(), c.limite_maximo(),
                    statusDe(c.percentual_atual(), c.percentual_ideal(), c.tolerancia(), c.limite_maximo()),
                    sugerido,
                    motivoDaClasse(c, sugerido, total),
                    subs));
        }
        return classes;
    }

    /** Status de equilíbrio de um nível frente à tolerância configurada. */
    private RankingAportesDTO.StatusNivel statusDe(BigDecimal percentualAtual, BigDecimal percentualIdeal,
                                                   BigDecimal tolerancia, BigDecimal limiteMaximo) {
        if (percentualIdeal == null || percentualIdeal.signum() <= 0) {
            return RankingAportesDTO.StatusNivel.SEM_ALVO;
        }
        double atual = (percentualAtual != null) ? percentualAtual.doubleValue() : 0d;
        double tol = (tolerancia != null) ? tolerancia.doubleValue() : 0d;
        if (limiteMaximo != null && limiteMaximo.signum() > 0 && atual >= limiteMaximo.doubleValue()) {
            return RankingAportesDTO.StatusNivel.ACIMA;
        }
        if (atual > percentualIdeal.doubleValue() + tol) {
            return RankingAportesDTO.StatusNivel.ACIMA;
        }
        if (atual < percentualIdeal.doubleValue() - tol) {
            return RankingAportesDTO.StatusNivel.ABAIXO;
        }
        return RankingAportesDTO.StatusNivel.EQUILIBRADO;
    }

    /** Por que a classe recebeu (ou não) parte do aporte. */
    private String motivoDaClasse(ComparativoResponseDTO.ClasseComparativoDTO c, BigDecimal sugerido, double total) {
        BigDecimal limite = c.limite_maximo();
        BigDecimal atual = c.percentual_atual();
        if (limite != null && limite.signum() > 0 && atual != null
                && atual.doubleValue() >= limite.doubleValue()) {
            return "Não recebe: limite de concentração de " + formatar(limite) + "% atingido.";
        }
        BigDecimal falta = alocacaoService.deficitComTolerancia(c.percentual_ideal(), c.tolerancia(), atual, total);
        if (falta.signum() <= 0) {
            return "Não recebe: está no alvo (dentro da tolerância de " + formatar(c.tolerancia()) + "%).";
        }
        if (sugerido.signum() <= 0) {
            return "Tem déficit de " + formatar(falta)
                    + ", mas nenhum ativo ELEGÍVEL da classe neste momento (preço/limite/critério).";
        }
        return "Recebe no máximo o déficit da classe: " + formatar(falta)
                + " (ideal + tolerância − atual).";
    }

    /**
     * REBALANCEAMENTO: quanto cada nível tem ACIMA do alvo + tolerância e
     * poderia financiar os déficits. Só o nível mais específico que explica o
     * excesso é sugerido (o filho abate o pai), para não contar duas vezes o
     * mesmo dinheiro.
     */
    private List<RankingAportesDTO.RebalanceamentoDTO> rebalanceamento(
            ComparativoResponseDTO comparativo, List<AporteCandidato> candidatos, ScoreConfigModel config) {
        if (config.getRebalancear() == null || !config.getRebalancear()) {
            return List.of();
        }
        double total = nz(comparativo.valor_total());
        double tol = AlocacaoService.tolerancia(total);
        List<RankingAportesDTO.RebalanceamentoDTO> itens = new ArrayList<>();

        // 1) NÍVEL ATIVO — só quem tem meta própria (os sem meta não têm alvo seu).
        Map<CategoriaInvestimento, Double> abatidoNaClasse = new HashMap<>();
        Map<Long, Double> abatidoNaSubclasse = new HashMap<>();
        for (AporteCandidato c : candidatos) {
            if (c.metaId() == null) {
                continue;
            }
            double excesso = c.excesso().doubleValue() - nz(c.tolerancia()) / 100d * total;
            if (excesso <= tol) {
                continue;
            }
            itens.add(new RankingAportesDTO.RebalanceamentoDTO(
                    "ATIVO", c.nome(), c.classe(), c.percentualAtual(), c.percentualIdeal(),
                    c.excesso(), moeda(excesso),
                    "Acima do alvo + tolerância de " + formatar(c.tolerancia()) + "%."));
            abatidoNaClasse.merge(c.classe(), excesso, Double::sum);
            if (c.subclasseId() != null) {
                abatidoNaSubclasse.merge(c.subclasseId(), excesso, Double::sum);
            }
        }

        // 2) NÍVEIS SUBCLASSE / SETOR / CLASSE — o que os filhos não explicaram.
        for (ComparativoResponseDTO.ClasseComparativoDTO cl : comparativo.classes()) {
            double restanteClasse = excessoEmReais(nz(cl.valor_atual()), nz(cl.valor_ideal()),
                    cl.tolerancia(), total) - abatidoNaClasse.getOrDefault(cl.classe(), 0d);

            for (ComparativoResponseDTO.SubclasseComparativoDTO s : cl.subclasses()) {
                double excessoSub = excessoEmReais(nz(s.valor_atual()), nz(s.valor_ideal()),
                        s.tolerancia(), nz(cl.valor_ideal()));

                for (ComparativoResponseDTO.SetorComparativoDTO st : s.setores()) {
                    double excessoSetor = excessoEmReais(nz(st.valor_atual()), nz(st.valor_ideal()),
                            st.tolerancia(), nz(s.valor_ideal()));
                    if (excessoSetor > tol && abatidoNaSubclasse.getOrDefault(s.id(), 0d) <= tol) {
                        itens.add(new RankingAportesDTO.RebalanceamentoDTO(
                                "SETOR", st.nome(), cl.classe(), st.percentual_atual(),
                                st.percentual_ideal(), st.excesso(), moeda(excessoSetor),
                                "Setor acima do alvo dentro da subclasse " + s.nome() + "."));
                        excessoSub -= excessoSetor;
                    }
                }

                double restanteSub = excessoSub - abatidoNaSubclasse.getOrDefault(s.id(), 0d);
                if (restanteSub > tol) {
                    itens.add(new RankingAportesDTO.RebalanceamentoDTO(
                            "SUBCLASSE", s.nome(), cl.classe(), s.percentual_atual(),
                            s.percentual_ideal(), s.excesso(), moeda(restanteSub),
                            "Subclasse acima do alvo, sem um ativo específico para reduzir."));
                    restanteClasse -= restanteSub;
                }
            }

            if (restanteClasse > tol) {
                itens.add(new RankingAportesDTO.RebalanceamentoDTO(
                        "CLASSE", cl.classe().name(), cl.classe(), cl.percentual_atual(),
                        cl.percentual_ideal(), cl.excesso(), moeda(restanteClasse),
                        "Classe acima do alvo: reduza dentro dela para financiar o que falta."));
            }
        }

        itens.sort(Comparator.comparing(RankingAportesDTO.RebalanceamentoDTO::sugerido_vender,
                Comparator.reverseOrder()));
        return itens;
    }

    /** Quanto um nível passou do alvo + tolerância, em reais. */
    private double excessoEmReais(double valorAtual, double valorIdeal,
                                  BigDecimal tolerancia, double baseDaTolerancia) {
        double alvo = valorIdeal + nz(tolerancia) / 100d * baseDaTolerancia;
        return Math.max(0d, valorAtual - alvo);
    }

    /** Quality Score e NOTA DE MOMENTO por ativo, com o fator de momento já calculado. */
    private Avaliacoes avaliacoes(ScoreConfigModel config) {
        Map<UUID, Avaliacao> porCatalogo = new HashMap<>();
        Map<Long, Avaliacao> porPosicao = new HashMap<>();

        for (ChecklistAtivoDTO.AtivoAvaliado a : checklistAtivoService.resumoPorAtivo()) {
            Avaliacao avaliacao = new Avaliacao(
                    a.quality_score(),
                    a.momento_score(),
                    scoreCalculator.fatorMomento(a.momento_score(),
                            config.getMomentoFaixa1(), config.getMomentoFaixa2(),
                            config.getMomentoFaixa3(), config.getMomentoFaixa4()),
                    (a.bloqueios() != null) ? a.bloqueios() : List.of());
            if (a.ativo_cadastro_id() != null) {
                porCatalogo.putIfAbsent(a.ativo_cadastro_id(), avaliacao);
            } else if (a.ativo_id() != null) {
                porPosicao.putIfAbsent(a.ativo_id(), avaliacao);
            }
        }
        return new Avaliacoes(porCatalogo, porPosicao);
    }

    /** Alertas — o que o usuário precisa saber ANTES de decidir. */
    private List<RankingAportesDTO.Alerta> alertas(ComparativoResponseDTO comparativo,
                                                   List<AporteCandidato> candidatos,
                                                   List<RankingAportesDTO.Item> itens,
                                                   BigDecimal naoAlocado,
                                                   ScoreConfigModel config) {
        List<RankingAportesDTO.Alerta> alertas = new ArrayList<>();

        for (ComparativoResponseDTO.ClasseComparativoDTO c : comparativo.classes()) {
            RankingAportesDTO.StatusNivel status = statusDe(c.percentual_atual(), c.percentual_ideal(),
                    c.tolerancia(), c.limite_maximo());
            if (status == RankingAportesDTO.StatusNivel.ACIMA) {
                alertas.add(new RankingAportesDTO.Alerta("CLASSE_ACIMA", "Classe " + c.classe()
                        + " acima do alvo: " + formatar(c.percentual_atual()) + "% vs "
                        + formatar(c.percentual_ideal()) + "% ideal."));
            } else if (status == RankingAportesDTO.StatusNivel.ABAIXO) {
                alertas.add(new RankingAportesDTO.Alerta("CLASSE_ABAIXO", "Classe " + c.classe()
                        + " abaixo do alvo: falta " + formatar(c.deficit()) + "."));
            }
            for (ComparativoResponseDTO.SubclasseComparativoDTO s : c.subclasses()) {
                if (statusDe(s.percentual_atual(), s.percentual_ideal(), s.tolerancia(), s.limite_maximo())
                        == RankingAportesDTO.StatusNivel.ACIMA) {
                    alertas.add(new RankingAportesDTO.Alerta("SUBCLASSE_ACIMA", "Subclasse " + s.nome()
                            + " (" + c.classe() + ") acima do alvo: " + formatar(s.percentual_atual())
                            + "% vs " + formatar(s.percentual_ideal()) + "%."));
                }
            }
        }

        // ── DESCARTES: o motivo sempre acompanha o descarte, nunca escondido ──
        for (AporteCandidato c : candidatos) {
            if (c.limiteAtingido()) {
                alertas.add(new RankingAportesDTO.Alerta("ATIVO_LIMITE", c.nome()
                        + " atingiu o limite de concentração de " + formatar(c.limiteMaximo())
                        + "% — não recebe novos aportes."));
            }
            if (c.status() == StatusElegibilidade.PRECO_ACIMA_DO_LIMITE) {
                alertas.add(new RankingAportesDTO.Alerta("PRECO_ACIMA", c.nome() + ": preço atual "
                        + formatar(c.precoAtual()) + " acima do preço máximo de compra de "
                        + formatar(c.precoMaximoCompra()) + " — fora do ranking de aporte."));
            }
            if (!c.bloqueios().isEmpty()) {
                alertas.add(new RankingAportesDTO.Alerta("BLOQUEIO", c.nome() + ": "
                        + String.join("; ", c.bloqueios()) + "."));
            }
            if (c.status() == StatusElegibilidade.MOMENTO_ZERO) {
                alertas.add(new RankingAportesDTO.Alerta("MOMENTO_ZERO", c.nome()
                        + ": fator de momento 0 (não é hora de aportar, segundo o seu checklist de momento)."));
            }
        }

        long semAvaliacao = candidatos.stream()
                .filter(c -> c.status() == StatusElegibilidade.SEM_AVALIACAO)
                .count();
        if (semAvaliacao > 0) {
            alertas.add(new RankingAportesDTO.Alerta("SEM_AVALIACAO", semAvaliacao
                    + " item(ns) sem avaliação: o termo de qualidade não entrou na conta deles (nada foi zerado). "
                    + "Para bloquear, marque um critério eliminatório no checklist."));
        }

        long semTickerSemSubclasse = itens.stream()
                .filter(i -> !i.vinculado() && i.subclasse_id() == null
                        && classeTemSubclasseComAlvo(comparativo, i.classe()))
                .count();
        if (semTickerSemSubclasse > 0) {
            alertas.add(new RankingAportesDTO.Alerta("SEM_SUBCLASSE", semTickerSemSubclasse
                    + " posição(ões) sem ticker fora de qualquer subclasse: em Carteira Ideal, escolha a subclasse "
                    + "delas. Sem isso o motor não sabe qual FATIA da classe elas representam (o valor já conta no "
                    + "total e no alvo da classe)."));
        }

        long semPrecoMaximo = candidatos.stream()
                .filter(c -> c.vinculado() && c.precoMaximoCompra() == null)
                .count();
        if (semPrecoMaximo > 0) {
            alertas.add(new RankingAportesDTO.Alerta("SEM_PRECO_MAXIMO", semPrecoMaximo
                    + " ativo(s) com ticker sem preço máximo de compra definido: eles concorrem sem regra de "
                    + "preço. Defina o teto de compra na meta para o motor descartar o que estiver caro."));
        }

        // Concentração RECENTE — quantas vezes o dinheiro já foi para o mesmo item.
        for (AporteCandidato c : candidatos) {
            if (c.aportesRecentesQtd() >= 2) {
                alertas.add(new RankingAportesDTO.Alerta("APORTE_RECENTE", c.nome() + " já recebeu "
                        + formatar(c.aportesRecentes()) + " em " + c.aportesRecentesQtd()
                        + " aporte(s) nos últimos 30 dias — confira antes de repetir."));
            }
        }

        if (naoAlocado != null && naoAlocado.signum() > 0) {
            alertas.add(new RankingAportesDTO.Alerta("NAO_ALOCADO", formatar(naoAlocado)
                    + " sem destino neste aporte"
                    + (Boolean.FALSE.equals(config.getRedistribuir())
                            ? " (redistribuição desligada)."
                            : ": os ativos restantes não atendem às condições para receber aporte.")));
        }
        return alertas;
    }

    /** true = a classe tem alguma subclasse com alvo > 0 (existe subdivisão a respeitar). */
    private boolean classeTemSubclasseComAlvo(ComparativoResponseDTO comparativo, CategoriaInvestimento classe) {
        for (ComparativoResponseDTO.ClasseComparativoDTO c : comparativo.classes()) {
            if (c.classe() == classe) {
                return c.subclasses().stream().anyMatch(s -> nz(s.percentual_ideal()) > 0d);
            }
        }
        return false;
    }

    /** Ordem das travas configurada pelo usuário, como lista de ids. */
    private List<String> precedenciaDaConfig(ScoreConfigModel config) {
        String bruta = (config.getPrecedencia() != null) ? config.getPrecedencia()
                : "BLOQUEIO,LIMITE,PRECO,CLASSE,SUBCLASSE,SETOR,TETO_ATIVO,MOMENTO,SCORE";
        List<String> ids = new ArrayList<>();
        for (String parte : bruta.split(",")) {
            String id = parte.trim().toUpperCase();
            if (!id.isEmpty() && !ids.contains(id)) {
                ids.add(id);
            }
        }
        return ids;
    }

    /**
     * Ordem das travas em texto, para a tela. É a MESMA ordem que decide qual
     * motivo aparece como status do ativo descartado.
     */
    private List<String> precedenciaLegivel(ScoreConfigModel config) {
        Map<String, String> labels = Map.of(
                "BLOQUEIO", "Critério eliminatório (bloqueia)",
                "LIMITE", "Limite máximo de concentração (bloqueia)",
                "PRECO", "Preço máximo de compra (bloqueia)",
                "CLASSE", "Déficit da CLASSE (define quanto entra no nível)",
                "SUBCLASSE", "Déficit da SUBCLASSE (dentro da classe)",
                "SETOR", "Déficit do SETOR (dentro da subclasse)",
                "TETO_ATIVO", "Capacidade do ATIVO (déficit + tolerância)",
                "MOMENTO", "Fator de momento (0 a 1)",
                "SCORE", "Priority Score e estratégia (divide dentro do nível)");

        List<String> lista = new ArrayList<>();
        int i = 1;
        for (String id : precedenciaDaConfig(config)) {
            String label = labels.get(id);
            if (label != null) {
                lista.add(i++ + ". " + label);
            }
        }
        return lista;
    }

    /** Explicação legível do valor não alocado (§14) — nunca apenas "R$ X não alocado". */
    private String explicacaoNaoAlocado(BigDecimal naoAlocado, List<AporteCandidato> candidatos,
                                        ComparativoResponseDTO comparativo) {
        if (naoAlocado == null || naoAlocado.signum() <= 0) {
            return null;
        }
        long descartados = candidatos.stream().filter(c -> !c.elegivel()).count();
        long elegiveisNoLimite = candidatos.stream().filter(c -> c.elegivel() && c.capacidade().signum() <= 0).count();
        long classesAcima = comparativo.classes().stream()
                .filter(c -> statusDe(c.percentual_atual(), c.percentual_ideal(), c.tolerancia(), c.limite_maximo())
                        == RankingAportesDTO.StatusNivel.ACIMA)
                .count();

        List<String> razoes = new ArrayList<>();
        if (descartados > 0) {
            razoes.add(descartados + " ativo(s) foram descartados na elegibilidade (preço acima do limite, "
                    + "critério eliminatório, limite de concentração ou momento zero)");
        }
        if (classesAcima > 0) {
            razoes.add(classesAcima + " classe(s) já estão acima do alvo");
        }
        if (elegiveisNoLimite > 0) {
            razoes.add(elegiveisNoLimite + " ativo(s) elegível(is) já atingiram a própria capacidade");
        }
        if (razoes.isEmpty()) {
            razoes.add("nenhuma classe/ativo elegível tinha espaço para receber mais");
        }
        return formatar(naoAlocado) + " não foram alocados porque " + String.join(", ", razoes)
                + ". O dinheiro não foi empurrado para quem não atende às condições de aporte.";
    }

    private String formatar(BigDecimal valor) {
        if (valor == null) {
            return "0,00";
        }
        return valor.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private String percentualDe(BigDecimal fracao) {
        if (fracao == null) {
            return "—";
        }
        return fracao.multiply(new BigDecimal("100")).setScale(2, RoundingMode.HALF_UP)
                .stripTrailingZeros().toPlainString() + "%";
    }

    private List<ScoreConfigDTO.Termo> termosDaConfig(ScoreConfigModel config) {
        List<ScoreConfigDTO.Termo> termos = new ArrayList<>();
        for (TermoScore termo : TermoScore.values()) {
            termos.add(new ScoreConfigDTO.Termo(
                    termo, termo.getLabel(), termo.getDescricao(),
                    config.pesoDe(termo), termo.getPesoPadrao()));
        }
        return termos;
    }

    private List<String> avisos(ComparativoResponseDTO comparativo, List<RankingAportesDTO.Item> itens,
                                MeusAtivosResponseDTO meus, BigDecimal naoAlocado) {
        List<String> avisos = new ArrayList<>();
        boolean temClasse = !comparativo.classes().isEmpty();
        boolean temDeficitDeClasse = comparativo.classes().stream().anyMatch(c -> nz(c.deficit()) > 0d);

        if (!temClasse) {
            avisos.add("Defina a Carteira Ideal desta carteira (classes e subclasses) para o sistema dizer onde aportar.");
        } else if (!temDeficitDeClasse
                && itens.stream().noneMatch(i -> i.sugestao_aporte() != null && i.sugestao_aporte().signum() > 0)) {
            // A classe pode estar dentro da tolerância (conta como equilibrada) e
            // mesmo assim a SUBCLASSE ter déficit — nesse caso o dinheiro ainda tem
            // destino, então avisar "não há para onde direcionar" seria falso.
            avisos.add("Nenhuma classe está abaixo do alvo — não há para onde direcionar o aporte agora.");
        }
        if (itens.isEmpty()) {
            avisos.add("Nenhuma posição nesta carteira ainda: cadastre as posições para o sistema indicar onde aportar.");
        }

        long semAvaliacao = itens.stream().filter(i -> !i.qualidade_avaliada()).count();
        if (semAvaliacao > 0) {
            avisos.add(semAvaliacao + " item(ns) ainda sem avaliação: o termo de qualidade não entra na conta deles "
                    + "(o peso é desconsiderado, não zerado).");
        }

        long semTickerSemSubclasse = meus.ativos().stream()
                .filter(a -> !a.vinculado() && a.subclasse_id() == null
                        && classeTemSubclasseComAlvo(comparativo, (a.classe() != null) ? a.classe() : CategoriaInvestimento.OUTROS))
                .count();
        if (semTickerSemSubclasse > 0) {
            avisos.add(semTickerSemSubclasse + " posição(ões) sem ticker (renda fixa, Tesouro, caixinha) não estão em "
                    + "nenhuma das subclasses da classe: o valor JÁ conta no total e no alvo da CLASSE, mas sem a "
                    + "subclasse o motor não sabe qual fatia da classe elas representam. Em Carteira Ideal, escolha a "
                    + "subclasse delas.");
        }

        if (naoAlocado != null && naoAlocado.signum() > 0) {
            avisos.add("Parte do valor ficou sem destino: os ativos elegíveis já estão completos.");
        }
        return avisos;
    }

    /* ── Cenários ── */

    private List<RankingAportesDTO.CenarioDTO> cenarios(MeusAtivosResponseDTO meus,
                                                        ComparativoResponseDTO comparativo,
                                                        Avaliacoes avaliacoes,
                                                        PrecoService.IndicePrecoMedio precos,
                                                        AportesRecentes recentes,
                                                        BigDecimal valorAporte,
                                                        ScoreConfigModel config) {
        if (valorAporte == null || valorAporte.signum() <= 0) {
            return List.of();
        }
        List<RankingAportesDTO.CenarioDTO> cenarios = new ArrayList<>();
        cenarios.add(cenario("Conservador",
                "Prioriza qualidade e evita o que já passou do alvo.",
                config, meus, comparativo, avaliacoes, precos, recentes, valorAporte,
                new BigDecimal("5"), new BigDecimal("2"), new BigDecimal("3"), new BigDecimal("1"),
                new BigDecimal("4"), new BigDecimal("2")));
        cenarios.add(cenario("Balanceado",
                "Equilibra déficit, qualidade, momento e preço (os seus pesos atuais).",
                config, meus, comparativo, avaliacoes, precos, recentes, valorAporte,
                config.getPesoQuality(), config.getPesoDeficit(), config.getPesoExcesso(),
                config.getPesoPrioridade(), config.getPesoMomento(), config.getPesoPreco()));
        cenarios.add(cenario("Estrutural",
                "Prioriza fechar o déficit de quem está mais longe da meta.",
                config, meus, comparativo, avaliacoes, precos, recentes, valorAporte,
                new BigDecimal("1"), new BigDecimal("6"), new BigDecimal("2"), new BigDecimal("1"),
                new BigDecimal("1"), new BigDecimal("1")));
        return cenarios;
    }

    private RankingAportesDTO.CenarioDTO cenario(String nome, String descricao, ScoreConfigModel base,
                                                 MeusAtivosResponseDTO meus, ComparativoResponseDTO comparativo,
                                                 Avaliacoes avaliacoes, PrecoService.IndicePrecoMedio precos,
                                                 AportesRecentes recentes, BigDecimal valorAporte,
                                                 BigDecimal pesoQuality, BigDecimal pesoDeficit,
                                                 BigDecimal pesoExcesso, BigDecimal pesoPrioridade,
                                                 BigDecimal pesoMomento, BigDecimal pesoPreco) {
        ScoreConfigModel cfg = comPesos(base, pesoQuality, pesoDeficit, pesoExcesso,
                pesoPrioridade, pesoMomento, pesoPreco);

        List<AporteCandidato> cs = candidatos(meus, comparativo, avaliacoes, precos, cfg, recentes,
                precedenciaDaConfig(cfg));
        cs.sort(Comparator
                .comparing(AporteCandidato::elegivel, Comparator.reverseOrder())
                .thenComparing(AporteCandidato::priorityScore, Comparator.reverseOrder()));
        OrcamentoAporte o = alocacaoService.ratear(cs, comparativo, valorAporte, cfg);

        String pesos = "qualidade " + fmt(pesoQuality) + " · déficit " + fmt(pesoDeficit)
                + " · excesso " + fmt(pesoExcesso) + " · prioridade " + fmt(pesoPrioridade)
                + " · momento " + fmt(pesoMomento) + " · preço " + fmt(pesoPreco);
        if (o == null) {
            return new RankingAportesDTO.CenarioDTO(nome, descricao, pesos,
                    moeda(0d), valorAporte, List.of());
        }
        List<RankingAportesDTO.ItemCenarioDTO> top = new ArrayList<>();
        for (int i = 0; i < cs.size() && top.size() < 5; i++) {
            BigDecimal sugestao = o.deCandidato(i);
            if (sugestao != null && sugestao.signum() > 0) {
                top.add(new RankingAportesDTO.ItemCenarioDTO(cs.get(i).nome(), sugestao));
            }
        }
        return new RankingAportesDTO.CenarioDTO(nome, descricao, pesos,
                o.alocado(), valorAporte.subtract(o.alocado()), top);
    }

    /** Cópia TRANSITÓRIA da configuração com outros pesos (não persiste nada). */
    private ScoreConfigModel comPesos(ScoreConfigModel base,
                                      BigDecimal quality, BigDecimal deficit, BigDecimal excesso,
                                      BigDecimal prioridade, BigDecimal momento, BigDecimal preco) {
        ScoreConfigModel cfg = new ScoreConfigModel();
        cfg.setPesoQuality(quality);
        cfg.setPesoDeficit(deficit);
        cfg.setPesoExcesso(excesso);
        cfg.setPesoPrioridade(prioridade);
        cfg.setPesoMomento(momento);
        cfg.setPesoPreco(preco);
        cfg.setEstrategiaAporte(base.getEstrategiaAporte());
        cfg.setRedistribuir(base.getRedistribuir());
        // O modo do teto faz parte do cálculo: o cenário precisa usar o mesmo.
        cfg.setTetoAtivoModo(base.getTetoAtivoModo());
        cfg.setMomentoFaixa1(base.getMomentoFaixa1());
        cfg.setMomentoFaixa2(base.getMomentoFaixa2());
        cfg.setMomentoFaixa3(base.getMomentoFaixa3());
        cfg.setMomentoFaixa4(base.getMomentoFaixa4());
        return cfg;
    }

    private String fmt(BigDecimal valor) {
        return (valor == null) ? "0" : valor.stripTrailingZeros().toPlainString();
    }

    private String fmt(double valor) {
        return BigDecimal.valueOf(valor).setScale(4, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
    }

    private double nz(BigDecimal valor) {
        return (valor != null) ? valor.doubleValue() : 0d;
    }

    private BigDecimal moeda(double valor) {
        return BigDecimal.valueOf(Math.max(0d, valor)).setScale(2, RoundingMode.HALF_UP);
    }
}
