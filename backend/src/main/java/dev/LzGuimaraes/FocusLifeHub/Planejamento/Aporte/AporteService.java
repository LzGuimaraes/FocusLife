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
 * O motor responde a TRÊS perguntas diferentes e não as mistura:
 *
 *   PERGUNTA 1  posso investir neste ativo?   → ELEGIBILIDADE (checklist, limite)
 *   PERGUNTA 2  quanto posso investir nele?   → CAPACIDADE (até o limite)
 *   PERGUNTA 3  quem deve receber mais?       → NOTA do checklist (peso)
 *
 * E uma quarta, que é consequência: quanto falta para a carteira desejada?
 * → DÉFICIT da classe/subclasse, que organiza o ORÇAMENTO (mas não veta ativo).
 *
 * A regra fundamental:
 *
 *     META não é bloqueio. NOTA não é bloqueio. DÉFICIT não é bloqueio.
 *
 * O que bloqueia: uma regra real de inelegibilidade (critério eliminatório,
 * limite atingido, classe fora da Carteira Ideal) ou a inexistência de espaço
 * até o LIMITE OPERACIONAL (meta + margem).
 *
 * Não existe mais: pesos configuráveis, termos de score, cenários,
 * rebalanceamento, precedência de travas, preço (bloqueio ou oportunidade),
 * momento nem prioridade manual.
 */
@Service
public class AporteService {

    private final CarteiraIdealService carteiraIdealService;
    private final ChecklistAtivoService checklistService;
    private final AlocacaoService alocacaoService;
    private final ElegibilidadeService elegibilidadeService;
    private final AporteConfigService configService;
    private final CarteiraLookup carteiraLookup;

