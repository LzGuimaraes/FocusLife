package dev.LzGuimaraes.FocusLifeHub.Planejamento.Aporte;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

import dev.LzGuimaraes.FocusLifeHub.Planejamento.CarteiraIdeal.dto.ComparativoResponseDTO;

/**
 * REFERÊNCIA DO APORTE — a base de TODOS os cálculos do motor, num lugar só.
 *
 * O aporte aumenta o PATRIMÔNIO, não o valor dos ativos. São três grandezas
 * distintas, e confundi-las é o bug clássico deste motor:
 *
 *   patrimonioAtual      = T   → o que existe hoje (soma dos valores atuais)
 *   aporte               = A   → o dinheiro novo, informado pelo usuário
 *   patrimonioProjetado  = R = T + A  → referência dos ALVOS
 *
 * E, para cada nível (classe, subclasse ou ativo), DUAS grandezas diferentes:
 *
 *   valorAtual                = valor que existe HOJE (nunca soma o aporte)
 *   percentualIdeal           = meta do usuário (nunca recalculada da carteira)
 *   valorAlvoProjetado        = percentualIdeal × R        (a META em reais)
 *   deficitAteMeta            = max(0, valorAlvoProjetado − valorAtual)
 *   limiteOperacional         = percentualIdeal × (1 + margem)
 *   limitePercentual          = min(limiteOperacional, limite cadastrado)
 *   limiteEmReais             = limitePercentual × R
 *   capacidade                = max(0, limiteEmReais − valorAtual)
 *   percentualAtualProjetado  = valorAtual ÷ R     ← CAI quando o aporte entra e
 *                                                    o ativo ainda não recebeu
 *
 * `deficitAteMeta` e `capacidade` NÃO são a mesma coisa e não devem ser
 * misturados: a meta informa o espaço DESEJÁVEL, a capacidade informa o MÁXIMO
 * que o ativo pode receber antes de furar o limite. É por isso que um ativo
 * exatamente NA meta continua podendo receber (a meta dele cresce com R).
 *
 * O que é PROIBIDO (era o que produzia "Atual = Ideal · Déficit R$ 0,00" com o
 * aporte informado):
 *   • medir o déficit contra T em vez de R — com metas que espelham a carteira,
 *     %ideal × T é exatamente o valor atual, então o déficit dá zero e o aporte
 *     fica sem destino;
 *   • incorporar o aporte em `valorAtual` antes de decidir a distribuição;
 *   • reconstruir `valorAtual` a partir de um percentual normalizado sobre R.
 *
 * O aporte só chega ao ativo DEPOIS, na distribuição:
 *   valorFinal = valorAtual + valorAporte  (feito pelo AlocacaoService).
 *
 * A TOLERÂNCIA não participa de nada disto: ela só classifica o percentual
 * projetado em ABAIXO / EQUILIBRADO / ACIMA para a tela.
 */
public final class ReferenciaAporte {

    private static final int ESCALA_MOEDA = 2;
    private static final int ESCALA_PERCENTUAL = 4;

    /**
     * MARGEM OPERACIONAL padrão, RELATIVA à meta: 5% de folga sobre o percentual
     * ideal. Com meta de 5% o limite fica 5,25% — nunca "5% + 5 pontos".
     * Configurável entre 3% e 5% pelo usuário (`AporteConfigService`).
     */
    public static final double MARGEM_PADRAO = 5.0d;
    public static final double MARGEM_MINIMA = 3.0d;
    public static final double MARGEM_MAXIMA = 5.0d;

    private ReferenciaAporte() {}

