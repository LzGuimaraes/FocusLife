import type { Comparativo } from "../types/planejamento";
import { catInfo, fmtMoeda, fmtPercentual, fmtPontosPercentuais, somaFechada } from "../utils/percentual";

/* ══════════════════════════════════════════════════════════════════════
   Comparativo Carteira Atual × Carteira Ideal (Módulos 1 e 10).

   Mostra por classe → subclasse → ativo:
   % atual, % ideal, diferença em pontos percentuais, valores e o
   déficit (falta aportar) / excesso (acima da meta).
   ══════════════════════════════════════════════════════════════════════ */

export default function ComparativoTable({ comparativo }: { comparativo: Comparativo }) {
  const { moeda, classes, valor_total, soma_percentuais_ideal, avisos } = comparativo;

  return (
    <div style={{ background: "white", borderRadius: "12px", padding: "18px", boxShadow: "0 1px 3px rgba(0,0,0,0.06)", border: "1px solid #f1f5f9" }}>
      <div style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-start", flexWrap: "wrap", gap: "10px", marginBottom: "14px" }}>
        <div>
          <h3 style={{ fontSize: "15px", fontWeight: 700, color: "#0f172a", margin: 0 }}>Carteira Atual × Carteira Ideal</h3>
          <p style={{ fontSize: "12px", color: "#64748b", margin: "2px 0 0" }}>
            Valor total da carteira: <strong>{fmtMoeda(valor_total, moeda)}</strong>
          </p>
        </div>
        <span style={{
          fontSize: "13px", fontWeight: 700, padding: "5px 12px", borderRadius: "9999px",
          background: somaFechada(soma_percentuais_ideal) ? "#d1fae5" : "#fef3c7",
          color: somaFechada(soma_percentuais_ideal) ? "#047857" : "#b45309",
        }}>
          Ideal somando {fmtPercentual(soma_percentuais_ideal)}
        </span>
      </div>

      {avisos.length > 0 && (
        <div style={{ background: "#fffbeb", border: "1px solid #fde68a", borderRadius: "10px", padding: "10px 14px", marginBottom: "14px" }}>
          {avisos.map((a, i) => (
            <p key={i} style={{ fontSize: "12px", color: "#92400e", margin: i === 0 ? 0 : "4px 0 0" }}>⚠ {a}</p>
          ))}
        </div>
      )}

      <div style={{ overflowX: "auto" }}>
        <table style={{ width: "100%", borderCollapse: "collapse", minWidth: "820px", fontSize: "13px" }}>
          <thead>
            <tr style={{ background: "#f8fafc" }}>
              {["Classe / Subclasse / Ativo", "Atual", "Ideal", "Diferença", "Valor atual", "Valor ideal", "Déficit", "Excesso"].map(h => (
                <th key={h} style={{ ...thStyle, textAlign: h === "Classe / Subclasse / Ativo" ? "left" : "right" }}>{h}</th>
              ))}
            </tr>
          </thead>
          <tbody>
            {classes.map(c => {
              const info = catInfo(c.classe);
              const diferenca = c.percentual_atual - c.percentual_ideal;
              return (
                <FragmentoClasse key={c.classe}
                  c={c}
                  nome={`${info.icon} ${info.label}`}
                  negrito
                  cor={info.color}
                  diferenca={diferenca}
                  moeda={moeda}
                  infoLabel={info.label}
                />
              );
            })}
          </tbody>
        </table>
      </div>

      {classes.length === 0 && (
        <p style={{ fontSize: "13px", color: "#94a3b8", textAlign: "center", padding: "20px 0", margin: 0 }}>
          Defina as classes da Carteira Ideal para ver o comparativo.
        </p>
      )}
    </div>
  );
}

