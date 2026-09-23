package dev.LzGuimaraes.FocusLifeHub.Planejamento.Avaliacao;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.List;

import org.springframework.stereotype.Component;

/**
 * Cálculo do Quality Score (Módulo 5) e a derivação de nota por faixa.
 *
 * QUALITY SCORE — o sistema apenas soma o que o usuário atribuiu:
 *
 *      Q = [ Σ (pesoᵢ × notaᵢ / notaMáximaᵢ) / Σ pesoᵢ ] × 100
 *
 * Somatório apenas sobre as perguntas que PONTUAM e que foram RESPONDIDAS.
 * Se nada pontuado foi respondido, o score é `null` — nunca 0 — para não
 * ranquear um ativo não avaliado como se fosse ruim.
 *
 * O Quality Score do ATIVO é a média dos checklists ponderada pelo peso de
 * cada checklist.
 */
@Component
public class ScoreCalculator {

    public static final int ESCALA = 4;
    private static final MathContext MC = new MathContext(12, RoundingMode.HALF_UP);
    private static final BigDecimal CEM = new BigDecimal("100");

    /** Pergunta já reduzida ao que importa para o cálculo. */
    public record PerguntaCalculavel(
            BigDecimal peso,
            BigDecimal notaMaxima,
            boolean pontua,
            BigDecimal notaAtribuida
    ) {}

    /** Faixa de pontuação (valor mínimo/máximo → nota). */
    public record FaixaCalculavel(
            BigDecimal valorMin,
            BigDecimal valorMax,
            BigDecimal nota,
            String texto
    ) {}