    /**
     * LIMITE OPERACIONAL de um nível — tudo o que decide a CAPACIDADE dele.
     *
     * É o coração da nova separação: a META em reais (`alvoEmReais`) é a
     * referência desejável; o LIMITE (`limiteEmReais`) é o máximo permitido.
     * Os dois são diferentes de propósito, e `capacidade` sai do LIMITE — não do
     * déficit. Sem isso, um ativo exatamente na meta nunca mais receberia.
     *
     * @param metaPercentual             meta do usuário (% do patrimônio projetado)
     * @param limiteCadastradoPercentual limite explícito do usuário (null = não há)
     * @param valorAtual                 valor que existe HOJE (nunca soma o aporte)
     * @param patrimonioProjetado        R = T + A, referência dos alvos
     * @param margemPercentual           margem RELATIVA à meta (ex.: 5 → 5% de folga)
     */
    public record Limite(
            BigDecimal metaPercentual,
            BigDecimal limiteCadastradoPercentual,
            BigDecimal limiteOperacionalPercentual,
            /** min(limite operacional, limite cadastrado) — o que realmente vale. */
            BigDecimal limitePercentual,
            /** meta × R (a meta em reais). */
            BigDecimal alvoEmReais,
            /** limite final × R. */
            BigDecimal limiteEmReais,
            /** max(0, alvoEmReais − valorAtual) — o espaço DESEJÁVEL. */
            BigDecimal deficitAteMeta,
            /** max(0, limiteEmReais − valorAtual) — o MÁXIMO que pode receber. */
            BigDecimal capacidade
    ) {
        public boolean semCapacidade() {
            return capacidade.signum() <= 0;
        }

        /** true = a capacidade está limitada pelo limite CADASTRADO, não pela margem. */
        public boolean limitadaPeloCadastro() {
            return limitePercentual != null && limiteCadastradoPercentual != null
                    && limiteCadastradoPercentual.signum() > 0
                    && limitePercentual.compareTo(limiteCadastradoPercentual) == 0;
        }
    }

    /** Limite operacional de um nível a partir da META do usuário. */
    public static Limite limite(BigDecimal metaPercentual, BigDecimal limiteCadastradoPercentual,
                                BigDecimal valorAtual, BigDecimal patrimonioProjetado,
                                double margemPercentual) {
        BigDecimal operacional = limiteOperacionalPercentual(metaPercentual, margemPercentual);
        BigDecimal finalPct = (limiteCadastradoPercentual != null && limiteCadastradoPercentual.signum() > 0)
                ? operacional.min(limiteCadastradoPercentual)
                : operacional;
        BigDecimal alvo = valorAlvoProjetado(metaPercentual, patrimonioProjetado);
        BigDecimal limiteReais = percentualEmReais(finalPct, patrimonioProjetado);
        return new Limite(metaPercentual, limiteCadastradoPercentual, operacional, finalPct,
                alvo, limiteReais, deficit(alvo, valorAtual), deficit(limiteReais, valorAtual));
    }

    public static Limite limite(BigDecimal metaPercentual, BigDecimal valorAtual,
                                BigDecimal patrimonioProjetado, double margemPercentual) {
        return limite(metaPercentual, null, valorAtual, patrimonioProjetado, margemPercentual);
    }

    /**
     * Limite de uma posição SEM meta própria: ela herda o ALVO (em R$) do bucket
     * a que pertence — a subclasse quando existe, senão a classe. O limite
     * operacional é esse alvo com a mesma margem aplicada.
     *
     * `metaPercentual` fica null porque não existe meta própria: o que existe é
     * um alvo emprestado. Quem rateia é que divide o espaço do bucket entre as
     * posições que o herdam (senão cada uma receberia o bucket inteiro).
     */
    public static Limite herdado(BigDecimal alvoEmReais, BigDecimal valorAtual, double margemPercentual) {
        BigDecimal alvo = moeda(nz(alvoEmReais));
        BigDecimal limiteReais = comMargem(alvo, margemPercentual);
        return new Limite(null, null, null, null,
                alvo, limiteReais, deficit(alvo, valorAtual), deficit(limiteReais, valorAtual));
    }

    /** Aplica a margem (em %) sobre um valor: {@code valor × (1 + margem/100)}. */
    public static BigDecimal comMargem(BigDecimal valor, double margemPercentual) {
        return moeda(nz(valor) * (1d + margemPercentual / 100d));
    }

