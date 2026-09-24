import { useEffect, useState } from "react";
import type { Comparativo } from "../types/planejamento";
import { catInfo, fmtMoeda, fmtPercentual, fmtPontosPercentuais } from "../utils/percentual";

/* ══════════════════════════════════════════════════════════════════════
   Gráficos da Carteira Ideal (Módulo 10).

   Três olhares sobre os mesmos dados:
     1. DistribuicaoAtualIdeal — duas barras empilhadas (atual × ideal) com a
        composição por classe: responde "onde eu estou × onde quero estar".
     2. BarrasAtualIdeal — barras lado a lado por classe: responde "quanto
        falta/sobra em cada classe".
     3. DonutsAtualIdeal — os dois retratos em DONUT INTERATIVO: a mesma
        informação, mas a fatia responde ao mouse (rótulo da %, tooltip com o
        valor em R$ e o centro contando o que está sob o cursor).

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
   DONUTS "Atual × Ideal" (comparação visual imediata, interativa).

   Dois anéis lado a lado, com a MESMA ordem de fatias e as MESMAS cores de
   classe — a leitura é "a fatia que cresceu/diminuiu", sem precisar ler
   percentuais. Passar o mouse (ou o foco do teclado) numa classe acende a
   fatia NOS DOIS anéis ao mesmo tempo: é o que transforma dois gráficos
   separados em uma comparação.

   O anel não depende de biblioteca de gráficos: as fatias são caminhos SVG
   (arcos de anel) calculados aqui, o que dá controle fino do vão entre elas,
   do destaque da fatia ativa e do balão de valor. Cores e rótulos continuam
   vindo de `catInfo` — nada é recalculado, o comparativo é a fonte única.
   ══════════════════════════════════════════════════════════════════════ */

interface FatiaDonut {
  key: string;
  label: string;
  icone: string;
  cor: string;
  /** % da carteira — é o peso da fatia no anel. */
  percentual: number;
  /** Valor em moeda (balão e centro). */
  valor: number;
}

