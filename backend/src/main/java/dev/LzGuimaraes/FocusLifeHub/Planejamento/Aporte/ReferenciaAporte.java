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
 * E, para cada nível (classe, subclasse ou ativo):
 *
 *   valorAtual                = valor que existe HOJE (nunca soma o aporte)
 *   percentualIdeal           = meta do usuário (nunca recalculada da carteira)
 *   valorAlvoProjetado        = percentualIdeal × R
 *   deficit                   = max(0, valorAlvoProjetado − valorAtual)
 *   capacidade                = deficit (limitado pelo teto de concentração)
 *   percentualAtualProjetado  = valorAtual ÷ R     ← CAI quando o aporte entra e
 *                                                    o ativo ainda não recebeu
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

    private ReferenciaAporte() {}

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