/* ── Linha da classe + subclasses + ativos ── */
function FragmentoClasse({ c, nome, negrito, cor, diferenca, moeda, infoLabel }: {
  c: Comparativo["classes"][number];
  nome: string;
  negrito?: boolean;
  cor: string;
  diferenca: number;
  moeda: string;
  infoLabel: string;
}) {
  return (
    <>
      <tr style={{ borderTop: "1px solid #e2e8f0", background: "#fcfdff" }}>
        <td style={{ ...tdStyle, fontWeight: 700, color: "#0f172a" }}>
          <span style={{ display: "inline-block", width: "4px", height: "14px", background: cor, borderRadius: "2px", marginRight: "8px", verticalAlign: "middle" }} />
          {nome}
        </td>
        <td style={tdStyleRight}>{fmtPercentual(c.percentual_atual)}</td>
        <td style={tdStyleRight}>{fmtPercentual(c.percentual_ideal)}</td>
        <td style={{ ...tdStyleRight, ...corDiferenca(diferenca) }}>{fmtPontosPercentuais(diferenca)}</td>
        <td style={tdStyleRight}>{fmtMoeda(c.valor_atual, moeda)}</td>
        <td style={tdStyleRight}>{fmtMoeda(c.valor_ideal, moeda)}</td>
        <td style={{ ...tdStyleRight, color: c.deficit > 0 ? "#1d4ed8" : "#cbd5e1" }}>{fmtMoeda(c.deficit, moeda)}</td>
        <td style={{ ...tdStyleRight, color: c.excesso > 0 ? "#b45309" : "#cbd5e1" }}>{fmtMoeda(c.excesso, moeda)}</td>
      </tr>

      {c.subclasses.map(s => {
        const dif = s.percentual_atual - s.percentual_ideal;
        return (
          <tr key={`sub-${s.id}`}>
            <td style={{ ...tdStyle, paddingLeft: "30px", color: "#475569" }}>↳ {s.nome}</td>
            <td style={tdStyleRightMuted}>{fmtPercentual(s.percentual_atual)}</td>
            <td style={tdStyleRightMuted}>{fmtPercentual(s.percentual_ideal)}</td>
            <td style={{ ...tdStyleRightMuted, ...corDiferenca(dif) }}>{fmtPontosPercentuais(dif)}</td>
            <td style={tdStyleRightMuted}>{fmtMoeda(s.valor_atual, moeda)}</td>
            <td style={tdStyleRightMuted}>{fmtMoeda(s.valor_ideal, moeda)}</td>
            <td style={{ ...tdStyleRightMuted, color: s.deficit > 0 ? "#1d4ed8" : "#cbd5e1" }}>{fmtMoeda(s.deficit, moeda)}</td>
            <td style={{ ...tdStyleRightMuted, color: s.excesso > 0 ? "#b45309" : "#cbd5e1" }}>{fmtMoeda(s.excesso, moeda)}</td>
          </tr>
        );
      })}

      {c.ativos.map(a => {
        const semMeta = !a.possui_meta;
        const dif = a.percentual_atual - a.percentual_ideal;
        return (
          <tr key={`ativo-${a.ativo_cadastro_id}`}>
            <td style={{ ...tdStyle, paddingLeft: "30px", color: "#475569" }}>
              {a.ticker}
              {a.prioridade_manual > 0 && (
                <span style={{ marginLeft: "6px", fontSize: "10px", fontWeight: 700, color: "#6366f1", background: "#eef2ff", padding: "1px 6px", borderRadius: "9999px" }}>
                  P{a.prioridade_manual}
                </span>
              )}
              {semMeta && (
                <span title="Você tem este ativo, mas ainda não definiu meta — ele conta como excesso da classe"
                  style={{ marginLeft: "6px", fontSize: "10px", fontWeight: 700, color: "#b45309", background: "#fef3c7", padding: "1px 6px", borderRadius: "9999px" }}>
                  sem meta
                </span>
              )}
            </td>
            <td style={tdStyleRightMuted}>{fmtPercentual(a.percentual_atual)}</td>
            <td style={tdStyleRightMuted}>{semMeta ? "—" : fmtPercentual(a.percentual_ideal)}</td>
            <td style={{ ...tdStyleRightMuted, ...(semMeta ? { color: "#cbd5e1" } : corDiferenca(dif)) }}>
              {semMeta ? "—" : fmtPontosPercentuais(dif)}
            </td>
            <td style={tdStyleRightMuted}>{fmtMoeda(a.valor_atual, moeda)}</td>
            <td style={tdStyleRightMuted}>{semMeta ? "—" : fmtMoeda(a.valor_ideal, moeda)}</td>
            <td style={{ ...tdStyleRightMuted, color: a.deficit > 0 ? "#1d4ed8" : "#cbd5e1" }}>{fmtMoeda(a.deficit, moeda)}</td>
            <td style={{ ...tdStyleRightMuted, color: a.excesso > 0 ? "#b45309" : "#cbd5e1" }}>{fmtMoeda(a.excesso, moeda)}</td>
          </tr>
        );
      })}

      {c.subclasses.length === 0 && c.ativos.length === 0 && (
        <tr>
          <td style={{ ...tdStyle, paddingLeft: "30px", color: "#94a3b8", fontSize: "12px" }} colSpan={8}>
            Sem subclasses nem metas individuais{negrito ? ` na classe ${infoLabel}` : ""}.
          </td>
        </tr>
      )}
    </>
  );
}

/* ── Estilos ── */
const thStyle: React.CSSProperties = {
  padding: "9px 10px", fontSize: "11px", fontWeight: 700, color: "#64748b", textTransform: "uppercase", letterSpacing: "0.3px",
};
const tdStyle: React.CSSProperties = { padding: "9px 10px", fontSize: "13px" };
const tdStyleRight: React.CSSProperties = { ...tdStyle, textAlign: "right", fontWeight: 600, color: "#0f172a", whiteSpace: "nowrap" };
const tdStyleRightMuted: React.CSSProperties = { ...tdStyle, textAlign: "right", color: "#475569", whiteSpace: "nowrap" };

/** Déficit (negativo) em azul; excesso (positivo) em âmbar; zero neutro. */
function corDiferenca(diferenca: number): React.CSSProperties {
  if (diferenca < -0.005) return { color: "#1d4ed8" };
  if (diferenca > 0.005) return { color: "#b45309" };
  return { color: "#94a3b8" };
}
