import type { CSSProperties } from "react";
import type { SeriePonto } from "../types/comprasVendas";

/* ══════════════════════════════════════════════════════════════════════
   Gráfico de ANDAMENTO de Compras/Vendas — SVG puro, sem dependência nova.

   Mostra DUAS leituras da mesma série, com legenda explícita:
     • barras — VALOR BRUTO movimentado no período (R$);
     • linha  — RENTABILIDADE ACUMULADA realizada até o período (%).

   Como no restante do app: o SVG usa preserveAspectRatio="none" e os rótulos
   ficam em HTML FORA do <svg> (texto dentro do SVG distorceria com a largura).
   Nunca fica mudo: sem dados mostra estado vazio com texto.
   ══════════════════════════════════════════════════════════════════════ */

interface Props {
  serie: SeriePonto[];
}

const LARGURA = 600;
const ALTURA_BARRAS = 132;
const ALTURA_LINHA = 96;

const fmtReais = (v: number) =>
  `R$ ${v.toLocaleString("pt-BR", { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;
const fmtPct = (v: number) =>
  `${v.toLocaleString("pt-BR", { minimumFractionDigits: 2, maximumFractionDigits: 2 })}%`;

export default function GraficoComprasVendas({ serie }: Props) {
  if (serie.length === 0) {
    return (
      <div style={vazio}>
        <span style={{ fontSize: "12px", color: "#94a3b8", textAlign: "center", padding: "0 12px" }}>
          Sem operações no período. Registre uma compra ou venda para o gráfico mostrar o andamento.
        </span>
      </div>
    );
  }

  const maxBruto = Math.max(1, ...serie.map(p => p.valor_bruto));
  const percentuais = serie
    .map(p => p.valor_percentual_acumulado)
    .filter((v): v is number => v != null);
  const minPct = Math.min(0, ...(percentuais.length ? percentuais : [0]));
  const maxPct = Math.max(0, ...(percentuais.length ? percentuais : [0]));
  const spanPct = (maxPct - minPct) || 1;

  const passo = LARGURA / serie.length;
  const larguraBarra = Math.max(6, Math.min(passo * 0.6, 60));
  const xCentro = (i: number) => i * passo + passo / 2;
  const yPct = (v: number) => ALTURA_LINHA - ((v - minPct) / spanPct) * ALTURA_LINHA;

  const pontosLinha = serie
    .map((p, i) => ({ v: p.valor_percentual_acumulado, i }))
    .filter((x): x is { v: number; i: number } => x.v != null);
  const linha = pontosLinha.length >= 2
    ? `M ${pontosLinha.map(({ v, i }) => `${xCentro(i)},${yPct(v)}`).join(" L ")}`
    : null;

  const ultimoPercentual = [...serie].reverse()
    .find(p => p.valor_percentual_acumulado != null)?.valor_percentual_acumulado ?? null;
  const ultimoBruto = serie[serie.length - 1].valor_bruto;

  return (
    <div>
      {/* Legenda explícita de qual leitura é qual */}
      <div style={{ display: "flex", justifyContent: "space-between", alignItems: "baseline", flexWrap: "wrap", gap: "8px", marginBottom: "10px" }}>
        <div style={{ display: "flex", gap: "14px", flexWrap: "wrap", fontSize: "12px", color: "#64748b" }}>
          <span style={{ display: "inline-flex", alignItems: "center", gap: "6px" }}>
            <span style={{ width: "12px", height: "12px", borderRadius: "3px", background: "#6366f1" }} />
            Valor bruto no período (R$)
          </span>
          <span style={{ display: "inline-flex", alignItems: "center", gap: "6px" }}>
            <span style={{ width: "16px", height: "2.5px", borderRadius: "2px", background: "#10b981" }} />
            Rentabilidade acumulada (%)
          </span>
        </div>
        <span style={{ fontSize: "12px", color: "#475569" }}>
          Último período: <strong>{fmtReais(ultimoBruto)}</strong>
          {ultimoPercentual != null && <> · {fmtPct(ultimoPercentual)}</>}
        </span>
      </div>

      {/* Barras — valor bruto (R$) */}
      <svg width="100%" height={ALTURA_BARRAS} viewBox={`0 0 ${LARGURA} ${ALTURA_BARRAS}`} preserveAspectRatio="none"
        role="img" aria-label="Valor bruto movimentado por período">
        <title>{serie.map(p => `${p.periodo}: ${fmtReais(p.valor_bruto)}`).join(" · ")}</title>
        {[0.25, 0.5, 0.75].map(f => (
          <line key={f} x1="0" y1={ALTURA_BARRAS * f} x2={LARGURA} y2={ALTURA_BARRAS * f}
            stroke="#e2e8f0" strokeWidth="1" vectorEffect="non-scaling-stroke" strokeDasharray="4 4" />
        ))}
        {serie.map((p, i) => {
          const altura = Math.max(2, (p.valor_bruto / maxBruto) * (ALTURA_BARRAS - 8));
          return (
            <rect key={p.periodo} x={xCentro(i) - larguraBarra / 2} y={ALTURA_BARRAS - altura}
              width={larguraBarra} height={altura} rx={3} fill="#6366f1" opacity={0.85}>
              <title>{`${p.periodo}: ${fmtReais(p.valor_bruto)}`}</title>
            </rect>
          );
        })}
        <line x1="0" y1={ALTURA_BARRAS} x2={LARGURA} y2={ALTURA_BARRAS} stroke="#cbd5e1" strokeWidth="1" vectorEffect="non-scaling-stroke" />
      </svg>

      {/* Linha — rentabilidade acumulada (%) */}
      <svg width="100%" height={ALTURA_LINHA} viewBox={`0 0 ${LARGURA} ${ALTURA_LINHA}`} preserveAspectRatio="none"
        role="img" aria-label="Rentabilidade acumulada por período" style={{ marginTop: "6px" }}>
        <title>{serie.map(p => `${p.periodo}: ${p.valor_percentual_acumulado != null ? fmtPct(p.valor_percentual_acumulado) : "sem venda"}`).join(" · ")}</title>
        <line x1="0" y1={yPct(0)} x2={LARGURA} y2={yPct(0)}
          stroke="#cbd5e1" strokeWidth="1" vectorEffect="non-scaling-stroke" strokeDasharray="4 4" />
        {linha && (
          <path d={linha} fill="none" stroke="#10b981" strokeWidth="2"
            strokeLinejoin="round" strokeLinecap="round" vectorEffect="non-scaling-stroke" />
        )}
        {pontosLinha.map(({ v, i }) => (
          <circle key={i} cx={xCentro(i)} cy={yPct(v)} r={3} fill="#10b981">
            <title>{`${serie[i].periodo}: ${fmtPct(v)}`}</title>
          </circle>
        ))}
      </svg>

      {/* Rótulos dos períodos em HTML (fora do SVG) */}
      <div style={{ display: "flex", marginTop: "6px" }}>
        {serie.map(p => (
          <span key={p.periodo} style={{
            flex: 1, textAlign: "center", fontSize: "10px", color: "#94a3b8",
            overflow: "hidden", textOverflow: "ellipsis", whiteSpace: "nowrap",
          }}>
            {p.periodo}
          </span>
        ))}
      </div>

      <div style={{ display: "flex", justifyContent: "space-between", marginTop: "4px", fontSize: "11px", color: "#94a3b8" }}>
        <span>Escala das barras: 0 → {fmtReais(maxBruto)}</span>
        <span>Escala da linha: {fmtPct(minPct)} → {fmtPct(maxPct)}</span>
      </div>
    </div>
  );
}

const vazio: CSSProperties = {
  display: "flex", alignItems: "center", justifyContent: "center",
  minHeight: "160px", background: "#f8fafc",
  border: "1px dashed #e2e8f0", borderRadius: "10px",
};
