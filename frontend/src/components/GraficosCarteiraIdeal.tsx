import type { Comparativo } from "../types/planejamento";
import { catInfo, fmtPercentual } from "../utils/percentual";

/* ══════════════════════════════════════════════════════════════════════
   Gráficos da Carteira Ideal (Módulo 10).

   Dois olhares sobre os mesmos dados:
     1. DistribuicaoAtualIdeal — duas barras empilhadas (atual × ideal) com a
        composição por classe: responde "onde eu estou × onde quero estar".
     2. BarrasAtualIdeal — barras lado a lado por classe: responde "quanto
        falta/sobra em cada classe".

   Tudo em SVG/CSS puro (sem dependência nova), com as cores de cada classe
   vindo de `catInfo` para casar com o resto do sistema.
   ══════════════════════════════════════════════════════════════════════ */

export function DistribuicaoAtualIdeal({ comparativo, titulo = "Distribuição por classe" }: {
  comparativo: Comparativo;
  titulo?: string;
}) {
  const classes = comparativo.classes;
  const somaAtual = classes.reduce((s, c) => s + c.percentual_atual, 0);
  const somaIdeal = classes.reduce((s, c) => s + c.percentual_ideal, 0);

  if (classes.length === 0) return null;

  return (
    <div style={card}>
      <h3 style={{ fontSize: "15px", fontWeight: 700, color: "#0f172a", margin: "0 0 14px" }}>{titulo}</h3>

      <BarraEmpilhada
        rotulo="Carteira atual"
        partes={classes.map(c => ({ label: catInfo(c.classe).label, valor: c.percentual_atual, cor: catInfo(c.classe).color }))}
        soma={somaAtual}
      />
      <BarraEmpilhada
        rotulo="Carteira ideal"
        partes={classes.map(c => ({ label: catInfo(c.classe).label, valor: c.percentual_ideal, cor: catInfo(c.classe).color }))}
        soma={somaIdeal}
      />

      <div style={{ display: "flex", flexWrap: "wrap", gap: "10px", marginTop: "14px" }}>
        {classes.map(c => {
          const info = catInfo(c.classe);
          return (
            <span key={c.classe} style={{ display: "inline-flex", alignItems: "center", gap: "6px", fontSize: "12px", color: "#475569" }}>
              <span style={{ width: "10px", height: "10px", borderRadius: "3px", background: info.color, flexShrink: 0 }} />
              {info.icon} {info.label}
            </span>
          );
        })}
      </div>
    </div>
  );
}

function BarraEmpilhada({ rotulo, partes, soma }: {
  rotulo: string;
  partes: { label: string; valor: number; cor: string }[];
  soma: number;
}) {
  const visiveis = partes.filter(p => p.valor > 0.005);
  return (
    <div style={{ marginBottom: "12px" }}>
      <div style={{ display: "flex", justifyContent: "space-between", marginBottom: "4px" }}>
        <span style={{ fontSize: "11px", fontWeight: 700, color: "#64748b", textTransform: "uppercase", letterSpacing: "0.3px" }}>
          {rotulo}
        </span>
        <span style={{ fontSize: "11px", fontWeight: 700, color: "#475569" }}>{fmtPercentual(soma)}</span>
      </div>
      <div style={{ display: "flex", height: "22px", borderRadius: "8px", overflow: "hidden", background: "#f1f5f9" }}>
        {visiveis.map(p => (
          <div key={p.label} title={`${p.label}: ${fmtPercentual(p.valor)}`}
            style={{ width: `${p.valor}%`, background: p.cor, transition: "width 0.4s ease" }} />
        ))}
      </div>
    </div>
  );
}

