package dev.LzGuimaraes.FocusLifeHub.Despesa;

/**
 * Categoria (opcional) de uma DESPESA, usada para agrupar os gastos da carteira
 * mensal e alimentar o gráfico de distribuição por categoria.
 *
 * <p>É um conjunto FIXO de categorias do sistema (lista fechada) — por isso o
 * campo é um enum e o front só oferece as opções conhecidas. A coluna é
 * nullable: a despesa PODE ficar sem categoria e, nesse caso, o gráfico a
 * agrupa em "Sem categoria".
 */
public enum CategoriaDespesa {
    MORADIA,
    ALIMENTACAO,
    TRANSPORTE,
    SAUDE,
    EDUCACAO,
    LAZER,
    CONTAS,
    LIBERDADE_FINANCEIRA,
    OUTROS
}
