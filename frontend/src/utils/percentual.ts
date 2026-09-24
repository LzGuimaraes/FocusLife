import type { CategoriaInvestimento } from "../types/planejamento";

/* ══════════════════════════════════════════════════════════════════════
   Categorias / formatação usadas pelo módulo de Planejamento.
   A lista inclui OUTROS (bucket da Carteira Ideal), que não existe no
   formulário de investimento — por isso é uma lista própria.
   ══════════════════════════════════════════════════════════════════════ */

export interface CategoriaInfo {
  key: CategoriaInvestimento;
  label: string;
  icon: string;
  color: string;
  bg: string;
}

export const CATEGORIAS: CategoriaInfo[] = [
  { key: "RENDA_FIXA", label: "Renda Fixa", icon: "📊", color: "#3b82f6", bg: "#dbeafe" },
  { key: "TESOURO_DIRETO", label: "Tesouro Direto", icon: "🏛️", color: "#10b981", bg: "#d1fae5" },
  { key: "ACOES", label: "Ações", icon: "📈", color: "#6366f1", bg: "#eef2ff" },
  { key: "FIIS", label: "FIIs", icon: "🏢", color: "#8b5cf6", bg: "#ede9fe" },
  { key: "ETFS", label: "ETFs", icon: "📦", color: "#06b6d4", bg: "#ecfeff" },
  { key: "CRIPTOMOEDAS", label: "Criptomoedas", icon: "₿", color: "#f59e0b", bg: "#fef3c7" },
  { key: "OUTROS", label: "Outros", icon: "🗂️", color: "#64748b", bg: "#f1f5f9" },
];

const CATEGORIA_PADRAO = CATEGORIAS[CATEGORIAS.length - 1];

export const catInfo = (classe: CategoriaInvestimento): CategoriaInfo =>
  CATEGORIAS.find(c => c.key === classe) ?? CATEGORIA_PADRAO;

/**
 * A classe é comprada em COTAS INTEIRAS?
 *
 * Ação, FII e ETF sim (ninguém compra 72,7 cotas); cripto, renda fixa e Tesouro
 * aceitam fração, então a quantidade é informativa e com casas decimais.
 * Mesma regra do backend (`AlocacaoService.compraEmUnidadesInteiras`).
 */
export const compraEmCotas = (classe: CategoriaInvestimento): boolean =>
  classe === "ACOES" || classe === "FIIS" || classe === "ETFS";

const MOEDA_SIMBOLO: Record<string, string> = {
  BRL: "R$", USD: "$", EUR: "€", GBP: "£", JPY: "¥",
};

/** Valor monetário: "R$ 1.234,56". */
export function fmtMoeda(valor: number | null | undefined, moeda: string): string {
  if (valor == null) return "-";
  const simbolo = MOEDA_SIMBOLO[moeda] || moeda;
  return `${simbolo} ${valor.toLocaleString("pt-BR", { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;
}

/** Percentual com 2 casas: "12,34%". */
export function fmtPercentual(valor: number | null | undefined): string {
  return `${(valor ?? 0).toLocaleString("pt-BR", { minimumFractionDigits: 2, maximumFractionDigits: 2 })}%`;
}

/** Diferença em pontos percentuais, com sinal: "+1,20 p.p.". */
export function fmtPontosPercentuais(valor: number): string {
  const sinal = valor > 0 ? "+" : "";
  return `${sinal}${valor.toLocaleString("pt-BR", { minimumFractionDigits: 2, maximumFractionDigits: 2 })} p.p.`;
}

/** Soma percentuais com tolerância de 0,01 p.p. (espelha a validação do backend). */
export function somaFechada(soma: number): boolean {
  return Math.abs(soma - 100) <= 0.01;
}

const CLASSE_POR_TIPO_CATALOGO: Record<string, CategoriaInvestimento> = {
  ACAO: "ACOES",
  FII: "FIIS",
  ETF: "ETFS",
  BDR: "ACOES",
  CRIPTOMOEDA: "CRIPTOMOEDAS",
};

/** Classe sugerida ao escolher um ticker do catálogo (o usuário pode trocar). */
export function classeSugerida(tipoCatalogo: string | null | undefined): CategoriaInvestimento {
  return (tipoCatalogo && CLASSE_POR_TIPO_CATALOGO[tipoCatalogo]) || "OUTROS";
}
