import { useEffect, useMemo, useState } from "react";
import type { ItemRanking, RankingAportes } from "../types/aporte";
import { catInfo, fmtMoeda, fmtPercentual } from "../utils/percentual";

/* ══════════════════════════════════════════════════════════════════════
   ROSCA DO APORTE — "onde este dinheiro vai entrar".

   Dois anéis, do jeito do gráfico de "Distribuição atual × ideal":

     ANEL DE DENTRO  = a CLASSE (quanto do aporte cada classe recebe)
     ANEL DE FORA    = os ATIVOS (as partes menores: cada fatia é um ativo)

   A fatia é proporcional ao valor SUGERIDO pelo motor (não ao % da carteira),
   e o centro conta o total alocado. Passar o mouse numa classe acende todos os
   ativos dela; num ativo, acende a classe dele — é assim que o "quanto entra" e
   o "em qual ativo" ficam na mesma imagem.

   O que não coube em ninguém NÃO vira fatia: o valor não alocado aparece escrito
   abaixo do anel. Uma fatia cinza de "não alocado" mentiria dizendo que esse
   dinheiro entrou em algum lugar.

   SVG puro (sem biblioteca), cores vindas de `catInfo` e tons derivados dela
   para separar os ativos dentro da mesma classe.
   ══════════════════════════════════════════════════════════════════════ */

interface FatiaAtivo {
  key: string;
  label: string;
  valor: number;
  cor: string;
  item: ItemRanking;
}

interface GrupoClasse {
  classe: string;
  label: string;
  icone: string;
  cor: string;
  valor: number;
  ativos: FatiaAtivo[];
}

