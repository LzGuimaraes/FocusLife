import { useState } from "react";
import { toast } from "sonner";
import type { CategoriaInvestimento } from "../types/planejamento";
import { CATEGORIAS, catInfo, fmtPercentual, somaFechada } from "../utils/percentual";
import { novaChave } from "../utils/chaves";
import { textoParaNum } from "../utils/numeros";
import { MODELOS_PLANEJAMENTO, type ModeloPlanejamento } from "../utils/modelosPlanejamento";
import {
  chip, controlStyle, iconBtn, linkBtnStyle, miniLabel, numGrande, numInput, numMedio,
  overline, sectionCard, sectionSubtitle, sectionTitle, stepperBtn,
} from "./FormStyles";

/* ══════════════════════════════════════════════════════════════════════
   Editor de classes e subclasses da Carteira Ideal (Módulo 1).

   VERSÃO EM CARDS: cada classe é um card com identidade visual própria
   (ícone + cor), o par ATUAL × ALVO lado a lado, o seletor numérico com
   +/- e uma sanfona de subclasses. A tela pode ser lida "de longe": o alvo
   é o número grande, o atual é o número médio, e a barra usa a MESMA escala
   (100%) em todos os cards, então eles são comparáveis entre si.

   Trabalha com "drafts" (percentuais como string) para que a digitação
   seja livre; a conversão para número acontece no save da página.
   Nenhuma regra de negócio mora aqui: as funções só editam o rascunho.
   ══════════════════════════════════════════════════════════════════════ */

export interface SetorDraft {
  key: string;
  /** ID no backend (null enquanto o setor não foi salvo). */
  id?: number | null;
  nome: string;
  percentual_ideal: string;
  tolerancia: string;
  limite_maximo: string;
}

export interface SubclasseDraft {
  key: string;
  /** ID no backend (null enquanto a subclasse não foi salva). */
  id?: number | null;
  nome: string;
  percentual_ideal: string;
  /** Faixa (p.p. da classe) de equilíbrio e teto do aporte. */
  tolerancia: string;
  limite_maximo: string;
  /** Setores (opcional): o % de cada um é uma fatia DESTA subclasse. */
  setores: SetorDraft[];
}

export interface ClasseDraft {
  key: string;
  classe: CategoriaInvestimento;
  percentual_ideal: string;
  tolerancia: string;
  limite_maximo: string;
  subclasses: SubclasseDraft[];
}

export { novaChave };

export function novaClasseDraft(classe: CategoriaInvestimento): ClasseDraft {
  return { key: novaChave(), classe, percentual_ideal: "", tolerancia: "", limite_maximo: "", subclasses: [] };
}

export function novaSubclasseDraft(): SubclasseDraft {
  return { key: novaChave(), nome: "", percentual_ideal: "", tolerancia: "", limite_maximo: "", setores: [] };
}

export function novoSetorDraft(): SetorDraft {
  return { key: novaChave(), nome: "", percentual_ideal: "", tolerancia: "", limite_maximo: "" };
}

/**
 * Leva a soma das classes para exatamente 100% sem o usuário fazer conta.
 *
 * MESMA REGRA de antes (agora chamada pelo cabeçalho fixo da página):
 *   • passou de 100% → reduz tudo proporcionalmente;
 *   • faltou          → completa em "Outros" (ou cria a classe, se não existir).
 * Devolve as classes novas, ou `null` quando não há nada a ajustar.
 */
export function ajustarClassesPara100(classes: ClasseDraft[]): ClasseDraft[] | null {
  if (classes.length === 0) return null;
  const soma = classes.reduce((s, c) => s + textoParaNum(c.percentual_ideal), 0);
  if (somaFechada(soma)) return null;

  if (soma > 100) {
    const fator = 100 / soma;
    return classes.map(c => ({
      ...c,
      percentual_ideal: (textoParaNum(c.percentual_ideal) * fator).toFixed(2).replace(".", ","),
    }));
  }
  const falta = 100 - soma;
  const indiceOutros = classes.findIndex(c => c.classe === "OUTROS");
  if (indiceOutros >= 0) {
    return classes.map((c, i) => (i === indiceOutros
      ? { ...c, percentual_ideal: (textoParaNum(c.percentual_ideal) + falta).toFixed(2).replace(".", ",") }
      : c));
  }
  return [...classes, {
    ...novaClasseDraft("OUTROS"),
    percentual_ideal: falta.toFixed(2).replace(".", ","),
  }];
}

