package dev.LzGuimaraes.FocusLifeHub.Planejamento.Avaliacao;

/**
 * Tipo de um checklist — separa duas perguntas que NÃO podem se substituir:
 *
 *   • QUALIDADE → "esse é um ativo que faz sentido possuir?" (fundamentos,
 *     governança, risco, qualidade do negócio). Alimenta o Quality Score.
 *
 *   • MOMENTO   → "faz sentido aumentar a posição AGORA?" (valuation, P/L,
 *     P/VP, DY, vacância, taxa contratada...). Alimenta a nota de momento, que
 *     vira um FATOR de prioridade de aporte (0 a 1) — e nunca altera a
 *     qualidade do ativo.
 *
 * Um ativo pode ser excelente e estar caro: qualidade alta, aporte bloqueado
 * ou reduzido. O sistema mantém os dois eixos separados de ponta a ponta.
 */
public enum TipoChecklist {

    QUALIDADE("Qualidade", "O ativo é bom? (fundamentos, gestão, risco)"),
    MOMENTO("Momento / valuation", "É um bom momento para aportar? (preço, valuation, taxa)");

    private final String label;
    private final String descricao;

    TipoChecklist(String label, String descricao) {
        this.label = label;
        this.descricao = descricao;
    }

    public String getLabel() {
        return label;
    }

    public String getDescricao() {
        return descricao;
    }

    public static TipoChecklist padrao() {
        return QUALIDADE;
    }
}
