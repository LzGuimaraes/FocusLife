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
import dev.LzGuimaraes.FocusLifeHub.Planejamento.comum.CarteiraLookup;

/**
 * Ranking de prioridade de aporte (Módulos 6 e 9).
 *
 * Junta o que já existe, sem recalcular nada por conta própria:
 *   • situação de cada ativo → {@link CarteiraIdealService#comparativo(Long)}
 *     (percentual atual/ideal, déficit, excesso, prioridade manual);
 *   • qualidade de cada ativo → {@link ChecklistAtivoService#resumoPorAtivo()}
 *     (Quality Score ponderado dos checklists);
 *   • pesos da fórmula → {@link ScoreConfigService} (configurável, não hardcoded).
 *
 * A única fórmula aplicada aqui é a do Contribution Score, em
 * {@link ScoreCalculator#contributionScore}. O sistema não opina sobre o ativo:
 * ele ordena a decisão que o próprio usuário configurou.
 */
@Service
public class AporteService {

    private final CarteiraLookup carteiraLookup;
    private final CarteiraIdealService carteiraIdealService;
    private final ChecklistAtivoService checklistAtivoService;
    private final ScoreConfigService scoreConfigService;
    private final ScoreCalculator scoreCalculator;

    public AporteService(CarteiraLookup carteiraLookup,
                         CarteiraIdealService carteiraIdealService,
                         ChecklistAtivoService checklistAtivoService,
                         ScoreConfigService scoreConfigService,
                         ScoreCalculator scoreCalculator) {
        this.carteiraLookup = carteiraLookup;
        this.carteiraIdealService = carteiraIdealService;
        this.checklistAtivoService = checklistAtivoService;
        this.scoreConfigService = scoreConfigService;
        this.scoreCalculator = scoreCalculator;
    }

    /**
     * Ranking dos ativos que possuem meta na carteira, ordenado por
     * Contribution Score (empate: prioridade manual, depois ticker).
     *
     * @param valorAporte quando informado, calcula também a sugestão de rateio
     *                    do aporte conforme a estratégia configurada.
     */
    @Transactional(readOnly = true)
    public RankingAportesDTO.Response ranking(Long carteiraId, BigDecimal valorAporte) {
        CarteiraInvestimentoModel carteira = carteiraLookup.exigirCarteiraDoUsuario(carteiraId);

        ComparativoResponseDTO comparativo = carteiraIdealService.comparativo(carteiraId);
        ScoreConfigModel config = scoreConfigService.obterOuPadrao();

        Map<UUID, BigDecimal> qualidadePorAtivo = qualidadePorAtivo();
        List<Calculado> calculados = calcular(comparativo, qualidadePorAtivo, config);
        calculados.sort(Comparator
                .comparing(Calculado::contribution, Comparator.reverseOrder())
                .thenComparing(c -> c.prioridade(), Comparator.reverseOrder())
                .thenComparing(c -> c.ticker(), Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));

        List<BigDecimal> sugestoes = (valorAporte != null && valorAporte.signum() > 0)
                ? ratear(calculados, valorAporte, config)
                : null;

        List<RankingAportesDTO.Item> itens = new ArrayList<>();
        BigDecimal alocado = BigDecimal.ZERO;
        for (int i = 0; i < calculados.size(); i++) {
            Calculado c = calculados.get(i);
            BigDecimal sugestao = (sugestoes != null) ? sugestoes.get(i) : null;
            if (sugestao != null) {
                alocado = alocado.add(sugestao);
            }
            itens.add(new RankingAportesDTO.Item(
                    i + 1,
                    c.ativoCadastroId(),
                    c.metaId(),
                    c.ticker(),
                    c.classe(),
                    c.quality(),
                    c.quality() != null,
                    (c.quality() != null) ? config.getPesoQuality() : BigDecimal.ZERO,
                    c.contribution(),
                    c.percentualAtual(),
                    c.percentualIdeal(),
                    c.valorAtual(),
                    c.valorIdeal(),
                    c.deficit(),
                    c.excesso(),
                    c.prioridade(),
                    sugestao));
        }

        return new RankingAportesDTO.Response(
                carteira.getId(),
                carteira.getMoeda(),
                comparativo.valor_total(),
                (valorAporte != null) ? valorAporte : null,
                (sugestoes != null) ? alocado : null,
                (sugestoes != null) ? valorAporte.subtract(alocado) : null,
                config.getEstrategiaAporte(),
                termosDaConfig(config),
                avisos(comparativo, itens),
                itens);
    }

    /* ── Cálculo ── */

    /** Estado intermediário com o Contribution Score já calculado. */
    private record Calculado(
            UUID ativoCadastroId, Long metaId, String ticker, CategoriaInvestimento classe,
            BigDecimal quality, BigDecimal contribution,
            BigDecimal percentualAtual, BigDecimal percentualIdeal,
            BigDecimal valorAtual, BigDecimal valorIdeal,
            BigDecimal deficit, BigDecimal excesso, Integer prioridade
    ) {}

    /** Ativo com meta + a classe a que pertence (o DTO de ativo não carrega a classe). */
    private record AtivoComClasse(ComparativoResponseDTO.AtivoComparativoDTO ativo, CategoriaInvestimento classe) {}

