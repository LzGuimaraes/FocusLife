package dev.LzGuimaraes.FocusLifeHub.Planejamento.Aporte;

/**
 * VEREDITO de elegibilidade: "esse ativo PODE receber dinheiro agora?".
 *
 * Conceito separado do déficit (quanto falta) e da NOTA do checklist (entre os
 * que podem, qual vem primeiro). Depois da simplificação existem quatro motivos
 * de descarte — todos estruturais, nenhum depende de preço:
 *
 *   • o nível (classe/subclasse) não tem déficit;
 *   • o ativo já está no próprio teto (déficit + tolerância);
 *   • o limite de concentração da meta foi atingido;
 *   • o checklist reprovou um critério ELIMINATÓRIO.
 */
public enum StatusElegibilidade {

    ELEGIVEL("Elegível", "Pode receber aporte neste momento."),
    SEM_AVALIACAO("Sem avaliação", "Ainda sem nota do checklist: participa do aporte, mas sem nota para a ordem."),
    CRITERIO_ELIMINATORIO("Critério eliminatório", "O checklist reprovou um critério eliminatório deste ativo."),
    LIMITE_ATINGIDO("Limite atingido", "Já atingiu o limite máximo de concentração definido na meta."),
    CLASSE_SEM_CAPACIDADE("Nível sem capacidade", "A classe (ou a subclasse) deste ativo não tem déficit."),
    SEM_CAPACIDADE("Sem capacidade", "O ativo já está no próprio alvo (déficit + tolerância).");

    private final String label;
    private final String descricao;

    StatusElegibilidade(String label, String descricao) {
        this.label = label;
        this.descricao = descricao;
    }

    public String getLabel() {
        return label;
    }

    public String getDescricao() {
        return descricao;
    }
}
