package dev.LzGuimaraes.FocusLifeHub.Planejamento.Aporte;

import java.math.BigDecimal;

/**
 * Termos do Contribution Score (Módulo 6).
 *
 * Os termos são FIXOS de propósito: a fórmula é sempre
 * {@code α·Qualidade + β·Déficit − γ·Excesso + δ·Prioridade}, e o que o
 * usuário configura são os PESOS (α, β, γ, δ). Assim a fórmula fica
 * auditável e explicável — sem avaliação de expressão/script arbitrário.
 *
 * "Outros fatores futuros" entram como novos termos desta enum + um peso
 * novo na configuração.
 */
public enum TermoScore {

    QUALITY("Qualidade do ativo",
            "Seu Quality Score do ativo: a soma ponderada das notas dos checklists.",
            new BigDecimal("3")),

    DEFICIT("Déficit até a meta",
            "Quanto falta em R$ para o ativo atingir o percentual ideal da Carteira Ideal.",
            new BigDecimal("4")),

    EXCESSO("Excesso sobre a meta",
            "Quanto o ativo passou do ideal. Quanto maior, MENOS prioridade de aporte.",
            new BigDecimal("2")),

    PRIORIDADE("Prioridade manual",
            "A prioridade de 0 a 10 que você definiu na meta do ativo (0 = desempate neutro).",
            new BigDecimal("1"));

    private final String label;
    private final String descricao;
    private final BigDecimal pesoPadrao;

    TermoScore(String label, String descricao, BigDecimal pesoPadrao) {
        this.label = label;
        this.descricao = descricao;
        this.pesoPadrao = pesoPadrao;
    }

    public String getLabel() {
        return label;
    }

    public String getDescricao() {
        return descricao;
    }

    public BigDecimal getPesoPadrao() {
        return pesoPadrao;
    }
}