    /** Score de um checklist (0–100), ou null quando nada pontuado foi respondido. */
    public BigDecimal score(List<PerguntaCalculavel> perguntas) {
        BigDecimal numerador = BigDecimal.ZERO;
        BigDecimal denominador = BigDecimal.ZERO;

        for (PerguntaCalculavel p : perguntas) {
            if (!p.pontua() || p.notaAtribuida() == null) {
                continue;
            }
            BigDecimal notaMaxima = p.notaMaxima();
            if (notaMaxima == null || notaMaxima.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            BigDecimal peso = (p.peso() != null) ? p.peso() : BigDecimal.ONE;
            BigDecimal proporcao = p.notaAtribuida().divide(notaMaxima, MC);
            numerador = numerador.add(peso.multiply(proporcao, MC), MC);
            denominador = denominador.add(peso, MC);
        }

        if (denominador.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }
        return numerador.divide(denominador, MC).multiply(CEM, MC).setScale(ESCALA, RoundingMode.HALF_UP);
    }

    /** Média ponderada dos scores dos checklists de um ativo (null se nenhum tem score). */
    public BigDecimal qualityScoreDoAtivo(List<BigDecimal> scores, List<BigDecimal> pesos) {
        BigDecimal numerador = BigDecimal.ZERO;
        BigDecimal denominador = BigDecimal.ZERO;
        for (int i = 0; i < scores.size(); i++) {
            BigDecimal score = scores.get(i);
            if (score == null) {
                continue;
            }
            BigDecimal peso = (i < pesos.size() && pesos.get(i) != null) ? pesos.get(i) : BigDecimal.ONE;
            numerador = numerador.add(score.multiply(peso, MC), MC);
            denominador = denominador.add(peso, MC);
        }
        if (denominador.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }
        return numerador.divide(denominador, MC).setScale(ESCALA, RoundingMode.HALF_UP);
    }

    /** Nota de uma faixa para o valor informado, ou null se nenhuma faixa cobrir o valor. */
    public BigDecimal notaDaFaixa(List<FaixaCalculavel> faixas, BigDecimal valor) {
        if (valor == null || faixas == null) {
            return null;
        }
        for (FaixaCalculavel f : faixas) {
            boolean acimaDoMinimo = (f.valorMin() == null) || valor.compareTo(f.valorMin()) >= 0;
            boolean abaixoDoMaximo = (f.valorMax() == null) || valor.compareTo(f.valorMax()) <= 0;
            if (acimaDoMinimo && abaixoDoMaximo) {
                return f.nota();
            }
        }
        return null;
    }

    /** Nota da opção escolhida (múltipla escolha), comparando pelo rótulo. */
    public BigDecimal notaDaOpcao(List<FaixaCalculavel> opcoes, String textoEscolhido) {
        if (textoEscolhido == null || opcoes == null) {
            return null;
        }
        String alvo = textoEscolhido.trim();
        for (FaixaCalculavel o : opcoes) {
            if (o.texto() != null && o.texto().trim().equalsIgnoreCase(alvo)) {
                return o.nota();
            }
        }
        return null;
    }

    /* ══════════════════════════════════════════════════════════════════
       CONTRIBUTION SCORE (Módulo 6) — prioridade de aporte
       ══════════════════════════════════════════════════════════════════ */

    /** Pesos configurados pelo usuário (α, β, γ, δ). */
    public record TermosContribution(
            BigDecimal quality,
            BigDecimal deficit,
            BigDecimal excesso,
            BigDecimal prioridade
    ) {}

    /**
     * Contribution Score (0–100) com os termos JÁ normalizados em 0..1:
     *
     *      C = 100 × (α·Q̂ + β·D̂ − γ·Ê + δ·P̂) / (α + β + γ + δ)
     *
     * `qualityNormalizada` nula significa "ativo ainda sem avaliação": o termo
     * de qualidade sai da conta e o denominador é renormalizado (α deixa de
     * contar), para não punir o ativo por uma nota que não existe.
     * O resultado é limitado ao intervalo [0, 100].
     */
    public BigDecimal contributionScore(Double qualityNormalizada,
                                        double deficitNormalizado,
                                        double excessoNormalizado,
                                        double prioridadeNormalizada,
                                        TermosContribution pesos) {

        BigDecimal pesoQuality = (qualityNormalizada != null) ? nz(pesos.quality()) : BigDecimal.ZERO;
        BigDecimal pesoDeficit = nz(pesos.deficit());
        BigDecimal pesoExcesso = nz(pesos.excesso());
        BigDecimal pesoPrioridade = nz(pesos.prioridade());

        BigDecimal numerador = BigDecimal.ZERO;
        if (qualityNormalizada != null) {
            numerador = numerador.add(pesoQuality.multiply(BigDecimal.valueOf(qualityNormalizada), MC), MC);
        }
        numerador = numerador
                .add(pesoDeficit.multiply(BigDecimal.valueOf(deficitNormalizado), MC), MC)
                .subtract(pesoExcesso.multiply(BigDecimal.valueOf(excessoNormalizado), MC), MC)
                .add(pesoPrioridade.multiply(BigDecimal.valueOf(prioridadeNormalizada), MC), MC);

        BigDecimal denominador = pesoQuality.add(pesoDeficit).add(pesoExcesso).add(pesoPrioridade);
        if (denominador.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO.setScale(ESCALA, RoundingMode.HALF_UP);
        }

        BigDecimal score = numerador.divide(denominador, MC).multiply(CEM, MC);
        if (score.compareTo(BigDecimal.ZERO) < 0) {
            score = BigDecimal.ZERO;
        }
        if (score.compareTo(CEM) > 0) {
            score = CEM;
        }
        return score.setScale(ESCALA, RoundingMode.HALF_UP);
    }

    /** Normaliza para 0..1 usando o maior valor do conjunto (0 se não houver). */
    public double normalizar(double valor, double maximo) {
        if (maximo <= 0d) {
            return 0d;
        }
        return Math.max(0d, valor) / maximo;
    }

    /** Prioridade manual 0–10 → 0..1. */
    public double normalizarPrioridade(Integer prioridadeManual) {
        int p = (prioridadeManual != null) ? Math.max(0, Math.min(10, prioridadeManual)) : 0;
        return p / 10d;
    }

    private BigDecimal nz(BigDecimal valor) {
        return (valor != null) ? valor : BigDecimal.ZERO;
    }

    /** Arredonda para 4 casas (padrão dos percentuais/notas do módulo). */
    public BigDecimal normalizar(BigDecimal valor) {
        if (valor == null) {
            return null;
        }
        return valor.setScale(ESCALA, RoundingMode.HALF_UP);
    }
}
