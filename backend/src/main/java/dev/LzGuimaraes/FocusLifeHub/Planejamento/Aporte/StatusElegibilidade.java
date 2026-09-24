package dev.LzGuimaraes.FocusLifeHub.Planejamento.Aporte;

/**
 * VEREDITO de elegibilidade de um ativo para receber aporte (§17 do spec).
 *
 * É a resposta para "esse ativo PODE receber dinheiro agora?" — pergunta
 * diferente de "quanto falta para ele?" (déficit) e de "entre os que podem,
 * qual vem primeiro?" (Priority Score).
 *
 * O rótulo é usado direto na tela: o usuário precisa entender o descarte sem
 * abrir o código.
 */
public enum StatusElegibilidade {

    ELEGIVEL("Elegível", "Pode receber aporte neste momento."),

    SEM_AVALIACAO("Sem avaliação",
            "Ainda sem checklist de qualidade/momento. NÃO é descartado: os termos "
                    + "sem dado apenas saem da conta do Priority Score."),

    PRECO_ACIMA_DO_LIMITE("Preço acima do limite",
            "O preço atual superou o preço máximo de compra definido para este ativo."),

    CRITERIO_ELIMINATORIO("Critério eliminatório",
            "Um critério do checklist marcado como eliminatório foi reprovado."),

    LIMITE_ATINGIDO("Limite de concentração",
            "O ativo já chegou ao limite máximo de concentração definido na meta."),

    MOMENTO_ZERO("Momento zero",
            "O fator de momento ficou em 0 (não é hora de comprar, segundo o seu checklist de momento)."),

    CLASSE_SEM_CAPACIDADE("Classe sem capacidade",
            "A classe/subclasse/setor deste ativo não tem déficit (está no alvo ou acima dele)."),

    SEM_CAPACIDADE("Sem capacidade",
            "O ativo já está no próprio alvo (déficit + tolerância): não há espaço para mais aporte.");

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

    /** true = o ativo é descartado do ranking e não recebe aporte. */
    public boolean elimina() {
        return this != ELEGIVEL && this != SEM_AVALIACAO;
    }
}
