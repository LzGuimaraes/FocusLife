import type { RankingAportes } from "../types/aporte";
import { ESTRATEGIAS_APORTE } from "../types/aporte";
import ScoreBadge from "./ScoreBadge";
import { catInfo, fmtMoeda, fmtPercentual } from "../utils/percentual";
import { fmtScore, scoreBg, scoreColor } from "../utils/avaliacao";
/* ══════════════════════════════════════════════════════════════════════
   Ranking de prioridade de aporte (Módulos 6 e 9).

   Componente de apresentação: recebe o ranking pronto do backend e mostra
   Quality Score e Contribution Score lado a lado, deixando claro que são
   coisas diferentes (qualidade do ativo × prioridade de aporte).
   ══════════════════════════════════════════════════════════════════════ */

export default function RankingAportesTable({ ranking, titulo }: { ranking: RankingAportes; titulo?: string }) {
  const { itens, moeda, avisos } = ranking;
  const comSugestao = itens.some(i => i.sugestao_aporte != null);
  const estrategia = ESTRATEGIAS_APORTE.find(e => e.value === ranking.estrategia_aporte);

  return (
    <div style={{
      background: "white", borderRadius: "12px", padding: "18px",
      boxShadow: "0 1px 3px rgba(0,0,0,0.06)", border: "1px solid #f1f5f9",
    }}>
      <div style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-start", flexWrap: "wrap", gap: "10px", marginBottom: "12px" }}>
        <div>
          <h3 style={{ fontSize: "15px", fontWeight: 700, color: "#0f172a", margin: 0 }}>
            {titulo ?? "Prioridade de aporte"}
          </h3>
          <p style={{ fontSize: "12px", color: "#64748b", margin: "2px 0 0" }}>
            Aporte decidido por <strong>classe → subclasse → ativo</strong>
            {estrategia && <> · dentro da classe: {estrategia.label}</>}
            {comSugestao && <> · teto: o déficit da classe</>}
          </p>
        </div>
        {comSugestao && ranking.valor_aporte != null && (
          <div style={{ textAlign: "right", fontSize: "12px", color: "#64748b" }}>
            <div>
              Aporte de <strong>{fmtMoeda(ranking.valor_aporte, moeda)}</strong>
            </div>
            <div>
              Alocado: <strong style={{ color: "#047857" }}>{fmtMoeda(ranking.valor_alocado, moeda)}</strong>
              {ranking.valor_nao_alocado != null && ranking.valor_nao_alocado > 0 && (
                <> · não alocado: <strong style={{ color: "#b45309" }}>{fmtMoeda(ranking.valor_nao_alocado, moeda)}</strong></>
              )}
            </div>
          </div>
        )}
      </div>

      {avisos.length > 0 && (
        <div style={{ background: "#fffbeb", border: "1px solid #fde68a", borderRadius: "10px", padding: "10px 14px", marginBottom: "14px" }}>
          {avisos.map((a, i) => (
            <p key={i} style={{ fontSize: "12px", color: "#92400e", margin: i === 0 ? 0 : "4px 0 0" }}>⚠ {a}</p>
          ))}
        </div>
      )}

      {/* Onde o dinheiro entra: a decisão é da classe/subclasse, antes de olhar ticker. */}
      <AportePorClasse ranking={ranking} />

      {itens.length === 0 ? (
        <p style={{ fontSize: "13px", color: "#94a3b8", textAlign: "center", padding: "20px 0", margin: 0 }}>
          Nada para priorizar ainda. Cadastre posições e defina a Carteira Ideal (classes e subclasses).
        </p>
      ) : (
        <>
          <div style={{ overflowX: "auto" }}>
          <table style={{ width: "100%", borderCollapse: "collapse", minWidth: comSugestao ? "1020px" : "900px", fontSize: "13px" }}>
            <thead>
              <tr style={{ background: "#f8fafc" }}>
                {["#", "Ativo", "Classe", "Quality", "Contribution", "Atual", "Ideal", "Déficit", "Excesso", "Prior."]
                  .map(h => <th key={h} style={{ ...th, textAlign: ["#", "Ativo", "Classe"].includes(h) ? "left" : "right" }}>{h}</th>)}
                {comSugestao && <th style={{ ...th, textAlign: "right" }}>Sugestão</th>}
              </tr>
            </thead>
            <tbody>
              {itens.map(i => {
                const info = catInfo(i.classe);
                const semMetaPropria = i.meta_id == null;
                return (
                  <tr key={i.ativo_cadastro_id ?? `nome:${i.ticker}`} style={{ borderTop: "1px solid #f1f5f9" }}>
                    <td style={{ ...td, color: "#94a3b8", fontWeight: 700 }}>{i.posicao}</td>
                    <td style={{ ...td, fontWeight: 700, color: "#0f172a" }}>
                      {i.ticker ?? "—"}
                      {!i.vinculado && (
                        <span title="Posição sem ticker de catálogo (renda fixa/caixinha): a meta é o percentual da subclasse"
                          style={{ marginLeft: "8px", fontSize: "10px", fontWeight: 700, color: "#92400e", background: "#fffbeb", border: "1px solid #fde68a", padding: "1px 7px", borderRadius: "9999px" }}>
                          sem ticker
                        </span>
                      )}
                    </td>
                    <td style={{ ...td, color: "#475569", whiteSpace: "nowrap" }}>
                      {info.icon} {info.label}
                      {i.subclasse_nome && (
                        <div style={{ fontSize: "11px", color: "#94a3b8" }}>{i.subclasse_nome}</div>
                      )}
                    </td>
                    <td style={{ ...td, textAlign: "right" }}>
                      {i.qualidade_avaliada
                        ? <ScoreBadge score={i.quality_score} size="sm" />
                        : <span title="Ativo ainda sem avaliação: o termo de qualidade não entra na conta dele"
                            style={naoAvaliado}>sem avaliação</span>}
                    </td>
                    <td style={{ ...td, textAlign: "right" }}>
                      <span style={{
                        display: "inline-flex", alignItems: "center", gap: "6px", justifyContent: "flex-end",
                        padding: "2px 10px", borderRadius: "9999px", fontWeight: 700, fontSize: "12px",
                        background: scoreBg(i.contribution_score), color: scoreColor(i.contribution_score),
                      }}>
                        {fmtScore(i.contribution_score)}
                      </span>
                    </td>
                    <td style={{ ...td, textAlign: "right", color: "#475569" }}>{fmtPercentual(i.percentual_atual)}</td>
                    <td style={{ ...td, textAlign: "right", color: "#475569" }}>
                      {semMetaPropria ? <span style={{ color: "#cbd5e1" }}>subclasse</span> : fmtPercentual(i.percentual_ideal)}
                    </td>
                    <td style={{ ...td, textAlign: "right", fontWeight: 600, color: i.deficit > 0 ? "#1d4ed8" : "#cbd5e1" }}>
                      {semMetaPropria ? <span style={{ color: "#cbd5e1" }}>—</span> : fmtMoeda(i.deficit, moeda)}
                    </td>
                    <td style={{ ...td, textAlign: "right", fontWeight: 600, color: i.excesso > 0 ? "#b45309" : "#cbd5e1" }}>
                      {semMetaPropria ? <span style={{ color: "#cbd5e1" }}>—</span> : fmtMoeda(i.excesso, moeda)}
                    </td>
                    <td style={{ ...td, textAlign: "right", color: i.prioridade_manual > 0 ? "#6366f1" : "#cbd5e1", fontWeight: 700 }}>
                      {i.prioridade_manual}
                    </td>
                    {comSugestao && (
                      <td style={{ ...td, textAlign: "right", fontWeight: 800, color: (i.sugestao_aporte ?? 0) > 0 ? "#047857" : "#cbd5e1" }}>
                        {fmtMoeda(i.sugestao_aporte ?? 0, moeda)}
                      </td>
                    )}
                  </tr>
                );
              })}
            </tbody>
          </table>
          </div>
        </>
      )}

      <p style={{ fontSize: "11px", color: "#94a3b8", marginTop: "12px", marginBottom: 0 }}>
        Contribution Score = prioridade de aporte segundo os SEUS pesos · Quality Score = soma das suas notas nos checklists.
        {comSugestao && " O teto é o déficit de cada CLASSE: nenhum ativo recebe dinheiro de uma classe que já está no alvo."}
      </p>
    </div>
  );
}

