package dev.LzGuimaraes.FocusLifeHub.Ativo;

/**
 * Categoria de investimento de uma posição e também CLASSE da Carteira Ideal.
 * OUTROS funciona como bucket para a Carteira Ideal (posições nunca são
 * cadastradas com ele — o formulário de investimento só oferece as demais).
 */
public enum CategoriaInvestimento {
    RENDA_FIXA,
    TESOURO_DIRETO,
    ACOES,
    FIIS,
    ETFS,
    CRIPTOMOEDAS,
    OUTROS
}
