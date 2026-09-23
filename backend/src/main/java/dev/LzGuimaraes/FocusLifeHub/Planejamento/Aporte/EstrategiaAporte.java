package dev.LzGuimaraes.FocusLifeHub.Planejamento.Aporte;

import java.math.BigDecimal;

/**
 * Como o valor do aporte é rateado entre os ativos deficitários (Módulo 9).
 *
 * Os pesos do ranking são os mesmos para todas as estratégias — o que muda é
 * a divisão do dinheiro.
 */
public enum EstrategiaAporte {

    DEFICIT_PROPORCIONAL("Proporcional ao déficit",
            "Divide o aporte na proporção do déficit de cada ativo."),

    SCORE_PROPORCIONAL("Proporcional ao Contribution Score",
            "Ativos com Contribution Score maior recebem uma fatia maior do aporte."),

    DEFICIT_COM_PRIORIDADE("Déficit com prioridade",
            "Como o proporcional, mas a prioridade manual aumenta o peso de cada ativo.");

    private final String label;
    private final String descricao;

    EstrategiaAporte(String label, String descricao) {
        this.label = label;
        this.descricao = descricao;
    }

    public String getLabel() {
        return label;
    }

    public String getDescricao() {
        return descricao;
    }

    public static EstrategiaAporte padrao() {
        return DEFICIT_PROPORCIONAL;
    }

    /** Peso relativo de um ativo dentro da estratégia escolhida. */
    public double pesoDoAtivo(BigDecimal contributionScore, double deficit, int prioridadeManual) {
        double base = switch (this) {
            case DEFICIT_PROPORCIONAL -> deficit;
            case SCORE_PROPORCIONAL -> (contributionScore != null) ? contributionScore.doubleValue() : 0d;
            case DEFICIT_COM_PRIORIDADE -> deficit * (1d + prioridadeManual / 10d);
        };
        return Math.max(0d, base);
    }
}
