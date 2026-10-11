import { useState } from "react";
import { categoriaDespesaInfo, semCategoriaInfo, type CategoriaDespesa } from "../types/despesas";

/* ══════════════════════════════════════════════════════════════════════
   GASTOS POR CATEGORIA (carteira mensal / de dívidas) — rosca INTERATIVA.

   Agrupa automaticamente as DESPESAS pelo campo opcional `categoria` e
   desenha a distribuição num donut. A categoria é uma lista FIXA do sistema
   (ver CategoriaDespesa no backend); despesas sem categoria caem em
   "Sem categoria" — o gráfico É gerado de qualquer forma.

   Interação no MESMO contrato dos demais gráficos do app: passar o mouse
   (ou focar com Tab) numa fatia OU na linha da legenda acende a fatia,
   apaga as outras e troca o centro pelo valor/percentual dela. Sem
   biblioteca de gráficos: os arcos são caminhos SVG calculados aqui e as
   cores saem dos tokens (var(--...)).
   ══════════════════════════════════════════════════════════════════════ */

interface Despesa {
  categoriaGasto: CategoriaDespesa | null;
  saldo: number;
}

interface Props {
  despesas: Despesa[];
  /** Formata dinheiro na moeda da carteira (ex.: R$ 1.234,56). */
  formatar: (valor: number) => string;
}

const T = 200;
const CX = T / 2;
const R_EXT = 78;
const R_INT = 54;

/** Fatia de anel (setor) como path de SVG, começando no topo e no sentido horário. */
function arco(a0: number, a1: number): string {
  const fim = Math.min(a1, a0 + Math.PI * 2 - 0.0001);
  const p = (r: number, a: number): [number, number] => [CX + r * Math.cos(a), CX + r * Math.sin(a)];
  const [x0, y0] = p(R_EXT, a0);
  const [x1, y1] = p(R_EXT, fim);
  const [x2, y2] = p(R_INT, fim);
  const [x3, y3] = p(R_INT, a0);
  const grande = fim - a0 > Math.PI ? 1 : 0;
  return `M ${x0} ${y0} A ${R_EXT} ${R_EXT} 0 ${grande} 1 ${x1} ${y1} `
    + `L ${x2} ${y2} A ${R_INT} ${R_INT} 0 ${grande} 0 ${x3} ${y3} Z`;
}

const fmtPctCurto = (v: number) => `${v.toLocaleString("pt-BR", { maximumFractionDigits: 1 })}%`;

