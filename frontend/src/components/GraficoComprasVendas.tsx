import { useCallback, useEffect, useRef, useState, type CSSProperties } from "react";
import type { SeriePonto } from "../types/comprasVendas";

/* ══════════════════════════════════════════════════════════════════════
   Gráfico de ANDAMENTO de Compras/Vendas — SVG puro e INTERATIVO.

   As duas leituras da mesma série dividem UM plano cartesiano, com eixos
   próprios (o "duplo eixo" do redesign):
     • barras — VALOR BRUTO movimentado no período (eixo ESQUERDO, R$);
     • linha  — RENTABILIDADE ACUMULADA realizada até o período (eixo DIREITO, %).

   Interação (o MESMO contrato dos gráficos da Carteira Ideal): passar o mouse
   ou focar com Tab num período ACENDE a barra e o ponto dele e APAGA os
   demais; um balão mostra os dois valores. Cada período tem uma faixa
   invisível de captura, então acerta-se a COLUNA inteira, não só a barra —
   e tudo é focável (role="button"), nunca um gráfico "morto".

   Sem biblioteca de gráficos. O SVG é medido em PIXELS (ResizeObserver) para
   os círculos da linha continuarem REDONDOS: o preserveAspectRatio="none"
   antigo transformava ponto em elipse. As cores saem dos TOKENS (var(--...)),
   então tema claro e escuro funcionam sem regra extra no bridge.
   ══════════════════════════════════════════════════════════════════════ */

interface Props {
  serie: SeriePonto[];
}

const ALTURA = 236;
const PAD_TOPO = 14;
const PAD_BASE = 14;

