import { useState } from "react";

/* ══════════════════════════════════════════════════════════════════════
   ROSCA do "Resumo do período" (Compras/Vendas).

   Uma leitura só, honesta: o VALOR BRUTO vendido no período se decompõe em
   CAPITAL (o custo do que foi vendido) + RESULTADO (o lucro). A fatia verde
   É o lucro; o resto é o capital que voltou. Se houve prejuízo, NÃO existe
   fatia verde — uma fatia de "prejuízo" seria mentira, porque não tem
   tamanho no anel; o número aparece em vermelho na legenda.

   Interativa como a rosca da Carteira Ideal: passar o mouse (ou focar com
   Tab) numa fatia OU na linha da legenda acende a fatia, apaga a outra e
   troca o centro pelo valor dela. Sem biblioteca de gráficos: os arcos são
   caminhos SVG calculados aqui; as cores saem dos tokens (var(--...)).
   ══════════════════════════════════════════════════════════════════════ */

interface Props {
  totalVendido: number;
  lucro: number;
  rentabilidade: number | null;
}

const fmtBRL = (v: number) =>
  `R$ ${v.toLocaleString("pt-BR", { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;
const fmtPct = (v: number) =>
  `${v.toLocaleString("pt-BR", { minimumFractionDigits: 2, maximumFractionDigits: 2 })}%`;
const fmtPercentualCurto = (v: number) =>
  `${v.toLocaleString("pt-BR", { maximumFractionDigits: 1 })}%`;

const T = 200;
const CX = T / 2;
const R_EXT = 78;
const R_INT = 56;

/** Fatia de anel (setor) como path de SVG. */
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

export default function ResumoPeriodoDonut({ totalVendido, lucro, rentabilidade }: Props) {
  const [ativo, setAtivo] = useState<string | null>(null);

  const vendido = Math.max(0, totalVendido);
  if (vendido <= 0) {
    return (
      <p style={{ fontSize: "12.5px", color: "var(--text-2)", margin: 0 }}>
        Sem vendas no período. Registre uma venda para ver o resumo.
      </p>
    );
  }

  const positivo = lucro >= 0;
  const capital = positivo ? Math.max(0, vendido - lucro) : vendido;
  const resultado = positivo ? lucro : 0;
  const base = capital + resultado || 1;

  const fatias = [
    { key: "capital", label: "Capital", valor: capital, cor: "var(--color-primary)" },
    {
      key: "resultado",
      label: positivo ? "Resultado" : "Prejuízo",
      valor: resultado,
      cor: positivo ? "var(--color-success)" : "var(--color-danger)",
    },
  ];

  const visiveis = fatias.filter(f => f.valor / base > 0.004);
  let cursor = -Math.PI / 2; // começa no topo, no sentido horário
  const segmentos = fatias.map(f => {
    const abertura = (f.valor / base) * Math.PI * 2;
    const a0 = cursor;
    cursor += abertura;
    const comVao = visiveis.length > 1 && abertura > 0.06;
    return { ...f, a0: a0 + (comVao ? 0.015 : 0), a1: a0 + abertura - (comVao ? 0.015 : 0) };
  });

  const aceso = (key: string) => ativo == null || ativo === key;
  const foco = ativo ? fatias.find(f => f.key === ativo) ?? null : null;

  const centroLabel = foco ? foco.label : "Valor bruto";
  const centroTexto = fmtBRL(foco ? foco.valor : vendido);
  const centroSub = foco ? fmtPercentualCurto((foco.valor / base) * 100) : "no período";
  const fonte = Math.max(9, Math.min(17, Math.round((2 * R_INT - 8) / Math.max(1, centroTexto.length * 0.62))));

  return (
    <div style={{ display: "flex", alignItems: "center", gap: "16px", flexWrap: "wrap" }}>
      <svg viewBox={`0 0 ${T} ${T}`} width="150" height="150" role="img" style={{ flexShrink: 0 }}
        aria-label={`Valor bruto de ${fmtBRL(vendido)} no período, com resultado de ${fmtBRL(lucro)}`}>
        <circle cx={CX} cy={CX} r={(R_EXT + R_INT) / 2} fill="none"
          stroke="var(--surface-3)" strokeWidth={R_EXT - R_INT} />

        {segmentos.map(s => s.valor / base > 0.004 && (
          <path key={s.key} d={arco(s.a0, s.a1)} role="button" tabIndex={0}
            aria-label={`${s.label}: ${fmtBRL(s.valor)}`}
            onMouseEnter={() => setAtivo(s.key)} onMouseLeave={() => setAtivo(null)}
            onFocus={() => setAtivo(s.key)} onBlur={() => setAtivo(null)}
            style={{
              fill: s.cor, cursor: "pointer", outline: "none",
              opacity: aceso(s.key) ? 1 : 0.35,
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

      <div style={{ flex: "1 1 150px", minWidth: 0, display: "flex", flexDirection: "column", gap: "4px" }}>
        <LinhaLegenda cor="var(--color-primary)" label="Valor bruto"
          nota={fmtBRL(vendido)} percentual={100}
          destacada={ativo === "capital"}
          onEntrar={() => setAtivo("capital")} onSair={() => setAtivo(null)} />

        <LinhaLegenda cor={positivo ? "var(--color-success)" : "var(--color-danger)"}
          label={positivo ? "Resultado" : "Prejuízo"}
          nota={fmtBRL(lucro)} percentual={positivo ? (resultado / base) * 100 : null}
          destacada={ativo === "resultado"}
          onEntrar={() => setAtivo("resultado")} onSair={() => setAtivo(null)} />

        {rentabilidade != null && (
          <p style={{ fontSize: "11.5px", color: "var(--text-3)", margin: "4px 0 0" }}>
            Rentabilidade de{" "}
            <strong style={{ color: positivo ? "var(--color-success)" : "var(--color-danger)" }}>
              {fmtPct(rentabilidade)}
            </strong>{" "}
            sobre o capital investido.
          </p>
        )}
      </div>
    </div>
  );
}

interface LinhaLegendaProps {
  cor: string;
  label: string;
  nota: string;
  percentual: number | null;
  destacada: boolean;
  onEntrar: () => void;
  onSair: () => void;
}

function LinhaLegenda({ cor, label, nota, percentual, destacada, onEntrar, onSair }: LinhaLegendaProps) {
  return (
    <div onMouseEnter={onEntrar} onMouseLeave={onSair}
      style={{
        display: "flex", alignItems: "center", justifyContent: "space-between", gap: "10px",
        padding: "5px 8px", borderRadius: "9px",
        background: destacada ? "var(--surface-2)" : "transparent",
        border: `1px solid ${destacada ? "var(--border)" : "transparent"}`,
        transition: "background .15s ease, border-color .15s ease",
      }}>
      <span style={{ display: "inline-flex", alignItems: "center", gap: "8px", fontSize: "12.5px", color: "var(--text-2)", minWidth: 0 }}>
        <span style={{ width: "10px", height: "10px", borderRadius: "3px", background: cor, flexShrink: 0 }} />
        {label}
      </span>
      <span style={{ fontSize: "12.5px", fontWeight: 700, color: "var(--text)", fontVariantNumeric: "tabular-nums", whiteSpace: "nowrap" }}>
        {nota}
        {percentual != null && (
          <span style={{ fontWeight: 600, color: "var(--text-3)" }}>
            {" · "}{fmtPercentualCurto(percentual)}
          </span>
        )}
      </span>
    </div>
  );
}
