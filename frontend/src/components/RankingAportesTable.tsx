import type { RankingAportes, EstadoAtivo, StatusNivel } from "../types/aporte";
import { ESTRATEGIAS_APORTE } from "../types/aporte";
import ScoreBadge from "./ScoreBadge";
import { catInfo, fmtMoeda, fmtPercentual } from "../utils/percentual";
import { fmtScore, scoreBg, scoreColor } from "../utils/avaliacao";
import { miniLabel } from "./FormStyles";
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
            {ranking.valor_nao_alocado != null && ranking.valor_nao_alocado > 0 && ranking.nao_alocado_explicacao && (
              <div style={{ fontSize: "11px", color: "#b45309", maxWidth: "420px", marginTop: "4px" }}>
                {ranking.nao_alocado_explicacao}
              </div>
            )}
          </div>
        )}
      </div>

      {ranking.alertas.length > 0 && (
        <div style={{ background: "#f8fafc", border: "1px solid #e2e8f0", borderRadius: "10px", padding: "10px 14px", marginBottom: "12px" }}>
          <p style={{ ...miniLabel, marginBottom: "5px" }}>Alertas do motor de decisão</p>
          {ranking.alertas.map((a, i) => (
            <p key={i} style={{ fontSize: "12px", color: alertaCor(a.tipo), margin: i === 0 ? 0 : "3px 0 0" }}>
              {alertaIcone(a.tipo)} {a.mensagem}
            </p>
          ))}
        </div>
      )}

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
          <table style={{ width: "100%", borderCollapse: "collapse", minWidth: comSugestao ? "1180px" : "1040px", fontSize: "13px" }}>
            <thead>
              <tr style={{ background: "#f8fafc" }}>
                {["#", "Ativo", "Classe", "Quality", "Momento", "Estado", "Contribution", "Atual", "Ideal", "Déficit", "Teto", "Prior."]
                  .map(h => <th key={h} style={{ ...th, textAlign: ["#", "Ativo", "Classe", "Estado"].includes(h) ? "left" : "right" }}>{h}</th>)}
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
                      {i.momento_avaliado ? (
                        <span title={`Fator de momento aplicado: ${i.fator_momento}`}
                          style={{ display: "inline-flex", flexDirection: "column", alignItems: "flex-end" }}>
                          <ScoreBadge score={i.momento_score} size="sm" />
                          <span style={{ fontSize: "10px", color: i.fator_momento <= 0 ? "#b91c1c" : i.fator_momento < 1 ? "#b45309" : "#047857" }}>
                            fator {i.fator_momento}
                          </span>
                        </span>
                      ) : (
                        <span title="Sem checklist de momento/valuation: o fator fica NEUTRO (1) — não é penalidade"
                          style={naoAvaliado}>sem momento</span>
                      )}
                    </td>
                    <td style={{ ...td }}>
                      <span title={i.bloqueios.length > 0 ? i.bloqueios.join("; ") : i.motivo ?? ""}
                        style={{ fontSize: "11px", fontWeight: 700, padding: "2px 9px", borderRadius: "9999px", whiteSpace: "nowrap",
                          color: estadoCor(i.estado), background: estadoBg(i.estado) }}>
                        {estadoLabel(i.estado)}
                      </span>
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
                    <td style={{ ...td, textAlign: "right", color: i.teto > 0 ? "#0f172a" : "#cbd5e1", fontWeight: 600 }}>
                      {fmtMoeda(i.teto, moeda)}
                    </td>
                    <td style={{ ...td, textAlign: "right", color: i.prioridade_manual > 0 ? "#6366f1" : "#cbd5e1", fontWeight: 700 }}>
                      {i.prioridade_manual}
                    </td>
                    {comSugestao && (
                      <td style={{ ...td, textAlign: "right", fontWeight: 800, color: (i.sugestao_aporte ?? 0) > 0 ? "#047857" : "#cbd5e1" }}>
                        <span title={i.motivo ?? ""}>
                          {fmtMoeda(i.sugestao_aporte ?? 0, moeda)}
                        </span>
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
        Contribution Score = prioridade de aporte segundo os SEUS pesos (qualidade + déficit − excesso + prioridade + momento) ·
        Quality Score = notas dos checklists de Qualidade · Momento = fator 0–1 dos checklists de Momento (não altera a qualidade).
        {comSugestao && " O teto de cada ativo é o déficit dele + tolerância: ninguém recebe de uma classe que já está no alvo."}
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
                  <span title={`Tolerância: ${c.tolerancia} p.p.${c.limite_maximo ? ` · limite máximo: ${c.limite_maximo}%` : ""}`}
                    style={{ marginLeft: "8px", fontSize: "10px", fontWeight: 700, padding: "2px 8px", borderRadius: "9999px",
                      color: statusCor(c.status), background: statusBg(c.status) }}>
                    {statusLabel(c.status)}
                  </span>
                  {c.limite_maximo != null && (
                    <span style={{ marginLeft: "6px", fontSize: "10px", color: "#94a3b8" }}>
                      máx {fmtPercentual(c.limite_maximo)}
                    </span>
                  )}
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
                        {s.status !== "ABAIXO" && s.status !== "SEM_ALVO" && (
                          <span style={{ marginLeft: "8px", fontSize: "10px", fontWeight: 700, color: statusCor(s.status) }}>
                            {statusLabel(s.status)}
                          </span>
                        )}
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

              {c.motivo && c.sugerido <= 0 && (
                <p style={{ fontSize: "11px", color: "#94a3b8", margin: "6px 0 0" }}>{c.motivo}</p>
              )}
            </div>
          );
        })}
      </div>
    </div>
  );
}