    public AporteService(CarteiraIdealService carteiraIdealService,
                         ChecklistAtivoService checklistService,
                         AlocacaoService alocacaoService,
                         ElegibilidadeService elegibilidadeService,
                         AporteConfigService configService,
                         CarteiraLookup carteiraLookup) {
        this.carteiraIdealService = carteiraIdealService;
        this.checklistService = checklistService;
        this.alocacaoService = alocacaoService;
        this.elegibilidadeService = elegibilidadeService;
        this.configService = configService;
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

        // Margem operacional do usuário (3% a 5%): o limite de cada ativo é
        // `meta × (1 + margem)`. Vem da config dele e vale para todas as carteiras.
        double margem = configService.margemPercentual();

        List<AporteCandidato> calculados = candidatos(meus, referencia, margem, notas);

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
                    c.nota(),
                    c.bloqueios(),
                    c.elegivel(),
                    c.status(),
                    c.motivos(),
                    c.limiteMaximo(),
                    c.limiteAtingido(),
                    c.limiteOperacionalPercentual(),
                    c.limitePercentual(),
                    c.limiteEmReais(),
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
                moeda(margem),
                alocado,
                naoAlocado,
                totalElegiveis,
                calculados.size() - totalElegiveis,
                explicacaoNaoAlocado(naoAlocado, calculados, orcamento),
                avisos(referencia, calculados, itens),
                alertas(calculados, itens, naoAlocado),
                classesDto(referencia, calculados, margem, (orcamento != null) ? orcamento.porClasse() : Map.of(),
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
       CANDIDATOS — PERGUNTA 1 (posso?) e PERGUNTA 2 (quanto?)
       ══════════════════════════════════════════════════════════════════ */

    /**
     * Fatos por CANDIDATO, na ordem da regra:
     *
     *   1. valorAtual (o aporte NUNCA entra aqui);
     *   2. alvo projetado = meta × R  → espaço DESEJÁVEL (`deficit`);
     *   3. limite = min(meta × (1+margem), limite cadastrado) × R
     *                                → espaço PERMITIDO (`capacidade`);
     *   4. elegibilidade (checklist + limite + capacidade);
     *   5. nota do checklist — PESO, nunca permissão.
     *
     * O ponto que muda tudo: a CAPACIDADE sai do LIMITE, não da meta. Um ativo
     * exatamente na meta hoje tem `deficit` ≈ 0 mas `capacidade` > 0, porque o
     * alvo dele cresceu para `meta × R`. É por isso que ele continua candidato
     * depois de um aporte grande — antes, `capacidade = deficit` descartava a
     * carteira inteira e o dinheiro ficava parado.
     *
     * A CLASSE e a SUBCLASSE NÃO entram aqui: elas organizam o ORÇAMENTO (quem
     * reparte é o `AlocacaoService`) e um nível sem espaço simplesmente não
     * recebe — ele não zera os ativos dele (§9, §15, §16).
     */
    private List<AporteCandidato> candidatos(MeusAtivosResponseDTO meus, ComparativoResponseDTO comparativo,
                                             double margemPercentual, Notas notas) {
        // `comparativo` aqui já é a referência: alvos e percentuais sobre R = T + A.
        BigDecimal r = comparativo.valor_total();
        double tol = AlocacaoService.tolerancia(nz(r));
        Heranca heranca = heranca(meus, comparativo, margemPercentual);

        List<AporteCandidato> lista = new ArrayList<>();
        for (MeusAtivosResponseDTO.MeuAtivoDTO a : meus.ativos()) {
            boolean temMeta = a.meta_id() != null;
            double valorAtual = nz(a.valor_atual());
            Nota nota = notas.de(a.ativo_cadastro_id(), a.ativo_ids());
            BigDecimal tolerancia = (a.tolerancia() != null) ? a.tolerancia() : BigDecimal.ZERO;

            // ── LIMITE OPERACIONAL e CAPACIDADE (pergunta 2) ──
            BigDecimal alvoProjetado;
            BigDecimal limiteOperacionalPct;
            BigDecimal limitePct;
            BigDecimal limiteReais;
            BigDecimal deficit;
            BigDecimal excesso;
            double capacidadeBruta;

            if (temMeta) {
                ReferenciaAporte.Limite limite = ReferenciaAporte.limite(
                        a.percentual_ideal(), a.limite_maximo(), a.valor_atual(), r, margemPercentual);
                alvoProjetado = limite.alvoEmReais();
                limiteOperacionalPct = limite.limiteOperacionalPercentual();
                limitePct = limite.limitePercentual();
                limiteReais = limite.limiteEmReais();
                deficit = limite.deficitAteMeta();
                excesso = ReferenciaAporte.moeda(Math.max(0d, valorAtual - nz(alvoProjetado) - tol));
                capacidadeBruta = limite.capacidade().doubleValue();
            } else {
                // Sem meta própria: herda o ALVO do bucket (subclasse ou classe) — é
                // o que faz a renda fixa participar. Não existe limite percentual
                // DELE (a meta não é dele), e a capacidade é a fatia dele no espaço
                // herdado.
                alvoProjetado = ReferenciaAporte.moeda(0d);
                limiteOperacionalPct = null;
                limitePct = null;
                limiteReais = null;
                deficit = ReferenciaAporte.moeda(0d);
                excesso = ReferenciaAporte.moeda(0d);
                capacidadeBruta = heranca.de(a);
            }

            // Piso de ruído: abaixo dele não há "capacidade" de verdade (metas com 4
            // casas erram centavos, e o rateio usa o mesmo piso). Sem isto, um ativo
            // apareceria ELEGÍVEL e nunca receberia nada.
            double capacidade = (capacidadeBruta > tol) ? capacidadeBruta : 0d;
            BigDecimal percentualProjetado = ReferenciaAporte.percentualAtualProjetado(moeda(valorAtual), r);
            boolean limiteAtingido = limitePct != null && limitePct.signum() > 0
                    && percentualProjetado.doubleValue() >= limitePct.doubleValue();

            ElegibilidadeService.Veredito veredito = elegibilidadeService.avaliar(
                    new ElegibilidadeService.Entrada(
                            (nota != null) ? nota.bloqueios() : List.of(),
                            nota == null || !nota.avaliada(),
                            limiteAtingido,
                            alocacaoService.moeda(capacidade)));

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
                    a.limite_maximo(), limiteAtingido,
                    limiteOperacionalPct, limitePct, limiteReais,
                    alocacaoService.moeda(capacidade),
                    // percentualAtualProjetado = valorAtual ÷ R — o MESMO valor atual
                    // de sempre (o aporte ainda não foi distribuído). É por isso que
                    // ele CAI quando o aporte é informado.
                    percentualProjetado,
                    a.percentual_ideal(),
                    moeda(valorAtual), alvoProjetado,
                    deficit, excesso, tolerancia,
                    a.preco_atual()));
        }
        return lista;
    }

