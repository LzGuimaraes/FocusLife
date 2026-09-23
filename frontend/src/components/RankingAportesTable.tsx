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
            Ordenado por Contribution Score
            {estrategia && <> · rateio: <strong>{estrategia.label}</strong></>}
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

      {itens.length === 0 ? (
        <p style={{ fontSize: "13px", color: "#94a3b8", textAlign: "center", padding: "20px 0", margin: 0 }}>
          Nada para priorizar ainda. Defina a Carteira Ideal e as metas por ativo.
        </p>
      ) : (
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
                return (
                  <tr key={i.meta_id} style={{ borderTop: "1px solid #f1f5f9" }}>
                    <td style={{ ...td, color: "#94a3b8", fontWeight: 700 }}>{i.posicao}</td>
                    <td style={{ ...td, fontWeight: 700, color: "#0f172a" }}>{i.ticker ?? "—"}</td>
                    <td style={{ ...td, color: "#475569", whiteSpace: "nowrap" }}>{info.icon} {info.label}</td>
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
                    <td style={{ ...td, textAlign: "right", color: "#475569" }}>{fmtPercentual(i.percentual_ideal)}</td>
                    <td style={{ ...td, textAlign: "right", fontWeight: 600, color: i.deficit > 0 ? "#1d4ed8" : "#cbd5e1" }}>
                      {fmtMoeda(i.deficit, moeda)}
                    </td>
                    <td style={{ ...td, textAlign: "right", fontWeight: 600, color: i.excesso > 0 ? "#b45309" : "#cbd5e1" }}>
                      {fmtMoeda(i.excesso, moeda)}
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
      )}

      <p style={{ fontSize: "11px", color: "#94a3b8", marginTop: "12px", marginBottom: 0 }}>
        Contribution Score = prioridade de aporte segundo os SEUS pesos · Quality Score = soma das suas notas nos checklists.
        {comSugestao && " Nenhum ativo recebe mais do que o seu déficit."}
      </p>
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
