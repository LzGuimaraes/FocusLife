package dev.LzGuimaraes.FocusLifeHub.Planejamento.Avaliacao;

/**
 * Tipo da pergunta criada pelo usuário (Módulo 3).
 *
 * O tipo define COMO a pergunta vira nota — e o sistema nunca inventa nota:
 *   • SIM_NAO ......... o usuário marca; nota = nota máxima ou 0
 *   • NOTA ............ o usuário digita a nota direto (0..nota máxima)
 *   • NUMERO/PERCENTUAL o usuário informa o valor e a nota vem da FAIXA
 *                       cadastrada (regra de pontuação)
 *   • MULTIPLA_ESCOLHA o usuário escolhe uma OPÇÃO que já carrega a nota
 *   • TEXTO/LISTA ..... informativos, não pontuam
 */
public enum TipoPergunta {
    SIM_NAO,
    NOTA,
    NUMERO,
    PERCENTUAL,
    TEXTO,
    LISTA,
    MULTIPLA_ESCOLHA;

    /** Tipos pontuados por FAIXA numérica (valor mínimo/máximo → nota). */
    public boolean usaFaixas() {
        return this == NUMERO || this == PERCENTUAL;
    }

    /** Tipos pontuados por OPÇÃO (texto → nota). */
    public boolean usaOpcoes() {
        return this == MULTIPLA_ESCOLHA;
    }

    /** Tipos puramente informativos: nunca entram no cálculo do score. */
    public boolean somenteInformativo() {
        return this == TEXTO || this == LISTA;
    }

    /** Tipos em que o usuário digita a nota diretamente. */
    public boolean notaDigitada() {
        return this == NOTA || this == SIM_NAO;
    }

    /** Valor padrão de `conta_no_score` quando o cliente não informa. */
    public boolean pontuaPorPadrao() {
        return !somenteInformativo();
    }
}