export function DonutsAtualIdeal({ comparativo }: { comparativo: Comparativo }) {
  // Classe destacada + de ONDE veio o destaque: o balão de valor só aparece no
  // anel que está sob o cursor (o outro só acende a fatia e conta no centro).
  const [hover, setHover] = useState<{ key: string; fonte: string } | null>(null);
  const [pronto, setPronto] = useState(false);

  // Entrada suave: o anel cresce uma vez, quando os dados chegam. Sem isso o
  // gráfico "aparece seco" no meio da tela.
  useEffect(() => {
    setPronto(false);
    const t = setTimeout(() => setPronto(true), 30);
    return () => clearTimeout(t);
  }, [comparativo.carteira_id]);

  const classes = comparativo.classes;
  if (classes.length === 0) return null;

  const ativo = hover?.key ?? null;
  const moeda = comparativo.moeda;
  const somaIdeal = classes.reduce((s, c) => s + c.percentual_ideal, 0);

  const fatiaDe = (c: (typeof classes)[number]) => {
    const info = catInfo(c.classe);
    return { key: c.classe as string, label: info.label, icone: info.icon, cor: info.color };
  };

  const atuais: FatiaDonut[] = classes.map(c => ({ ...fatiaDe(c), percentual: c.percentual_atual, valor: c.valor_atual }));
  const ideais: FatiaDonut[] = classes.map(c => ({ ...fatiaDe(c), percentual: c.percentual_ideal, valor: c.valor_ideal }));

  // A Carteira Ideal pode não fechar 100%: o que falta vira uma fatia neutra,
  // em vez de esticar as outras e mentir sobre a distribuição desejada.
  if (somaIdeal < 99.995) {
    const falta = 100 - somaIdeal;
    ideais.push({
      key: "__a-definir", label: "A definir", icone: "✏️", cor: "#cbd5e1",
      percentual: falta, valor: (falta / 100) * comparativo.valor_total,
    });
  }

  return (
    <div style={card}>
      <div style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-start", gap: "12px", flexWrap: "wrap" }}>
        <div>
          <h3 style={{ fontSize: "16px", fontWeight: 800, color: "#0f172a", margin: 0, letterSpacing: "-0.01em" }}>
            Distribuição atual × ideal
          </h3>
          <p style={{ fontSize: "12.5px", color: "#64748b", margin: "5px 0 0" }}>
            A mesma carteira, dois retratos: onde você está hoje e onde a sua metodologia diz que deve estar.
          </p>
        </div>
        <span style={{ ...pill, color: "#4338ca", background: "#eef2ff" }}>
          {classes.length} classe{classes.length > 1 ? "s" : ""} · total {fmtMoeda(comparativo.valor_total, moeda)}
        </span>
      </div>

      <div style={{ display: "flex", justifyContent: "center", alignItems: "flex-start", gap: "clamp(4px, 3vw, 28px)", flexWrap: "wrap", marginTop: "14px" }}>
        <Donut id="atual" titulo="Carteira atual" subtitulo="onde você está hoje"
          fatias={atuais} moeda={moeda} total={comparativo.valor_total}
          ativo={ativo} fonte={hover?.fonte ?? null} onAtivo={setHover} pronto={pronto} corAnel="#94a3b8" />
        <Donut id="ideal" titulo="Carteira ideal" subtitulo="sua metodologia"
          fatias={ideais} moeda={moeda} total={comparativo.valor_total}
          ativo={ativo} fonte={hover?.fonte ?? null} onAtivo={setHover} pronto={pronto} corAnel="#6366f1" />
      </div>

      {/* Legenda comparativa: cada classe com o quanto falta (ou sobra) em p.p.
          É onde a comparação vira decisão, então ela fica junto dos anéis. */}
      <div style={{ marginTop: "18px", paddingTop: "14px", borderTop: "1px solid #f1f5f9" }}>
        <div style={{ display: "grid", gridTemplateColumns: COLUNAS, gap: "10px", alignItems: "center", ...legendaCabecalho }}>
          <span>Classe</span>
          <span style={{ textAlign: "right" }}>Atual</span>
          <span style={{ textAlign: "right" }}>Ideal</span>
          <span style={{ textAlign: "right" }}>Ajuste</span>
        </div>
        {classes.map(c => {
          const info = catInfo(c.classe);
          const falta = c.percentual_ideal - c.percentual_atual;   // + = precisa aumentar
          const naMeta = Math.abs(falta) < 0.005;
          const destacada = ativo === c.classe;
          return (
            <div key={c.classe}
              onMouseEnter={() => setHover({ key: c.classe, fonte: "legenda" })} onMouseLeave={() => setHover(null)}
              style={{
                display: "grid", gridTemplateColumns: COLUNAS,
                gap: "10px", alignItems: "center", padding: "7px 10px", borderRadius: "10px",
                background: destacada ? "#f8fafc" : "transparent",
                border: `1px solid ${destacada ? "#e2e8f0" : "transparent"}`,
                transition: "background .15s ease, border-color .15s ease",
              }}>
              <span style={{ display: "inline-flex", alignItems: "center", gap: "8px", fontSize: "12.5px", fontWeight: 600, color: "#334155", minWidth: 0 }}>
                <span style={{ width: "10px", height: "10px", borderRadius: "3px", background: info.color, flexShrink: 0 }} />
                <span style={{ overflow: "hidden", textOverflow: "ellipsis", whiteSpace: "nowrap" }}>
                  {info.icon} {info.label}
                </span>
              </span>
              <span style={{ textAlign: "right", fontSize: "12.5px", color: "#64748b", fontVariantNumeric: "tabular-nums" }}>
                {fmtPercentual(c.percentual_atual)}
              </span>
              <span style={{ textAlign: "right", fontSize: "12.5px", fontWeight: 700, color: "#4338ca", fontVariantNumeric: "tabular-nums" }}>
                {fmtPercentual(c.percentual_ideal)}
              </span>
              <span style={{ textAlign: "right" }}>
                {naMeta ? (
                  <span style={{ ...pill, color: "#64748b", background: "#f1f5f9" }}>na meta</span>
                ) : (
                  <span style={{
                    ...pill,
                    color: falta > 0 ? "#1d4ed8" : "#b45309",
                    background: falta > 0 ? "#dbeafe" : "#fef3c7",
                  }}>
                    {falta > 0 ? "falta " : "sobra "}{fmtPontosPercentuais(Math.abs(falta))}
                  </span>
                )}
              </span>
            </div>
          );
        })}
        <p style={{ fontSize: "11.5px", color: "#94a3b8", margin: "10px 0 0" }}>
          Passe o mouse (ou use Tab) numa classe para ver o valor em {moeda} nas duas carteiras.
          {somaIdeal < 99.995 && <> As metas somam {fmtPercentual(somaIdeal)} — os {fmtRestante(somaIdeal)} restantes aparecem como “A definir”.</>}
        </p>
      </div>
    </div>
  );
}