    private List<Calculado> calcular(ComparativoResponseDTO comparativo,
                                     Map<UUID, BigDecimal> qualidadePorAtivo,
                                     ScoreConfigModel config) {

        List<AtivoComClasse> ativos = ativosComMeta(comparativo);

        double maiorDeficit = ativos.stream()
                .mapToDouble(a -> nz(a.ativo().deficit())).max().orElse(0d);
        double maiorExcesso = ativos.stream()
                .mapToDouble(a -> nz(a.ativo().excesso())).max().orElse(0d);

        ScoreCalculator.TermosContribution pesos = new ScoreCalculator.TermosContribution(
                config.getPesoQuality(), config.getPesoDeficit(),
                config.getPesoExcesso(), config.getPesoPrioridade());

        List<Calculado> calculados = new ArrayList<>();
        for (AtivoComClasse item : ativos) {
            ComparativoResponseDTO.AtivoComparativoDTO a = item.ativo();
            BigDecimal quality = (a.ativo_cadastro_id() != null)
                    ? qualidadePorAtivo.get(a.ativo_cadastro_id())
                    : null;

            BigDecimal contribution = scoreCalculator.contributionScore(
                    (quality != null) ? quality.doubleValue() / 100d : null,
                    scoreCalculator.normalizar(nz(a.deficit()), maiorDeficit),
                    scoreCalculator.normalizar(nz(a.excesso()), maiorExcesso),
                    scoreCalculator.normalizarPrioridade(a.prioridade_manual()),
                    pesos);

            calculados.add(new Calculado(
                    a.ativo_cadastro_id(), a.meta_id(), a.ticker(), item.classe(),
                    quality, contribution,
                    a.percentual_atual(), a.percentual_ideal(),
                    a.valor_atual(), a.valor_ideal(),
                    a.deficit(), a.excesso(), a.prioridade_manual()));
        }
        return calculados;
    }

    /**
     * Rateia o valor do aporte conforme a estratégia configurada.
     *
     * Duas travas de coerência: ativo com EXCESSO não recebe aporte, e nenhum
     * ativo recebe mais do que o seu déficit — o que sobrar volta como
     * `valor_nao_alocado` em vez de ser empurrado para quem não precisa.
     */
    private List<BigDecimal> ratear(List<Calculado> calculados, BigDecimal valorAporte, ScoreConfigModel config) {
        List<Double> pesos = calculados.stream().map(c -> {
            double deficit = nz(c.deficit());
            if (deficit <= 0d) {
                return 0d;
            }
            int prioridade = (c.prioridade() != null) ? c.prioridade() : 0;
            return config.getEstrategiaAporte().pesoDoAtivo(c.contribution(), deficit, prioridade);
        }).toList();

        double somaPesos = pesos.stream().mapToDouble(Double::doubleValue).sum();

        List<BigDecimal> valores = new ArrayList<>(calculados.size());
        for (int i = 0; i < calculados.size(); i++) {
            double peso = pesos.get(i);
            if (somaPesos <= 0d || peso <= 0d) {
                valores.add(moeda(0d));
                continue;
            }
            double ideal = valorAporte.doubleValue() * (peso / somaPesos);
            double deficit = nz(calculados.get(i).deficit());
            valores.add(moeda(Math.min(ideal, deficit)));
        }
        return valores;
    }

    /** Ativos (com meta) achatados com a classe a que pertencem, para o ranking. */
    private List<AtivoComClasse> ativosComMeta(ComparativoResponseDTO comparativo) {
        return comparativo.classes().stream()
                .flatMap(c -> c.ativos().stream().map(a -> new AtivoComClasse(a, c.classe())))
                .toList();
    }

    /** Quality Score consolidado por ativo do catálogo (reuso do módulo de avaliação). */
    private Map<UUID, BigDecimal> qualidadePorAtivo() {
        Map<UUID, BigDecimal> mapa = new HashMap<>();
        for (ChecklistAtivoDTO.AtivoAvaliado a : checklistAtivoService.resumoPorAtivo()) {
            if (a.ativo_cadastro_id() != null && a.quality_score() != null) {
                mapa.putIfAbsent(a.ativo_cadastro_id(), a.quality_score());
            }
        }
        return mapa;
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

    private List<String> avisos(ComparativoResponseDTO comparativo, List<RankingAportesDTO.Item> itens) {
        List<String> avisos = new ArrayList<>();
        if (comparativo.classes().isEmpty()) {
            avisos.add("Defina a Carteira Ideal desta carteira para ver a prioridade de aporte.");
        } else if (itens.isEmpty()) {
            avisos.add("Nenhum ativo tem meta individual na Carteira Ideal — as metas por ativo definem a prioridade de aporte.");
        }
        long semAvaliacao = itens.stream().filter(i -> !i.qualidade_avaliada()).count();
        if (semAvaliacao > 0) {
            avisos.add(semAvaliacao + " ativo(s) ainda sem avaliação: o termo de qualidade não entra na conta deles "
                    + "(o peso é desconsiderado, não zerado).");
        }
        return avisos;
    }

    private double nz(BigDecimal valor) {
        return (valor != null) ? valor.doubleValue() : 0d;
    }

    private BigDecimal moeda(double valor) {
        return BigDecimal.valueOf(Math.max(0d, valor)).setScale(2, RoundingMode.HALF_UP);
    }
}
