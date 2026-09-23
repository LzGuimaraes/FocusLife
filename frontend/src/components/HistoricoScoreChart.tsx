import type { CSSProperties } from "react";

/* ══════════════════════════════════════════════════════════════════════
   Gráfico de evolução (linha + área) em SVG puro — sem dependência nova.

   Os rótulos ficam FORA do SVG (em HTML) de propósito: o SVG usa
   preserveAspectRatio="none" para ocupar a largura disponível, e isso
   distorceria textos desenhados dentro dele.
   ══════════════════════════════════════════════════════════════════════ */

export interface PontoGrafico {
  label: string;
  valor: number | null;
  /** Texto auxiliar mostrado ao passar o dedo/mouse (tooltip nativo). */
  detalhe?: string;
}

interface Props {
  pontos: PontoGrafico[];
  cor?: string;
  altura?: number;
  minimo?: number;
  maximo?: number;
  /** Formata o valor mostrado nos rótulos de topo. */
  formatar?: (valor: number) => string;
  vazioTexto?: string;
}

const LARGURA = 600;

export default function HistoricoScoreChart({
  pontos,
  cor = "#6366f1",
  altura = 140,
  minimo = 0,
  maximo = 100,
  formatar = v => `${v.toFixed(1)}%`,
  vazioTexto = "Registre a avaliação em dias diferentes para ver a evolução.",
}: Props) {
  const validos = pontos.filter(p => p.valor != null) as (PontoGrafico & { valor: number })[];

  if (validos.length < 2) {
    return (
      <div style={{ ...vazio, height: altura }}>
        <span style={{ fontSize: "12px", color: "#94a3b8", textAlign: "center", padding: "0 12px" }}>
          {pontos.length === 0 ? "Sem registros no histórico ainda." : vazioTexto}
        </span>
      </div>
    );
  }

  const passo = LARGURA / (pontos.length - 1);
  const fator = maximo - minimo === 0 ? 1 : maximo - minimo;
  const paraY = (valor: number) => altura - ((valor - minimo) / fator) * altura;

  const segmentos: string[] = [];
  const pontosLinha: string[] = [];
  pontos.forEach((p, i) => {
    if (p.valor == null) return;
    const x = i * passo;
    const y = paraY(p.valor);
    pontosLinha.push(`${x},${y}`);
    segmentos.push(`${p.label}: ${formatar(p.valor)}`);
  });
  const titulos = segmentos.join(" · ");

  const linha = `M ${pontosLinha.join(" L ")}`;
  const area = `${linha} L ${(pontos.length - 1) * passo} ${altura} L 0 ${altura} Z`;

  const primeiro = validos[0];
  const ultimo = validos[validos.length - 1];
  const variacao = ultimo.valor - primeiro.valor;

  return (
    <div>
      <div style={{ display: "flex", justifyContent: "space-between", alignItems: "baseline", gap: "8px", flexWrap: "wrap", marginBottom: "6px" }}>
        <span style={{ fontSize: "11px", color: "#64748b" }}>
          {pontos[0]?.label} → {pontos[pontos.length - 1]?.label}
        </span>
        <span style={{
          fontSize: "12px", fontWeight: 700,
          color: variacao > 0.005 ? "#047857" : variacao < -0.005 ? "#b91c1c" : "#64748b",
        }}>
          {variacao > 0 ? "▲" : variacao < 0 ? "▼" : "•"} {variacao > 0 ? "+" : ""}{formatar(variacao)}
        </span>
      </div>

      <svg width="100%" height={altura} viewBox={`0 0 ${LARGURA} ${altura}`} preserveAspectRatio="none"
        role="img" aria-label={`Evolução de ${formatar(primeiro.valor)} a ${formatar(ultimo.valor)}`}>
        <title>{titulos}</title>
        <defs>
          <linearGradient id={`grad-${cor.replace("#", "")}`} x1="0" y1="0" x2="0" y2="1">
            <stop offset="0%" stopColor={cor} stopOpacity="0.28" />
            <stop offset="100%" stopColor={cor} stopOpacity="0.02" />
          </linearGradient>
        </defs>

        {[0.25, 0.5, 0.75].map(f => (
          <line key={f} x1="0" y1={altura * f} x2={LARGURA} y2={altura * f}
            stroke="#e2e8f0" strokeWidth="1" vectorEffect="non-scaling-stroke" strokeDasharray="4 4" />
        ))}

        <path d={area} fill={`url(#grad-${cor.replace("#", "")})`} />
        <path d={linha} fill="none" stroke={cor} strokeWidth="2"
          strokeLinejoin="round" strokeLinecap="round" vectorEffect="non-scaling-stroke" />
      </svg>

      <div style={{ display: "flex", justifyContent: "space-between", marginTop: "4px", fontSize: "11px", color: "#94a3b8" }}>
        <span>{formatar(minimo)}</span>
        <span>{pontos.length} registro(s)</span>
        <span>{formatar(maximo)}</span>
      </div>
    </div>
  );
}

const vazio: CSSProperties = {
  display: "flex", alignItems: "center", justifyContent: "center",
  background: "#f8fafc", border: "1px dashed #e2e8f0", borderRadius: "10px",
};