export default function GraficoDespesasCategoria({ despesas, formatar }: Props) {
  const [ativo, setAtivo] = useState<string | null>(null);

  const despesasValidas = despesas.filter(d => (d.saldo || 0) > 0);
  const base = despesasValidas.reduce((s, d) => s + (d.saldo || 0), 0);

  if (despesasValidas.length === 0 || base <= 0) {
    return (
      <p style={{ fontSize: "12.5px", color: "var(--text-2)", margin: 0 }}>
        Nenhuma despesa lançada nesta carteira. Cadastre uma despesa (com ou sem categoria)
        para ver a distribuição dos gastos.
      </p>
    );
  }

  // Agrupa por categoria; despesas sem categoria viram uma fatia "Sem categoria".
  const porCategoria = new Map<string, { key: string; label: string; icon: string; cor: string; valor: number; count: number }>();
  for (const d of despesasValidas) {
    const info = d.categoriaGasto ? categoriaDespesaInfo[d.categoriaGasto] : semCategoriaInfo;
    const key = d.categoriaGasto ?? "__SEM__";
    const atual = porCategoria.get(key) ?? { key, label: info.label, icon: info.icon, cor: info.cor, valor: 0, count: 0 };
    atual.valor += d.saldo || 0;
    atual.count += 1;
    porCategoria.set(key, atual);
  }
  const fatias = Array.from(porCategoria.values()).sort((a, b) => b.valor - a.valor);

  let cursor = -Math.PI / 2; // topo, sentido horário
  const segmentos = fatias.map(f => {
    const abertura = (f.valor / base) * Math.PI * 2;
    const a0 = cursor;
    cursor += abertura;
    const comVao = fatias.length > 1 && abertura > 0.06;
    return { ...f, a0: a0 + (comVao ? 0.014 : 0), a1: a0 + abertura - (comVao ? 0.014 : 0) };
  });

  const aceso = (key: string) => ativo == null || ativo === key;
  const foco = ativo ? fatias.find(f => f.key === ativo) ?? null : null;

  const centroLabel = foco ? foco.label : "Total de gastos";
  const centroTexto = formatar(foco ? foco.valor : base);
  const centroSub = foco ? fmtPctCurto((foco.valor / base) * 100) : `${despesasValidas.length} despesa(s)`;
  const fonte = Math.max(9, Math.min(17, Math.round((2 * R_INT - 8) / Math.max(1, centroTexto.length * 0.62))));

  return (
    <div style={{ display: "flex", alignItems: "center", gap: "18px", flexWrap: "wrap" }}>
      <svg viewBox={`0 0 ${T} ${T}`} width="168" height="168" role="img" style={{ flexShrink: 0 }}
        aria-label={`Total de gastos ${formatar(base)} distribuídos em ${fatias.length} categoria(s)`}>
        <circle cx={CX} cy={CX} r={(R_EXT + R_INT) / 2} fill="none"
          stroke="var(--surface-3)" strokeWidth={R_EXT - R_INT} />

        {segmentos.map(s => (
          <path key={s.key} d={arco(s.a0, s.a1)} role="button" tabIndex={0}
            aria-label={`${s.label}: ${formatar(s.valor)} (${fmtPctCurto((s.valor / base) * 100)})`}
            onMouseEnter={() => setAtivo(s.key)} onMouseLeave={() => setAtivo(null)}
            onFocus={() => setAtivo(s.key)} onBlur={() => setAtivo(null)}
            style={{
              fill: s.cor, cursor: "pointer", outline: "none",
              opacity: aceso(s.key) ? 1 : 0.3,
              filter: ativo === s.key ? "drop-shadow(0 6px 14px rgba(15,23,42,0.25))" : "none",
              transition: "opacity .18s ease, filter .18s ease",
            }} />
        ))}

        <text x={CX} y={CX - 8} textAnchor="middle" fontSize="10" fontWeight="700"
          style={{ fill: "var(--text-3)", letterSpacing: "0.3px" }}>
          {centroLabel.toUpperCase()}
        </text>
        <text x={CX} y={CX + 13} textAnchor="middle" fontSize={fonte} fontWeight="800"
          style={{ fill: "var(--text)", fontVariantNumeric: "tabular-nums" }}>
          {centroTexto}
        </text>
        <text x={CX} y={CX + 29} textAnchor="middle" fontSize="10.5" fontWeight="700"
          style={{ fill: "var(--text-3)" }}>
          {centroSub}
        </text>
      </svg>

      <div style={{ flex: "1 1 190px", minWidth: 0, display: "flex", flexDirection: "column", gap: "2px" }}>
        {fatias.map(f => (
          <div key={f.key}
            onMouseEnter={() => setAtivo(f.key)} onMouseLeave={() => setAtivo(null)}
            title={`${f.label}: ${formatar(f.valor)} · ${f.count} despesa(s)`}
            style={{
              display: "flex", alignItems: "center", justifyContent: "space-between", gap: "10px",
              padding: "5px 8px", borderRadius: "9px",
              background: ativo === f.key ? "var(--surface-2)" : "transparent",
              border: `1px solid ${ativo === f.key ? "var(--border)" : "transparent"}`,
              transition: "background .15s ease, border-color .15s ease",
            }}>
            <span style={{ display: "inline-flex", alignItems: "center", gap: "8px", fontSize: "12.5px", color: "var(--text-2)", minWidth: 0 }}>
              <span style={{ width: "10px", height: "10px", borderRadius: "3px", background: f.cor, flexShrink: 0 }} />
              <span style={{ overflow: "hidden", textOverflow: "ellipsis", whiteSpace: "nowrap" }}>{f.icon} {f.label}</span>
            </span>
            <span style={{ fontSize: "12.5px", fontWeight: 700, color: "var(--text)", fontVariantNumeric: "tabular-nums", whiteSpace: "nowrap" }}>
              {formatar(f.valor)}
              <span style={{ fontWeight: 600, color: "var(--text-3)" }}>{" · "}{fmtPctCurto((f.valor / base) * 100)}</span>
            </span>
          </div>
        ))}
      </div>
    </div>
  );
}