export function BarrasAtualIdeal({ comparativo }: { comparativo: Comparativo }) {
  const classes = comparativo.classes;
  if (classes.length === 0) return null;

  // Escala: o maior valor entre atual e ideal define a largura das barras.
  const maior = Math.max(
    10,
    ...classes.flatMap(c => [c.percentual_atual, c.percentual_ideal]),
  );
  const escala = Math.ceil(maior / 10) * 10;

  return (
    <div style={card}>
      <div style={{ display: "flex", justifyContent: "space-between", alignItems: "baseline", flexWrap: "wrap", gap: "8px", marginBottom: "14px" }}>
        <h3 style={{ fontSize: "15px", fontWeight: 700, color: "#0f172a", margin: 0 }}>Atual × ideal por classe</h3>
        <div style={{ display: "flex", gap: "12px", fontSize: "11px", color: "#64748b" }}>
          <span style={{ display: "inline-flex", alignItems: "center", gap: "5px" }}>
            <span style={{ width: "10px", height: "10px", borderRadius: "2px", background: "#94a3b8" }} /> atual
          </span>
          <span style={{ display: "inline-flex", alignItems: "center", gap: "5px" }}>
            <span style={{ width: "10px", height: "10px", borderRadius: "2px", background: "#6366f1" }} /> ideal
          </span>
        </div>
      </div>

      <div style={{ display: "flex", flexDirection: "column", gap: "14px" }}>
        {classes.map(c => {
          const info = catInfo(c.classe);
          const diferenca = c.percentual_atual - c.percentual_ideal;
          const desvio = Math.abs(diferenca) < 0.005;
          return (
            <div key={c.classe}>
              <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", gap: "8px", marginBottom: "5px" }}>
                <span style={{ fontSize: "13px", fontWeight: 700, color: "#334155" }}>
                  {info.icon} {info.label}
                </span>
                <span style={{
                  fontSize: "11px", fontWeight: 700, padding: "2px 8px", borderRadius: "9999px",
                  background: desvio ? "#f1f5f9" : diferenca > 0 ? "#fef3c7" : "#dbeafe",
                  color: desvio ? "#64748b" : diferenca > 0 ? "#b45309" : "#1d4ed8",
                }}>
                  {desvio ? "na meta" : `${diferenca > 0 ? "+" : ""}${diferenca.toFixed(2)} p.p.`}
                </span>
              </div>

              <div style={{ display: "flex", flexDirection: "column", gap: "4px" }}>
                <LinhaBarra valor={c.percentual_atual} escala={escala} cor="#94a3b8" />
                <LinhaBarra valor={c.percentual_ideal} escala={escala} cor="#6366f1" />
              </div>
            </div>
          );
        })}
      </div>

      <div style={{ display: "flex", justifyContent: "space-between", fontSize: "10px", color: "#94a3b8", marginTop: "10px" }}>
        <span>0%</span>
        <span>{(escala / 2).toFixed(0)}%</span>
        <span>{escala}%</span>
      </div>
    </div>
  );
}

function LinhaBarra({ valor, escala, cor }: { valor: number; escala: number; cor: string }) {
  const largura = escala > 0 ? Math.min(100, (valor / escala) * 100) : 0;
  return (
    <div style={{ display: "flex", alignItems: "center", gap: "8px" }}>
      <div style={{ flex: 1, height: "10px", background: "#f1f5f9", borderRadius: "5px", overflow: "hidden" }}>
        <div style={{ width: `${largura}%`, height: "100%", background: cor, borderRadius: "5px", transition: "width 0.4s ease" }} />
      </div>
      <span style={{ fontSize: "11px", fontWeight: 700, color: "#475569", minWidth: "48px", textAlign: "right" }}>
        {fmtPercentual(valor)}
      </span>
    </div>
  );
}

const card: React.CSSProperties = {
  background: "white", borderRadius: "16px", padding: "22px 24px",
  boxShadow: "0 1px 3px rgba(15,23,42,0.05)", border: "1px solid #eef2f7",
};

/* ══════════════════════════════════════════════════════════════════════
   DONUTS "Atual × Ideal" (comparação visual imediata).

   Dois círculos lado a lado, com a MESMA ordem e as MESMAS cores de classe:
   a leitura é "a fatia que cresceu/diminuiu", sem precisar ler percentuais.
   Legenda única embaixo (uma vez só), porque repetir a legenda em cada
   gráfico só aumentaria o ruído da tela.

   Cores e rótulos continuam vindo de `catInfo` — a distribuição é a mesma
   do comparativo, nada é recalculado aqui.
   ══════════════════════════════════════════════════════════════════════ */