const fmtReais = (v: number) =>
  `R$ ${v.toLocaleString("pt-BR", { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;
const fmtEixoReais = (v: number) =>
  `R$ ${v.toLocaleString("pt-BR", { maximumFractionDigits: 2 })}`;
const fmtPct = (v: number) =>
  `${v.toLocaleString("pt-BR", { minimumFractionDigits: 2, maximumFractionDigits: 2 })}%`;
const fmtPctCurto = (v: number) =>
  `${v.toLocaleString("pt-BR", { maximumFractionDigits: 0 })}%`;

const clamp = (v: number, min: number, max: number) => Math.min(max, Math.max(min, v));

/** Passo "redondo" (1 · 2 · 2,5 · 5 · 10 × 10ⁿ) — mantém o eixo de R$ legível. */
function passoBonito(bruto: number): number {
  const exp = Math.floor(Math.log10(Math.max(bruto, 1e-9)));
  const base = 10 ** exp;
  const n = bruto / base;
  const fator = n <= 1 ? 1 : n <= 2 ? 2 : n <= 2.5 ? 2.5 : n <= 5 ? 5 : 10;
  return fator * base;
}

/** Eixo de R$: pé em zero, topo "redondo" que cobre o maior valor bruto. */
function escalaReais(max: number): { hi: number; ticks: number[] } {
  const divisoes = 4;
  const passo = passoBonito(max / divisoes);
  const hi = passo * divisoes;
  return { hi, ticks: Array.from({ length: divisoes + 1 }, (_, i) => i * passo) };
}

/** Eixo de %: encaixa os dados em passos REDONDOS (sempre incluindo o zero). */
function escalaPct(min: number, max: number): { lo: number; hi: number; ticks: number[] } {
  const lo0 = Math.min(0, min);
  const hi0 = Math.max(0, max);
  const alvo = (hi0 - lo0) / 4 || 1;
  const passo = [1, 2, 5, 10, 20, 25, 50, 100].find(p => p >= alvo) ?? 100;
  const lo = Math.floor(lo0 / passo) * passo;
  const hi = Math.max(lo + passo, Math.ceil(hi0 / passo) * passo);
  const ticks: number[] = [];
  for (let v = lo; v <= hi + 1e-9; v += passo) ticks.push(v);
  return { lo, hi, ticks };
}

/** Largura (px) do elemento; 0 até a primeira medição. */
function useLargura() {
  const [largura, setLargura] = useState(0);
  const roRef = useRef<ResizeObserver | null>(null);
  const ref = useCallback((node: HTMLDivElement | null) => {
    roRef.current?.disconnect();
    if (!node) return;
    setLargura(node.clientWidth);
    const ro = new ResizeObserver(() => setLargura(node.clientWidth));
    ro.observe(node);
    roRef.current = ro;
  }, []);
  useEffect(() => () => roRef.current?.disconnect(), []);
  return [ref, largura] as const;
}

export default function GraficoComprasVendas({ serie }: Props) {
  const [ref, largura] = useLargura();
  const [ativo, setAtivo] = useState<number | null>(null);

  if (serie.length === 0) {
    return (
      <div style={vazio}>
        <span style={{ fontSize: "12px", color: "var(--text-3)", textAlign: "center", padding: "0 12px" }}>
          Sem operações no período. Registre uma compra ou venda para o gráfico mostrar o andamento.
        </span>
      </div>
    );
  }

  const W = Math.max(320, largura || 640);
  const H = ALTURA;
  const topo = PAD_TOPO;
  const base = H - PAD_BASE;
  const alturaPlot = base - topo;

  const maxBruto = Math.max(1, ...serie.map(p => p.valor_bruto));
  const { hi: hiReais, ticks: ticksReais } = escalaReais(maxBruto);

  const percentuais = serie
    .map(p => p.valor_percentual_acumulado)
    .filter((v): v is number => v != null);
  const minPct = Math.min(0, ...(percentuais.length ? percentuais : [0]));
  const maxPct = Math.max(0, ...(percentuais.length ? percentuais : [0]));
  const { lo: loPct, hi: hiPct, ticks: ticksPct } = escalaPct(minPct, maxPct);

  const n = serie.length;
  const passo = W / n;
  const larguraBarra = Math.max(8, Math.min(passo * 0.5, 72));
  const xCentro = (i: number) => i * passo + passo / 2;
  const yReais = (v: number) => base - (v / hiReais) * alturaPlot;
  const yPct = (v: number) => base - ((v - loPct) / (hiPct - loPct)) * alturaPlot;

  const pontosPct = serie
    .map((p, i) => ({ v: p.valor_percentual_acumulado, i }))
    .filter((x): x is { v: number; i: number } => x.v != null);
  const linha = pontosPct.length >= 2
    ? `M ${pontosPct.map(({ v, i }) => `${xCentro(i)},${yPct(v)}`).join(" L ")}`
    : null;

  const ultimo = serie[n - 1];

  return (
    <div>
      {/* Legenda explícita de qual leitura é qual */}
      <div style={{ display: "flex", justifyContent: "space-between", alignItems: "baseline", flexWrap: "wrap", gap: "8px", marginBottom: "10px" }}>
        <div style={{ display: "flex", gap: "16px", flexWrap: "wrap", fontSize: "12px", color: "var(--text-2)" }}>
          <span style={{ display: "inline-flex", alignItems: "center", gap: "6px" }}>
            <span style={{ width: "12px", height: "12px", borderRadius: "3px", background: "var(--color-primary)" }} />
            Valor bruto no período (R$)
          </span>
          <span style={{ display: "inline-flex", alignItems: "center", gap: "6px" }}>
            <span style={{ width: "16px", height: "2.5px", borderRadius: "2px", background: "var(--color-success)" }} />
            Rentabilidade acumulada (%)
          </span>
        </div>
        <span style={{ fontSize: "12px", color: "var(--text-2)" }}>
          Último período: <strong style={{ color: "var(--text)" }}>{fmtReais(ultimo.valor_bruto)}</strong>
          {ultimo.valor_percentual_acumulado != null && <> · {fmtPct(ultimo.valor_percentual_acumulado)}</>}
        </span>
      </div>

      <div style={{ display: "flex", alignItems: "stretch", gap: "8px" }}>
        {/* Eixo esquerdo — R$ */}
        <div style={{ position: "relative", width: "56px", height: H, flexShrink: 0 }}>
          {ticksReais.map((t, i) => (
            <span key={i} style={{ ...rotuloEixo, right: 0, top: yReais(t) }}>{fmtEixoReais(t)}</span>
          ))}
        </div>

        {/* Área do gráfico (medida em px) */}
        <div ref={ref} style={{ position: "relative", flex: "1 1 auto", minWidth: 0 }}>
          <svg width="100%" height={H} viewBox={`0 0 ${W} ${H}`} preserveAspectRatio="none"
            role="img" aria-label="Valor bruto (R$) e rentabilidade acumulada (%) por período">
            {ticksReais.map((t, i) => (
              <line key={i} x1={0} y1={yReais(t)} x2={W} y2={yReais(t)}
                stroke="var(--border-subtle)" strokeWidth={1} vectorEffect="non-scaling-stroke"
                strokeDasharray={i === 0 ? undefined : "4 4"} />
            ))}

            {/* Barras — valor bruto */}
            {serie.map((pt, i) => {
              const y = yReais(pt.valor_bruto);
              const aceso = ativo == null || ativo === i;
              return (
                <rect key={pt.periodo} x={xCentro(i) - larguraBarra / 2} y={y}
                  width={larguraBarra} height={Math.max(0, base - y)} rx={6}
                  style={{
                    fill: "var(--color-primary)",
                    opacity: aceso ? 1 : 0.38,
                    filter: ativo === i ? "drop-shadow(0 6px 16px rgba(99,102,241,0.45))" : "none",
                    transition: "opacity .18s ease, filter .18s ease",
                  }} />
              );
            })}

            {/* Linha — rentabilidade acumulada */}
            {linha && (
              <path d={linha} fill="none" stroke="var(--color-success)" strokeWidth={2}
                strokeLinejoin="round" strokeLinecap="round" vectorEffect="non-scaling-stroke" />
            )}
            {pontosPct.map(({ v, i }) => {
              const aceso = ativo == null || ativo === i;
              return (
                <circle key={i} cx={xCentro(i)} cy={yPct(v)} r={ativo === i ? 6 : 4}
                  style={{
                    fill: "var(--color-success)",
                    stroke: "var(--surface)",
                    strokeWidth: 1.5,
                    opacity: aceso ? 1 : 0.38,
                    transition: "opacity .18s ease",
                  }} />
              );
            })}

            {/* Faixas de captura: a COLUNA inteira responde ao mouse/teclado */}
            {serie.map((pt, i) => (
              <rect key={`hit-${pt.periodo}`} x={i * passo} y={0} width={passo} height={H}
                fill="transparent" role="button" tabIndex={0}
                aria-label={`${pt.periodo}: ${fmtReais(pt.valor_bruto)}${pt.valor_percentual_acumulado != null ? `, rentabilidade ${fmtPct(pt.valor_percentual_acumulado)}` : ""}`}
                onMouseEnter={() => setAtivo(i)} onMouseLeave={() => setAtivo(null)}
                onFocus={() => setAtivo(i)} onBlur={() => setAtivo(null)}
                style={{ cursor: "pointer", outline: "none" }} />
            ))}
          </svg>

          {/* Balão com os dois valores do período apontado */}
          {ativo != null && serie[ativo] && (() => {
            const pt = serie[ativo];
            return (
              <div style={{
                position: "absolute", left: clamp(xCentro(ativo), 86, Math.max(86, W - 86)),
                top: Math.max(4, yReais(pt.valor_bruto) - 58), transform: "translateX(-50%)",
                pointerEvents: "none", background: "#0f172a", color: "#fff", borderRadius: "10px",
                padding: "8px 11px", boxShadow: "0 10px 24px rgba(15,23,42,0.35)",
                fontSize: "11.5px", lineHeight: 1.5, whiteSpace: "nowrap", zIndex: 2,
              }}>
                <div style={{ fontWeight: 700, color: "#cbd5e1", marginBottom: "2px" }}>{pt.periodo}</div>
                <div style={{ display: "flex", alignItems: "center", gap: "7px", fontVariantNumeric: "tabular-nums" }}>
                  <span style={{ width: "8px", height: "8px", borderRadius: "3px", background: "var(--color-primary)" }} />
                  {fmtReais(pt.valor_bruto)}
                </div>
                {pt.valor_percentual_acumulado != null && (
                  <div style={{ display: "flex", alignItems: "center", gap: "7px", fontVariantNumeric: "tabular-nums" }}>
                    <span style={{ width: "8px", height: "8px", borderRadius: "3px", background: "var(--color-success)" }} />
                    {fmtPct(pt.valor_percentual_acumulado)}
                  </div>
                )}
              </div>
            );
          })()}

          {/* Rótulos dos períodos (HTML, alinhados com as colunas) */}
          <div style={{ display: "flex", marginTop: "6px" }}>
            {serie.map((pt, i) => (
              <span key={pt.periodo} style={{
                flex: 1, textAlign: "center", fontSize: "10.5px",
                color: ativo === i ? "var(--text)" : "var(--text-3)",
                fontWeight: ativo === i ? 700 : 400,
                overflow: "hidden", textOverflow: "ellipsis", whiteSpace: "nowrap",
                transition: "color .18s ease",
              }}>
                {pt.periodo}
              </span>
            ))}
          </div>
        </div>

        {/* Eixo direito — % */}
        <div style={{ position: "relative", width: "52px", height: H, flexShrink: 0 }}>
          {ticksPct.map((t, i) => (
            <span key={i} style={{ ...rotuloEixo, left: 0, top: yPct(t) }}>{fmtPctCurto(t)}</span>
          ))}
        </div>
      </div>

      <div style={{ display: "flex", justifyContent: "space-between", gap: "12px", marginTop: "4px", fontSize: "11px", color: "var(--text-3)", flexWrap: "wrap" }}>
        <span>Escala das barras: 0 → {fmtReais(maxBruto)}</span>
        <span>Escala da linha: {fmtPct(minPct)} → {fmtPct(maxPct)}</span>
      </div>
    </div>
  );
}

const rotuloEixo: CSSProperties = {
  position: "absolute", transform: "translateY(-50%)",
  fontSize: "10.5px", color: "var(--text-3)", fontVariantNumeric: "tabular-nums", whiteSpace: "nowrap",
};

const vazio: CSSProperties = {
  display: "flex", alignItems: "center", justifyContent: "center",
  minHeight: "160px", background: "var(--surface-2)",
  border: "1px dashed var(--border)", borderRadius: "10px",
};
