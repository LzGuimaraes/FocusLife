import type { CategoriaInvestimento } from "../types/planejamento";
import AtivoAutocomplete, { type AtivoCadastro } from "./AtivoAutocomplete";
import { CATEGORIAS, catInfo, classeSugerida, fmtMoeda, fmtPercentual } from "../utils/percentual";
import { novaChave } from "../utils/chaves";
import { boxStyle, controlStyle, iconBtn, linkBtnStyle, miniLabel } from "./FormStyles";
import { apenasNumero } from "../utils/numeros";
import { type ClasseDraft } from "./CarteiraIdealEditor";

/* ══════════════════════════════════════════════════════════════════════
   Metas por ativo (Módulos 1 e 7).

   A lista parte dos ATIVOS QUE O USUÁRIO JÁ TEM na carteira (posições reais):
   cada linha já vem com o valor e o percentual atuais, e o usuário só decide
   o alvo. Nada de recadastrar ativos nem de planejar num espaço paralelo —
   o catálogo só é usado para ativos que ele ainda pretende comprar.
   ══════════════════════════════════════════════════════════════════════ */

export interface MetaDraft {
  key: string;
  ativo_cadastro_id: string;
  ticker: string;
  classe: CategoriaInvestimento;
  subclasse_nome: string;
  percentual_ideal: string;
  prioridade_manual: string;
  /** Linha marcada participa do payload (as desmarcadas perdem a meta). */
  incluir: boolean;
  /** "carteira" = ativo que já existe; "planejado" = ativo que ainda vai comprar. */
  origem: "carteira" | "planejado";

  /* ── Situação atual (informativo, vem das posições) ── */
  percentual_atual: number | null;
  valor_atual: number | null;
}

export function novaMetaPlanejada(classe: CategoriaInvestimento): MetaDraft {
  return {
    key: novaChave(),
    ativo_cadastro_id: "",
    ticker: "",
    classe,
    subclasse_nome: "",
    percentual_ideal: "",
    prioridade_manual: "0",
    incluir: true,
    origem: "planejado",
    percentual_atual: null,
    valor_atual: null,
  };
}

interface Props {
  metas: MetaDraft[];
  classes: ClasseDraft[];
  moeda: string;
  valorTotal: number;
  posicoesSemCatalogo: number;
  onChange: (metas: MetaDraft[]) => void;
}