    /** Valor em reais de um percentual do patrimônio: {@code percentual × R}. */
    public static BigDecimal percentualEmReais(BigDecimal percentual, BigDecimal patrimonioProjetado) {
        return valorAlvoProjetado(percentual, patrimonioProjetado);
    }

    /**
     * Limite operacional em PERCENTUAL: {@code percentualIdeal × (1 + margem)}.
     *
     * A margem é RELATIVA: meta 5% com margem 5% dá 5,25% — não 10%. Aplicar a
     * margem como soma de pontos percentuais transformaria 5% de meta em 10% de
     * limite, dobrando a concentração permitida.
     */
    public static BigDecimal limiteOperacionalPercentual(BigDecimal percentualIdeal, double margemPercentual) {
        if (percentualIdeal == null || percentualIdeal.signum() <= 0) {
            return BigDecimal.ZERO.setScale(ESCALA_PERCENTUAL, RoundingMode.HALF_UP);
        }
        return BigDecimal.valueOf(percentualIdeal.doubleValue() * (1d + margemPercentual / 100d))
                .setScale(ESCALA_PERCENTUAL, RoundingMode.HALF_UP);
    }

    /** As três grandezas da referência, explícitas. */
    public record Fatores(BigDecimal patrimonioAtual, BigDecimal aporte, BigDecimal patrimonioProjetado) {

        /** Sem aporte, R = T (nada muda: é o caso do ranking sem valor informado). */
        public boolean semAporte() {
            return aporte == null || aporte.signum() <= 0;
        }
    }

    public static Fatores fatores(BigDecimal patrimonioAtual, BigDecimal aporte) {
        BigDecimal t = moeda(nz(patrimonioAtual));
        BigDecimal a = (aporte != null && aporte.signum() > 0) ? moeda(aporte) : moeda(0);
        return new Fatores(t, a, moeda(t.doubleValue() + a.doubleValue()));
    }

    /** Alvo projetado em R$ de um percentual: {@code percentualIdeal × R}. */
    public static BigDecimal valorAlvoProjetado(BigDecimal percentualIdeal, BigDecimal patrimonioProjetado) {
        if (percentualIdeal == null || patrimonioProjetado == null) {
            return moeda(0);
        }
        return moeda(percentualIdeal.doubleValue() / 100d * patrimonioProjetado.doubleValue());
    }

    /** {@code deficit = max(0, alvo − valorAtual)} — nunca soma o aporte ao atual. */
    public static BigDecimal deficit(BigDecimal valorAlvoProjetado, BigDecimal valorAtual) {
        return positivo(nz(valorAlvoProjetado) - nz(valorAtual));
    }

    /** {@code percentualAtualProjetado = valorAtual ÷ R} (escala 4). */
    public static BigDecimal percentualAtualProjetado(BigDecimal valorAtual, BigDecimal patrimonioProjetado) {
        if (patrimonioProjetado == null || patrimonioProjetado.signum() <= 0) {
            return BigDecimal.ZERO.setScale(ESCALA_PERCENTUAL, RoundingMode.HALF_UP);
        }
        return BigDecimal.valueOf(nz(valorAtual) / patrimonioProjetado.doubleValue() * 100d)
                .setScale(ESCALA_PERCENTUAL, RoundingMode.HALF_UP);
    }