export default function DonutAporte({ ranking }: { ranking: RankingAportes }) {
  const [hover, setHover] = useState<{ tipo: "classe" | "ativo"; key: string } | null>(null);
  const [pronto, setPronto] = useState(false);

  const moeda = ranking.moeda;
  const alocado = ranking.valor_alocado ?? 0;

  // Agrupa só o que RECEBEU dinheiro, na ordem de decisão das classes (a mesma
  // do painel de déficit) e, dentro dela, a ordem do ranking.
  const grupos = useMemo<GrupoClasse[]>(() => {
    const ordem = ranking.classes.map(c => c.classe as string);
    const porClasse = new Map<string, GrupoClasse>();

    for (const item of ranking.itens) {
      const valor = item.sugestao_aporte ?? 0;
      if (valor <= 0) continue;
      const info = catInfo(item.classe);
      const classe = item.classe as string;
      const grupo = porClasse.get(classe) ?? {
        classe, label: info.label, icone: info.icon, cor: info.color, valor: 0, ativos: [],
      };
      grupo.valor += valor;
      grupo.ativos.push({
        key: item.ativo_cadastro_id ?? `pos:${item.ticker}`,
        label: item.ticker ?? "—",
        valor,
        cor: info.color,
        item,
      });
      porClasse.set(classe, grupo);
    }

    const lista = [...porClasse.values()];
    // Dentro da classe, o ativo com tom próprio (o primeiro fica com a cor cheia).
    for (const grupo of lista) {
      grupo.ativos = grupo.ativos.map((a, i) => ({
        ...a,
        cor: tomDoAtivo(grupo.cor, i, grupo.ativos.length),
      }));
    }
    return lista.sort((a, b) => ordem.indexOf(a.classe) - ordem.indexOf(b.classe));
  }, [ranking]);

  useEffect(() => {
    setPronto(false);
    const t = setTimeout(() => setPronto(true), 30);
    return () => clearTimeout(t);
  }, [ranking.valor_aporte, ranking.carteira_id]);

  if (grupos.length === 0 || alocado <= 0) {
    return (
      <div style={card}>
        <h3 style={{ fontSize: 16, fontWeight: 800, color: "#0f172a", margin: 0 }}>Onde o aporte entra</h3>
        <p style={{ fontSize: 12.5, color: "#64748b", margin: "6px 0 0" }}>
          Nenhum valor foi alocado neste aporte — não há classe abaixo do alvo com ativo elegível.
          {ranking.valor_aporte != null && ranking.valor_aporte > 0 && (
            <> O valor informado foi {fmtMoeda(ranking.valor_aporte, moeda)}.</>
          )}
        </p>
      </div>
    );
  }

  const total = grupos.reduce((s, g) => s + g.valor, 0);
  const naoAlocado = ranking.valor_nao_alocado ?? 0;
  const classeAtiva = hover == null
    ? null
    : hover.tipo === "classe"
      ? hover.key
      : grupos.find(g => g.ativos.some(a => a.key === hover.key))?.classe ?? null;
  const ativoAtivo = hover?.tipo === "ativo" ? hover.key : null;

  const destacado = hover == null
    ? null
    : hover.tipo === "ativo"
      ? grupos.flatMap(g => g.ativos).find(a => a.key === hover.key) ?? null
      : grupos.find(g => g.classe === hover.key) ?? null;

  const apagado = (classe: string) => classeAtiva != null && classeAtiva !== classe;

  /* ── Ângulos dos dois anéis (mesma proporção, fatias contíguas) ── */
  let cursor = -Math.PI / 2;
  const aneis = grupos.map(g => {
    const abertura = (g.valor / total) * Math.PI * 2;
    const inicio = cursor;
    cursor += abertura;

    let cursorInterno = inicio;
    const ativos = g.ativos.map(a => {
      const fatia = (a.valor / total) * Math.PI * 2;
      const de = cursorInterno;
      cursorInterno += fatia;
      return { ...a, a0: de, a1: cursorInterno };
    });

    return { ...g, a0: inicio, a1: cursor, ativos };
  });

  const T = 360;
  const cx = T / 2;
  const cy = T / 2;
  const rInternoDe = 48;
  const rInternoAte = 86;
  const rExternoDe = 90;
  const rExternoAte = 122;

  return (
    <div style={card}>
      <div style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-start", gap: "12px", flexWrap: "wrap" }}>
        <div>
          <h3 style={{ fontSize: 16, fontWeight: 800, color: "#0f172a", margin: 0, letterSpacing: "-0.01em" }}>
            Onde o aporte entra
          </h3>
          <p style={{ fontSize: 12.5, color: "#64748b", margin: "5px 0 0" }}>
            Anel de dentro: a <strong>classe</strong>. Anel de fora: o <strong>ativo</strong> — o quanto entra em cada um.
            {ranking.valor_total_com_aporte != null && (
              <> Os alvos foram recalculados sobre o patrimônio de {fmtMoeda(ranking.valor_total_com_aporte, moeda)}.</>
            )}
          </p>
        </div>
        <span style={{ ...pill, color: "#047857", background: "#ecfdf5" }}>
          alocado {fmtMoeda(total, moeda)}
          {ranking.valor_aporte != null && <> de {fmtMoeda(ranking.valor_aporte, moeda)}</>}
        </span>
      </div>

      <div style={{ display: "flex", justifyContent: "center", gap: "clamp(6px, 3vw, 26px)", flexWrap: "wrap", alignItems: "center", marginTop: "10px" }}>
        <div style={{ position: "relative", flex: "0 1 420px", maxWidth: "100%" }}>
          <svg viewBox={`0 0 ${T} ${T}`} width="100%" role="img"
            aria-label={`Divisão do aporte: ${aneis.map(g => `${g.label} ${fmtMoeda(g.valor, moeda)}`).join(", ")}`}
            style={{ display: "block", transform: pronto ? "scale(1) rotate(0deg)" : "scale(0.92) rotate(-6deg)", opacity: pronto ? 1 : 0, transition: "transform .5s cubic-bezier(.2,.8,.2,1), opacity .4s ease" }}>
            {aneis.map(g => {
              const destacada = classeAtiva === g.classe;
              return (
                <path key={`c-${g.classe}`}
                  d={arco(cx, cy, rInternoDe, rInternoAte, g.a0, g.a1)}
                  fill={g.cor}
                  opacity={apagado(g.classe) ? 0.25 : 1}
                  stroke={destacada ? "#0f172a" : "none"}
                  strokeWidth={destacada ? 1.5 : 0}
                  style={{ cursor: "pointer", transition: "opacity .18s ease" }}
                  onMouseEnter={() => setHover({ tipo: "classe", key: g.classe })}
                  onMouseLeave={() => setHover(null)} />
              );
            })}

            {aneis.flatMap(g => g.ativos.map(a => (
              <path key={`a-${a.key}`}
                d={arco(cx, cy, rExternoDe, rExternoAte, a.a0, a.a1)}
                fill={a.cor}
                opacity={apagado(g.classe) ? 0.22 : ativoAtivo != null && ativoAtivo !== a.key ? 0.55 : 1}
                stroke={ativoAtivo === a.key ? "#0f172a" : "#ffffff"}
                strokeWidth={ativoAtivo === a.key ? 1.6 : 1}
                style={{ cursor: "pointer", transition: "opacity .18s ease" }}
                onMouseEnter={() => setHover({ tipo: "ativo", key: a.key })}
                onMouseLeave={() => setHover(null)} />
            )))}

            {/* Nome curto do ativo fora do anel: só quando a fatia é larga o
                bastante para o texto não colidir com o vizinho. O nome inteiro
                (que pode ser longo, ex.: "Tesouro Selic 2029") fica na legenda
                ao lado — aqui o rótulo é só para ligar a fatia ao ativo. */}
            {aneis.flatMap(g => g.ativos.map(a => {
              const meio = (a.a0 + a.a1) / 2;
              if ((a.a1 - a.a0) < 0.34) return null;
              const x = cx + Math.cos(meio) * (rExternoAte + 10);
              const y = cy + Math.sin(meio) * (rExternoAte + 10);
              const ancoragem = Math.cos(meio) >= 0 ? "start" : "end";
              return (
                <text key={`t-${a.key}`} x={x} y={y + 3} fontSize="10" fontWeight="700"
                  fill={ativoAtivo === a.key ? "#0f172a" : "#64748b"} textAnchor={ancoragem}
                  style={{ pointerEvents: "none" }}>
                  {encurta(a.label)}
                </text>
              );
            }))}

            {/* Centro: o total que sai do bolso hoje. */}
            <text x={cx} y={cy - 4} textAnchor="middle" fontSize="10"
              letterSpacing="0.6" fill="#94a3b8" fontWeight="700">
              {destacado ? "SELECIONADO" : "APORTE ALOCADO"}
            </text>
            <text x={cx} y={cy + 17} textAnchor="middle" fontWeight="800" fill="#0f172a"
              fontSize={fonteQueCabe(fmtMoeda(destacado?.valor ?? total, moeda), 84, 20, 11)}>
              {centroCurto(destacado?.valor ?? total, moeda)}
            </text>
            {destacado && (
              <text x={cx} y={cy + 32} textAnchor="middle" fontSize="10" fill="#64748b">
                {"label" in destacado ? destacado.label : ""}
              </text>
            )}
          </svg>
        </div>

        {/* ── Legenda: a mesma divisão, agora com os números ── */}
        <div style={{ flex: "1 1 280px", minWidth: "260px", display: "flex", flexDirection: "column", gap: "12px" }}>
          {aneis.map(g => (
            <div key={g.classe}
              onMouseEnter={() => setHover({ tipo: "classe", key: g.classe })} onMouseLeave={() => setHover(null)}
              style={{
                border: `1px solid ${classeAtiva === g.classe ? "#e2e8f0" : "transparent"}`,
                background: classeAtiva === g.classe ? "#f8fafc" : "transparent",
                borderRadius: "10px", padding: "8px 10px", transition: "background .15s ease",
              }}>
              <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", gap: "8px" }}>
                <span style={{ display: "inline-flex", alignItems: "center", gap: "7px", fontSize: "13px", fontWeight: 700, color: "#334155" }}>
                  <span style={{ width: "10px", height: "10px", borderRadius: "3px", background: g.cor, flexShrink: 0 }} />
                  {g.icone} {g.label}
                </span>
                <span style={{ fontSize: "12.5px", fontWeight: 800, color: "#0f172a", fontVariantNumeric: "tabular-nums" }}>
                  {fmtMoeda(g.valor, moeda)}
                  <span style={{ fontWeight: 600, color: "#94a3b8" }}> · {fmtPercentual((g.valor / total) * 100)}</span>
                </span>
              </div>

              <div style={{ display: "flex", flexDirection: "column", gap: "3px", marginTop: "6px" }}>
                {g.ativos.map(a => (
                  <div key={a.key}
                    onMouseEnter={() => setHover({ tipo: "ativo", key: a.key })} onMouseLeave={() => setHover(null)}
                    style={{
                      display: "flex", justifyContent: "space-between", alignItems: "center", gap: "8px",
                      fontSize: "12px", color: "#475569", padding: "2px 6px", borderRadius: "7px",
                      background: ativoAtivo === a.key ? "#eef2ff" : "transparent",
                    }}>
                    <span style={{ display: "inline-flex", alignItems: "center", gap: "6px", minWidth: 0 }}>
                      <span style={{ width: "8px", height: "8px", borderRadius: "50%", background: a.cor, flexShrink: 0 }} />
                      <span style={{ fontWeight: 700, color: "#0f172a" }}>{a.label}</span>
                      {a.item.nota != null && (
                        <span style={{ fontSize: "10.5px", color: "#64748b" }}>nota {a.item.nota.toFixed(0)}</span>
                      )}
                    </span>
                    <span style={{ fontVariantNumeric: "tabular-nums", whiteSpace: "nowrap" }}>
                      <strong>{fmtMoeda(a.valor, moeda)}</strong>
                      <span style={{ color: "#94a3b8" }}> · {fmtPercentual((a.valor / total) * 100)}</span>
                    </span>
                  </div>
                ))}
              </div>
            </div>
          ))}

          {naoAlocado > 0 && (
            <p style={{ fontSize: "11.5px", color: "#b45309", margin: 0 }}>
              💤 {fmtMoeda(naoAlocado, moeda)} não coube em nenhum ativo elegível — ficou fora do anel, porque não foi para lugar nenhum.
            </p>
          )}
        </div>
      </div>
    </div>
  );
}

