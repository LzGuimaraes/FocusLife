package dev.LzGuimaraes.FocusLifeHub.Planejamento.Aporte;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import dev.LzGuimaraes.FocusLifeHub.Planejamento.Avaliacao.ScoreCalculator;

/**
 * PRIORITY SCORE — "entre os ativos que PODEM receber aporte agora, qual tem
 * maior prioridade?"
 *
 * Esta fase só ORDENA. Ela recebe apenas ativos que já passaram pela
 * elegibilidade: um Quality Score 98 não entra aqui se o preço estiver acima do
 * limite de compra — nesse caso o ativo foi descartado antes.
 *
 *      P = 100 × (α·Q̂ + β·D̂ − γ·Ê + δ·P̂ + ε·M̂ + ζ·Ô) / (α + β + γ + δ + ε + ζ)
 *
 * Cada termo tem um significado isolado (e a conta fica aberta na tela):
 *   Q̂ qualidade   → o que o investidor acha do ativo (checklists de qualidade)
 *   D̂ déficit     → quanto falta para a meta (necessidade)
 *   Ê excesso     → quanto passou da meta (reduz a prioridade)
 *   P̂ prioridade  → o desempate manual 0–10
 *   M̂ momento     → fator 0–1 do checklist de momento
 *   Ô preço       → oportunidade de preço 0–1 até o preço máximo de compra
 *
 * Termo sem dado (sem avaliação, sem checklist de momento, sem preço máximo)
 * sai do numerador E do denominador: o ativo não é punido por uma informação
 * que o investidor ainda não cadastrou.
 */
@Service
public class PrioridadeService {

    private final ScoreCalculator scoreCalculator;

    public PrioridadeService(ScoreCalculator scoreCalculator) {
        this.scoreCalculator = scoreCalculator;
    }

    /** Termos já normalizados em 0..1, prontos para o cálculo. */
    public record Termos(
            /** Quality Score / 100 (null = sem avaliação). */
            Double qualidade,
            double deficit,
            double excesso,
            double prioridadeManual,
            /** Fator de momento 0..1 (null = sem checklist de momento). */
            Double momento,
            /** Oportunidade de preço 0..1 (null = sem preço máximo configurado). */
            Double preco
    ) {}

    /** Priority Score (0–100) com os pesos configurados pelo investidor. */
    public BigDecimal calcular(Termos termos, ScoreConfigModel config) {
        ScoreCalculator.TermosPrioridade pesos = new ScoreCalculator.TermosPrioridade(
                config.getPesoQuality(), config.getPesoDeficit(), config.getPesoExcesso(),
                config.getPesoPrioridade(), config.getPesoMomento(), config.getPesoPreco());
        return scoreCalculator.priorityScore(
                termos.qualidade(), termos.deficit(), termos.excesso(),
                termos.prioridadeManual(), termos.momento(), termos.preco(), pesos);
    }

    /**
     * Fórmula ABERTA do cálculo — a mesma conta, com os termos normalizados e os
     * pesos aplicados, para o usuário reproduzir na calculadora (§18).
     */
    public String formula(BigDecimal priorityScore, Termos termos, ScoreConfigModel config) {
        List<String> partes = new ArrayList<>();
        BigDecimal soma = BigDecimal.ZERO;

        if (termos.qualidade() != null) {
            partes.add("+" + numero(config.getPesoQuality()) + "×" + numero(termos.qualidade()));
            soma = soma.add(nz(config.getPesoQuality()));
        }
        if (termos.momento() != null) {
            partes.add("+" + numero(config.getPesoMomento()) + "×" + numero(termos.momento()));
            soma = soma.add(nz(config.getPesoMomento()));
        }
        if (termos.preco() != null) {
            partes.add("+" + numero(config.getPesoPreco()) + "×" + numero(termos.preco()));
            soma = soma.add(nz(config.getPesoPreco()));
        }
        partes.add("+" + numero(config.getPesoDeficit()) + "×" + numero(termos.deficit()));
        soma = soma.add(nz(config.getPesoDeficit()));
        partes.add("−" + numero(config.getPesoExcesso()) + "×" + numero(termos.excesso()));
        soma = soma.add(nz(config.getPesoExcesso()));
        partes.add("+" + numero(config.getPesoPrioridade()) + "×" + numero(termos.prioridadeManual()));
        soma = soma.add(nz(config.getPesoPrioridade()));

        if (soma.compareTo(BigDecimal.ZERO) == 0) {
            return "sem pesos configurados — o Priority Score fica 0";
        }
        return "100 × [ " + String.join(" ", partes) + " ] ÷ " + numero(soma)
                + " = " + numero(priorityScore);
    }

    private BigDecimal nz(BigDecimal valor) {
        return (valor != null) ? valor : BigDecimal.ZERO;
    }

    private String numero(BigDecimal valor) {
        return (valor == null) ? "0" : valor.stripTrailingZeros().toPlainString();
    }

    private String numero(double valor) {
        return BigDecimal.valueOf(valor).setScale(4, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
    }
}
