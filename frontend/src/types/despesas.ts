/**
 * Categorias de despesa (carteira mensal / de dívidas).
 *
 * Espelha o enum `CategoriaDespesa` do backend: é uma lista FIXA do sistema.
 * O campo é OPCIONAL no cadastro — despesas sem categoria aparecem agrupadas
 * em "Sem categoria" no gráfico de gastos por categoria.
 *
 * Fica fora do componente do gráfico para que a fonte única (rótulo, ícone,
 * cor) seja importável tanto pelo gráfico quanto pelo formulário, sem
 * misturar componentes e constantes no mesmo arquivo.
 */

/** Categorias do sistema (espelham o enum CategoriaDespesa do backend). */
export type CategoriaDespesa =
  | "MORADIA" | "ALIMENTACAO" | "TRANSPORTE" | "SAUDE"
  | "EDUCACAO" | "LAZER" | "CONTAS" | "OUTROS";

/** Rótulo, ícone e cor de cada categoria. */
export const categoriaDespesaInfo: Record<CategoriaDespesa, { icon: string; label: string; cor: string }> = {
  MORADIA: { icon: "🏠", label: "Moradia", cor: "#6366f1" },
  ALIMENTACAO: { icon: "🍽️", label: "Alimentação", cor: "#f59e0b" },
  TRANSPORTE: { icon: "🚗", label: "Transporte", cor: "#06b6d4" },
  SAUDE: { icon: "🩺", label: "Saúde", cor: "#ef4444" },
  EDUCACAO: { icon: "🎓", label: "Educação", cor: "#8b5cf6" },
  LAZER: { icon: "🎉", label: "Lazer", cor: "#ec4899" },
  CONTAS: { icon: "🧾", label: "Contas", cor: "#0ea5e9" },
  OUTROS: { icon: "📦", label: "Outros", cor: "#64748b" },
};

/** Categoria para despesas sem categoria definida (null). */
export const semCategoriaInfo = { icon: "❔", label: "Sem categoria", cor: "#94a3b8" };

/** Ordem canônica das categorias (mesma do enum no backend). */
export const categoriasDespesa = Object.keys(categoriaDespesaInfo) as CategoriaDespesa[];
