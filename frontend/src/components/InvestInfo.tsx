import type { CSSProperties } from "react";

/* ══════════════════════════════════════════════════════════════
   Cálculos de rentabilidade de investimentos (compartilhado
   entre Contas.tsx e CarteiraDetalhe.tsx)

   Base: Preço Médio × Quantidade  (investido)
         Preço Atual × Quantidade  (saldo atual de mercado)
   ══════════════════════════════════════════════════════════════ */

const moedaS: Record<string, string> = { BRL: "R$", USD: "$", EUR: "€", GBP: "£", JPY: "¥" };

const fmt = (v: number | null | undefined, m: string) => v != null ? `${moedaS[m] || m} ${v.toFixed(2)}` : "-";

/** Formata porcentagem com 2 casas decimais e símbolo %. */
export const fmtPct = (v: number) => `${v.toFixed(2)}%`;

/** Formata valor monetário com sinal (+/-) para lucro/prejuízo. */
export const fmtSigned = (v: number, m: string) => (v > 0 ? `+${fmt(v, m)}` : v < 0 ? `-${fmt(Math.abs(v), m)}` : fmt(v, m));

export interface InvestAtivo {
  categoria: string;
  quantidade: number | null;
  valorUnitario: number | null;
  precoAtual: number | null;
  saldo: number;
}

const ehInvestComQtd = (a: InvestAtivo): boolean => a.categoria === "INVESTIMENTO" && a.quantidade != null && a.valorUnitario != null;

/** Total Investido = Preço Médio × Quantidade (fallback: saldo para ativos sem quantidade). */
export const totalInvestido = (a: InvestAtivo): number => ehInvestComQtd(a) ? a.valorUnitario! * a.quantidade! : (a.saldo ?? 0);

/** Saldo Atual = Preço Atual × Quantidade (fallback: Preço Médio × Quantidade ou saldo). */
export const saldoAtual = (a: InvestAtivo): number => ehInvestComQtd(a) ? (a.precoAtual ?? a.valorUnitario!) * a.quantidade! : (a.saldo ?? 0);

/** Lucro/Prejuízo (R$) = Saldo Atual − Total Investido. */
export const lucro = (a: InvestAtivo): number => saldoAtual(a) - totalInvestido(a);

/** Rentabilidade (%) = ((Saldo Atual − Total Investido) / Total Investido) × 100. Evita divisão por zero. */
export const rentabilidade = (a: InvestAtivo): number => {
  const inv = totalInvestido(a);
  if (inv === 0) return 0;
  return ((saldoAtual(a) - inv) / inv) * 100;
};

const rowStyle: CSSProperties = { display: "flex", justifyContent: "space-between", alignItems: "center" };
const labelStyle: CSSProperties = { fontSize: "11px", color: "#64748b" };
const valueStyle: CSSProperties = { fontSize: "12px", fontWeight: 600, color: "#334155" };

/** Painel com os indicadores de um ativo de investimento (Preço Médio, Preço Atual,
    Quantidade, Total Investido, Saldo Atual, Lucro/Prejuízo e Rentabilidade). */
export function InvestInfo({ ativo, moeda }: { ativo: InvestAtivo; moeda: string }) {
  if (!ehInvestComQtd(ativo)) return null;

  const inv = totalInvestido(ativo);
  const saldo = saldoAtual(ativo);
  const lc = lucro(ativo);
  const rent = rentabilidade(ativo);
  const rentStyle = rent > 0 ? { color: "#10b981", arrow: "▲" } : rent < 0 ? { color: "#ef4444", arrow: "▼" } : { color: "#64748b", arrow: "•" };
  const lcColor = lc > 0 ? "#10b981" : lc < 0 ? "#ef4444" : "#64748b";
  const precoAtualColor = ativo.precoAtual == null ? undefined : ativo.precoAtual >= ativo.valorUnitario! ? "#10b981" : "#ef4444";

  return (
    <div style={{ display: "flex", flexDirection: "column", gap: "5px", marginBottom: "12px" }}>
      <div style={rowStyle}><span style={labelStyle}>💵 Preço Médio</span><span style={valueStyle}>{fmt(ativo.valorUnitario, moeda)}</span></div>
      <div style={rowStyle}><span style={labelStyle}>📊 Preço Atual</span><span style={{ ...valueStyle, color: precoAtualColor }}>{ativo.precoAtual != null ? fmt(ativo.precoAtual, moeda) : "--"}</span></div>
      <div style={rowStyle}><span style={labelStyle}>📦 Quantidade</span><span style={valueStyle}>{ativo.quantidade} un</span></div>
      <div style={rowStyle}><span style={labelStyle}>💰 Total Investido</span><span style={{ ...valueStyle, fontWeight: 700 }}>{fmt(inv, moeda)}</span></div>
      <div style={rowStyle}><span style={labelStyle}>📈 Saldo Atual</span><span style={{ ...valueStyle, fontWeight: 700, color: "#10b981" }}>{fmt(saldo, moeda)}</span></div>
      <div style={rowStyle}><span style={labelStyle}>📊 Lucro/Prejuízo</span><span style={{ ...valueStyle, fontWeight: 700, color: lcColor }}>{fmtSigned(lc, moeda)}</span></div>
      <div style={rowStyle}><span style={labelStyle}>🎯 Rentabilidade</span><span style={{ ...valueStyle, fontWeight: 800, color: rentStyle.color }}>{rentStyle.arrow} {fmtPct(rent)}</span></div>
    </div>
  );
}