    /**
     * Espaço que as posições SEM meta própria (renda fixa, Tesouro, caixinha)
     * herdam do seu BUCKET — a subclasse quando existe, senão a classe.
     *
     * O bucket tem UM espaço (alvo até o limite operacional); dividi-lo entre as
     * posições na proporção do valor atual evita que a segunda posição da mesma
     * subclasse receba, sozinha, o bucket inteiro.
     */
    private record Heranca(
            Map<Long, Double> capacidadePorSubclasse,
            Map<Long, Double> basePorSubclasse,
            Map<CategoriaInvestimento, Double> capacidadePorClasse,
            Map<CategoriaInvestimento, Double> basePorClasse) {

        /** Fatia da posição no espaço herdado (0 quando ela não está na Carteira Ideal). */
        double de(MeusAtivosResponseDTO.MeuAtivoDTO a) {
            double valor = AlocacaoService.nz(a.valor_atual());
            if (a.subclasse_id() != null) {
                return fatia(capacidadePorSubclasse.get(a.subclasse_id()),
                        basePorSubclasse.get(a.subclasse_id()), valor);
            }
            return fatia(capacidadePorClasse.get(a.classe()), basePorClasse.get(a.classe()), valor);
        }

        private static double fatia(Double capacidade, Double base, double valor) {
            if (capacidade == null || base == null || base <= 0d) {
                return 0d;
            }
            return Math.max(0d, capacidade) * (valor / base);
        }
    }

    private static Heranca heranca(MeusAtivosResponseDTO meus, ComparativoResponseDTO comparativo,
                                   double margemPercentual) {
        BigDecimal r = comparativo.valor_total();
        Map<Long, Double> capSubclasse = new HashMap<>();
        Map<Long, Double> baseSubclasse = new HashMap<>();
        Map<CategoriaInvestimento, Double> capClasse = new HashMap<>();
        Map<CategoriaInvestimento, Double> baseClasse = new HashMap<>();

        // 1) Espaço de cada bucket até o LIMITE OPERACIONAL dele.
        for (ComparativoResponseDTO.ClasseComparativoDTO c : comparativo.classes()) {
            capClasse.put(c.classe(), ReferenciaAporte
                    .limite(c.percentual_ideal(), c.limite_maximo(), c.valor_atual(), r, margemPercentual)
                    .capacidade().doubleValue());
            for (ComparativoResponseDTO.SubclasseComparativoDTO s : c.subclasses()) {
                // O alvo da subclasse já é uma FATIA da classe, então aplicar a
                // margem sobre ele dá o limite operacional dela em reais.
                double limiteEmReais = ReferenciaAporte
                        .comMargem(s.valor_ideal(), margemPercentual).doubleValue();
                if (s.limite_maximo() != null && s.limite_maximo().signum() > 0) {
                    limiteEmReais = Math.min(limiteEmReais,
                            ReferenciaAporte.percentualEmReais(s.limite_maximo(), r).doubleValue());
                }
                capSubclasse.put(s.id(), Math.max(0d, limiteEmReais - AlocacaoService.nz(s.valor_atual())));
            }
        }

        // 2) Base do rateio: o VALOR das posições sem meta, por bucket.
        for (MeusAtivosResponseDTO.MeuAtivoDTO a : meus.ativos()) {
            if (a.meta_id() != null) {
                continue;
            }
            double valor = AlocacaoService.nz(a.valor_atual());
            if (a.subclasse_id() != null) {
                baseSubclasse.merge(a.subclasse_id(), valor, Double::sum);
            } else {
                baseClasse.merge(a.classe(), valor, Double::sum);
            }
        }
        return new Heranca(capSubclasse, baseSubclasse, capClasse, baseClasse);
    }