    /**
     * Cópia do comparativo com os ALVOS do patrimônio projetado (R).
     *
     * Só mexeu em três coisas por nível: `valor_ideal` = %ideal × R,
     * `percentual_atual` = valorAtual ÷ R e o déficit/excesso decorrentes.
     * `valor_atual` fica EXATAMENTE como estava — é dinheiro que já existe e o
     * aporte ainda não foi distribuído.
     *
     * O percentual de SUBCLASSE continua sendo fatia da CLASSE (a soma das
     * subclasses fecha 100% da classe), então ele não muda com o aporte.
     */
    public static ComparativoResponseDTO comAporte(ComparativoResponseDTO comparativo, BigDecimal aporte) {
        Fatores fatores = fatores(comparativo.valor_total(), aporte);
        if (fatores.semAporte()) {
            return comparativo;   // sem aporte não há projeção: T = R
        }
        BigDecimal r = fatores.patrimonioProjetado();

        List<ComparativoResponseDTO.ClasseComparativoDTO> classes = new ArrayList<>();
        for (ComparativoResponseDTO.ClasseComparativoDTO classe : comparativo.classes()) {
            BigDecimal valorAtualClasse = moeda(nz(classe.valor_atual()));
            BigDecimal valorAlvoClasse = valorAlvoProjetado(classe.percentual_ideal(), r);

            List<ComparativoResponseDTO.SubclasseComparativoDTO> subclasses = classe.subclasses().stream()
                    .map(sub -> {
                        BigDecimal valorAtualSub = moeda(nz(sub.valor_atual()));
                        // O ideal da subclasse é uma FATIA do ideal da CLASSE.
                        BigDecimal valorAlvoSub = fatia(sub.percentual_ideal(), valorAlvoClasse);
                        return new ComparativoResponseDTO.SubclasseComparativoDTO(
                                sub.id(), sub.nome(), sub.percentual_ideal(), sub.percentual_atual(),
                                valorAlvoSub, valorAtualSub,
                                deficit(valorAlvoSub, valorAtualSub), excesso(valorAtualSub, valorAlvoSub),
                                sub.tolerancia(), sub.limite_maximo());
                    })
                    .toList();

            List<ComparativoResponseDTO.AtivoComparativoDTO> ativos = classe.ativos().stream()
                    .map(ativo -> {
                        BigDecimal valorAtualAtivo = moeda(nz(ativo.valor_atual()));
                        BigDecimal valorAlvoAtivo = valorAlvoProjetado(ativo.percentual_ideal(), r);
                        return new ComparativoResponseDTO.AtivoComparativoDTO(
                                ativo.meta_id(), ativo.ativo_cadastro_id(), ativo.ticker(), ativo.subclasse_id(),
                                ativo.percentual_ideal(),
                                percentualAtualProjetado(valorAtualAtivo, r),
                                valorAlvoAtivo, valorAtualAtivo,
                                deficit(valorAlvoAtivo, valorAtualAtivo), excesso(valorAtualAtivo, valorAlvoAtivo),
                                ativo.tolerancia(), ativo.limite_maximo(), ativo.possui_meta());
                    })
                    .toList();

            classes.add(new ComparativoResponseDTO.ClasseComparativoDTO(
                    classe.classe(), classe.percentual_ideal(),
                    percentualAtualProjetado(valorAtualClasse, r),
                    valorAlvoClasse, valorAtualClasse,
                    deficit(valorAlvoClasse, valorAtualClasse), excesso(valorAtualClasse, valorAlvoClasse),
                    classe.tolerancia(), classe.limite_maximo(), subclasses, ativos));
        }

        return new ComparativoResponseDTO(comparativo.carteira_id(), comparativo.moeda(), r,
                comparativo.soma_percentuais_ideal(), classes, comparativo.avisos());
    }

    /** Fatia de um valor-base (o percentual da subclasse é fatia da CLASSE). */
    private static BigDecimal fatia(BigDecimal percentual, BigDecimal base) {
        if (percentual == null || base == null) {
            return moeda(0);
        }
        return moeda(percentual.doubleValue() / 100d * base.doubleValue());
    }

    private static BigDecimal excesso(BigDecimal valorAtual, BigDecimal valorAlvo) {
        return positivo(nz(valorAtual) - nz(valorAlvo));
    }

    /** Só a parte positiva, em reais (0 quando negativo). */
    private static BigDecimal positivo(double valor) {
        return moeda(Math.max(0d, valor));
    }

    /** Valor em reais, 2 casas, nunca negativo. */
    public static BigDecimal moeda(double valor) {
        return BigDecimal.valueOf(Math.max(0d, valor)).setScale(ESCALA_MOEDA, RoundingMode.HALF_UP);
    }

    public static BigDecimal moeda(BigDecimal valor) {
        return moeda(nz(valor));
    }

    public static double nz(BigDecimal valor) {
        return (valor != null) ? valor.doubleValue() : 0d;
    }
}
