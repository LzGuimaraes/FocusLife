package dev.LzGuimaraes.FocusLifeHub.Planejamento.Aporte;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
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
import dev.LzGuimaraes.FocusLifeHub.Planejamento.Avaliacao.ChecklistAtivoService;
import dev.LzGuimaraes.FocusLifeHub.Planejamento.Avaliacao.dto.ChecklistAtivoDTO;
import dev.LzGuimaraes.FocusLifeHub.Planejamento.Aporte.dto.RankingAportesDTO;
import dev.LzGuimaraes.FocusLifeHub.Planejamento.CarteiraIdeal.CarteiraIdealService;
import dev.LzGuimaraes.FocusLifeHub.Planejamento.CarteiraIdeal.dto.ComparativoResponseDTO;
import dev.LzGuimaraes.FocusLifeHub.Planejamento.CarteiraIdeal.dto.MeusAtivosResponseDTO;
import dev.LzGuimaraes.FocusLifeHub.Planejamento.comum.CarteiraLookup;

/**
 * RANKING DE APORTE — o motor de decisão, na versão curta.
 *
 *   DÉFICIT       quanto falta para a Carteira Ideal (classe e subclasse);
 *   ELEGIBILIDADE esse ativo PODE receber agora (déficit do nível, teto próprio,
 *                 limite de concentração e critério eliminatório do checklist);
 *   NOTA          entre os que podem, qual vem primeiro — a NOTA DO CHECKLIST,
 *                 que o usuário dá por subclasse (uma página, uma nota por ativo);
 *   ALOCAÇÃO      quanto cabe em cada um, com teto no déficit.
 *
 * Não existe mais: pesos configuráveis, termos de score, cenários, rebalanceamento,
 * precedência de travas, preço (bloqueio ou oportunidade), momento nem prioridade
 * manual. A decisão inteira é "onde falta" + "quem tem a melhor nota".
 */
@Service
public class AporteService {

    private final CarteiraIdealService carteiraIdealService;
    private final ChecklistAtivoService checklistService;
    private final AlocacaoService alocacaoService;
    private final ElegibilidadeService elegibilidadeService;
    private final CarteiraLookup carteiraLookup;

    public AporteService(CarteiraIdealService carteiraIdealService,
                         ChecklistAtivoService checklistService,
                         AlocacaoService alocacaoService,
                         ElegibilidadeService elegibilidadeService,
                         CarteiraLookup carteiraLookup) {
        this.carteiraIdealService = carteiraIdealService;
        this.checklistService = checklistService;
        this.alocacaoService = alocacaoService;
        this.elegibilidadeService = elegibilidadeService;
        this.carteiraLookup = carteiraLookup;
    }

    /**
     * Ranking de aporte da carteira.
     *
     * @param valorAporte quando informado, calcula também a distribuição do aporte.
     */
    @Transactional(readOnly = true)
    public RankingAportesDTO.Response ranking(Long carteiraId, BigDecimal valorAporte) {
        CarteiraInvestimentoModel carteira = carteiraLookup.exigirCarteiraDoUsuario(carteiraId);

        ComparativoResponseDTO comparativo = carteiraIdealService.comparativo(carteiraId);
        MeusAtivosResponseDTO meus = carteiraIdealService.meusAtivos(carteiraId);
        Notas notas = notas();

        // A REFERÊNCIA do cálculo é o patrimônio DEPOIS do aporte: o alvo de cada
        // classe/ativo é um % do total, então o dinheiro novo aumenta o alvo — e é
        // essa diferença que o aporte preenche. Os valores ATUAIS não são tocados
        // aqui: o aporte só chega ao ativo na distribuição (valorFinal) —
        // ver `ReferenciaAporte`.
        ComparativoResponseDTO referencia = ReferenciaAporte.comAporte(comparativo, valorAporte);

        List<AporteCandidato> calculados = candidatos(meus, referencia, nz(comparativo.valor_total()), notas);

        // Elegível primeiro (o descartado NÃO concorre), depois NOTA do checklist,
        // depois nome. É toda a ordenação que existe.
        calculados.sort(Comparator
                .comparing(AporteCandidato::elegivel, Comparator.reverseOrder())
                .thenComparing(AporteCandidato::nota, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(AporteCandidato::nome, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));

        OrcamentoAporte orcamento = alocacaoService.ratear(calculados, referencia, valorAporte);

        List<RankingAportesDTO.Item> itens = new ArrayList<>();
        int posicao = 0;
        for (int i = 0; i < calculados.size(); i++) {
            AporteCandidato c = calculados.get(i);
            BigDecimal sugestao = (orcamento != null) ? orcamento.deCandidato(i) : null;
            // A posição no ranking é só dos ELEGÍVEIS: o descartado aparece para
            // explicar o motivo, mas não ocupa colocação.
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
                    c.nota(),
                    c.avaliada(),
                    c.perguntas(),
                    c.respondidas(),
                    c.bloqueios(),
                    c.elegivel(),
                    c.status(),
                    c.motivos(),
                    c.limiteMaximo(),
                    c.limiteAtingido(),
                    c.capacidade(),
                    c.percentualAtual(),
                    c.percentualIdeal(),
                    c.valorAtual(),
                    c.valorIdeal(),
                    c.deficit(),
                    c.excesso(),
                    c.tolerancia(),
                    sugestao,
                    c.precoUnitario(),
                    quantidadeDe(c, sugestao),
                    motivo(c, sugestao),
                    acaoDe(c, sugestao)));
        }