    /* ══════════════════════════════════════════════════════════════════
       ONTEM ENTRA O DINHEIRO (classe → subclasse)
       ══════════════════════════════════════════════════════════════════ */

    private List<RankingAportesDTO.ClasseAporteDTO> classesDto(
            ComparativoResponseDTO comparativo,
            List<AporteCandidato> candidatos,
            double margemOperacional,
            Map<CategoriaInvestimento, BigDecimal> sugeridoPorClasse,
            Map<Long, BigDecimal> sugeridoPorSubclasse) {

        double total = nz(comparativo.valor_total());
        List<RankingAportesDTO.ClasseAporteDTO> classes = new ArrayList<>();
        for (ComparativoResponseDTO.ClasseComparativoDTO c : comparativo.classes()) {
            BigDecimal sugerido = sugeridoPorClasse.getOrDefault(c.classe(), moeda(0d));
            // O que os ATIVOS ELEGÍVEIS da classe ainda absorvem. É este número — e
            // não o déficit da classe — que limita o orçamento dela (§15).
            BigDecimal capacidadeElegivel = moeda(capacidadeElegivelDaClasse(candidatos, c.classe()));
            BigDecimal limiteOperacional = ReferenciaAporte
                    .limiteOperacionalPercentual(c.percentual_ideal(), margemOperacional);

            List<RankingAportesDTO.SubclasseAporteDTO> subs = c.subclasses().stream()
                    .map(s -> {
                        BigDecimal sugeridoSub = sugeridoPorSubclasse.getOrDefault(s.id(), moeda(0d));
                        // O percentual da subclasse é FATIA DA CLASSE: para virar um %
                        // do patrimônio ele precisa do ideal da classe junto.
                        BigDecimal limiteSub = ReferenciaAporte.limiteOperacionalPercentual(
                                fatiaPercentual(c.percentual_ideal(), s.percentual_ideal()), margemOperacional);
                        return new RankingAportesDTO.SubclasseAporteDTO(
                                s.id(), s.nome(), s.percentual_atual(), s.percentual_ideal(),
                                s.valor_atual(), s.valor_ideal(), s.deficit(), s.excesso(),
                                s.tolerancia(), s.limite_maximo(), limiteSub,
                                moeda(capacidadeElegivelDaSubclasse(candidatos, s.id())),
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
                    c.tolerancia(), c.limite_maximo(), limiteOperacional, capacidadeElegivel,
                    statusDe(c.percentual_atual(), c.percentual_ideal(), c.tolerancia(), c.limite_maximo()),
                    sugerido,
                    motivoDaClasse(c, sugerido, total, candidatos, capacidadeElegivel),
                    subs));
        }
        return classes;
    }

    /** Percentual de um nível que é FATIA de outro: {@code base × fatia / 100}. */
    private BigDecimal fatiaPercentual(BigDecimal basePercentual, BigDecimal fatiaPercentual) {
        if (basePercentual == null || fatiaPercentual == null) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(basePercentual.doubleValue() * fatiaPercentual.doubleValue() / 100d)
                .setScale(4, RoundingMode.HALF_UP);
    }

    /** Soma das capacidades dos candidatos ELEGÍVEIS de uma classe. */
    private double capacidadeElegivelDaClasse(List<AporteCandidato> candidatos, CategoriaInvestimento classe) {
        return candidatos.stream()
                .filter(c -> c.classe() == classe && c.elegivel())
                .mapToDouble(c -> nz(c.capacidade()))
                .sum();
    }

    /** Soma das capacidades dos candidatos ELEGÍVEIS de uma subclasse. */
    private double capacidadeElegivelDaSubclasse(List<AporteCandidato> candidatos, Long subclasseId) {
        return candidatos.stream()
                .filter(c -> subclasseId != null && subclasseId.equals(c.subclasseId()) && c.elegivel())
                .mapToDouble(c -> nz(c.capacidade()))
                .sum();
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
     * O motivo diz o que REALMENTE aconteceu, e não usa mais "sem déficit" como
     * explicação: déficit deixou de ser trava. Se a classe não recebeu, é porque
     * (a) o limite dela foi atingido, (b) ela não tem ativo na carteira, (c) os
     * ativos dela já estão no limite operacional, ou (d) o valor disponível não
     * chegou até ela.
     */
    private String motivoDaClasse(ComparativoResponseDTO.ClasseComparativoDTO c, BigDecimal sugerido,
                                  double total, List<AporteCandidato> candidatos,
                                  BigDecimal capacidadeElegivel) {
        BigDecimal limite = c.limite_maximo();
        BigDecimal atual = c.percentual_atual();
        List<AporteCandidato> daClasse = candidatos.stream()
                .filter(cand -> cand.classe() == c.classe())
                .toList();

        if (sugerido.signum() > 0) {
            return "Recebe o que os ativos dela absorvem: " + formatar(capacidadeElegivel)
                    + " (soma das capacidades elegíveis — o déficit da classe não é reserva).";
        }
        if (limite != null && limite.signum() > 0 && atual != null
                && atual.doubleValue() >= limite.doubleValue()) {
            return "Não recebe: limite de concentração de " + formatar(limite) + "% atingido.";
        }
        if (daClasse.isEmpty()) {
            return "Não recebe: nenhum ativo desta classe está na carteira (meta de ativo ainda não "
                    + "comprado não recebe — o aporte só entra em posição que existe).";
        }
        long travados = daClasse.stream()
                .filter(cand -> !cand.elegivel() && cand.status() != StatusElegibilidade.SEM_CAPACIDADE)
                .count();
        if (capacidadeElegivel.signum() <= 0) {
            if (travados > 0) {
                return "Não recebe: os ativos dela estão travados (limite atingido ou critério eliminatório "
                        + "do checklist).";
            }
            return "Não recebe: os ativos da classe já estão no limite operacional (meta + margem) — não "
                    + "sobrou espaço neste aporte.";
        }
        return "Não recebe neste aporte: o valor disponível não chegou até ela "
                + "(capacidade elegível de " + formatar(capacidadeElegivel) + ").";
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
                    : " Respeita a capacidade de " + formatar(c.capacidade())
                        + " (limite " + percentual(c.limitePercentual()) + " × R).";
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

    /** Sobra que não é troco de arredondamento: falta de ESPAÇO até o limite. */
    private String explicacaoNaoAlocadoGenerica(BigDecimal naoAlocado, List<AporteCandidato> candidatos) {
        if (naoAlocado == null || naoAlocado.signum() <= 0) {
            return null;
        }
        long descartados = candidatos.stream().filter(c -> !c.elegivel()).count();
        List<String> razoes = new ArrayList<>();
        if (descartados > 0) {
            razoes.add(descartados + " ativo(s) não podem receber (limite atingido, critério eliminatório do "
                    + "checklist ou classe fora da Carteira Ideal)");
        }
        razoes.add("os ativos elegíveis já estão no limite operacional (meta + margem)");
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
                        + " atingiu o limite operacional de " + percentual(c.limitePercentual()) + " (meta de "
                        + percentual(c.percentualIdeal()) + " + margem"
                        + (c.limiteMaximo() != null && c.limiteMaximo().signum() > 0
                                ? ", reduzida pelo limite cadastrado de " + percentual(c.limiteMaximo()) : "")
                        + ")."));
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