/** Quanto falta para as metas fecharem 100%, em % curto: "12,5%". */
const fmtRestante = (soma: number): string =>
  `${(100 - soma).toLocaleString("pt-BR", { maximumFractionDigits: 1 })}%`;

/** Balão/fonte do valor: sem os centavos quando o valor é redondo ("R$ 19.400"). */
const fmtCurto = (valor: number, moeda: string): string => fmtMoeda(valor, moeda).replace(/,00$/, "");

/**
 * Valor do CENTRO do anel: arredonda para a unidade. O furo tem ~128 de
 * diâmetro e "R$ 21.006,68" não cabe nele sem espremer o anel — os centavos
 * continuam no balão, onde há espaço.
 */
const fmtCentro = (valor: number, moeda: string): string => fmtMoeda(Math.round(valor), moeda).replace(/,00$/, "");

/**
 * Tamanho de fonte que faz o texto caber na largura disponível.
 *
 * `fator` é o consumo por caractere em "em" — medido nas fontes do app: 0,58
 * para o valor (dígitos tabulares, fonte 800) e 0,70 para CAIXA ALTA com
 * letter-spacing (o título do centro). É o que impede o número de invadir o
 * anel quando a carteira cresce de dígitos.
 */
const fonteQueCabe = (texto: string, largura: number, max: number, min: number, fator = 0.58): number =>
  Math.max(min, Math.min(max, Math.floor(largura / Math.max(1, texto.length * fator))));

const clamp = (valor: number, min: number, max: number): number =>
  Math.min(max, Math.max(min, valor));

/**
 * Um anel interativo em SVG puro.
 *
 * Geometria: fatias desenhadas como ARCOS DE ANEL (não `stroke-dasharray`),
 * o que permite vão proporcional entre elas, espessura própria na fatia ativa
 * e rótulo posicionado no meio do arco. O balão é um retângulo escuro dentro
 * do próprio SVG — assim ele escala junto com o gráfico, sem medida em pixel
 * para sincronizar.
 */