/* ── Rótulos/cores dos estados e alertas do motor ── */

const estadoLabel = (e: EstadoAtivo): string => ({
  APROVADO: "aprovado",
  RESTRITO: "restrito",
  NAO_APORTAR: "não aportar",
  SEM_AVALIACAO: "sem avaliação",
}[e] ?? e);

const estadoCor = (e: EstadoAtivo): string => ({
  APROVADO: "#047857",
  RESTRITO: "#b45309",
  NAO_APORTAR: "#b91c1c",
  SEM_AVALIACAO: "#64748b",
}[e] ?? "#64748b");

const estadoBg = (e: EstadoAtivo): string => ({
  APROVADO: "#ecfdf5",
  RESTRITO: "#fffbeb",
  NAO_APORTAR: "#fef2f2",
  SEM_AVALIACAO: "#f1f5f9",
}[e] ?? "#f1f5f9");

const statusLabel = (s: StatusNivel): string => ({
  ABAIXO: "abaixo do alvo",
  EQUILIBRADO: "equilibrado",
  ACIMA: "acima do alvo",
  SEM_ALVO: "sem alvo",
}[s] ?? s);

const statusCor = (s: StatusNivel): string => ({
  ABAIXO: "#1d4ed8",
  EQUILIBRADO: "#047857",
  ACIMA: "#b45309",
  SEM_ALVO: "#64748b",
}[s] ?? "#64748b");

const statusBg = (s: StatusNivel): string => ({
  ABAIXO: "#eff6ff",
  EQUILIBRADO: "#ecfdf5",
  ACIMA: "#fffbeb",
  SEM_ALVO: "#f1f5f9",
}[s] ?? "#f1f5f9");

const alertaIcone = (tipo: string): string => {
  if (tipo === "BLOQUEIO" || tipo === "ATIVO_LIMITE") return "⛔";
  if (tipo === "NAO_ALOCADO") return "💤";
  if (tipo === "SEM_AVALIACAO" || tipo === "SEM_SUBCLASSE") return "⚠";
  if (tipo === "CLASSE_ACIMA" || tipo === "SUBCLASSE_ACIMA") return "📈";
  return "📉";
};

const alertaCor = (tipo: string): string => {
  if (tipo === "BLOQUEIO" || tipo === "ATIVO_LIMITE") return "#b91c1c";
  if (tipo === "NAO_ALOCADO") return "#b45309";
  if (tipo === "SEM_AVALIACAO" || tipo === "SEM_SUBCLASSE") return "#92400e";
  return "#334155";
};

const th: React.CSSProperties = {
  padding: "9px 10px", fontSize: "11px", fontWeight: 700, color: "#64748b",
  textTransform: "uppercase", letterSpacing: "0.3px",
};
const td: React.CSSProperties = { padding: "9px 10px", fontSize: "13px" };
const naoAvaliado: React.CSSProperties = {
  fontSize: "11px", fontWeight: 600, color: "#94a3b8", background: "#f1f5f9",
  padding: "2px 8px", borderRadius: "9999px",
};
