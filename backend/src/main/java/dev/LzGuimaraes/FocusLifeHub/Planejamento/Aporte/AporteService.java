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
    private final dev.LzGuimaraes.FocusLifeHub.Planejamento.Historico.AporteRegistroService aporteRegistroService;

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
                         ScoreCalculator scoreCalculator,
                         dev.LzGuimaraes.FocusLifeHub.Planejamento.Historico.AporteRegistroService aporteRegistroService) {
        this.carteiraLookup = carteiraLookup;
        this.carteiraIdealService = carteiraIdealService;
        this.checklistAtivoService = checklistAtivoService;
        this.scoreConfigService = scoreConfigService;
        this.scoreCalculator = scoreCalculator;
        this.aporteRegistroService = aporteRegistroService;
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

        Avaliacoes avaliacoes = avaliacoes(config);
        AportesRecentes recentes = aportesRecentes(carteiraId);
        List<Candidato> calculados = candidatos(meus, comparativo, avaliacoes, config, recentes);
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
                    c.setorId(),
                    c.setorNome(),
                    c.quality(),
                    c.quality() != null,
                    (c.quality() != null) ? config.getPesoQuality() : BigDecimal.ZERO,
                    c.momento(),
                    c.momento() != null,
                    c.fator(),
                    c.estado(),
                    c.bloqueios(),
                    c.limiteMaximo(),
                    c.limiteAtingido(),
                    c.contribution(),
                    c.percentualAtual(),
                    c.percentualIdeal(),
                    c.valorAtual(),
                    c.valorIdeal(),
                    c.deficit(),
                    c.excesso(),
                    c.tolerancia(),
                    c.teto(),
                    c.prioridade(),
                    sugestao,
                    motivo(c, sugestao),
                    c.aportesRecentes(),
                    c.aportesRecentesQtd(),
                    formula(c, config)));
        }

        List<RankingAportesDTO.ClasseAporteDTO> classes = classesDto(
                comparativo,
                (orcamento != null) ? orcamento.porClasse() : Map.of(),
                (orcamento != null) ? orcamento.porSubclasse() : Map.of(),
                (orcamento != null) ? orcamento.porSetor() : Map.of());

        BigDecimal alocado = (orcamento != null) ? orcamento.alocado() : null;
        BigDecimal naoAlocado = (orcamento != null) ? valorAporte.subtract(alocado) : null;
        List<RankingAportesDTO.Alerta> alertas = alertas(comparativo, calculados, itens, naoAlocado, config);

        return new RankingAportesDTO.Response(
                carteira.getId(),
                carteira.getMoeda(),
                comparativo.valor_total(),
                (valorAporte != null) ? valorAporte : null,
                alocado,
                naoAlocado,
                explicacaoNaoAlocado(naoAlocado, calculados, comparativo),
                config.getRedistribuir(),
                config.getEstrategiaAporte(),
                termosDaConfig(config),
                avisos(comparativo, itens, meus, naoAlocado),
                alertas,
                precedencia(),
                cenarios(meus, comparativo, avaliacoes, recentes, valorAporte, config),
                classes,
                itens);
    }

    /* ── Cálculo ── */

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
     * É o sinal de CONCENTRAÇÃO RECENTE do §24: informa, não decide sozinho.
     */
    private AportesRecentes aportesRecentes(Long carteiraId) {
        Map<UUID, java.util.List<BigDecimal>> catValores = new HashMap<>();
        Map<UUID, Integer> catQtd = new HashMap<>();
        Map<Long, java.util.List<BigDecimal>> posValores = new HashMap<>();
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

    private BigDecimal somar(java.util.List<BigDecimal> valores) {
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
     * Estado intermediário de um candidato a receber aporte.
     *
     * Candidato = tudo que o usuário JÁ TEM na carteira: ticker do catálogo
     * (com ou sem meta) e posição sem ticker agrupada pelo nome (renda fixa,
     * Tesouro, caixinhas). É o que permite "Caixa PICPAY" participar do rateio
     * da classe Renda Fixa — antes da V26 ela ficava fora de tudo.
     */
    private record Candidato(
            UUID ativoCadastroId, Long metaId, String nome, boolean vinculado,
            Long subclasseId, String subclasseNome, Long setorId, String setorNome,
            CategoriaInvestimento classe,
            BigDecimal quality, BigDecimal momento, BigDecimal fator,
            RankingAportesDTO.EstadoAtivo estado, List<String> bloqueios,
            BigDecimal limiteMaximo, boolean limiteAtingido,
            BigDecimal contribution,
            BigDecimal percentualAtual, BigDecimal percentualIdeal,
            BigDecimal valorAtual, BigDecimal valorIdeal,
            BigDecimal deficit, BigDecimal excesso, BigDecimal tolerancia,
            BigDecimal teto, Integer prioridade,
            BigDecimal aportesRecentes, int aportesRecentesQtd,
            /** Termos normalizados do Contribution Score (§34) — para a fórmula aberta. */
            Double qualityNorm, double deficitNorm, double excessoNorm,
            double prioridadeNorm, Double momentoNorm
    ) {}

    /** Orçamento do aporte já distribuído (classe → subclasse → setor → candidato). */
    private record Orcamento(
            Map<CategoriaInvestimento, BigDecimal> porClasse,
            Map<Long, BigDecimal> porSubclasse,
            Map<Long, BigDecimal> porSetor,
            List<BigDecimal> porCandidato,
            BigDecimal alocado
    ) {}

    /** Monta os candidatos (posições reais) com estado, teto e Contribution Score. */
    private List<Candidato> candidatos(MeusAtivosResponseDTO meus, ComparativoResponseDTO comparativo,
                                       Avaliacoes avaliacoes, ScoreConfigModel config,
                                       AportesRecentes recentes) {
        double totalValor = nz(comparativo.valor_total());
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
                config.getPesoExcesso(), config.getPesoPrioridade(), config.getPesoMomento());

        List<Candidato> lista = new ArrayList<>();
        for (MeusAtivosResponseDTO.MeuAtivoDTO a : meus.ativos()) {
            boolean temMeta = a.meta_id() != null;
            double vAtual = nz(a.valor_atual());
            double vIdeal = temMeta ? valorIdealDe(a.percentual_ideal(), totalValor) : 0d;
            double deficit = (temMeta && vIdeal - vAtual > tol) ? vIdeal - vAtual : 0d;
            double excesso = (temMeta && vAtual - vIdeal > tol) ? vAtual - vIdeal : 0d;

            Avaliacao avaliacao = avaliacoes.de(a.ativo_cadastro_id(), a.ativo_ids());
            Recente recente = recentes.de(a.ativo_cadastro_id(), a.ativo_ids());
            BigDecimal tolerancia = (a.tolerancia() != null) ? a.tolerancia() : BigDecimal.ZERO;

            // TETO do ativo = déficit + tolerância (o que a meta dele ainda aceita),
            // limitado pelo teto de concentração quando houver.
            // Posição SEM meta própria (renda fixa, caixinha) não tem alvo próprio:
            // herda o teto da SUBCLASSE (ou da classe) a que pertence — é o alvo dela.
            double teto = temMeta
                    ? tetoDe(vIdeal, tolerancia, vAtual, totalValor, a.limite_maximo())
                    : tetoHerdado(a.subclasse_id(), a.classe(), comparativo, totalValor);
            boolean limiteAtingido = limiteAtingido(vAtual, totalValor, a.limite_maximo());

            List<String> bloqueios = new ArrayList<>(avaliacao.bloqueios());
            if (limiteAtingido) {
                bloqueios.add("Limite de concentração atingido (" + formatar(a.limite_maximo())
                        + "% da carteira)");
            }

            BigDecimal contribution = scoreCalculator.contributionScore(
                    (avaliacao.quality() != null) ? avaliacao.quality().doubleValue() / 100d : null,
                    scoreCalculator.normalizar(deficit, maiorDeficit),
                    scoreCalculator.normalizar(excesso, maiorExcesso),
                    scoreCalculator.normalizarPrioridade(a.prioridade_manual()),
                    (avaliacao.momento() == null) ? null : avaliacao.fator().doubleValue(),
                    pesos);

            lista.add(new Candidato(
                    a.ativo_cadastro_id(), a.meta_id(), a.ticker(), a.vinculado(),
                    a.subclasse_id(), a.subclasse_nome(), a.setor_id(), a.setor_nome(),
                    (a.classe() != null) ? a.classe() : CategoriaInvestimento.OUTROS,
                    avaliacao.quality(), avaliacao.momento(), avaliacao.fator(),
                    estadoDe(avaliacao, bloqueios, teto),
                    List.copyOf(bloqueios),
                    a.limite_maximo(), limiteAtingido,
                    contribution,
                    a.percentual_atual(), a.percentual_ideal(),
                    moeda(vAtual), moeda(vIdeal),
                    moeda(deficit), moeda(excesso), tolerancia,
                    moeda(teto), a.prioridade_manual(),
                    recente.valor(), recente.quantidade(),
                    (avaliacao.quality() != null) ? avaliacao.quality().doubleValue() / 100d : null,
                    scoreCalculator.normalizar(deficit, maiorDeficit),
                    scoreCalculator.normalizar(excesso, maiorExcesso),
                    scoreCalculator.normalizarPrioridade(a.prioridade_manual()),
                    (avaliacao.momento() == null) ? null : avaliacao.fator().doubleValue()));
        }
        return lista;
    }

    /** Valor devido de um item: % ideal × total (0 quando o item não tem meta). */
    private double valorIdealDe(BigDecimal percentualIdeal, double total) {
        return (percentualIdeal != null) ? percentualIdeal.doubleValue() / 100d * total : 0d;
    }

    /**
     * Teto do ativo: (ideal% + tolerância%) × total − atual, sem nunca passar do
     * limite de concentração (limite% × total − atual) quando ele existir.
     */
    private double tetoDe(double valorIdeal, BigDecimal tolerancia, double valorAtual,
                          double total, BigDecimal limiteMaximo) {
        double alvoComTolerancia = valorIdeal + nz(tolerancia) / 100d * total;
        double teto = Math.max(0d, alvoComTolerancia - valorAtual);
        if (limiteMaximo != null && limiteMaximo.signum() > 0) {
            double espacoAteLimite = Math.max(0d, limiteMaximo.doubleValue() / 100d * total - valorAtual);
            teto = Math.min(teto, espacoAteLimite);
        }
        return teto;
    }

    /** true = a posição já chegou ao limite máximo de concentração configurado. */
    private boolean limiteAtingido(double valorAtual, double total, BigDecimal limiteMaximo) {
        if (limiteMaximo == null || limiteMaximo.signum() <= 0 || total <= 0d) {
            return false;
        }
        return valorAtual / total * 100d >= limiteMaximo.doubleValue() - tolerancia(total);
    }

    /**
     * Teto de uma posição SEM meta própria: o déficit da SUBCLASSE a que ela
     * pertence (ou da classe, quando não está em nenhuma subclasse com alvo).
     *
     * É o que faz a renda fixa/caixinha participar: o alvo não é dela, é da
     * subclasse — e o teto é quanto ainda falta para a subclasse.
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
                return deficitComTolerancia(c.percentual_ideal(), c.tolerancia(),
                        c.percentual_atual(), total).doubleValue();
            }
        }
        return 0d;
    }

    /** Estado do ativo (§9) — rótulo explicativo; o efeito no dinheiro é do fator/teto. */
    private RankingAportesDTO.EstadoAtivo estadoDe(Avaliacao avaliacao, List<String> bloqueios, double teto) {
        if (!bloqueios.isEmpty()) {
            return RankingAportesDTO.EstadoAtivo.NAO_APORTAR;
        }
        boolean semAvaliacao = avaliacao.quality() == null && avaliacao.momento() == null;
        if (semAvaliacao) {
            return RankingAportesDTO.EstadoAtivo.SEM_AVALIACAO;
        }
        if (teto <= 0d) {
            return RankingAportesDTO.EstadoAtivo.RESTRITO;   // já no alvo: nada a receber
        }
        if (avaliacao.fator().doubleValue() <= 0d) {
            return RankingAportesDTO.EstadoAtivo.NAO_APORTAR;   // fator 0 = não aportar (§12)
        }
        if (avaliacao.fator().doubleValue() < 1d) {
            return RankingAportesDTO.EstadoAtivo.RESTRITO;      // aporte reduzido pelo momento
        }
        return RankingAportesDTO.EstadoAtivo.APROVADO;
    }

    /** Explicação objetiva da decisão para o item (§29). */
    private String motivo(Candidato c, BigDecimal sugestao) {
        if (c.estado() == RankingAportesDTO.EstadoAtivo.NAO_APORTAR) {
            if (!c.bloqueios().isEmpty()) {
                return "Não aportar: " + String.join("; ", c.bloqueios())
                        + ". O déficit continua existindo — o ativo só está fora da fila de novos aportes.";
            }
            return "Não aportar: fator de momento 0 (nota de momento " + formatar(c.momento())
                    + "/100 — é hora de esperar, não de comprar). O déficit continua existindo.";
        }
        if (sugestao == null) {
            return (c.estado() == RankingAportesDTO.EstadoAtivo.SEM_AVALIACAO)
                    ? "Sem avaliação configurada (a nota de qualidade não entrou na conta)."
                    : "Informe o valor do aporte para ver quanto caberia a este ativo.";
        }
        if (sugestao.signum() <= 0) {
            if (c.teto().signum() <= 0) {
                return "Nada: já está no alvo (déficit + tolerância esgotados).";
            }
            return "Nada: não sobrou valor elegível para " + c.classe()
                    + " neste aporte (a classe recebeu o que o déficit dela permitia).";
        }
        return "Recebe " + formatar(sugestao) + " de um teto de " + formatar(c.teto())
                + " (déficit " + formatar(c.deficit()) + " + tolerância " + formatar(c.tolerancia())
                + "%) — estado " + c.estado().getLabel()
                + ", fator de momento " + formatar(c.fator())
                + (c.aportesRecentesQtd() > 0
                        ? ". Já recebeu " + formatar(c.aportesRecentes()) + " em "
                          + c.aportesRecentesQtd() + " aporte(s) nos últimos 30 dias."
                        : ".");
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
                                (sugeridoSub.signum() <= 0 && sugerido.signum() > 0)
                                        ? "Neste aporte a verba da classe foi para as subclasses com déficit maior."
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

    /** Status de equilíbrio de um nível frente à tolerância configurada (§18). */
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
        BigDecimal falta = deficitComTolerancia(c.percentual_ideal(), c.tolerancia(), atual, total);
        if (falta.signum() <= 0) {
            return "Não recebe: está no alvo (dentro da tolerância de " + formatar(c.tolerancia()) + "%).";
        }
        if (sugerido.signum() <= 0) {
            return "Tem déficit de " + formatar(falta) + ", mas nenhum ativo da classe ficou elegível neste aporte.";
        }
        return "Recebe no máximo o déficit da classe: " + formatar(falta)
                + " (ideal + tolerância − atual).";
    }

    /**
     * Déficit de um nível CONSIDERANDO a tolerância: dentro da faixa o item conta
     * como EQUILIBRADO e não puxa aporte. Diferente do déficit "cru" do
     * comparativo (que é sempre ideal − atual, sem faixa).
     */
    private BigDecimal deficitComTolerancia(BigDecimal percentualIdeal, BigDecimal tolerancia,
                                            BigDecimal percentualAtual, double total) {
        if (percentualIdeal == null || percentualIdeal.signum() <= 0) {
            return moeda(0d);
        }
        double alvo = (percentualIdeal.doubleValue() + nz(tolerancia)) / 100d * total;
        double atual = ((percentualAtual != null) ? percentualAtual.doubleValue() : 0d) / 100d * total;
        return moeda(Math.max(0d, alvo - atual));
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

        double total = nz(comparativo.valor_total());
        double tol = tolerancia(total);

        // Déficit de cada classe JÁ com a tolerância aplicada: dentro da faixa a
        // classe conta como equilibrada e não puxa aporte.
        Map<CategoriaInvestimento, Double> deficitClasse = new LinkedHashMap<>();
        for (ComparativoResponseDTO.ClasseComparativoDTO c : comparativo.classes()) {
            double deficit = deficitComTolerancia(c.percentual_ideal(), c.tolerancia(), c.percentual_atual(), total)
                    .doubleValue();
            if (deficit > tol) {
                deficitClasse.put(c.classe(), deficit);
            }
        }
        if (deficitClasse.isEmpty()) {
            return null;   // nenhuma classe abaixo do alvo (fora da tolerância)
        }

        Map<CategoriaInvestimento, List<ComparativoResponseDTO.SubclasseComparativoDTO>> subsPorClasse =
                new HashMap<>();
        Map<CategoriaInvestimento, Double> valorIdealClasse = new HashMap<>();
        for (ComparativoResponseDTO.ClasseComparativoDTO c : comparativo.classes()) {
            subsPorClasse.put(c.classe(), c.subclasses());
            valorIdealClasse.put(c.classe(), nz(c.valor_ideal()));
        }

        Map<CategoriaInvestimento, BigDecimal> porClasse = new LinkedHashMap<>();
        Map<Long, BigDecimal> porSubclasse = new LinkedHashMap<>();
        Map<Long, BigDecimal> porSetor = new LinkedHashMap<>();
        List<BigDecimal> porCandidato = new ArrayList<>(
                Collections.nCopies(candidatos.size(), moeda(0d)));

        // REDISTRIBUIÇÃO (§21): o valor que não achou destino elegível numa classe
        // pode procurar outra classe com déficit — quantas rodadas forem precisas
        // (limitado, para não girar em falso). Desligado, uma única rodada e o que
        // sobrar fica não alocado, com o motivo explicado.
        boolean redistribuir = config.getRedistribuir() == null || config.getRedistribuir();
        int rodadas = redistribuir ? 6 : 1;
        double restanteTotal = valorAporte.doubleValue();

        for (int rodada = 0; rodada < rodadas && restanteTotal > tol; rodada++) {
            Map<CategoriaInvestimento, Double> capacidade = new LinkedHashMap<>();
            for (Map.Entry<CategoriaInvestimento, Double> entrada : deficitClasse.entrySet()) {
                double jaRecebeu = porClasse.getOrDefault(entrada.getKey(), moeda(0d)).doubleValue();
                double sobra = entrada.getValue() - jaRecebeu;
                if (sobra > tol) {
                    capacidade.put(entrada.getKey(), sobra);
                }
            }
            double somaCapacidade = capacidade.values().stream().mapToDouble(Double::doubleValue).sum();
            if (somaCapacidade <= tol) {
                break;
            }
            double fator = Math.min(1d, restanteTotal / somaCapacidade);
            double distribuidoNaRodada = 0d;

            for (Map.Entry<CategoriaInvestimento, Double> entrada : capacidade.entrySet()) {
                CategoriaInvestimento classe = entrada.getKey();
                double orcamentoClasse = Math.min(entrada.getValue(), entrada.getValue() * fator);
                double antes = porClasse.getOrDefault(classe, moeda(0d)).doubleValue();

                List<Integer> daClasse = indicesDaClasse(candidatos, classe);
                if (daClasse.isEmpty()) {
                    continue;   // classe com alvo e sem nenhum ativo: valor fica sem destino
                }

                List<ComparativoResponseDTO.SubclasseComparativoDTO> subs = subsPorClasse
                        .getOrDefault(classe, List.of()).stream()
                        .filter(s -> nz(s.percentual_ideal()) > 0d)
                        .toList();

                // Classe sem subclasse com alvo: ela mesma é o bucket.
                if (subs.isEmpty()) {
                    distribuidoNaRodada += distribuir(candidatos, daClasse, orcamentoClasse, config, tol, porCandidato);
                    continue;
                }

                double restanteClasse = orcamentoClasse;
                double valorIdealDaClasse = valorIdealClasse.getOrDefault(classe, 0d);
                for (ComparativoResponseDTO.SubclasseComparativoDTO sub : subs) {
                    // O percentual da subclasse é uma FATIA DA CLASSE: o teto dela
                    // só pode ser calculado em R$ (valor_ideal/valor_atual da
                    // subclasse), nunca tratando o percentual como % da carteira.
                    double alvoSub = nz(sub.valor_ideal())
                            + nz(sub.tolerancia()) / 100d * valorIdealDaClasse;
                    double tetoSub = Math.max(0d, alvoSub - nz(sub.valor_atual()));
                    double jaNaSub = porSubclasse.getOrDefault(sub.id(), moeda(0d)).doubleValue();
                    double orcamentoSub = Math.min(Math.max(0d, tetoSub - jaNaSub), restanteClasse);
                    List<Integer> daSub = (orcamentoSub > tol)
                            ? indicesDaSubclasse(candidatos, daClasse, sub.id())
                            : List.of();
                    if (daSub.isEmpty()) {
                        continue;
                    }

                    // SETOR (nível opcional): se a subclasse tem setores com alvo, o
                    // orçamento dela desce um nível antes de chegar ao ativo. O
                    // percentual do setor é fatia da SUBCLASSE (teto em R$).
                    List<ComparativoResponseDTO.SetorComparativoDTO> setores = sub.setores().stream()
                            .filter(s -> nz(s.percentual_ideal()) > 0d)
                            .toList();
                    double distribuido;
                    if (setores.isEmpty()) {
                        distribuido = distribuir(candidatos, daSub, orcamentoSub, config, tol, porCandidato);
                    } else {
                        double restanteSub = orcamentoSub;
                        double valorIdealDaSub = nz(sub.valor_ideal());
                        for (ComparativoResponseDTO.SetorComparativoDTO st : setores) {
                            double alvoSetor = nz(st.valor_ideal())
                                    + nz(st.tolerancia()) / 100d * valorIdealDaSub;
                            double tetoSetor = Math.max(0d, alvoSetor - nz(st.valor_atual()));
                            double jaNoSetor = porSetor.getOrDefault(st.id(), moeda(0d)).doubleValue();
                            double orcamentoSetor = Math.min(Math.max(0d, tetoSetor - jaNoSetor), restanteSub);
                            List<Integer> doSetor = (orcamentoSetor > tol)
                                    ? indicesDaSetor(candidatos, daSub, st.id())
                                    : List.of();
                            if (doSetor.isEmpty()) {
                                continue;
                            }
                            double doSetorDistribuido = distribuir(candidatos, doSetor, orcamentoSetor,
                                    config, tol, porCandidato);
                            if (doSetorDistribuido > 0d) {
                                porSetor.merge(st.id(), moeda(doSetorDistribuido), BigDecimal::add);
                                restanteSub -= doSetorDistribuido;
                            }
                        }
                        // Sobra da subclasse → candidatos fora de qualquer setor com alvo.
                        List<Long> idsComAlvo = setores.stream()
                                .map(ComparativoResponseDTO.SetorComparativoDTO::id).toList();
                        List<Integer> foraDeSetor = daSub.stream()
                                .filter(i -> candidatos.get(i).setorId() == null
                                        || !idsComAlvo.contains(candidatos.get(i).setorId()))
                                .toList();
                        distribuido = orcamentoSub - restanteSub;
                        if (restanteSub > tol && !foraDeSetor.isEmpty()) {
                            distribuido += distribuir(candidatos, foraDeSetor, restanteSub, config, tol, porCandidato);
                        }
                    }

                    if (distribuido > 0d) {
                        porSubclasse.merge(sub.id(), moeda(distribuido), BigDecimal::add);
                        restanteClasse -= distribuido;
                        distribuidoNaRodada += distribuido;
                    }
                }

                // Sobra da classe → candidatos fora de qualquer subclasse com alvo.
                List<Long> idsComAlvo = subs.stream().map(ComparativoResponseDTO.SubclasseComparativoDTO::id).toList();
                List<Integer> semSubclasse = daClasse.stream()
                        .filter(i -> candidatos.get(i).subclasseId() == null
                                || !idsComAlvo.contains(candidatos.get(i).subclasseId()))
                        .toList();
                if (restanteClasse > tol && !semSubclasse.isEmpty()) {
                    distribuidoNaRodada += distribuir(candidatos, semSubclasse, restanteClasse, config, tol, porCandidato);
                }

                double depois = 0d;
                for (int i : daClasse) {
                    depois += porCandidato.get(i).doubleValue();
                }
                // A classe só "gastou" o que efetivamente saiu para os ativos dela.
                porClasse.put(classe, moeda(depois));
                if (depois - antes <= tol) {
                    // nada foi absorvido nesta classe: as demais ainda podem tentar
                }
            }

            if (distribuidoNaRodada <= tol) {
                break;   // ninguém tem espaço: para de tentar (evita laço infinito)
            }
            restanteTotal -= distribuidoNaRodada;
        }

        double alocado = porCandidato.stream().mapToDouble(BigDecimal::doubleValue).sum();
        return new Orcamento(Map.copyOf(porClasse), Map.copyOf(porSubclasse), Map.copyOf(porSetor),
                List.copyOf(porCandidato), moeda(alocado));
    }

    /**
     * Distribui um orçamento dentro de um bucket respeitando o TETO de cada
     * candidato (§17).
     *
     * Teto = déficit do ativo (ideal + tolerância − atual), limitado pelo limite
     * de concentração quando houver; posições sem meta própria herdam o teto do
     * bucket (subclasse/classe). Ninguém recebe acima do próprio teto — o que não
     * couber vira valor não alocado com o motivo explicado (§21).
     *
     * Peso = FATOR DE MOMENTO × estratégia configurada, usando o teto como medida
     * de necessidade estrutural. Fator 0 = não aportar, mesmo com déficit (§12).
     */
    private double distribuir(List<Candidato> candidatos, List<Integer> indices, double orcamento,
                              ScoreConfigModel config, double tol, List<BigDecimal> porCandidato) {
        if (orcamento <= tol || indices.isEmpty()) {
            return 0d;
        }

        List<Integer> elegiveis = new ArrayList<>();
        List<Double> pesos = new ArrayList<>();
        List<Double> espacos = new ArrayList<>();
        for (int i : indices) {
            Candidato c = candidatos.get(i);
            if (!c.bloqueios().isEmpty()) {
                continue;   // NÃO APORTAR (critério eliminatório ou limite atingido)
            }
            double espaco = c.teto().doubleValue() - porCandidato.get(i).doubleValue();
            if (espaco <= tol) {
                continue;   // já no teto
            }
            double peso = c.fator().doubleValue() * config.getEstrategiaAporte().pesoDoAtivo(
                    c.contribution(), c.teto().doubleValue(),
                    (c.prioridade() != null) ? c.prioridade() : 0);
            if (peso <= 0d) {
                continue;
            }
            elegiveis.add(i);
            pesos.add(peso);
            espacos.add(espaco);
        }
        if (elegiveis.isEmpty()) {
            return 0d;
        }

        // "Water-filling": divide proporcional ao peso e REPETE com o que sobrou
        // entre quem ainda tem espaço, senão um ativo no teto travaria o rateio.
        double restante = orcamento;
        double distribuido = 0d;
        List<Double> ja = new ArrayList<>(Collections.nCopies(elegiveis.size(), 0d));
        for (int rodada = 0; rodada < 12 && restante > tol; rodada++) {
            double somaPesos = 0d;
            for (int k = 0; k < elegiveis.size(); k++) {
                if (espacos.get(k) - ja.get(k) > tol) {
                    somaPesos += pesos.get(k);
                }
            }
            if (somaPesos <= 0d) {
                break;
            }
            double nestaRodada = 0d;
            for (int k = 0; k < elegiveis.size(); k++) {
                double espacoLivre = espacos.get(k) - ja.get(k);
                if (espacoLivre <= tol) {
                    continue;
                }
                double cota = restante * (pesos.get(k) / somaPesos);
                double adicionar = Math.min(cota, espacoLivre);
                ja.set(k, ja.get(k) + adicionar);
                nestaRodada += adicionar;
            }
            if (nestaRodada <= tol) {
                break;
            }
            distribuido += nestaRodada;
            restante -= nestaRodada;
        }

        for (int k = 0; k < elegiveis.size(); k++) {
            if (ja.get(k) > 0d) {
                int i = elegiveis.get(k);
                porCandidato.set(i, moeda(porCandidato.get(i).doubleValue() + ja.get(k)));
            }
        }
        return distribuido;
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

    private List<Integer> indicesDaSetor(List<Candidato> candidatos, List<Integer> daSubclasse, Long setorId) {
        return daSubclasse.stream()
                .filter(i -> setorId.equals(candidatos.get(i).setorId()))
                .toList();
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

    /** Alertas do motor (§31) — o que o usuário precisa saber ANTES de decidir. */
    private List<RankingAportesDTO.Alerta> alertas(ComparativoResponseDTO comparativo,
                                                   List<Candidato> candidatos,
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

        for (Candidato c : candidatos) {
            if (c.limiteAtingido()) {
                alertas.add(new RankingAportesDTO.Alerta("ATIVO_LIMITE", c.nome()
                        + " atingiu o limite de concentração de " + formatar(c.limiteMaximo())
                        + "% — não recebe novos aportes."));
            }
            if (!c.bloqueios().isEmpty()) {
                alertas.add(new RankingAportesDTO.Alerta("BLOQUEIO", c.nome() + ": "
                        + String.join("; ", c.bloqueios()) + "."));
            }
        }

        long semAvaliacao = candidatos.stream()
                .filter(c -> c.estado() == RankingAportesDTO.EstadoAtivo.SEM_AVALIACAO)
                .count();
        if (semAvaliacao > 0) {
            alertas.add(new RankingAportesDTO.Alerta("SEM_AVALIACAO", semAvaliacao
                    + " item(ns) sem avaliação: o termo de qualidade não entrou na conta deles (nada foi zerado). "
                    + "Para bloquear, marque um critério eliminatório no checklist."));
        }

        long semTickerSemSubclasse = itens.stream()
                .filter(i -> !i.vinculado() && i.subclasse_id() == null)
                .count();
        if (semTickerSemSubclasse > 0) {
            alertas.add(new RankingAportesDTO.Alerta("SEM_SUBCLASSE", semTickerSemSubclasse
                    + " posição(ões) sem ticker fora de qualquer subclasse: classifique-as em Carteira Ideal "
                    + "para entrarem no alvo da classe."));
        }

        // §24: concentração RECENTE — quantas vezes o dinheiro já foi para o mesmo item.
        for (Candidato c : candidatos) {
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
                            : ": todas as classes elegíveis já estão completas.")));
        }
        return alertas;
    }

    /**
     * Ordem FIXA em que as travas do motor são aplicadas (§36). Aparece na tela
     * para o usuário saber exatamente o que decide antes do quê — o sistema
     * nunca esconde um conflito entre regras.
     */
    private List<String> precedencia() {
        return List.of(
                "1. Critério eliminatório (bloqueia)",
                "2. Limite máximo de concentração (bloqueia)",
                "3. Déficit da CLASSE (define quanto entra no nível)",
                "4. Déficit da SUBCLASSE (dentro da classe)",
                "5. Déficit do SETOR (dentro da subclasse)",
                "6. Déficit do ATIVO (teto: ideal + tolerância)",
                "7. Fator de momento (0 a 1)",
                "8. Contribution Score e estratégia (divide dentro do nível)");
    }

    /**
     * Cálculo aberto do Contribution Score (§34): a conta exata que foi feita,
     * com os termos normalizados e os pesos aplicados, para o usuário reproduzir.
     */
    private String formula(Candidato c, ScoreConfigModel config) {
        List<String> termos = new ArrayList<>();
        BigDecimal soma = BigDecimal.ZERO;

        if (c.qualityNorm() != null) {
            termos.add("+" + fmt(config.getPesoQuality()) + "×" + fmt(c.qualityNorm()));
            soma = soma.add(config.getPesoQuality());
        }
        if (c.momentoNorm() != null) {
            termos.add("+" + fmt(config.getPesoMomento()) + "×" + fmt(c.momentoNorm()));
            soma = soma.add(config.getPesoMomento());
        }
        termos.add("+" + fmt(config.getPesoDeficit()) + "×" + fmt(c.deficitNorm()));
        soma = soma.add(config.getPesoDeficit());
        termos.add("−" + fmt(config.getPesoExcesso()) + "×" + fmt(c.excessoNorm()));
        soma = soma.add(config.getPesoExcesso());
        termos.add("+" + fmt(config.getPesoPrioridade()) + "×" + fmt(c.prioridadeNorm()));
        soma = soma.add(config.getPesoPrioridade());

        if (soma.compareTo(BigDecimal.ZERO) == 0) {
            return "sem pesos configurados — o Contribution Score fica 0";
        }
        return "100 × [ " + String.join(" ", termos) + " ] ÷ " + fmt(soma)
                + " = " + fmt(c.contribution());
    }

    private String fmt(BigDecimal valor) {
        return (valor == null) ? "0" : valor.stripTrailingZeros().toPlainString();
    }

    private String fmt(double valor) {
        return BigDecimal.valueOf(valor).setScale(4, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
    }

    /**
     * CENÁRIOS (§33): mesmos dados da carteira, PESOS de decisão diferentes. Não
     * altera nada da carteira — só mostra como a distribuição mudaria se outro
     * critério pesasse mais. Serve para o usuário ver o efeito antes de escolher.
     */
    private List<RankingAportesDTO.CenarioDTO> cenarios(MeusAtivosResponseDTO meus,
                                                        ComparativoResponseDTO comparativo,
                                                        Avaliacoes avaliacoes,
                                                        AportesRecentes recentes,
                                                        BigDecimal valorAporte,
                                                        ScoreConfigModel config) {
        if (valorAporte == null || valorAporte.signum() <= 0) {
            return List.of();
        }
        List<RankingAportesDTO.CenarioDTO> cenarios = new ArrayList<>();
        cenarios.add(cenario("Conservador",
                "Prioriza qualidade e evita o que já passou do alvo.",
                config, meus, comparativo, avaliacoes, recentes, valorAporte,
                new BigDecimal("5"), new BigDecimal("2"), new BigDecimal("3"), new BigDecimal("1"), new BigDecimal("4")));
        cenarios.add(cenario("Balanceado",
                "Equilibra déficit, qualidade e momento (os seus pesos atuais).",
                config, meus, comparativo, avaliacoes, recentes, valorAporte,
                config.getPesoQuality(), config.getPesoDeficit(), config.getPesoExcesso(),
                config.getPesoPrioridade(), config.getPesoMomento()));
        cenarios.add(cenario("Estrutural",
                "Prioriza fechar o déficit de quem está mais longe da meta.",
                config, meus, comparativo, avaliacoes, recentes, valorAporte,
                new BigDecimal("1"), new BigDecimal("6"), new BigDecimal("2"), new BigDecimal("1"), new BigDecimal("1")));
        return cenarios;
    }

    private RankingAportesDTO.CenarioDTO cenario(String nome, String descricao, ScoreConfigModel base,
                                                MeusAtivosResponseDTO meus, ComparativoResponseDTO comparativo,
                                                Avaliacoes avaliacoes, AportesRecentes recentes,
                                                BigDecimal valorAporte,
                                                BigDecimal pesoQuality, BigDecimal pesoDeficit,
                                                BigDecimal pesoExcesso, BigDecimal pesoPrioridade,
                                                BigDecimal pesoMomento) {
        ScoreConfigModel cfg = comPesos(base, pesoQuality, pesoDeficit, pesoExcesso,
                pesoPrioridade, pesoMomento);

        List<Candidato> cs = candidatos(meus, comparativo, avaliacoes, cfg, recentes);
        cs.sort(Comparator.comparing(Candidato::contribution, Comparator.reverseOrder()));
        Orcamento o = ratear(cs, comparativo, valorAporte, cfg);

        String pesos = "qualidade " + fmt(pesoQuality) + " · déficit " + fmt(pesoDeficit)
                + " · excesso " + fmt(pesoExcesso) + " · prioridade " + fmt(pesoPrioridade)
                + " · momento " + fmt(pesoMomento);
        if (o == null) {
            return new RankingAportesDTO.CenarioDTO(nome, descricao, pesos,
                    moeda(0d), valorAporte, List.of());
        }
        List<RankingAportesDTO.ItemCenarioDTO> top = new ArrayList<>();
        for (int i = 0; i < cs.size() && top.size() < 5; i++) {
            BigDecimal sugestao = o.porCandidato().get(i);
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
                                      BigDecimal prioridade, BigDecimal momento) {
        ScoreConfigModel cfg = new ScoreConfigModel();
        cfg.setPesoQuality(quality);
        cfg.setPesoDeficit(deficit);
        cfg.setPesoExcesso(excesso);
        cfg.setPesoPrioridade(prioridade);
        cfg.setPesoMomento(momento);
        cfg.setEstrategiaAporte(base.getEstrategiaAporte());
        cfg.setRedistribuir(base.getRedistribuir());
        cfg.setMomentoFaixa1(base.getMomentoFaixa1());
        cfg.setMomentoFaixa2(base.getMomentoFaixa2());
        cfg.setMomentoFaixa3(base.getMomentoFaixa3());
        cfg.setMomentoFaixa4(base.getMomentoFaixa4());
        return cfg;
    }

    /** Explicação legível do valor não alocado (§32) — nunca apenas "R$ X não alocado". */
    private String explicacaoNaoAlocado(BigDecimal naoAlocado, List<Candidato> candidatos,
                                        ComparativoResponseDTO comparativo) {
        if (naoAlocado == null || naoAlocado.signum() <= 0) {
            return null;
        }
        long noTeto = candidatos.stream().filter(c -> c.teto().signum() <= 0).count();
        long bloqueados = candidatos.stream().filter(c -> !c.bloqueios().isEmpty()).count();
        long classesAcima = comparativo.classes().stream()
                .filter(c -> statusDe(c.percentual_atual(), c.percentual_ideal(), c.tolerancia(), c.limite_maximo())
                        == RankingAportesDTO.StatusNivel.ACIMA)
                .count();

        List<String> razoes = new ArrayList<>();
        if (classesAcima > 0) {
            razoes.add(classesAcima + " classe(s) já estão acima do alvo");
        }
        if (noTeto > 0) {
            razoes.add(noTeto + " ativo(s) já atingiram o próprio alvo (déficit + tolerância)");
        }
        if (bloqueados > 0) {
            razoes.add(bloqueados + " ativo(s) bloqueados por critério eliminatório ou limite de concentração");
        }
        if (razoes.isEmpty()) {
            razoes.add("nenhuma classe/ativo elegível tinha espaço para receber mais");
        }
        return formatar(naoAlocado) + " ficaram não alocados porque " + String.join(", ", razoes)
                + ". O dinheiro não foi empurrado para quem já está no alvo.";
    }

    private String formatar(BigDecimal valor) {
        if (valor == null) {
            return "0,00";
        }
        return valor.setScale(2, RoundingMode.HALF_UP).toPlainString();
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