export function DonutsAtualIdeal({ comparativo }: { comparativo: Comparativo }) {
  const classes = comparativo.classes;
  if (classes.length === 0) return null;

  const fatias = classes.map(c => ({
    key: c.classe,
    label: catInfo(c.classe).label,
    icone: catInfo(c.classe).icon,
    cor: catInfo(c.classe).color,
    atual: c.percentual_atual,
    ideal: c.percentual_ideal,
  }));
  const somaAtual = fatias.reduce((s, f) => s + f.atual, 0);
  const somaIdeal = fatias.reduce((s, f) => s + f.ideal, 0);

  return (
    <div style={card}>
      <h3 style={{ fontSize: "16px", fontWeight: 800, color: "#0f172a", margin: 0, letterSpacing: "-0.01em" }}>
        Distribuição atual × ideal
      </h3>
      <p style={{ fontSize: "12.5px", color: "#64748b", margin: "5px 0 0" }}>
        A mesma carteira, dois retratos: onde você está hoje e onde a sua metodologia diz que deve estar.
      </p>

      <div style={{ display: "flex", justifyContent: "center", alignItems: "center", gap: "clamp(12px, 5vw, 44px)", flexWrap: "wrap", marginTop: "18px" }}>
        <Donut titulo="Atual" soma={somaAtual}
          fatias={fatias.map(f => ({ label: f.label, valor: f.atual, cor: f.cor }))} />
        <Donut titulo="Ideal" soma={somaIdeal}
          fatias={fatias.map(f => ({ label: f.label, valor: f.ideal, cor: f.cor }))} destaque />
      </div>

      <div style={{ display: "flex", flexWrap: "wrap", gap: "8px 16px", marginTop: "20px", paddingTop: "16px", borderTop: "1px solid #f1f5f9" }}>
        {fatias.map(f => (
          <span key={f.key} style={{ display: "inline-flex", alignItems: "center", gap: "7px", fontSize: "12.5px", color: "#475569" }}>
            <span style={{ width: "10px", height: "10px", borderRadius: "3px", background: f.cor, flexShrink: 0 }} />
            {f.icone} {f.label}
          </span>
        ))}
      </div>
    </div>
  );
}

/** Um donut em SVG puro: sem dependência nova e sem imagem. */
function Donut({ titulo, soma, fatias, destaque = false }: {
  titulo: string;
  soma: number;
  fatias: { label: string; valor: number; cor: string }[];
  destaque?: boolean;
}) {
  const raio = 54;
  const espessura = 20;
  const circunferencia = 2 * Math.PI * raio;
  const total = fatias.reduce((s, f) => s + Math.max(0, f.valor), 0);
  const base = total > 0 ? total : 1;

  let acumulado = 0;
  const arcos = fatias
    .filter(f => f.valor > 0.005)
    .map(f => {
      const fracao = f.valor / base;
      const inicio = acumulado;
      acumulado += fracao;
      return { ...f, fracao, inicio };
    });

  return (
    <div style={{ textAlign: "center" }}>
      <svg width="164" height="164" viewBox="0 0 164 164" role="img"
        aria-label={`Distribuição ${titulo}: ${fmtPercentual(soma)}`}>
        <g transform="rotate(-90 82 82)">
          <circle cx="82" cy="82" r={raio} fill="none" stroke="#f1f5f9" strokeWidth={espessura} />
          {arcos.map(a => (
            <circle key={a.label} cx="82" cy="82" r={raio} fill="none"
              stroke={a.cor} strokeWidth={espessura}
              // O vão de 1,5% separa fatias de cores parecidas (a paleta do app
              // usa tons próximos de azul/roxo) sem depender de contorno.
              strokeDasharray={`${Math.max(0, a.fracao * circunferencia - 6)} ${circunferencia}`}
              strokeDashoffset={-a.inicio * circunferencia}>
              <title>{`${a.label}: ${fmtPercentual(a.valor)}`}</title>
            </circle>
          ))}
        </g>
        <text x="82" y="77" textAnchor="middle" fontSize="20" fontWeight="800" fill="#0f172a">
          {fmtPercentual(soma)}
        </text>
        <text x="82" y="96" textAnchor="middle" fontSize="11" fontWeight="700" fill="#94a3b8"
          letterSpacing="0.6">
          {titulo.toUpperCase()}
        </text>
      </svg>
      <p style={{
        margin: "6px 0 0", fontSize: "12px", fontWeight: 700,
        color: destaque ? "#4338ca" : "#64748b",
      }}>
        {titulo === "Ideal" ? "Sua metodologia" : "Sua carteira hoje"}
      </p>
    </div>
  );
}