function Donut({ id, titulo, subtitulo, fatias, moeda, total, ativo, fonte, onAtivo, pronto, corAnel }: {
  id: string;
  titulo: string;
  subtitulo: string;
  fatias: FatiaDonut[];
  moeda: string;
  total: number;
  ativo: string | null;
  /** Donut de onde veio o destaque (o balão só aparece nele). */
  fonte: string | null;
  onAtivo: (hover: { key: string; fonte: string } | null) => void;
  pronto: boolean;
  corAnel: string;
}) {
  const T = 300;                 // lado do viewBox
  const cx = T / 2;
  const cy = T / 2;
  const rExt = 88;
  // Furo com 64 de raio (128 de diâmetro): é o espaço REAL do texto do centro.
  // Com o furo antigo (58) o valor em reais não cabia e encostava no anel.
  const rInt = 64;
  const rRotulo = 114;           // onde ficam as % de fora
  const VAO = 0.03;              // vão entre fatias (radianos)
  const LARGURA_INTERNA = 2 * rInt - 16;   // folga de 8 de cada lado no furo

  const base = fatias.reduce((s, f) => s + Math.max(0, f.percentual), 0) || 1;
  const visiveis = fatias.filter(f => f.percentual > 0.004);

  let cursor = -Math.PI / 2;     // começa no topo, no sentido horário
  const segmentos = visiveis.map(f => {
    const abertura = (f.percentual / base) * Math.PI * 2;
    const inicio = cursor;
    cursor += abertura;
    // Fatia estreita demais perderia o miolo com o vão: nela o vão é ignorado.
    const comVao = visiveis.length > 1 && abertura > VAO * 2.2;
    return {
      ...f,
      a0: inicio + (comVao ? VAO / 2 : 0),
      a1: inicio + abertura - (comVao ? VAO / 2 : 0),
      meio: inicio + abertura / 2,
    };
  });

  const apontado = segmentos.find(s => s.key === ativo) ?? null;
  const usa = (key: string) => ativo == null || ativo === key;

  const ponto = (r: number, a: number): [number, number] => [cx + r * Math.cos(a), cy + r * Math.sin(a)];

  /** Arco de anel: vai pela borda externa e volta pela interna. */
  const anel = (rIn: number, rEx: number, a0: number, a1: number): string => {
    const [x0, y0] = ponto(rEx, a0);
    const [x1, y1] = ponto(rEx, a1);
    const [x2, y2] = ponto(rIn, a1);
    const [x3, y3] = ponto(rIn, a0);
    const grande = a1 - a0 > Math.PI ? 1 : 0;
    return `M ${x0} ${y0} A ${rEx} ${rEx} 0 ${grande} 1 ${x1} ${y1} `
      + `L ${x2} ${y2} A ${rIn} ${rIn} 0 ${grande} 0 ${x3} ${y3} Z`;
  };

  /* ── Balão do valor: segue a fatia e não sai do viewBox ── */
  const mostrarBalao = apontado != null && fonte === id;
  const largura = mostrarBalao ? clamp(apontado.label.length * 6.9 + 92, 176, 250) : 0;
  const altura = 52;
  const acima = apontado ? Math.sin(apontado.meio) <= 0 : true;
  const [ax, ay] = apontado ? ponto(rExt + 8, apontado.meio) : [cx, cy];
  const balaoX = clamp(ax - largura / 2, 8, T - largura - 8);
  const balaoY = acima ? clamp(ay - altura - 14, 6, T - altura - 6) : clamp(ay + 14, 6, T - altura - 6);

  const centroValor = fmtCentro(apontado ? apontado.valor : total, moeda);
  const centroTitulo = apontado ? apontado.label : titulo;
  const centroPct = apontado ? fmtPercentual(apontado.percentual) : "100,00%";
  const tituloCentro = centroTitulo.length > 16 ? `${centroTitulo.slice(0, 15)}…` : centroTitulo;
  // Fonte do centro dimensionada pelo texto: o número encolhe até caber no furo
  // (e para de invadir o anel quando o valor tem 6 ou 7 dígitos).
  const fonteValor = fonteQueCabe(centroValor, LARGURA_INTERNA, 21, 12);
  const fonteTitulo = fonteQueCabe(tituloCentro.toUpperCase(), 2 * rInt - 8, 11.5, 8.5, 0.7);

  return (
    <div style={{ textAlign: "center", width: "min(300px, 84vw)" }}>
      <svg viewBox={`0 0 ${T} ${T}`} role="img" style={{
        width: "100%", height: "auto", display: "block", overflow: "visible",
        opacity: pronto ? 1 : 0,
        transform: pronto ? "scale(1)" : "scale(0.9)",
        transition: "opacity .45s ease, transform .45s cubic-bezier(.22,1,.36,1)",
      }} aria-label={`${titulo}: ${base > 0 ? fmtPercentual(base) : "sem dados"} distribuídos em ${segmentos.length} classe(s)`}>
        {/* Anel de fundo: mantém a leitura do "círculo completo" quando há poucas fatias. */}
        <circle cx={cx} cy={cy} r={(rExt + rInt) / 2} fill="none" stroke="#f1f5f9" strokeWidth={rExt - rInt} />

        {segmentos.map(s => {
          const ativa = apontado?.key === s.key;
          return (
            <path key={s.key} d={anel(ativa ? rInt - 4 : rInt, ativa ? rExt + 5 : rExt, s.a0, s.a1)}
              fill={s.cor} tabIndex={0} role="button"
              aria-label={`${s.label}: ${fmtPercentual(s.percentual)} (${fmtCurto(s.valor, moeda)})`}
              onMouseEnter={() => onAtivo({ key: s.key, fonte: id })} onMouseLeave={() => onAtivo(null)}
              onFocus={() => onAtivo({ key: s.key, fonte: id })} onBlur={() => onAtivo(null)}
              style={{
                cursor: "pointer", outline: "none",
                opacity: usa(s.key) ? 1 : 0.42,
                filter: ativa ? "drop-shadow(0 6px 14px rgba(15,23,42,0.22))" : "none",
                transition: "opacity .18s ease, filter .18s ease",
              }} />
          );
        })}

        {/* Rótulo externo: só a fatia relevante ganha número, para não virar sopa. */}
        {segmentos.filter(s => s.percentual >= 3).map(s => {
          const cosseno = Math.cos(s.meio);
          const [x, y] = ponto(rRotulo, s.meio);
          return (
            <text key={s.key} x={x} y={y + 4.5}
              textAnchor={cosseno > 0.35 ? "start" : cosseno < -0.35 ? "end" : "middle"}
              fontSize="12.5" fontWeight="700" letterSpacing="0.1"
              fill={apontado?.key === s.key ? "#0f172a" : "#64748b"}
              style={{ fontVariantNumeric: "tabular-nums", transition: "fill .18s ease" }}>
              {s.percentual.toLocaleString("pt-BR", { maximumFractionDigits: 1 })}%
            </text>
          );
        })}

        {/* Centro: sem hover mostra o total; com hover, a classe sob o cursor. */}
        <text x={cx} y={cy - 10} textAnchor="middle" fontSize={fonteTitulo} fontWeight="700" fill="#94a3b8"
          letterSpacing="0.4">
          {tituloCentro.toUpperCase()}
        </text>
        <text x={cx} y={cy + 13} textAnchor="middle" fontWeight="800" fill="#0f172a"
          fontSize={fonteValor} style={{ fontVariantNumeric: "tabular-nums" }}>
          {centroValor}
        </text>
        <text x={cx} y={cy + 32} textAnchor="middle" fontSize="11.5" fontWeight="700"
          fill={apontado ? "#475569" : "#94a3b8"}>
          {centroPct}
        </text>

        {/* Balão escuro com o valor da classe apontada. */}
        {mostrarBalao && (
          <g style={{ filter: "drop-shadow(0 10px 22px rgba(15,23,42,0.30))", pointerEvents: "none" }}>
            <rect x={balaoX} y={balaoY} width={largura} height={altura} rx="12" fill="#0f172a" />
            <circle cx={balaoX + 15} cy={balaoY + 18} r="3.6" fill={apontado.cor} />
            <text x={balaoX + 25} y={balaoY + 22} fontSize="12" fontWeight="600" fill="#cbd5e1">
              {apontado.icone} {apontado.label.length > 18 ? `${apontado.label.slice(0, 17)}…` : apontado.label}
            </text>
            <text x={balaoX + 15} y={balaoY + 41} fontSize="14.5" fontWeight="800" fill="#ffffff"
              style={{ fontVariantNumeric: "tabular-nums" }}>
              {fmtCurto(apontado.valor, moeda)}
            </text>
            <text x={balaoX + largura - 15} y={balaoY + 41} textAnchor="end" fontSize="12" fontWeight="700" fill="#94a3b8">
              {fmtPercentual(apontado.percentual)}
            </text>
          </g>
        )}
      </svg>

      <p style={{ margin: "2px 0 0", fontSize: "12.5px", fontWeight: 700, color: corAnel }}>
        {titulo}
      </p>
      <p style={{ margin: "1px 0 0", fontSize: "11.5px", color: "#94a3b8" }}>{subtitulo}</p>
    </div>
  );
}

const pill = {
  display: "inline-block", fontSize: "11px", fontWeight: 700,
  padding: "3px 9px", borderRadius: "9999px", whiteSpace: "nowrap" as const,
};

/** Colunas da legenda comparativa. O rótulo da classe encolhe (e corta com
 *  reticências) em vez de empurrar a tabela para fora da tela no celular. */
const COLUNAS = "minmax(0, 1fr) 64px 64px minmax(96px, auto)";

const legendaCabecalho = {
  fontSize: "10.5px", fontWeight: 700, color: "#94a3b8",
  textTransform: "uppercase" as const, letterSpacing: "0.4px",
};