/* ── Apoio ── */

/**
 * Rótulo curto do ativo dentro do anel. O nome completo (pode ser "Tesouro Selic
 * 2029") fica na legenda: aqui o texto existe só para ligar a fatia ao ativo, e
 * um nome longo sairia do desenho.
 */
const encurta = (texto: string, max = 8): string =>
  texto.length <= max ? texto : `${texto.slice(0, max - 1)}…`;

/** Fatia de anel (setor circular) como path de SVG. */
function arco(cx: number, cy: number, rInt: number, rExt: number, a0: number, a1: number): string {
  // Um arco de 360° não existe em SVG (início e fim coincidem): fecha com uma
  // volta quase completa e o traço até o centro fecha o desenho.
  const fim = Math.min(a1, a0 + Math.PI * 2 - 0.0001);
  const p = (r: number, a: number) => [cx + r * Math.cos(a), cy + r * Math.sin(a)];
  const [x0, y0] = p(rExt, a0);
  const [x1, y1] = p(rExt, fim);
  const [x2, y2] = p(rInt, fim);
  const [x3, y3] = p(rInt, a0);
  const grande = (fim - a0) > Math.PI ? 1 : 0;
  return `M ${x0} ${y0} A ${rExt} ${rExt} 0 ${grande} 1 ${x1} ${y1} `
    + `L ${x2} ${y2} A ${rInt} ${rInt} 0 ${grande} 0 ${x3} ${y3} Z`;
}