        BigDecimal alocado = (orcamento != null) ? orcamento.alocado() : null;
        BigDecimal naoAlocado = (orcamento != null) ? valorAporte.subtract(alocado) : null;
        int totalElegiveis = (int) calculados.stream().filter(AporteCandidato::elegivel).count();

        return new RankingAportesDTO.Response(
                carteira.getId(),
                carteira.getMoeda(),
                comparativo.valor_total(),
                (valorAporte != null) ? valorAporte : null,
                (valorAporte != null && valorAporte.signum() > 0) ? referencia.valor_total() : null,
                alocado,
                naoAlocado,
                totalElegiveis,
                calculados.size() - totalElegiveis,
                explicacaoNaoAlocado(naoAlocado, calculados, orcamento),
                avisos(referencia, calculados, itens),
                alertas(calculados, itens, naoAlocado),
                classesDto(referencia, calculados, (orcamento != null) ? orcamento.porClasse() : Map.of(),
                        (orcamento != null) ? orcamento.porSubclasse() : Map.of()),
                itens);
    }


    /* ══════════════════════════════════════════════════════════════════
       NOTAS DO CHECKLIST
       ══════════════════════════════════════════════════════════════════ */

    /** Nota do checklist de um ativo (por ticker do catálogo ou por posição). */
    private record Nota(BigDecimal valor, int perguntas, int respondidas, List<String> bloqueios) {
        boolean avaliada() {
            return valor != null;
        }
    }

    private record Notas(Map<UUID, Nota> porCatalogo, Map<Long, Nota> porPosicao) {
        Nota de(UUID catalogoId, List<Long> ativoIds) {
            if (catalogoId != null) {
                Nota porTicker = porCatalogo.get(catalogoId);
                if (porTicker != null) {
                    return porTicker;
                }
            }
            for (Long posicao : ativoIds) {
                Nota daPosicao = porPosicao.get(posicao);
                if (daPosicao != null) {
                    return daPosicao;
                }
            }
            return null;
        }
    }

    /** Consolida as notas do usuário: o checklist por subclasse já virou nota por ativo. */
    private Notas notas() {
        Map<UUID, Nota> porCatalogo = new HashMap<>();
        Map<Long, Nota> porPosicao = new HashMap<>();
        for (ChecklistAtivoDTO.AtivoAvaliado a : checklistService.resumoPorAtivo()) {
            Nota nota = new Nota(a.quality_score(), a.total_perguntas(), a.total_respondidas(), a.bloqueios());
            if (a.ativo_cadastro_id() != null) {
                porCatalogo.putIfAbsent(a.ativo_cadastro_id(), nota);
            }
            if (a.ativo_id() != null) {
                porPosicao.putIfAbsent(a.ativo_id(), nota);
            }
        }
        return new Notas(porCatalogo, porPosicao);
    }

    /* ══════════════════════════════════════════════════════════════════
       CANDIDATOS (situação + elegibilidade + capacidade)
       ══════════════════════════════════════════════════════════════════ */

    private List<AporteCandidato> candidatos(MeusAtivosResponseDTO meus, ComparativoResponseDTO comparativo,
                                             double patrimonioAtual, Notas notas) {
        // `comparativo` aqui já é a referência (alvos sobre o patrimônio PROJETADO).
        double patrimonioProjetado = nz(comparativo.valor_total());
        double tol = AlocacaoService.tolerancia(patrimonioProjetado);

        List<AporteCandidato> lista = new ArrayList<>();
        for (MeusAtivosResponseDTO.MeuAtivoDTO a : meus.ativos()) {
            boolean temMeta = a.meta_id() != null;

            // As três grandezas do ativo, com os nomes da regra:
            //   valorAtual        = o que existe HOJE (o aporte não entra aqui)
            //   valorAlvoProjetado= percentualIdeal × R
            //   deficit           = max(0, alvo − atual)
            double valorAtual = nz(a.valor_atual());
            double valorAlvoProjetado = temMeta ? valorIdealDe(a.percentual_ideal(), patrimonioProjetado) : 0d;
            double deficit = (temMeta && valorAlvoProjetado - valorAtual > tol)
                    ? valorAlvoProjetado - valorAtual : 0d;
            double excesso = (temMeta && valorAtual - valorAlvoProjetado > tol)
                    ? valorAtual - valorAlvoProjetado : 0d;

            Nota nota = notas.de(a.ativo_cadastro_id(), a.ativo_ids());
            BigDecimal tolerancia = (a.tolerancia() != null) ? a.tolerancia() : BigDecimal.ZERO;

            // CAPACIDADE = déficit puro (`valorAlvoProjetado − valorAtual`), limitada
            // pelo teto de concentração. A TOLERÂNCIA não entra: ela só classifica
            // ABAIXO/EQUILIBRADO/ACIMA na tela.
            // Posição SEM meta própria herda a capacidade do bucket (subclasse/classe)
            // — é o que faz a renda fixa participar.
            double capacidade = temMeta
                    ? capacidadeDoAtivo(valorAlvoProjetado, valorAtual, patrimonioProjetado, a.limite_maximo())
                    : capacidadeHerdada(a, comparativo, patrimonioProjetado);
            BigDecimal capacidadeMoeda = alocacaoService.moeda(capacidade);

            boolean limiteAtingido = limiteAtingido(valorAtual, patrimonioProjetado, a.limite_maximo());
            boolean semAvaliacao = nota == null || !nota.avaliada();
            boolean nivelSemCapacidade = temMeta
                    ? !temDeficitNoNivel(a, comparativo, tol)
                    : classeSemCapacidade(a.classe(), comparativo, tol);

            ElegibilidadeService.Veredito veredito = elegibilidadeService.avaliar(
                    new ElegibilidadeService.Entrada(
                            (nota != null) ? nota.bloqueios() : List.of(),
                            semAvaliacao, limiteAtingido, nivelSemCapacidade, capacidadeMoeda));

            lista.add(new AporteCandidato(
                    a.ativo_cadastro_id(), a.meta_id(), a.ticker(), a.vinculado(),
                    a.subclasse_id(), a.subclasse_nome(),
                    (a.classe() != null) ? a.classe() : CategoriaInvestimento.OUTROS,
                    (nota != null) ? nota.valor() : null,
                    (nota != null) && nota.avaliada(),
                    (nota != null) ? nota.perguntas() : 0,
                    (nota != null) ? nota.respondidas() : 0,
                    (nota != null) ? nota.bloqueios() : List.of(),
                    veredito.elegivel(), veredito.status(), veredito.motivos(),
                    a.limite_maximo(), limiteAtingido, capacidadeMoeda,
                    // percentualAtualProjetado = valorAtual ÷ R — o MESMO valor atual
                    // de sempre (o aporte ainda não foi distribuído). É por isso que
                    // ele CAI quando o aporte é informado.
                    ReferenciaAporte.percentualAtualProjetado(moeda(valorAtual), comparativo.valor_total()),
                    a.percentual_ideal(),
                    moeda(valorAtual), moeda(valorAlvoProjetado),
                    moeda(deficit), moeda(excesso), tolerancia,
                    a.preco_atual()));
        }
        return lista;
    }

    /**
     * Teto do ativo: {@code valorAlvoProjetado − valorAtual} (déficit puro),
     * limitado pelo teto de concentração (% do patrimônio projetado − atual).
     */
    private double capacidadeDoAtivo(double valorAlvoProjetado, double valorAtual,
                                     double patrimonioProjetado, BigDecimal limite) {
        double teto = Math.max(0d, valorAlvoProjetado - valorAtual);
        if (limite != null && limite.signum() > 0) {
            double tetoLimite = Math.max(0d, limite.doubleValue() / 100d * patrimonioProjetado - valorAtual);
            teto = Math.min(teto, tetoLimite);
        }
        return teto;
    }

    /** Capacidade herdada por posição sem meta: a da subclasse dela ou, na falta, a da classe. */
    private double capacidadeHerdada(MeusAtivosResponseDTO.MeuAtivoDTO a, ComparativoResponseDTO comparativo,
                                     double patrimonioProjetado) {
        for (ComparativoResponseDTO.ClasseComparativoDTO c : comparativo.classes()) {
            if (c.classe() != a.classe()) {
                continue;
            }
            if (a.subclasse_id() != null) {
                for (ComparativoResponseDTO.SubclasseComparativoDTO s : c.subclasses()) {
                    if (s.id().equals(a.subclasse_id())) {
                        return Math.max(0d, nz(s.valor_ideal()) - nz(s.valor_atual()));
                    }
                }
            }
            return alocacaoService
                    .deficit(c.percentual_ideal(), c.percentual_atual(), patrimonioProjetado)
                    .doubleValue();
        }
        return 0d;
    }

    /** Limite atingido: o % do ativo sobre o patrimônio PROJETADO alcançou o teto. */
    private boolean limiteAtingido(double valorAtual, double patrimonioProjetado, BigDecimal limite) {
        if (limite == null || limite.signum() <= 0 || patrimonioProjetado <= 0d) {
            return false;
        }
        return valorAtual / patrimonioProjetado * 100d >= limite.doubleValue();
    }

    /**
     * true = a CLASSE (e a subclasse, quando o ativo está numa) tem déficit — é o
     * que dá capacidade ao nível.
     *
     * A conta da subclasse é em REAIS, igual à do rateio: alvo − valor atual. O
     * percentual da subclasse é uma FATIA DA CLASSE, então usá-lo aqui como % do
     * patrimônio daria "déficit zero" para todo ativo dentro de uma subclasse de
     * 100%.
     */
    private boolean temDeficitNoNivel(MeusAtivosResponseDTO.MeuAtivoDTO a,
                                      ComparativoResponseDTO comparativo, double tol) {
        for (ComparativoResponseDTO.ClasseComparativoDTO c : comparativo.classes()) {
            if (c.classe() != a.classe()) {
                continue;
            }
            if (!temDeficit(c.percentual_ideal(), c.percentual_atual(), comparativo, tol)) {
                return false;
            }
            if (a.subclasse_id() == null) {
                return true;
            }
            for (ComparativoResponseDTO.SubclasseComparativoDTO s : c.subclasses()) {
                if (s.id().equals(a.subclasse_id())) {
                    return Math.max(0d, nz(s.valor_ideal()) - nz(s.valor_atual())) > tol;
                }
            }
            return true;
        }
        return false;
    }

    private boolean classeSemCapacidade(CategoriaInvestimento classe, ComparativoResponseDTO comparativo,
                                        double tol) {
        for (ComparativoResponseDTO.ClasseComparativoDTO c : comparativo.classes()) {
            if (c.classe() == classe) {
                return !temDeficit(c.percentual_ideal(), c.percentual_atual(), comparativo, tol);
            }
        }
        return true;   // classe fora da Carteira Ideal: sem alvo, sem capacidade
    }

    private boolean temDeficit(BigDecimal percentualIdeal, BigDecimal percentualAtual,
                               ComparativoResponseDTO comparativo, double tol) {
        double total = nz(comparativo.valor_total());
        return alocacaoService.deficit(percentualIdeal, percentualAtual, total)
                .doubleValue() > tol;
    }

    /**
     * Alvo do item: {@code percentualIdeal × patrimonioProjetado}
     * (0 quando o item não tem meta própria).
     */
    private double valorIdealDe(BigDecimal percentualIdeal, double patrimonioProjetado) {
        return ReferenciaAporte.valorAlvoProjetado(percentualIdeal,
                BigDecimal.valueOf(patrimonioProjetado)).doubleValue();
    }

    /* ══════════════════════════════════════════════════════════════════
       ONTEM ENTRA O DINHEIRO (classe → subclasse)
       ══════════════════════════════════════════════════════════════════ */

    private List<RankingAportesDTO.ClasseAporteDTO> classesDto(
            ComparativoResponseDTO comparativo,
            List<AporteCandidato> candidatos,
            Map<CategoriaInvestimento, BigDecimal> sugeridoPorClasse,
            Map<Long, BigDecimal> sugeridoPorSubclasse) {

        double total = nz(comparativo.valor_total());
        List<RankingAportesDTO.ClasseAporteDTO> classes = new ArrayList<>();
        for (ComparativoResponseDTO.ClasseComparativoDTO c : comparativo.classes()) {
            BigDecimal sugerido = sugeridoPorClasse.getOrDefault(c.classe(), moeda(0d));
            List<RankingAportesDTO.SubclasseAporteDTO> subs = c.subclasses().stream()
                    .map(s -> {
                        BigDecimal sugeridoSub = sugeridoPorSubclasse.getOrDefault(s.id(), moeda(0d));
                        return new RankingAportesDTO.SubclasseAporteDTO(
                                s.id(), s.nome(), s.percentual_atual(), s.percentual_ideal(),
                                s.valor_atual(), s.valor_ideal(), s.deficit(), s.excesso(),
                                s.tolerancia(), s.limite_maximo(),
                                statusDe(s.percentual_atual(), s.percentual_ideal(), s.tolerancia(), s.limite_maximo()),
                                sugeridoSub,
                                (sugeridoSub.signum() <= 0 && sugerido.signum() > 0)
                                        ? "Neste aporte nada foi direcionado a posições desta subclasse."
                                        : null);
                    })
                    .toList();
            classes.add(new RankingAportesDTO.ClasseAporteDTO(
                    c.classe(), c.percentual_atual(), c.percentual_ideal(),
                    c.valor_atual(), c.valor_ideal(), c.deficit(), c.excesso(),
                    c.tolerancia(), c.limite_maximo(),
                    statusDe(c.percentual_atual(), c.percentual_ideal(), c.tolerancia(), c.limite_maximo()),
                    sugerido,
                    motivoDaClasse(c, sugerido, total, candidatos),
                    subs));
        }
        return classes;
    }

    private RankingAportesDTO.StatusNivel statusDe(BigDecimal atual, BigDecimal ideal,
                                                   BigDecimal tolerancia, BigDecimal limite) {
        if (ideal == null || ideal.signum() <= 0) {
            return RankingAportesDTO.StatusNivel.SEM_ALVO;
        }
        double tol = nz(tolerancia);
        double a = nz(atual);
        double i = ideal.doubleValue();
        if (limite != null && limite.signum() > 0 && a >= limite.doubleValue()) {
            return RankingAportesDTO.StatusNivel.ACIMA;
        }
        if (a > i + tol) {
            return RankingAportesDTO.StatusNivel.ACIMA;
        }
        if (a < i - tol) {
            return RankingAportesDTO.StatusNivel.ABAIXO;
        }
        return RankingAportesDTO.StatusNivel.EQUILIBRADO;
    }

    /**
     * Por que a classe recebeu (ou não) parte do aporte.
     *
     * O motivo diz o que REALMENTE travou: antes ele dizia "nenhum ativo
     * ELEGÍVEL" mesmo quando havia elegíveis sem capacidade (metas que já
     * espelhavam a carteira), o que fazia o usuário procurar o problema no
     * lugar errado.
     */
    private String motivoDaClasse(ComparativoResponseDTO.ClasseComparativoDTO c, BigDecimal sugerido,
                                  double total, List<AporteCandidato> candidatos) {
        BigDecimal limite = c.limite_maximo();
        BigDecimal atual = c.percentual_atual();
        if (limite != null && limite.signum() > 0 && atual != null
                && atual.doubleValue() >= limite.doubleValue()) {
            return "Não recebe: limite de concentração de " + formatar(limite) + "% atingido.";
        }
        BigDecimal falta = alocacaoService.deficit(c.percentual_ideal(), atual, total);
        if (falta.signum() <= 0) {
            return "Não recebe: está no alvo (dentro da tolerância de " + formatar(c.tolerancia()) + "%).";
        }
        if (sugerido.signum() <= 0) {
            List<AporteCandidato> daClasse = candidatos.stream()
                    .filter(cand -> cand.classe() == c.classe())
                    .toList();
            boolean algumElegivel = daClasse.stream().anyMatch(AporteCandidato::elegivel);
            boolean algumaCapacidade = daClasse.stream()
                    .anyMatch(cand -> cand.elegivel() && nz(cand.capacidade()) > 0);
            // "Travou por capacidade" ≠ "travou por regra": o ativo que só está
            // no próprio alvo não é um problema de configuração, é o esperado.
            long soSemCapacidade = daClasse.stream()
                    .filter(cand -> cand.status() == StatusElegibilidade.SEM_CAPACIDADE
                            || cand.status() == StatusElegibilidade.CLASSE_SEM_CAPACIDADE)
                    .count();
            long travados = daClasse.size() - soSemCapacidade;

            if (daClasse.isEmpty()) {
                return "Tem déficit de " + formatar(falta)
                        + ", mas nenhum ativo da classe está na carteira: o aporte só entra em posição que existe "
                        + "(meta de ativo ainda não comprado não recebe).";
            }
            if (algumElegivel && !algumaCapacidade) {
                return "Tem déficit de " + formatar(falta)
                        + ", mas os ativos da classe já estão no próprio alvo — nada cabe aqui neste aporte "
                        + "(aumente o alvo do ativo para ele receber mais).";
            }
            if (!algumElegivel && soSemCapacidade > 0 && travados == 0) {
                return "Tem déficit de " + formatar(falta)
                        + ", mas os ativos da classe já estão no próprio alvo — o dinheiro fica não alocado.";
            }
            if (travados == 0) {
                return "Tem déficit de " + formatar(falta)
                        + ", mas nenhum ativo da classe pode receber agora (limite atingido, critério eliminatório "
                        + "ou sem checklist).";
            }
            return "Tem déficit de " + formatar(falta)
                    + ", mas nada foi direcionado a ela neste aporte: uns ativos já estão no próprio alvo e outros "
                    + "estão travados (limite, critério eliminatório ou sem checklist).";
        }
        return "Recebe no máximo o déficit da classe: " + formatar(falta)
                + " (ideal + tolerância − atual).";
    }

    /* ══════════════════════════════════════════════════════════════════
       EXPLICAÇÕES, AVISOS E ALERTAS
       ══════════════════════════════════════════════════════════════════ */

    /**
     * QUANTAS UNIDADES comprar com o valor sugerido.
     *
     * Ação, FII e ETF são cotas INTEIRAS (o valor já vem arredondado para baixo
     * pela alocação); cripto, renda fixa e Tesouro aceitam fração, então aqui a
     * divisão é informativa (8 casas). Sem preço conhecido, não há quantidade.
     */
    private BigDecimal quantidadeDe(AporteCandidato c, BigDecimal sugestao) {
        BigDecimal preco = c.precoUnitario();
        if (sugestao == null || sugestao.signum() <= 0 || preco == null || preco.signum() <= 0) {
            return null;
        }
        return sugestao.divide(preco,
                AlocacaoService.compraEmUnidadesInteiras(c.classe()) ? 0 : 8, RoundingMode.DOWN);
    }

    private RankingAportesDTO.AcaoAtivo acaoDe(AporteCandidato c, BigDecimal sugestao) {
        if (!c.elegivel()) {
            return (c.status() == StatusElegibilidade.SEM_AVALIACAO)
                    ? RankingAportesDTO.AcaoAtivo.AVALIAR
                    : RankingAportesDTO.AcaoAtivo.NAO_APORTAR;
        }
        if (sugestao != null && sugestao.signum() > 0) {
            return RankingAportesDTO.AcaoAtivo.APORTAR;
        }
        return (c.nota() == null) ? RankingAportesDTO.AcaoAtivo.AVALIAR : RankingAportesDTO.AcaoAtivo.MANTER;
    }

    /** Explicação objetiva da decisão (§29), do jeito mais curto que explique. */
    private String motivo(AporteCandidato c, BigDecimal sugestao) {
        String nota = (c.nota() != null)
                ? "Nota " + percentual(c.nota()) + " (" + c.respondidas() + "/" + c.perguntas() + " perguntas)"
                : "Sem nota no checklist";
        if (!c.elegivel()) {
            String primeiro = c.motivos().isEmpty() ? c.status().getLabel() : c.motivos().get(0);
            return "Não aporta agora: " + primeiro;
        }
        if (sugestao != null && sugestao.signum() > 0) {
            String teto = (c.limiteAtingido())
                    ? " Limite de concentração atingido: o resto vai para os próximos."
                    : " Respeita o teto de " + formatar(c.capacidade()) + ".";
            BigDecimal unidades = quantidadeDe(c, sugestao);
            String compra = (unidades != null)
                    ? " Compre " + unidadesTexto(unidades, c)
                        + " a " + formatar(c.precoUnitario()) + " = " + formatar(sugestao) + "."
                    : " Recebe " + formatar(sugestao) + " neste aporte.";
            return compra + " " + nota + "." + teto;
        }
        if (sugestao == null) {
            return nota + ". Informe o valor do aporte para ver quanto entra.";
        }
        return "Nada neste aporte: " + nota.toLowerCase() + ", mas o valor já foi direcionado a quem tem prioridade.";
    }

    /** Unidades em texto: "72 cotas" ou "0,0031 unidades" (cripto/renda fixa). */
    private String unidadesTexto(BigDecimal unidades, AporteCandidato c) {
        String numero = unidades.stripTrailingZeros().toPlainString()
                .replace(".", ",");
        if (unidades.compareTo(BigDecimal.ONE) > 0) {
            String plural = AlocacaoService.compraEmUnidadesInteiras(c.classe()) ? " cotas" : " unidades";
            return numero + plural;
        }
        return AlocacaoService.compraEmUnidadesInteiras(c.classe())
                ? numero + " cota"
                : numero + " unidades";
    }

    /** "R$ 1.234,56" — o texto das explicações. */
    /** Nota em porcentagem: "90,0%". */
    private String percentual(BigDecimal valor) {
        if (valor == null) {
            return "-";
        }
        return valor.setScale(1, RoundingMode.HALF_UP).toPlainString().replace(".", ",") + "%";
    }

    private String formatar(BigDecimal valor) {
        if (valor == null) {
            return "-";
        }
        return "R$ " + valor.setScale(2, RoundingMode.HALF_UP).toPlainString().replace(".", ",");
    }

    private String explicacaoNaoAlocado(BigDecimal naoAlocado, List<AporteCandidato> candidatos,
                                        OrcamentoAporte orcamento) {
        if (naoAlocado == null || naoAlocado.signum() <= 0) {
            return null;
        }
        // Sobrou dinheiro e ALGUÉM ainda tinha espaço? Então o que travou foi a
        // UNIDADE: o valor não fecha uma cota inteira do que sobrou com espaço.
        for (int i = 0; i < candidatos.size(); i++) {
            AporteCandidato c = candidatos.get(i);
            BigDecimal preco = c.precoUnitario();
            boolean temEspaco = orcamento != null
                    && (orcamento.deCandidato(i).doubleValue() + 1e-9) < nz(c.capacidade());
            if (c.elegivel() && temEspaco && preco != null && preco.signum() > 0) {
                return "Sobraram " + formatar(naoAlocado) + ": é troco de arredondamento — não fecha uma "
                        + "cota inteira dos ativos que ainda têm espaço (a cota mais barata deles custa "
                        + formatar(preco) + "). Guarde para o próximo aporte.";
            }
        }
        return explicacaoNaoAlocadoGenerica(naoAlocado, candidatos);
    }

    /** Sobra que não é troco de arredondamento: falta de espaço nos ativos. */
    private String explicacaoNaoAlocadoGenerica(BigDecimal naoAlocado, List<AporteCandidato> candidatos) {
        if (naoAlocado == null || naoAlocado.signum() <= 0) {
            return null;
        }
        long descartados = candidatos.stream().filter(c -> !c.elegivel()).count();
        List<String> razoes = new ArrayList<>();
        if (descartados > 0) {
            razoes.add(descartados + " ativo(s) foram descartados na elegibilidade (nível sem déficit, "
                    + "teto próprio, limite de concentração ou critério eliminatório do checklist)");
        }
        razoes.add("os ativos elegíveis já estão completos (teto = déficit do ativo)");
        return formatar(naoAlocado) + " sem destino neste aporte: " + String.join(" e ", razoes) + ".";
    }

    private List<String> avisos(ComparativoResponseDTO comparativo, List<AporteCandidato> candidatos,
                                List<RankingAportesDTO.Item> itens) {
        List<String> avisos = new ArrayList<>(comparativo.avisos());

        long semNota = itens.stream().filter(i -> !i.avaliada()).count();
        if (semNota > 0) {
            avisos.add(semNota + " ativo(s) ainda sem nota no checklist: eles participam do aporte, mas ficam "
                    + "depois de quem já foi avaliado.");
        }
        long semTickerSemSubclasse = candidatos.stream()
                .filter(c -> !c.vinculado() && c.subclasseId() == null)
                .count();
        if (semTickerSemSubclasse > 0) {
            avisos.add(semTickerSemSubclasse + " posição(ões) sem ticker não estão em nenhuma subclasse: elas "
                    + "contam no total da classe e podem ficar de fora do aporte por ticker.");
        }
        return avisos;
    }

    private List<RankingAportesDTO.Alerta> alertas(List<AporteCandidato> candidatos,
                                                   List<RankingAportesDTO.Item> itens,
                                                   BigDecimal naoAlocado) {
        List<RankingAportesDTO.Alerta> alertas = new ArrayList<>();

        for (AporteCandidato c : candidatos) {
            if (c.status() == StatusElegibilidade.CRITERIO_ELIMINATORIO) {
                alertas.add(new RankingAportesDTO.Alerta("BLOQUEIO", c.nome() + ": "
                        + String.join("; ", c.bloqueios())));
            }
            if (c.limiteAtingido()) {
                alertas.add(new RankingAportesDTO.Alerta("ATIVO_LIMITE", c.nome()
                        + " atingiu o limite de concentração de " + formatar(c.limiteMaximo()) + "%."));
            }
        }

        long semNota = itens.stream().filter(i -> !i.avaliada()).count();
        if (semNota > 0) {
            alertas.add(new RankingAportesDTO.Alerta("SEM_AVALIACAO", semNota
                    + " item(ns) sem nota no checklist: a ordem entre eles fica pelo nome. "
                    + "Dê as notas na página de avaliação da subclasse."));
        }
        if (naoAlocado != null && naoAlocado.signum() > 0) {
            alertas.add(new RankingAportesDTO.Alerta("NAO_ALOCADO", formatar(naoAlocado)
                    + " não coube em nenhum ativo elegível neste aporte."));
        }
        return alertas;
    }

    /* ── Helpers numéricos ── */

    private double nz(BigDecimal valor) {
        return AlocacaoService.nz(valor);
    }

    private BigDecimal moeda(double valor) {
        return alocacaoService.moeda(valor);
    }
}
