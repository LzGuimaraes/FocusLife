package dev.LzGuimaraes.FocusLifeHub.Planejamento.Aporte;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
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

    /**
     * Piso de ruído do cálculo, em reais: 0,01% do patrimônio (nunca menos de
     * meio centavo).
     *
     * POR QUE RELATIVO: o percentual é guardado com 4 casas, então uma meta que
     * espelha a carteira atual difere do valor real em até 0,00005 p.p. — que num
     * patrimônio grande vira centavos. Com um piso fixo de meio centavo, esses
     * centavos contavam como déficit/excesso de verdade e o rateio escolhia o
     * destino do dinheiro por ruído de arredondamento.
     */
    private static double tolerancia(double total) {
        return Math.max(0.005d, total * 0.0001d);
    }

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
     * Ranking dos candidatos a receber aporte, ordenado por Contribution Score
     * (empate: prioridade manual, depois nome).
     *
     * O RATEIO É FEITO POR CLASSE → SUBCLASSE → ATIVO: a estratégia da carteira
     * é definida em classes e subclasses, então é o déficit DELAS que decide
     * quanto dinheiro cada parte recebe. O ticker (ou a posição sem ticker) só
     * escolhe depois, dentro do orçamento já aprovado pela classe.
     *
     * @param valorAporte quando informado, calcula também a sugestão de rateio
     *                    do aporte conforme a estratégia configurada.
     */
    @Transactional(readOnly = true)
    public RankingAportesDTO.Response ranking(Long carteiraId, BigDecimal valorAporte) {
        CarteiraInvestimentoModel carteira = carteiraLookup.exigirCarteiraDoUsuario(carteiraId);

        ComparativoResponseDTO comparativo = carteiraIdealService.comparativo(carteiraId);
        MeusAtivosResponseDTO meus = carteiraIdealService.meusAtivos(carteiraId);
        ScoreConfigModel config = scoreConfigService.obterOuPadrao();

        Map<UUID, BigDecimal> qualidadePorAtivo = qualidadePorAtivo();
        List<Candidato> calculados = candidatos(meus, comparativo.valor_total(), qualidadePorAtivo, config);
        calculados.sort(Comparator
                .comparing(Candidato::contribution, Comparator.reverseOrder())
                // prioridade pode ser null (posição sem meta) — sem nullsLast, o
                // Comparator.sort estoura NullPointerException.
                .thenComparing(Candidato::prioridade, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(Candidato::nome, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));

        Orcamento orcamento = ratear(calculados, comparativo, valorAporte, config);

        List<RankingAportesDTO.Item> itens = new ArrayList<>();
        for (int i = 0; i < calculados.size(); i++) {
            Candidato c = calculados.get(i);
            BigDecimal sugestao = (orcamento != null) ? orcamento.porCandidato().get(i) : null;
            itens.add(new RankingAportesDTO.Item(
                    i + 1,
                    c.ativoCadastroId(),
                    c.metaId(),
                    c.nome(),
                    c.classe(),
                    c.vinculado(),
                    c.subclasseId(),
                    c.subclasseNome(),
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

        List<RankingAportesDTO.ClasseAporteDTO> classes = classesDto(
                comparativo, (orcamento != null) ? orcamento.porClasse() : Map.of(),
                (orcamento != null) ? orcamento.porSubclasse() : Map.of());

        BigDecimal alocado = (orcamento != null) ? orcamento.alocado() : null;
        BigDecimal naoAlocado = (orcamento != null) ? valorAporte.subtract(alocado) : null;

        return new RankingAportesDTO.Response(
                carteira.getId(),
                carteira.getMoeda(),
                comparativo.valor_total(),
                (valorAporte != null) ? valorAporte : null,
                alocado,
                naoAlocado,
                config.getEstrategiaAporte(),
                termosDaConfig(config),
                avisos(comparativo, itens, meus, naoAlocado),
                classes,
                itens);
    }

    /* ── Cálculo ── */

    /**
     * Estado intermediário de um candidato a receber aporte.
     *
     * Candidato = tudo que o usuário JÁ TEM na carteira: ticker do catálogo
     * (com ou sem meta) e posição sem ticker agrupada pelo nome (renda fixa,
     * Tesouro, caixinhas). É o que permite "Caixa PICPAY" participar do rateio
     * da classe Renda Fixa — antes da V26 ela ficava fora de tudo.
     */
    private record Candidato(
            UUID ativoCadastroId, Long metaId, String nome, boolean vinculado,
            Long subclasseId, String subclasseNome, CategoriaInvestimento classe,
            BigDecimal quality, BigDecimal contribution,
            BigDecimal percentualAtual, BigDecimal percentualIdeal,
            BigDecimal valorAtual, BigDecimal valorIdeal,
            BigDecimal deficit, BigDecimal excesso, Integer prioridade
    ) {}

    /** Orçamento do aporte já distribuído (classe → subclasse → candidato). */
    private record Orcamento(
            Map<CategoriaInvestimento, BigDecimal> porClasse,
            Map<Long, BigDecimal> porSubclasse,
            List<BigDecimal> porCandidato,
            BigDecimal alocado
    ) {}

    /** Monta os candidatos (posições reais) com o Contribution Score de cada um. */
    private List<Candidato> candidatos(MeusAtivosResponseDTO meus, BigDecimal total,
                                       Map<UUID, BigDecimal> qualidadePorAtivo,
                                       ScoreConfigModel config) {
        double totalValor = nz(total);
        double tol = tolerancia(totalValor);

        // 1ª passada: quanto cada candidato tem e quanto DEVERIA ter (déficit/excesso
        // próprios), para normalizar o termo de déficit do Contribution Score.
        // Só itens COM meta têm alvo próprio — para os demais o alvo é da classe/
        // subclasse, e tratá-los como "em excesso" distorceria toda a conta.
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
        // Fração de centavo é ruído de arredondamento (meta que espelha a carteira).
        // Sem esse piso, a normalização amplifica o ruído e o destino do dinheiro
        // vira sorteio.
        if (maiorDeficit <= tol) {
            maiorDeficit = 0d;
        }
        if (maiorExcesso <= tol) {
            maiorExcesso = 0d;
        }

        ScoreCalculator.TermosContribution pesos = new ScoreCalculator.TermosContribution(
                config.getPesoQuality(), config.getPesoDeficit(),
                config.getPesoExcesso(), config.getPesoPrioridade());

        List<Candidato> lista = new ArrayList<>();
        for (MeusAtivosResponseDTO.MeuAtivoDTO a : meus.ativos()) {
            boolean temMeta = a.meta_id() != null;
            double vAtual = nz(a.valor_atual());
            double vIdeal = temMeta ? valorIdealDe(a.percentual_ideal(), totalValor) : 0d;
            double deficit = (temMeta && vIdeal - vAtual > tol) ? vIdeal - vAtual : 0d;
            double excesso = (temMeta && vAtual - vIdeal > tol) ? vAtual - vIdeal : 0d;
            BigDecimal quality = (a.ativo_cadastro_id() != null)
                    ? qualidadePorAtivo.get(a.ativo_cadastro_id())
                    : null;

            BigDecimal contribution = scoreCalculator.contributionScore(
                    (quality != null) ? quality.doubleValue() / 100d : null,
                    scoreCalculator.normalizar(deficit, maiorDeficit),
                    scoreCalculator.normalizar(excesso, maiorExcesso),
                    scoreCalculator.normalizarPrioridade(a.prioridade_manual()),
                    pesos);

            lista.add(new Candidato(
                    a.ativo_cadastro_id(), a.meta_id(), a.ticker(), a.vinculado(),
                    a.subclasse_id(), a.subclasse_nome(),
                    (a.classe() != null) ? a.classe() : CategoriaInvestimento.OUTROS,
                    quality, contribution,
                    a.percentual_atual(), a.percentual_ideal(),
                    moeda(vAtual), moeda(vIdeal),
                    moeda(deficit), moeda(excesso),
                    a.prioridade_manual()));
        }
        return lista;
    }

    /** Valor devido de um item: % ideal × total (0 quando o item não tem meta). */
    private double valorIdealDe(BigDecimal percentualIdeal, double total) {
        return (percentualIdeal != null) ? percentualIdeal.doubleValue() / 100d * total : 0d;
    }

    /** Onde entra o dinheiro: uma linha por classe (e suas subclasses). */
    private List<RankingAportesDTO.ClasseAporteDTO> classesDto(
            ComparativoResponseDTO comparativo,
            Map<CategoriaInvestimento, BigDecimal> sugeridoPorClasse,
            Map<Long, BigDecimal> sugeridoPorSubclasse) {

        List<RankingAportesDTO.ClasseAporteDTO> classes = new ArrayList<>();
        for (ComparativoResponseDTO.ClasseComparativoDTO c : comparativo.classes()) {
            List<RankingAportesDTO.SubclasseAporteDTO> subs = c.subclasses().stream()
                    .map(s -> new RankingAportesDTO.SubclasseAporteDTO(
                            s.id(), s.nome(), s.percentual_atual(), s.percentual_ideal(),
                            s.valor_atual(), s.valor_ideal(), s.deficit(), s.excesso(),
                            sugeridoPorSubclasse.getOrDefault(s.id(), moeda(0d))))
                    .toList();
            classes.add(new RankingAportesDTO.ClasseAporteDTO(
                    c.classe(), c.percentual_atual(), c.percentual_ideal(),
                    c.valor_atual(), c.valor_ideal(), c.deficit(), c.excesso(),
                    sugeridoPorClasse.getOrDefault(c.classe(), moeda(0d)),
                    subs));
        }
        return classes;
    }

    /**
     * Rateia o valor do aporte POR CLASSE → SUBCLASSE → CANDIDATO.
     *
     * Regras, todas com teto no déficit (nunca empurra dinheiro para quem já
     * está no alvo):
     *   1. só classes com déficit entram, e o valor é repartido entre elas na
     *      proporção do déficit de cada uma;
     *   2. dentro da classe, se ela tem subclasses com alvo, o orçamento é
     *      repartido do mesmo jeito entre as subclasses — e o que sobrar fica
     *      com os candidatos que não estão em nenhuma subclasse;
     *   3. dentro do "bucket", o peso vem da estratégia configurada (déficit por
     *      ticker, Contribution Score ou prioridade). Quando as metas por ticker
     *      espelham a carteira atual (déficit zero em todos), o peso cai para a
     *      participação ATUAL dentro do bucket — sem isso o aporte trava em
     *      R$ 0,00, que era o comportamento antigo;
     *   4. candidato com EXCESSO no próprio alvo não recebe (se todos estiverem
     *      em excesso, volta para a participação atual, senão nada seria feito).
     */
    private Orcamento ratear(List<Candidato> candidatos, ComparativoResponseDTO comparativo,
                             BigDecimal valorAporte, ScoreConfigModel config) {
        if (valorAporte == null || valorAporte.signum() <= 0) {
            return null;
        }

        Map<CategoriaInvestimento, Double> deficitClasse = new LinkedHashMap<>();
        for (ComparativoResponseDTO.ClasseComparativoDTO c : comparativo.classes()) {
            double deficit = nz(c.deficit());
            if (deficit > 0d) {
                deficitClasse.put(c.classe(), deficit);
            }
        }
        double totalDeficit = deficitClasse.values().stream().mapToDouble(Double::doubleValue).sum();
        if (totalDeficit <= 0d) {
            return null;   // nenhuma classe abaixo do alvo: nada a sugerir
        }
        double fator = Math.min(1d, valorAporte.doubleValue() / totalDeficit);
        double tol = tolerancia(nz(comparativo.valor_total()));

        Map<CategoriaInvestimento, List<ComparativoResponseDTO.SubclasseComparativoDTO>> subsPorClasse =
                new HashMap<>();
        for (ComparativoResponseDTO.ClasseComparativoDTO c : comparativo.classes()) {
            subsPorClasse.put(c.classe(), c.subclasses());
        }

        Map<CategoriaInvestimento, BigDecimal> porClasse = new LinkedHashMap<>();
        Map<Long, BigDecimal> porSubclasse = new LinkedHashMap<>();
        List<BigDecimal> porCandidato = new ArrayList<>(
                Collections.nCopies(candidatos.size(), moeda(0d)));
        double alocado = 0d;

        for (Map.Entry<CategoriaInvestimento, Double> entrada : deficitClasse.entrySet()) {
            CategoriaInvestimento classe = entrada.getKey();
            double orcamentoClasse = entrada.getValue() * fator;
            porClasse.put(classe, moeda(orcamentoClasse));

            List<Integer> daClasse = indicesDaClasse(candidatos, classe);
            if (daClasse.isEmpty()) {
                continue;   // classe com alvo e sem nenhum ativo: o valor fica sem destino
            }

            List<ComparativoResponseDTO.SubclasseComparativoDTO> subs = subsPorClasse
                    .getOrDefault(classe, List.of()).stream()
                    .filter(s -> nz(s.percentual_ideal()) > 0d)
                    .toList();

            // Classe sem subclasse com alvo: ela mesma é o bucket.
            if (subs.isEmpty()) {
                alocado += distribuir(candidatos, daClasse, orcamentoClasse, config, tol, porCandidato);
                continue;
            }

            double restante = orcamentoClasse;
            for (ComparativoResponseDTO.SubclasseComparativoDTO sub : subs) {
                double orcamentoSub = Math.min(nz(sub.deficit()) * fator, restante);
                List<Integer> daSub = (orcamentoSub > 0d)
                        ? indicesDaSubclasse(candidatos, daClasse, sub.id())
                        : List.of();
                if (daSub.isEmpty()) {
                    // Subclasse no alvo, ou planejada e ainda sem posição: não recebe.
                    porSubclasse.put(sub.id(), moeda(0d));
                    continue;
                }
                double distribuido = distribuir(candidatos, daSub, orcamentoSub, config, tol, porCandidato);
                porSubclasse.put(sub.id(), moeda(distribuido));
                restante -= distribuido;
                alocado += distribuido;
            }

            // Sobra da classe → candidatos fora de qualquer subclasse com alvo.
            List<Long> idsComAlvo = subs.stream().map(ComparativoResponseDTO.SubclasseComparativoDTO::id).toList();
            List<Integer> semSubclasse = daClasse.stream()
                    .filter(i -> candidatos.get(i).subclasseId() == null
                            || !idsComAlvo.contains(candidatos.get(i).subclasseId()))
                    .toList();
            if (restante > tol && !semSubclasse.isEmpty()) {
                alocado += distribuir(candidatos, semSubclasse, restante, config, tol, porCandidato);
            }
        }

        return new Orcamento(Map.copyOf(porClasse), Map.copyOf(porSubclasse),
                List.copyOf(porCandidato), moeda(alocado));
    }

    /** Distribui um orçamento entre os candidatos de um bucket, pelas regras de peso. */
    private double distribuir(List<Candidato> candidatos, List<Integer> indices, double orcamento,
                              ScoreConfigModel config, double tol, List<BigDecimal> porCandidato) {
        if (orcamento <= 0d || indices.isEmpty()) {
            return 0d;
        }
        List<Double> pesos = pesosDoBucket(candidatos, indices, config, tol);
        double soma = pesos.stream().mapToDouble(Double::doubleValue).sum();
        if (soma <= 0d) {
            return 0d;
        }
        double distribuido = 0d;
        for (int k = 0; k < indices.size(); k++) {
            double valor = orcamento * (pesos.get(k) / soma);
            porCandidato.set(indices.get(k), moeda(valor));
            distribuido += valor;
        }
        return distribuido;
    }

    /**
     * Peso de cada candidato dentro do bucket:
     *   1º pela estratégia configurada;
     *   se todos zerarem (metas espelhando a carteira, déficit zero), pela
     *   participação ATUAL no bucket;
     *   e, com o bucket ainda vazio (nada comprado), divide igual.
     */
    private List<Double> pesosDoBucket(List<Candidato> candidatos, List<Integer> indices,
                                       ScoreConfigModel config, double tol) {
        List<Double> pesos = new ArrayList<>(indices.size());
        for (int i : indices) {
            Candidato c = candidatos.get(i);
            if (nz(c.excesso()) > tol) {
                pesos.add(0d);   // passou do próprio alvo: não recebe
                continue;
            }
            pesos.add(config.getEstrategiaAporte().pesoDoAtivo(
                    c.contribution(), nz(c.deficit()),
                    (c.prioridade() != null) ? c.prioridade() : 0));
        }
        if (soma(pesos) > 0d) {
            return pesos;
        }
        List<Double> porParticipacao = new ArrayList<>(indices.size());
        for (int i : indices) {
            porParticipacao.add(nz(candidatos.get(i).valorAtual()));
        }
        if (soma(porParticipacao) > 0d) {
            return porParticipacao;
        }
        return new ArrayList<>(Collections.nCopies(indices.size(), 1d));
    }

    private double soma(List<Double> valores) {
        return valores.stream().mapToDouble(Double::doubleValue).sum();
    }

    private List<Integer> indicesDaClasse(List<Candidato> candidatos, CategoriaInvestimento classe) {
        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < candidatos.size(); i++) {
            if (candidatos.get(i).classe() == classe) {
                indices.add(i);
            }
        }
        return indices;
    }

    private List<Integer> indicesDaSubclasse(List<Candidato> candidatos, List<Integer> daClasse, Long subclasseId) {
        return daClasse.stream()
                .filter(i -> subclasseId.equals(candidatos.get(i).subclasseId()))
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

    private List<String> avisos(ComparativoResponseDTO comparativo, List<RankingAportesDTO.Item> itens,
                                MeusAtivosResponseDTO meus, BigDecimal naoAlocado) {
        List<String> avisos = new ArrayList<>();
        boolean temClasse = !comparativo.classes().isEmpty();
        boolean temDeficitDeClasse = comparativo.classes().stream().anyMatch(c -> nz(c.deficit()) > 0d);

        if (!temClasse) {
            avisos.add("Defina a Carteira Ideal desta carteira (classes e subclasses) para o sistema dizer onde aportar.");
        } else if (!temDeficitDeClasse) {
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
                .filter(a -> !a.vinculado() && a.subclasse_id() == null)
                .count();
        if (semTickerSemSubclasse > 0) {
            avisos.add(semTickerSemSubclasse + " posição(ões) sem ticker (renda fixa, Tesouro, caixinha) não estão em "
                    + "nenhuma subclasse: em Carteira Ideal, escolha a subclasse delas para que passem a contar no "
                    + "alvo da classe.");
        }

        if (naoAlocado != null && naoAlocado.signum() > 0) {
            avisos.add("Parte do valor ficou sem destino: as classes abaixo do alvo já estão completas.");
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