/** Tom do ativo dentro da classe: clareia a cor da classe conforme a posição. */
function tomDoAtivo(base: string, i: number, n: number): string {
  if (n <= 1) return base;
  const quanto = (i / (n - 1)) * 0.55;
  return misturaComBranco(base, i === 0 ? 0 : 0.12 + quanto);
}

function misturaComBranco(hex: string, t: number): string {
  const cor = hex.replace("#", "");
  if (cor.length !== 6) return hex;
  const canais = [0, 2, 4].map(i => parseInt(cor.slice(i, i + 2), 16));
  const mistos = canais.map(c => Math.round(c + (255 - c) * Math.min(1, Math.max(0, t))));
  return `#${mistos.map(c => c.toString(16).padStart(2, "0")).join("")}`;
}

const fmtSemCentavos = (valor: number, moeda: string): string =>
  fmtMoeda(Math.round(valor), moeda).replace(/,00$/, "");

/** Valor do centro: sem centavos — o furo tem ~108 de diâmetro. */
const centroCurto = (valor: number, moeda: string): string => fmtSemCentavos(valor, moeda);

/** Fonte que cabe na largura do furo (mesma regra do gráfico da carteira). */
const fonteQueCabe = (texto: string, largura: number, max: number, min: number, fator = 0.58): number =>
  Math.max(min, Math.min(max, Math.floor(largura / Math.max(1, texto.length * fator))));

const card: React.CSSProperties = {
  background: "white", borderRadius: "16px", padding: "20px 22px",
  boxShadow: "0 1px 3px rgba(15,23,42,0.05)", border: "1px solid #eef2f7",
};

const pill: React.CSSProperties = {
  fontSize: "11px", fontWeight: 700, padding: "3px 10px", borderRadius: "9999px",
};