/* ══════════════════════════════════════════════════════════════════════
   Onde entra o dinheiro, no nível em que a decisão é tomada: a CLASSE (e a
   subclasse). O ticker é destino, não ponto de partida — antes o rateio era
   por ticker, então metas que espelhavam a carteira atual zeravam tudo.
   ══════════════════════════════════════════════════════════════════════ */
function AportePorClasse({ ranking }: { ranking: RankingAportes }) {
  const { classes, moeda, valor_aporte } = ranking;
  if (classes.length === 0) return null;

  const base = (valor_aporte ?? 0) > 0 ? (valor_aporte as number) : 0;
  const comSugestao = classes.some(c => c.sugerido > 0);
  const ordenadas = [...classes].sort((a, b) => (b.sugerido - a.sugerido) || (b.deficit - a.deficit));

  return (
    <div style={{ marginBottom: "16px" }}>
      <div style={{ display: "flex", justifyContent: "space-between", alignItems: "baseline", gap: "10px", flexWrap: "wrap", marginBottom: "8px" }}>
        <h4 style={{ fontSize: "13px", fontWeight: 700, color: "#0f172a", margin: 0 }}>
          {comSugestao ? "Onde entra o dinheiro" : "Déficit por classe"} — classes e subclasses
        </h4>
        <span style={{ fontSize: "11px", color: "#94a3b8" }}>
          A classe define o quanto; o ativo só escolhe dentro do orçamento dela
        </span>
      </div>

      <div style={{ display: "flex", flexDirection: "column", gap: "8px" }}>
        {ordenadas.map(c => {
          const info = catInfo(c.classe);
          const noAlvo = c.deficit <= 0;
          const pct = (base > 0) ? Math.min(100, (c.sugerido / base) * 100) : 0;
          const subs = c.subclasses.filter(s => s.percentual_ideal > 0 || s.sugerido > 0);
          return (
            <div key={c.classe} style={{
              border: "1px solid #f1f5f9", borderRadius: "10px", padding: "9px 12px",
              background: noAlvo ? "#f8fafc" : "white", opacity: noAlvo && !comSugestao ? 0.65 : 1,
            }}>
              <div style={{ display: "flex", justifyContent: "space-between", gap: "10px", flexWrap: "wrap", alignItems: "baseline" }}>
                <span style={{ fontSize: "13px", fontWeight: 700, color: "#0f172a" }}>
                  {info.icon} {info.label}
                  <span style={{ fontWeight: 600, color: "#64748b", marginLeft: "8px", fontSize: "11px" }}>
                    {fmtPercentual(c.percentual_atual)} → {fmtPercentual(c.percentual_ideal)}
                  </span>
                </span>
                <span style={{ fontSize: "12px", display: "flex", gap: "12px", alignItems: "baseline" }}>
                  <span style={{ color: noAlvo ? "#b45309" : "#1d4ed8", fontWeight: 600 }}>
                    {noAlvo ? `acima do alvo em ${fmtMoeda(c.excesso, moeda)}` : `falta ${fmtMoeda(c.deficit, moeda)}`}
                  </span>
                  {comSugestao && (
                    <strong style={{ color: c.sugerido > 0 ? "#047857" : "#cbd5e1" }}>
                      {fmtMoeda(c.sugerido, moeda)}
                    </strong>
                  )}
                </span>
              </div>

              {comSugestao && (
                <div style={{ height: "6px", background: "#f1f5f9", borderRadius: "9999px", marginTop: "7px", overflow: "hidden" }}>
                  <div style={{ width: `${pct}%`, height: "100%", background: info.color, borderRadius: "9999px" }} />
                </div>
              )}

              {subs.length > 0 && (
                <div style={{ marginTop: "8px", display: "flex", flexDirection: "column", gap: "4px" }}>
                  {subs.map(s => (
                    <div key={s.id} style={{ display: "flex", justifyContent: "space-between", gap: "10px", fontSize: "12px", color: "#475569", paddingLeft: "14px" }}>
                      <span>
                        ↳ <strong style={{ color: "#334155" }}>{s.nome}</strong>
                        <span style={{ marginLeft: "8px", fontSize: "11px", color: "#94a3b8" }}>
                          {fmtPercentual(s.percentual_atual)} → {fmtPercentual(s.percentual_ideal)}
                        </span>
                      </span>
                      <span style={{ display: "flex", gap: "12px" }}>
                        <span style={{ color: s.deficit > 0 ? "#1d4ed8" : "#cbd5e1" }}>
                          {s.deficit > 0 ? `falta ${fmtMoeda(s.deficit, moeda)}` : "no alvo"}
                        </span>
                        {comSugestao && (
                          <strong style={{ color: s.sugerido > 0 ? "#047857" : "#cbd5e1" }}>
                            {fmtMoeda(s.sugerido, moeda)}
                          </strong>
                        )}
                      </span>
                    </div>
                  ))}
                </div>
              )}
            </div>
          );
        })}
      </div>
    </div>
  );
}

const th: React.CSSProperties = {
  padding: "9px 10px", fontSize: "11px", fontWeight: 700, color: "#64748b",
  textTransform: "uppercase", letterSpacing: "0.3px",
};
const td: React.CSSProperties = { padding: "9px 10px", fontSize: "13px" };
const naoAvaliado: React.CSSProperties = {
  fontSize: "11px", fontWeight: 600, color: "#94a3b8", background: "#f1f5f9",
  padding: "2px 8px", borderRadius: "9999px",
};
