package dev.LzGuimaraes.FocusLifeHub.Planejamento.Aporte;

import java.math.BigDecimal;

/**
 * Termos do Contribution Score (Módulo 6).
 *
 * Os termos são FIXOS de propósito: a fórmula é sempre
 * {@code α·Qualidade + β·Déficit − γ·Excesso + δ·Prioridade + ε·Momento}, e o
 * que o usuário configura são os PESOS (α, β, γ, δ, ε). Assim a fórmula fica
 * auditável e explicável — sem avaliação de expressão/script arbitrário.
 *
 * O termo MOMENTO é sempre multiplicado pelo FATOR DE MOMENTO do ativo (0 a 1),
 * derivado da nota de momento por faixas configuráveis.
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
            new BigDecimal("1")),

    MOMENTO("Momento / valuation",
            "Nota dos checklists de MOMENTO (é hora de aportar?). Entra como FATOR 0 a 1: 0 bloqueia, 1 prioriza. "
                    + "Não altera a qualidade do ativo.",
            new BigDecimal("3")),

    /**
     * OPORTUNIDADE DE PREÇO — só existe para quem PASSOU pela elegibilidade.
     *
     * Mede quanto o preço atual está abaixo do PREÇO MÁXIMO DE COMPRA que o
     * investidor definiu: (máximo − atual) / máximo, de 0 a 1. Sem preço máximo
     * configurado, o termo sai da conta do ativo (não é penalidade) — do mesmo
     * jeito que o termo de qualidade sai quando não há avaliação.
     */
    PRECO("Oportunidade de preço",
            "Distância entre o preço atual e o seu preço máximo de compra. Só conta para ativos que já são elegíveis.",
            new BigDecimal("2"));

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
