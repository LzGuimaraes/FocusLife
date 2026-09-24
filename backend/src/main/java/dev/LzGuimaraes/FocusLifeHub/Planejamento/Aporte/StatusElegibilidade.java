package dev.LzGuimaraes.FocusLifeHub.Planejamento.Aporte;

/**
 * VEREDITO de elegibilidade: "esse ativo PODE receber dinheiro agora?".
 *
 * Conceito separado do DÉFICIT (quanto falta) e da NOTA do checklist (entre os
 * que podem, qual vem primeiro).
 *
 * O que NÃO é motivo de descarte (era o bug do motor antigo):
 *   ✗ estar na meta (ou acima dela) — a meta cresce com o patrimônio projetado;
 *   ✗ a CLASSE ou a SUBCLASSE estar sem déficit — elas distribuem o orçamento,
 *     não vetam ativos. A classe sem espaço simplesmente não recebe dinheiro; os
 *     ativos dela continuam sendo avaliados um a um.
 * Por isso NÃO existe mais o status `CLASSE_SEM_CAPACIDADE`.
 *
 * O que resta são três travas REAIS:
 *
 *   • o checklist reprovou um critério ELIMINATÓRIO do ativo;
 *   • o ativo já está no LIMITE (operacional = meta + margem, ou o cadastrado);
 *   • o ativo não tem capacidade positiva até esse limite.
 */
public enum StatusElegibilidade {

    ELEGIVEL("Elegível", "Pode receber aporte neste momento."),
    SEM_AVALIACAO("Sem avaliação", "Ainda sem nota do checklist: participa do aporte, mas sem nota para a ordem."),
    CRITERIO_ELIMINATORIO("Critério eliminatório", "O checklist reprovou um critério eliminatório deste ativo."),
    LIMITE_ATINGIDO("Limite atingido",
            "Já está no limite (meta + margem operacional, ou o limite cadastrado): não há espaço até o limite."),
    SEM_CAPACIDADE("Sem capacidade",
            "Não sobrou espaço até o limite operacional deste ativo (ou a classe dele não está na Carteira Ideal).");

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