interface Props {
  classes: ClasseDraft[];
  onChange: (classes: ClasseDraft[]) => void;
  /** % atual de cada classe na carteira (vem do comparativo) — só leitura. */
  atualPorClasse?: Partial<Record<CategoriaInvestimento, number>>;
}

export default function CarteiraIdealEditor({ classes, onChange, atualPorClasse = {} }: Props) {
  /** Quais sanfonas de subclasse estão abertas (por key da classe). */
  const [abertas, setAbertas] = useState<Record<string, boolean>>({});

  // ATENÇÃO: usar `textoParaNum` (e não parseFloat) porque o usuário digita com
  // vírgula — `parseFloat("12,5")` devolveria 12 e a soma sairia errada.
  const soma = classes.reduce((s, c) => s + textoParaNum(c.percentual_ideal), 0);
  const usadas = classes.map(c => c.classe);
  const disponiveis = CATEGORIAS.filter(c => !usadas.includes(c.key));

  const alternarSanfona = (key: string) =>
    setAbertas(prev => ({ ...prev, [key]: !prev[key] }));

  /** Aplica um modelo pronto como ponto de partida (substitui as classes atuais). */
  const aplicarModelo = (modelo: ModeloPlanejamento) => {
    onChange(modelo.classes.map(c => ({
      ...novaClasseDraft(c.classe),
      percentual_ideal: c.percentual.toFixed(2).replace(".", ","),
    })));
    toast.success(`Modelo "${modelo.nome}" aplicado — revise os percentuais e ajuste o que quiser.`);
  };

  const atualizarClasse = (key: string, patch: Partial<ClasseDraft>) =>
    onChange(classes.map(c => (c.key === key ? { ...c, ...patch } : c)));

  /** +/- do seletor numérico do alvo da classe (passo de 1 p.p., entre 0 e 100). */
  const passoClasse = (c: ClasseDraft, delta: number) => {
    const novo = Math.min(100, Math.max(0, textoParaNum(c.percentual_ideal) + delta));
    atualizarClasse(c.key, { percentual_ideal: novo.toFixed(2).replace(".", ",") });
  };

  const moverClasse = (index: number, delta: number) => {
    const destino = index + delta;
    if (destino < 0 || destino >= classes.length) return;
    const copia = [...classes];
    [copia[index], copia[destino]] = [copia[destino], copia[index]];
    onChange(copia);
  };

  const adicionarSubclasse = (key: string) =>
    onChange(classes.map(c =>
      c.key === key ? { ...c, subclasses: [...c.subclasses, novaSubclasseDraft()] } : c));

  const removerSubclasse = (key: string, subKey: string) =>
    onChange(classes.map(c =>
      c.key === key ? { ...c, subclasses: c.subclasses.filter(s => s.key !== subKey) } : c));

  const atualizarSubclasse = (key: string, subKey: string, patch: Partial<SubclasseDraft>) =>
    onChange(classes.map(c =>
      c.key === key
        ? { ...c, subclasses: c.subclasses.map(s => (s.key === subKey ? { ...s, ...patch } : s)) }
        : c));

  const adicionarSetor = (key: string, subKey: string) =>
    onChange(classes.map(c => c.key === key
      ? { ...c, subclasses: c.subclasses.map(s => s.key === subKey
          ? { ...s, setores: [...s.setores, novoSetorDraft()] } : s) }
      : c));

  const removerSetor = (key: string, subKey: string, setorKey: string) =>
    onChange(classes.map(c => c.key === key
      ? { ...c, subclasses: c.subclasses.map(s => s.key === subKey
          ? { ...s, setores: s.setores.filter(st => st.key !== setorKey) } : s) }
      : c));

  const atualizarSetor = (key: string, subKey: string, setorKey: string, patch: Partial<SetorDraft>) =>
    onChange(classes.map(c => c.key === key
      ? { ...c, subclasses: c.subclasses.map(s => s.key === subKey
          ? { ...s, setores: s.setores.map(st => (st.key === setorKey ? { ...st, ...patch } : st)) } : s) }
      : c));

  return (
    <section style={sectionCard}>
      <header style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-start", flexWrap: "wrap", gap: "12px" }}>
        <div style={{ maxWidth: "560px" }}>
          <h3 style={sectionTitle}>Classes e subclasses</h3>
          <p style={sectionSubtitle}>
            As classes precisam somar 100%. As subclasses são um detalhamento dentro da classe —
            e é nelas que ficam a tolerância e o limite de concentração.
          </p>
        </div>
        {classes.length > 0 && (
          <span style={chip(somaFechada(soma) ? "#047857" : "#b91c1c",
            somaFechada(soma) ? "#ecfdf5" : "#fef2f2")}>
            {somaFechada(soma) ? "✓" : "⚠"} Soma {fmtPercentual(soma)} / 100%
          </span>
        )}
      </header>

      {classes.length === 0 && (
        <div style={{ marginTop: "18px", background: "#f8fafc", border: "1px dashed #e2e8f0", borderRadius: "12px", padding: "16px 18px" }}>
          <p style={{ fontSize: "13px", fontWeight: 700, color: "#475569", margin: "0 0 10px" }}>
            Comece por um modelo pronto (você edita tudo depois) ou monte do zero:
          </p>
          <div style={{ display: "flex", gap: "8px", flexWrap: "wrap" }}>
            {MODELOS_PLANEJAMENTO.map(m => (
              <button key={m.nome} type="button" onClick={() => aplicarModelo(m)}
                title={m.descricao}
                style={{ ...linkBtnStyle, borderStyle: "solid", borderColor: "#c7d2fe", color: "#4338ca", background: "white" }}>
                {m.icon} {m.nome}
              </button>
            ))}
          </div>
        </div>
      )}

      {/* ── Grid de cards: uma classe por card ── */}
      <div style={{
        display: "grid", gap: "16px", marginTop: classes.length > 0 ? "19px" : "16px",
        gridTemplateColumns: "repeat(auto-fill, minmax(min(100%, 320px), 1fr))",
      }}>
        {classes.map((c, index) => {
          const info = catInfo(c.classe);
          const alvo = textoParaNum(c.percentual_ideal);
          const atual = atualPorClasse[c.classe];
          const somaSub = c.subclasses.reduce((s, x) => s + textoParaNum(x.percentual_ideal), 0);
          // O percentual da subclasse é uma FATIA DA CLASSE: a soma dela fecha em
          // 100% da CLASSE (nunca em 100% da carteira). Comparar com o alvo da
          // classe aqui dava alarme falso em toda classe com menos de 100%.
          const subExcede = c.subclasses.length > 0 && somaSub > 100.01;
          const aberta = abertas[c.key] ?? false;
          const desvio = atual != null ? atual - alvo : null;

          return (
            <article key={c.key} style={{
              border: "1px solid #e9eef5", borderRadius: "14px", background: "white",
              display: "flex", flexDirection: "column", overflow: "hidden",
            }}>
              {/* Cabeçalho do card: identidade da classe */}
              <div style={{
                display: "flex", alignItems: "center", gap: "9px",
                padding: "12px 14px", background: info.bg, borderBottom: "1px solid rgba(15,23,42,0.05)",
              }}>
                <span style={{ fontSize: "17px", lineHeight: 1 }}>{info.icon}</span>
                <select value={c.classe} aria-label={`Classe ${info.label}`}
                  onChange={e => atualizarClasse(c.key, { classe: e.target.value as CategoriaInvestimento })}
                  style={{ ...controlStyle, border: "none", background: "transparent", padding: "2px 4px", fontWeight: 800, fontSize: "13.5px", color: info.color, flex: 1, minWidth: 0 }}>
                  {CATEGORIAS.filter(op => op.key === c.classe || !usadas.includes(op.key))
                    .map(op => <option key={op.key} value={op.key}>{op.label}</option>)}
                </select>
                <div style={{ display: "flex", gap: "3px" }}>
                  <button type="button" onClick={() => moverClasse(index, -1)} disabled={index === 0}
                    aria-label={`Mover ${info.label} para cima`} style={iconBtn(index === 0)}>↑</button>
                  <button type="button" onClick={() => moverClasse(index, 1)} disabled={index === classes.length - 1}
                    aria-label={`Mover ${info.label} para baixo`} style={iconBtn(index === classes.length - 1)}>↓</button>
                  <button type="button" onClick={() => onChange(classes.filter(x => x.key !== c.key))}
                    aria-label={`Remover ${info.label}`} style={{ ...iconBtn(false), color: "#ef4444" }}>🗑</button>
                </div>
              </div>

              <div style={{ padding: "16px 16px 14px", display: "flex", flexDirection: "column", gap: "14px", flex: 1 }}>
                {/* Comparação Atual × Alvo */}
                <div style={{ display: "flex", alignItems: "flex-end", justifyContent: "space-between", gap: "12px" }}>
                  <div>
                    <span style={overline}>Hoje</span>
                    <p style={{ ...numMedio, color: "#64748b", margin: "3px 0 0" }}>
                      {atual != null ? fmtPercentual(atual) : "—"}
                    </p>
                  </div>
                  <div style={{ textAlign: "right" }}>
                    <span style={overline}>Alvo</span>
                    <p style={{ ...numGrande, margin: "3px 0 0" }}>{fmtPercentual(alvo)}</p>
                  </div>
                </div>

                {/* Barra na mesma escala (100%) — os cards ficam comparáveis */}
                <div>
                  <div style={{ position: "relative", height: "10px", background: "#f1f5f9", borderRadius: "6px" }}>
                    <div style={{
                      width: `${Math.min(100, atual ?? 0)}%`, height: "100%", borderRadius: "6px",
                      background: info.color, opacity: 0.9, transition: "width 0.35s ease",
                    }} />
                    <div title={`Alvo: ${fmtPercentual(alvo)}`} style={{
                      position: "absolute", left: `calc(${Math.min(100, alvo)}% - 2px)`, top: "-4px",
                      width: "4px", height: "18px", borderRadius: "3px", background: "#0f172a",
                    }} />
                  </div>
                  <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginTop: "5px" }}>
                    <span style={{ ...overline, letterSpacing: "0.3px" }}>0%</span>
                    {desvio != null && (
                      <span style={chip(
                        Math.abs(desvio) < 0.005 ? "#047857" : desvio > 0 ? "#b45309" : "#1d4ed8",
                        Math.abs(desvio) < 0.005 ? "#ecfdf5" : desvio > 0 ? "#fffbeb" : "#eff6ff")}>
                        {Math.abs(desvio) < 0.005
                          ? "na meta"
                          : `${desvio > 0 ? "+" : "−"}${Math.abs(desvio).toFixed(2).replace(".", ",")} p.p.`}
                      </span>
                    )}
                    <span style={{ ...overline, letterSpacing: "0.3px" }}>100%</span>
                  </div>
                </div>

                {/* Seletor numérico com +/- */}
                <div style={{ display: "flex", alignItems: "center", gap: "8px", flexWrap: "wrap" }}>
                  <button type="button" onClick={() => passoClasse(c, -1)} disabled={alvo <= 0}
                    aria-label={`Diminuir o alvo de ${info.label}`} style={stepperBtn(alvo <= 0)}>−</button>
                  <div style={{ display: "flex", alignItems: "center", gap: "4px" }}>
                    <input value={c.percentual_ideal} inputMode="decimal" placeholder="0,00"
                      aria-label={`Percentual ideal de ${info.label}`}
                      onChange={e => atualizarClasse(c.key, { percentual_ideal: apenasNumero(e.target.value) })}
                      style={numInput} />
                    <span style={{ fontSize: "14px", fontWeight: 700, color: "#94a3b8" }}>%</span>
                  </div>
                  <button type="button" onClick={() => passoClasse(c, 1)} disabled={alvo >= 100}
                    aria-label={`Aumentar o alvo de ${info.label}`} style={stepperBtn(alvo >= 100)}>+</button>
                </div>

                {/* Tolerância e limite de concentração */}
                <div style={{ display: "flex", gap: "14px", alignItems: "flex-end" }}
                  title="Tolerância (p.p.): dentro desta faixa a classe conta como EQUILIBRADA — e é o que dá espaço ao aporte quando a meta já foi atingida. Limite máximo: acima dele a classe não recebe novos aportes.">
                  <div>
                    <label style={miniLabel}>Tolerância ±</label>
                    <div style={{ display: "flex", alignItems: "center", gap: "4px" }}>
                      <input value={c.tolerancia} inputMode="decimal" placeholder="0"
                        aria-label={`Tolerância de ${info.label} em pontos percentuais`}
                        onChange={e => atualizarClasse(c.key, { tolerancia: apenasNumero(e.target.value) })}
                        style={{ ...controlStyle, width: "68px", textAlign: "right", fontSize: "12.5px" }} />
                      <span style={{ fontSize: "11px", color: "#94a3b8" }}>p.p.</span>
                    </div>
                  </div>
                  <div>
                    <label style={miniLabel}>Limite máximo</label>
                    <div style={{ display: "flex", alignItems: "center", gap: "4px" }}>
                      <input value={c.limite_maximo} inputMode="decimal" placeholder="—"
                        aria-label={`Limite máximo de ${info.label} em percentual`}
                        onChange={e => atualizarClasse(c.key, { limite_maximo: apenasNumero(e.target.value) })}
                        style={{ ...controlStyle, width: "68px", textAlign: "right", fontSize: "12.5px" }} />
                      <span style={{ fontSize: "11px", color: "#94a3b8" }}>%</span>
                    </div>
                  </div>
                </div>

                {/* Sanfona de subclasses */}
                <div style={{ marginTop: "auto", borderTop: "1px solid #f1f5f9", paddingTop: "12px" }}>
                  <button type="button" onClick={() => alternarSanfona(c.key)}
                    aria-expanded={aberta}
                    style={{
                      display: "flex", alignItems: "center", gap: "8px", width: "100%",
                      background: "transparent", border: "none", cursor: "pointer", padding: 0, textAlign: "left",
                    }}>
                    <span style={{ fontSize: "11px", color: "#94a3b8", transition: "transform 0.2s ease", transform: aberta ? "rotate(90deg)" : "none" }}>▶</span>
                    <span style={{ fontSize: "12.5px", fontWeight: 700, color: "#475569" }}>
                      Subclasses {c.subclasses.length > 0 && `(${c.subclasses.length})`}
                    </span>
                    {c.subclasses.length > 0 && (
                      <span style={{ ...overline, marginLeft: "auto" }}>{fmtPercentual(somaSub)} da classe</span>
                    )}
                  </button>

                  {subExcede && (
                    <p style={{ fontSize: "11.5px", color: "#b45309", margin: "8px 0 0" }}>
                      ⚠ As subclasses somam {fmtPercentual(somaSub)} da classe — precisam fechar em 100%.
                    </p>
                  )}

                  {aberta && (
                    <div style={{ display: "flex", flexDirection: "column", gap: "12px", marginTop: "12px" }}>
                      {c.subclasses.length === 0 && (
                        <p style={{ fontSize: "12px", color: "#94a3b8", margin: 0 }}>
                          Sem subclasses: a classe é tratada como um bloco único.
                        </p>
                      )}

                      {c.subclasses.map(s => (
                        <div key={s.key} style={{ border: "1px solid #eef2f7", borderRadius: "10px", padding: "11px 12px", background: "#fcfdff" }}>
                          <div style={{ display: "flex", gap: "8px", alignItems: "center" }}>
                            <input value={s.nome} placeholder="Subclasse (ex: Bancos)" aria-label="Nome da subclasse"
                              onChange={e => atualizarSubclasse(c.key, s.key, { nome: e.target.value })}
                              style={{ ...controlStyle, flex: 1, minWidth: 0, fontWeight: 600 }} />
                            <div style={{ display: "flex", alignItems: "center", gap: "3px" }}>
                              <input value={s.percentual_ideal} inputMode="decimal" placeholder="0,00"
                                aria-label={`Percentual ideal da subclasse ${s.nome || "sem nome"}`}
                                onChange={e => atualizarSubclasse(c.key, s.key, { percentual_ideal: apenasNumero(e.target.value) })}
                                style={{ ...controlStyle, width: "74px", textAlign: "right", fontWeight: 700 }} />
                              <span style={{ fontSize: "12px", color: "#94a3b8" }}>%</span>
                            </div>
                            <button type="button" onClick={() => removerSubclasse(c.key, s.key)}
                              aria-label="Remover subclasse" style={{ ...iconBtn(false), color: "#ef4444" }}>🗑</button>
                          </div>

                          <div style={{ display: "flex", gap: "12px", marginTop: "9px" }}
                            title="Tolerância (p.p. da classe) e limite máximo de concentração desta subclasse.">
                            <div>
                              <label style={miniLabel}>Tolerância ±</label>
                              <input value={s.tolerancia} inputMode="decimal" placeholder="0"
                                aria-label={`Tolerância da subclasse ${s.nome || "sem nome"}`}
                                onChange={e => atualizarSubclasse(c.key, s.key, { tolerancia: apenasNumero(e.target.value) })}
                                style={{ ...controlStyle, width: "62px", textAlign: "right", fontSize: "12px" }} />
                            </div>
                            <div>
                              <label style={miniLabel}>Limite máximo</label>
                              <input value={s.limite_maximo} inputMode="decimal" placeholder="—"
                                aria-label={`Limite máximo da subclasse ${s.nome || "sem nome"}`}
                                onChange={e => atualizarSubclasse(c.key, s.key, { limite_maximo: apenasNumero(e.target.value) })}
                                style={{ ...controlStyle, width: "62px", textAlign: "right", fontSize: "12px" }} />
                            </div>
                            <div style={{ marginLeft: "auto", alignSelf: "flex-end" }}>
                              <button type="button" onClick={() => adicionarSetor(c.key, s.key)}
                                style={{ ...linkBtnStyle, fontSize: "11px", padding: "4px 10px" }}>
                                + Setor
                              </button>
                            </div>
                          </div>

                          {/* ── SETOR: nível opcional dentro da subclasse (o % é fatia dela) ── */}
                          {s.setores.map(st => (
                            <div key={st.key} style={{ display: "flex", gap: "7px", alignItems: "center", flexWrap: "wrap", marginTop: "9px", paddingLeft: "10px", borderLeft: "2px solid #eef2f7" }}>
                              <input value={st.nome} placeholder="Setor (ex: Bancos)"
                                aria-label={`Nome do setor ${st.nome || "sem nome"}`}
                                onChange={e => atualizarSetor(c.key, s.key, st.key, { nome: e.target.value })}
                                style={{ ...controlStyle, flex: 1, minWidth: "90px", fontSize: "12px" }} />
                              <div style={{ display: "flex", alignItems: "center", gap: "3px" }}>
                                <input value={st.percentual_ideal} inputMode="decimal" placeholder="0,00"
                                  aria-label={`Percentual ideal do setor ${st.nome || "sem nome"}`}
                                  title="Percentual do setor DENTRO da subclasse"
                                  onChange={e => atualizarSetor(c.key, s.key, st.key, { percentual_ideal: apenasNumero(e.target.value) })}
                                  style={{ ...controlStyle, width: "70px", textAlign: "right", fontSize: "12px" }} />
                                <span style={{ fontSize: "11px", color: "#94a3b8" }}>%</span>
                              </div>
                              <div style={{ display: "flex", alignItems: "center", gap: "4px" }}
                                title="Tolerância (p.p. da subclasse) e limite máximo de concentração do setor.">
                                <span style={{ fontSize: "10px", fontWeight: 700, color: "#94a3b8" }}>±</span>
                                <input value={st.tolerancia} inputMode="decimal" placeholder="0"
                                  aria-label={`Tolerância do setor ${st.nome || "sem nome"}`}
                                  onChange={e => atualizarSetor(c.key, s.key, st.key, { tolerancia: apenasNumero(e.target.value) })}
                                  style={{ ...controlStyle, width: "52px", textAlign: "right", fontSize: "12px" }} />
                                <span style={{ fontSize: "10px", fontWeight: 700, color: "#94a3b8" }}>máx</span>
                                <input value={st.limite_maximo} inputMode="decimal" placeholder="—"
                                  aria-label={`Limite máximo do setor ${st.nome || "sem nome"}`}
                                  onChange={e => atualizarSetor(c.key, s.key, st.key, { limite_maximo: apenasNumero(e.target.value) })}
                                  style={{ ...controlStyle, width: "52px", textAlign: "right", fontSize: "12px" }} />
                              </div>
                              <button type="button" onClick={() => removerSetor(c.key, s.key, st.key)}
                                aria-label="Remover setor" style={{ ...iconBtn(false), color: "#ef4444" }}>🗑</button>
                            </div>
                          ))}
                        </div>
                      ))}

                      <button type="button" onClick={() => adicionarSubclasse(c.key)}
                        style={{ ...linkBtnStyle, alignSelf: "flex-start" }}>
                        + Subclasse
                      </button>
                    </div>
                  )}
                </div>
              </div>
            </article>
          );
        })}
      </div>

      <div style={{ marginTop: "18px", display: "flex", alignItems: "center", gap: "10px", flexWrap: "wrap" }}>
        <button type="button" disabled={disponiveis.length === 0}
          onClick={() => onChange([...classes, novaClasseDraft(disponiveis[0].key)])}
          style={{ ...linkBtnStyle, opacity: disponiveis.length === 0 ? 0.5 : 1, cursor: disponiveis.length === 0 ? "not-allowed" : "pointer" }}>
          + Adicionar classe
        </button>
        {disponiveis.length === 0 && classes.length > 0 && (
          <span style={{ fontSize: "11.5px", color: "#94a3b8" }}>Todas as classes já foram usadas.</span>
        )}
      </div>
    </section>
  );
}

/* ── Helper local ── */

/** Mantém apenas dígitos, vírgula e ponto (a vírgula é normalizada no save). */
function apenasNumero(valor: string): string {
  return valor.replace(/[^0-9.,]/g, "");
}
