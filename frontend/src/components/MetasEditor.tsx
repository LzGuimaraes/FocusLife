import type { CategoriaInvestimento } from "../types/planejamento";
import AtivoAutocomplete, { type AtivoCadastro } from "./AtivoAutocomplete";
import { CATEGORIAS, catInfo, classeSugerida, fmtPercentual } from "../utils/percentual";
import { novaChave } from "../utils/chaves";
import { boxStyle, controlStyle, linkBtnStyle, miniLabel } from "./FormStyles";
import { type ClasseDraft } from "./CarteiraIdealEditor";

/* ══════════════════════════════════════════════════════════════════════
   Editor das metas individuais por ativo (Módulo 1) + prioridade manual
   (Módulo 7). O ativo é escolhido no catálogo (ticker), com sugestão de
   classe a partir do tipo do catálogo — que o usuário pode alterar.
   ══════════════════════════════════════════════════════════════════════ */

export interface MetaDraft {
  key: string;
  ativo_cadastro_id: string;
  ticker: string;
  classe: CategoriaInvestimento;
  subclasse_nome: string;
  percentual_ideal: string;
  prioridade_manual: string;
}

export function novaMetaDraft(classe: CategoriaInvestimento): MetaDraft {
  return {
    key: novaChave(),
    ativo_cadastro_id: "",
    ticker: "",
    classe,
    subclasse_nome: "",
    percentual_ideal: "",
    prioridade_manual: "0",
  };
}

interface Props {
  metas: MetaDraft[];
  classes: ClasseDraft[];
  onChange: (metas: MetaDraft[]) => void;
}

export default function MetasEditor({ metas, classes, onChange }: Props) {
  const atualizar = (key: string, patch: Partial<MetaDraft>) =>
    onChange(metas.map(m => (m.key === key ? { ...m, ...patch } : m)));

  const subclassesDaClasse = (classe: CategoriaInvestimento) =>
    classes.find(c => c.classe === classe)?.subclasses ?? [];

  const totalPorClasse = (classe: CategoriaInvestimento) =>
    metas.filter(m => m.classe === classe)
      .reduce((s, m) => s + (parseFloat(m.percentual_ideal) || 0), 0);

  const classePadrao = classes[0]?.classe ?? "ACOES";

  return (
    <div style={boxStyle}>
      <div style={{ marginBottom: "14px" }}>
        <h3 style={{ fontSize: "15px", fontWeight: 700, color: "#0f172a", margin: 0 }}>Metas de ativos</h3>
        <p style={{ fontSize: "12px", color: "#64748b", margin: "2px 0 0" }}>
          Quanto cada ativo deve representar da carteira e a prioridade de aporte (0 a 10).
        </p>
      </div>

      {metas.length === 0 && (
        <p style={{ fontSize: "13px", color: "#94a3b8", margin: "0 0 12px" }}>
          Nenhuma meta individual definida. É opcional: as classes já orientam a distribuição.
        </p>
      )}

      <div style={{ display: "flex", flexDirection: "column", gap: "8px" }}>
        {metas.map(m => {
          const info = catInfo(m.classe);
          const subs = subclassesDaClasse(m.classe);
          const somaClasse = totalPorClasse(m.classe);
          return (
            <div key={m.key} style={{ border: "1px solid #e2e8f0", borderRadius: "10px", padding: "10px 12px", background: "#fcfdff" }}>
              <div style={{ display: "flex", gap: "8px", alignItems: "flex-end", flexWrap: "wrap" }}>
                <div style={{ minWidth: "190px", flex: "1 1 190px" }}>
                  <label style={miniLabel}>Ativo (ticker)</label>
                  <AtivoAutocomplete
                    value={m.ticker}
                    onSelect={(a: AtivoCadastro) => atualizar(m.key, {
                      ativo_cadastro_id: a.id,
                      ticker: a.nome,
                      classe: m.ativo_cadastro_id ? m.classe : classeSugerida(a.tipo),
                    })}
                  />
                </div>

                <div>
                  <label style={miniLabel}>Classe</label>
                  <select value={m.classe} aria-label="Classe da meta"
                    onChange={e => atualizar(m.key, {
                      classe: e.target.value as CategoriaInvestimento,
                      subclasse_nome: "",
                    })}
                    style={{ ...controlStyle, minWidth: "130px" }}>
                    {!CATEGORIAS.some(c => c.key === m.classe) && <option value={m.classe}>{m.classe}</option>}
                    {CATEGORIAS.map(c => <option key={c.key} value={c.key}>{c.icon} {c.label}</option>)}
                  </select>
                </div>

                <div>
                  <label style={miniLabel}>Subclasse</label>
                  <select value={m.subclasse_nome} aria-label="Subclasse da meta"
                    onChange={e => atualizar(m.key, { subclasse_nome: e.target.value })}
                    disabled={subs.length === 0}
                    style={{ ...controlStyle, minWidth: "130px", opacity: subs.length === 0 ? 0.6 : 1 }}>
                    <option value="">—</option>
                    {subs.map(s => <option key={s.key} value={s.nome}>{s.nome || "(sem nome)"}</option>)}
                  </select>
                </div>

                <div>
                  <label style={miniLabel}>% ideal</label>
                  <div style={{ display: "flex", alignItems: "center", gap: "4px" }}>
                    <input value={m.percentual_ideal} inputMode="decimal" placeholder="0,00"
                      aria-label={`Percentual ideal de ${m.ticker || "ativo"}`}
                      onChange={e => atualizar(m.key, { percentual_ideal: e.target.value.replace(/[^0-9.,]/g, "") })}
                      style={{ ...controlStyle, width: "85px", textAlign: "right" }} />
                    <span style={{ fontSize: "13px", color: "#64748b" }}>%</span>
                  </div>
                </div>

                <div>
                  <label style={miniLabel}>Prioridade</label>
                  <input value={m.prioridade_manual} inputMode="numeric" placeholder="0"
                    aria-label={`Prioridade manual de ${m.ticker || "ativo"}`}
                    onChange={e => atualizar(m.key, { prioridade_manual: e.target.value.replace(/[^0-9]/g, "") })}
                    style={{ ...controlStyle, width: "70px", textAlign: "right" }} />
                </div>

                <button type="button" onClick={() => onChange(metas.filter(x => x.key !== m.key))}
                  aria-label="Remover meta" style={{ ...linkBtnStyle, borderStyle: "solid", color: "#ef4444", borderColor: "#fecaca" }}>🗑</button>
              </div>

              <div style={{ display: "flex", gap: "10px", flexWrap: "wrap", marginTop: "6px" }}>
                <span style={{ fontSize: "11px", color: "#94a3b8" }}>
                  {m.ticker ? `Ticker: ${m.ticker}` : "Selecione um ativo do catálogo para salvar esta meta"}
                </span>
                {somaClasse > 0 && (
                  <span style={{ fontSize: "11px", color: "#64748b" }}>
                    Metas de {info.label}: {fmtPercentual(somaClasse)}
                  </span>
                )}
              </div>
            </div>
          );
        })}
      </div>

      <div style={{ marginTop: "12px" }}>
        <button type="button" onClick={() => onChange([...metas, novaMetaDraft(classePadrao)])} style={linkBtnStyle}>
          + Adicionar meta
        </button>
        <span style={{ fontSize: "11px", color: "#94a3b8", marginLeft: "10px" }}>
          Só é possível salvar metas de ativos que existem no catálogo.
        </span>
      </div>
    </div>
  );
}