export default function MetasEditor({
  metas, classes, moeda, valorTotal, posicoesSemCatalogo, onChange,
}: Props) {
  const atualizar = (key: string, patch: Partial<MetaDraft>) =>
    onChange(metas.map(m => (m.key === key ? { ...m, ...patch } : m)));

  const subclassesDaClasse = (classe: CategoriaInvestimento) =>
    classes.find(c => c.classe === classe)?.subclasses ?? [];

  const incluídas = metas.filter(m => m.incluir && m.ativo_cadastro_id);
  const somaIdeal = incluídas.reduce((s, m) => s + (parseFloat(m.percentual_ideal.replace(",", ".")) || 0), 0);

  /** Atalho: registrar a distribuição atual como alvo (um clique, zero digitação). */
  const usarDistribuicaoAtual = () => {
    onChange(metas.map(m => {
      if (m.origem !== "carteira" || m.percentual_atual == null) return m;
      return { ...m, incluir: true, percentual_ideal: m.percentual_atual.toFixed(2).replace(".", ",") };
    }));
  };

  const limparAlvos = () =>
    onChange(metas.map(m => (m.origem === "carteira"
      ? { ...m, incluir: false, percentual_ideal: "", prioridade_manual: "0", subclasse_nome: "" }
      : m)));

  const daCarteira = metas.filter(m => m.origem === "carteira");
  const planejados = metas.filter(m => m.origem === "planejado");
  const classePadrao = classes[0]?.classe ?? "ACOES";

  return (
    <div style={boxStyle}>
      <div style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-start", flexWrap: "wrap", gap: "10px", marginBottom: "12px" }}>
        <div>
          <h3 style={{ fontSize: "15px", fontWeight: 700, color: "#0f172a", margin: 0 }}>
            Metas dos seus ativos
          </h3>
          <p style={{ fontSize: "12px", color: "#64748b", margin: "2px 0 0" }}>
            Marque os ativos que devem ter meta e informe o alvo. O percentual atual já está preenchido a partir da sua carteira.
          </p>
        </div>
        <span style={{ fontSize: "12px", fontWeight: 700, padding: "5px 12px", borderRadius: "9999px", background: "#eef2ff", color: "#4338ca" }}>
          {incluídas.length} meta(s) · soma {fmtPercentual(somaIdeal)}
        </span>
      </div>

      {daCarteira.length === 0 && planejados.length === 0 && (
        <p style={{ fontSize: "13px", color: "#94a3b8", margin: "0 0 12px" }}>
          Você ainda não tem ativos cadastrados nesta carteira. Cadastre-os em Finanças (ou adicione abaixo um ativo que pretende comprar).
        </p>
      )}

      {/* ── Ativos que o usuário já tem ── */}
      {daCarteira.length > 0 && (
        <>
          <div style={{ display: "flex", gap: "8px", flexWrap: "wrap", marginBottom: "10px" }}>
            <button type="button" onClick={usarDistribuicaoAtual} style={linkBtnStyle}>
              ⚡ Usar minha distribuição atual como alvo
            </button>
            <button type="button" onClick={limparAlvos} style={{ ...linkBtnStyle, color: "#64748b", borderColor: "#e2e8f0" }}>
              Limpar alvos
            </button>
          </div>

          <div style={{ overflowX: "auto", marginBottom: "6px" }}>
            <table style={{ width: "100%", borderCollapse: "collapse", minWidth: "760px" }}>
              <thead>
                <tr style={{ background: "#f8fafc" }}>
                  <th style={th}>Meta?</th>
                  <th style={{ ...th, textAlign: "left" }}>Ativo</th>
                  <th style={{ ...th, textAlign: "right" }}>Hoje</th>
                  <th style={{ ...th, textAlign: "right" }}>% atual</th>
                  <th style={{ ...th, textAlign: "right" }}>% ideal</th>
                  <th style={{ ...th, textAlign: "right" }}>Valor ideal</th>
                  <th style={{ ...th, textAlign: "center" }}>Prioridade</th>
                  <th style={{ ...th, textAlign: "left" }}>Subclasse</th>
                </tr>
              </thead>
              <tbody>
                {daCarteira.map(m => {
                  const info = catInfo(m.classe);
                  const alvo = parseFloat(m.percentual_ideal.replace(",", "."));
                  const valorIdeal = Number.isFinite(alvo) ? (valorTotal * alvo) / 100 : null;
                  const subs = subclassesDaClasse(m.classe);
                  return (
                    <tr key={m.key} style={{ borderTop: "1px solid #f1f5f9", opacity: m.incluir ? 1 : 0.55 }}>
                      <td style={{ ...td, textAlign: "center" }}>
                        <input type="checkbox" checked={m.incluir}
                          aria-label={`Definir meta para ${m.ticker}`}
                          onChange={e => atualizar(m.key, { incluir: e.target.checked })}
                          style={{ width: "17px", height: "17px", accentColor: "#6366f1", cursor: "pointer" }} />
                      </td>
                      <td style={{ ...td, fontWeight: 700, color: "#0f172a" }}>
                        {m.ticker}
                        <span style={{ marginLeft: "8px", fontSize: "11px", fontWeight: 600, color: info.color, background: info.bg, padding: "2px 8px", borderRadius: "9999px" }}>
                          {info.icon} {info.label}
                        </span>
                      </td>
                      <td style={{ ...td, textAlign: "right", color: "#475569", whiteSpace: "nowrap" }}>
                        {fmtMoeda(m.valor_atual, moeda)}
                      </td>
                      <td style={{ ...td, textAlign: "right", color: "#475569" }}>
                        {m.percentual_atual != null ? fmtPercentual(m.percentual_atual) : "—"}
                      </td>
                      <td style={{ ...td, textAlign: "right" }}>
                        <div style={{ display: "inline-flex", alignItems: "center", gap: "4px" }}>
                          <input value={m.percentual_ideal} disabled={!m.incluir} inputMode="decimal"
                            placeholder="0,00" aria-label={`Percentual ideal de ${m.ticker}`}
                            onChange={e => atualizar(m.key, { percentual_ideal: apenasNumero(e.target.value) })}
                            style={{ ...controlStyle, width: "80px", textAlign: "right", opacity: m.incluir ? 1 : 0.5 }} />
                          <span style={{ fontSize: "12px", color: "#64748b" }}>%</span>
                        </div>
                      </td>
                      <td style={{ ...td, textAlign: "right", color: m.incluir && valorIdeal != null ? "#4338ca" : "#cbd5e1", fontWeight: 600, whiteSpace: "nowrap" }}>
                        {m.incluir && valorIdeal != null ? fmtMoeda(valorIdeal, moeda) : "—"}
                      </td>
                      <td style={{ ...td, textAlign: "center" }}>
                        <input value={m.prioridade_manual} disabled={!m.incluir} inputMode="numeric"
                          aria-label={`Prioridade de ${m.ticker}`}
                          onChange={e => atualizar(m.key, { prioridade_manual: e.target.value.replace(/[^0-9]/g, "") })}
                          title="0 a 10 — desempata a ordem dos aportes"
                          style={{ ...controlStyle, width: "60px", textAlign: "right", opacity: m.incluir ? 1 : 0.5 }} />
                      </td>
                      <td style={{ ...td }}>
                        <select value={m.subclasse_nome} disabled={!m.incluir || subs.length === 0}
                          aria-label={`Subclasse de ${m.ticker}`}
                          onChange={e => atualizar(m.key, { subclasse_nome: e.target.value })}
                          style={{ ...controlStyle, minWidth: "120px", opacity: (!m.incluir || subs.length === 0) ? 0.5 : 1 }}>
                          <option value="">—</option>
                          {subs.map(s => <option key={s.key} value={s.nome}>{s.nome || "(sem nome)"}</option>)}
                        </select>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        </>
      )}

      {/* ── Ativos planejados (que ele ainda não tem) ── */}
      {planejados.length > 0 && (
        <div style={{ marginTop: "14px", borderTop: "1px solid #f1f5f9", paddingTop: "12px" }}>
          <p style={{ ...miniLabel, marginBottom: "8px" }}>
            Ativos que você ainda vai comprar (não estão na carteira hoje)
          </p>
          <div style={{ display: "flex", flexDirection: "column", gap: "10px" }}>
            {planejados.map(m => {
              const subs = subclassesDaClasse(m.classe);
              return (
                <div key={m.key} style={{ border: "1px solid #e2e8f0", borderRadius: "10px", padding: "10px 12px", background: "#fcfdff", display: "flex", gap: "10px", alignItems: "flex-end", flexWrap: "wrap" }}>
                  <div style={{ minWidth: "190px", flex: "1 1 190px" }}>
                    <label style={miniLabel}>Ativo (catálogo)</label>
                    <AtivoAutocomplete value={m.ticker}
                      onSelect={(a: AtivoCadastro) => atualizar(m.key, {
                        ativo_cadastro_id: a.id,
                        ticker: a.nome,
                        classe: classeSugerida(a.tipo),
                      })} />
                  </div>
                  <div>
                    <label style={miniLabel}>Classe</label>
                    <select value={m.classe} aria-label="Classe da meta"
                      onChange={e => atualizar(m.key, { classe: e.target.value as CategoriaInvestimento, subclasse_nome: "" })}
                      style={{ ...controlStyle, minWidth: "140px" }}>
                      {CATEGORIAS.map(c => <option key={c.key} value={c.key}>{c.icon} {c.label}</option>)}
                    </select>
                  </div>
                  <div>
                    <label style={miniLabel}>Subclasse</label>
                    <select value={m.subclasse_nome} disabled={subs.length === 0} aria-label="Subclasse da meta"
                      onChange={e => atualizar(m.key, { subclasse_nome: e.target.value })}
                      style={{ ...controlStyle, minWidth: "120px", opacity: subs.length === 0 ? 0.5 : 1 }}>
                      <option value="">—</option>
                      {subs.map(s => <option key={s.key} value={s.nome}>{s.nome || "(sem nome)"}</option>)}
                    </select>
                  </div>
                  <div>
                    <label style={miniLabel}>% ideal</label>
                    <div style={{ display: "flex", alignItems: "center", gap: "4px" }}>
                      <input value={m.percentual_ideal} inputMode="decimal" placeholder="0,00"
                        aria-label={`Percentual ideal de ${m.ticker || "ativo planejado"}`}
                        onChange={e => atualizar(m.key, { percentual_ideal: apenasNumero(e.target.value) })}
                        style={{ ...controlStyle, width: "85px", textAlign: "right" }} />
                      <span style={{ fontSize: "12px", color: "#64748b" }}>%</span>
                    </div>
                  </div>
                  <div>
                    <label style={miniLabel}>Prioridade</label>
                    <input value={m.prioridade_manual} inputMode="numeric" aria-label="Prioridade"
                      onChange={e => atualizar(m.key, { prioridade_manual: e.target.value.replace(/[^0-9]/g, "") })}
                      style={{ ...controlStyle, width: "65px", textAlign: "right" }} />
                  </div>
                  <button type="button" aria-label="Remover ativo planejado"
                    onClick={() => onChange(metas.filter(x => x.key !== m.key))}
                    style={{ ...iconBtn(false), color: "#ef4444" }}>
                    🗑
                  </button>
                </div>
              );
            })}
          </div>
        </div>
      )}

      <div style={{ marginTop: "12px", display: "flex", gap: "10px", alignItems: "center", flexWrap: "wrap" }}>
        <button type="button" onClick={() => onChange([...metas, novaMetaPlanejada(classePadrao)])} style={linkBtnStyle}>
          + Ativo que ainda não tenho
        </button>
        {posicoesSemCatalogo > 0 && (
          <span style={{ fontSize: "11px", color: "#b45309" }}>
            ⚠ {posicoesSemCatalogo} posição(ões) sem vínculo com o catálogo (ex.: renda fixa) não podem ter meta por ticker —
            elas continuam contando na classe.
          </span>
        )}
      </div>
    </div>
  );
}

const th: React.CSSProperties = {
  padding: "8px 10px", fontSize: "10px", fontWeight: 700, color: "#64748b",
  textTransform: "uppercase", letterSpacing: "0.3px",
};
const td: React.CSSProperties = { padding: "8px 10px", fontSize: "13px" };
