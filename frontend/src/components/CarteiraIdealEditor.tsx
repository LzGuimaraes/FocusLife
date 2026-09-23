import { toast } from "sonner";
import type { CategoriaInvestimento } from "../types/planejamento";
import { CATEGORIAS, catInfo, fmtPercentual, somaFechada } from "../utils/percentual";
import { novaChave } from "../utils/chaves";
import { textoParaNum } from "../utils/numeros";
import { MODELOS_PLANEJAMENTO, type ModeloPlanejamento } from "../utils/modelosPlanejamento";
import { boxStyle, controlStyle, iconBtn, linkBtnStyle } from "./FormStyles";

/* ══════════════════════════════════════════════════════════════════════
   Editor de classes e subclasses da Carteira Ideal (Módulo 1).

   Trabalha com "drafts" (percentuais como string) para que a digitação
   seja livre; a conversão para número acontece no save da página.
   ══════════════════════════════════════════════════════════════════════ */

export interface SubclasseDraft {
  key: string;
  /** ID no backend (null enquanto a subclasse não foi salva). */
  id?: number | null;
  nome: string;
  percentual_ideal: string;
  /** Faixa (p.p. da classe) de equilíbrio e teto do aporte. */
  tolerancia: string;
  limite_maximo: string;
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
  return { key: novaChave(), nome: "", percentual_ideal: "", tolerancia: "", limite_maximo: "" };
}

interface Props {
  classes: ClasseDraft[];
  onChange: (classes: ClasseDraft[]) => void;
}

export default function CarteiraIdealEditor({ classes, onChange }: Props) {
  // ATENÇÃO: usar `textoParaNum` (e não parseFloat) porque o usuário digita com
  // vírgula — `parseFloat("12,5")` devolveria 12 e a soma sairia errada.
  const soma = classes.reduce((s, c) => s + textoParaNum(c.percentual_ideal), 0);
  const usadas = classes.map(c => c.classe);
  const disponiveis = CATEGORIAS.filter(c => !usadas.includes(c.key));

  /** Aplica um modelo pronto como ponto de partida (substitui as classes atuais). */
  const aplicarModelo = (modelo: ModeloPlanejamento) => {
    onChange(modelo.classes.map(c => ({
      ...novaClasseDraft(c.classe),
      percentual_ideal: c.percentual.toFixed(2).replace(".", ","),
    })));
    toast.success(`Modelo "${modelo.nome}" aplicado — revise os percentuais e ajuste o que quiser.`);
  };

  /** Leva a soma para exatamente 100% sem o usuário fazer conta. */
  const ajustarPara100 = () => {
    if (classes.length === 0) {
      toast.info("Adicione ou aplique um modelo antes de ajustar.");
      return;
    }
    if (somaFechada(soma)) {
      toast.info("A soma das classes já está em 100%.");
      return;
    }
    if (soma > 100) {
      const fator = 100 / soma;
      onChange(classes.map(c => ({
        ...c,
        percentual_ideal: (textoParaNum(c.percentual_ideal) * fator).toFixed(2).replace(".", ","),
      })));
      toast.success("Percentuais reduzidos proporcionalmente para somar 100%.");
      return;
    }
    const falta = 100 - soma;
    const indiceOutros = classes.findIndex(c => c.classe === "OUTROS");
    if (indiceOutros >= 0) {
      onChange(classes.map((c, i) => (i === indiceOutros
        ? { ...c, percentual_ideal: (textoParaNum(c.percentual_ideal) + falta).toFixed(2).replace(".", ",") }
        : c)));
    } else {
      onChange([...classes, {
        ...novaClasseDraft("OUTROS"),
        percentual_ideal: falta.toFixed(2).replace(".", ","),
      }]);
    }
    toast.success(`Completei os ${falta.toFixed(2).replace(".", ",")}% que faltavam com "Outros".`);
  };

  const atualizarClasse = (key: string, patch: Partial<ClasseDraft>) =>
    onChange(classes.map(c => (c.key === key ? { ...c, ...patch } : c)));

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

  return (
    <div style={boxStyle}>
      <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", flexWrap: "wrap", gap: "8px", marginBottom: "14px" }}>
        <div>
          <h3 style={{ fontSize: "15px", fontWeight: 700, color: "#0f172a", margin: 0 }}>Classes e subclasses</h3>
          <p style={{ fontSize: "12px", color: "#64748b", margin: "2px 0 0" }}>
            As classes precisam somar 100%. As subclasses são um detalhamento dentro da classe.
          </p>
        </div>
        <span style={{
          fontSize: "13px", fontWeight: 700, padding: "5px 12px", borderRadius: "9999px",
          background: somaFechada(soma) ? "#d1fae5" : "#fee2e2",
          color: somaFechada(soma) ? "#047857" : "#b91c1c",
        }}>
          Soma: {fmtPercentual(soma)}
        </span>
      </div>

      {classes.length === 0 && (
        <p style={{ fontSize: "13px", color: "#94a3b8", margin: "0 0 12px" }}>
          Nenhuma classe definida ainda. Adicione as classes da sua carteira ideal.
        </p>
      )}

      {classes.length === 0 && (
        <div style={{ background: "#f8fafc", border: "1px dashed #e2e8f0", borderRadius: "10px", padding: "12px 14px", marginBottom: "12px" }}>
          <p style={{ fontSize: "12px", fontWeight: 700, color: "#475569", margin: "0 0 8px" }}>
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

      <div style={{ display: "flex", gap: "8px", flexWrap: "wrap", alignItems: "center", marginBottom: "12px" }}>
        <button type="button" onClick={ajustarPara100} style={linkBtnStyle}>
          ⚖️ Ajustar para 100%
        </button>
        <span style={{ fontSize: "11px", color: "#94a3b8" }}>
          se faltar, completa com "Outros"; se passar, reduz proporcionalmente.
        </span>
      </div>

      <div style={{ display: "flex", flexDirection: "column", gap: "10px" }}>
        {classes.map((c, index) => {
          const info = catInfo(c.classe);
          const somaSub = c.subclasses.reduce((s, x) => s + textoParaNum(x.percentual_ideal), 0);
          const subExcede = somaSub > textoParaNum(c.percentual_ideal) + 0.01;
          return (
            <div key={c.key} style={{ border: "1px solid #e2e8f0", borderRadius: "10px", padding: "10px 12px", background: "#fcfdff" }}>
              <div style={{ display: "flex", gap: "8px", alignItems: "center", flexWrap: "wrap" }}>
                <span style={{ fontSize: "15px", width: "20px", textAlign: "center" }}>{info.icon}</span>
                <select value={c.classe} aria-label="Classe"
                  onChange={e => atualizarClasse(c.key, { classe: e.target.value as CategoriaInvestimento })}
                  style={{ ...controlStyle, minWidth: "150px" }}>
                  {CATEGORIAS.filter(op => op.key === c.classe || !usadas.includes(op.key))
                    .map(op => <option key={op.key} value={op.key}>{op.label}</option>)}
                </select>
                <div style={{ display: "flex", alignItems: "center", gap: "4px" }}>
                  <input value={c.percentual_ideal} inputMode="decimal" placeholder="0,00"
                    aria-label={`Percentual ideal de ${info.label}`}
                    onChange={e => atualizarClasse(c.key, { percentual_ideal: apenasNumero(e.target.value) })}
                    style={{ ...controlStyle, width: "90px", textAlign: "right" }} />
                  <span style={{ fontSize: "13px", color: "#64748b" }}>%</span>
                </div>
                <div style={{ display: "flex", alignItems: "center", gap: "4px" }}
                  title="Tolerância (p.p.): dentro desta faixa a classe conta como EQUILIBRADA — e é o que dá espaço ao aporte quando a meta já foi atingida. Limite máximo: acima dele a classe não recebe novos aportes.">
                  <span style={{ fontSize: "10px", fontWeight: 700, color: "#94a3b8" }}>±</span>
                  <input value={c.tolerancia} inputMode="decimal" placeholder="0"
                    aria-label={`Tolerância de ${info.label} em pontos percentuais`}
                    onChange={e => atualizarClasse(c.key, { tolerancia: apenasNumero(e.target.value) })}
                    style={{ ...controlStyle, width: "62px", textAlign: "right", fontSize: "12px" }} />
                  <span style={{ fontSize: "10px", fontWeight: 700, color: "#94a3b8" }}>máx</span>
                  <input value={c.limite_maximo} inputMode="decimal" placeholder="—"
                    aria-label={`Limite máximo de ${info.label} em percentual`}
                    onChange={e => atualizarClasse(c.key, { limite_maximo: apenasNumero(e.target.value) })}
                    style={{ ...controlStyle, width: "62px", textAlign: "right", fontSize: "12px" }} />
                </div>
                <div style={{ display: "flex", gap: "2px", marginLeft: "auto" }}>
                  <button type="button" onClick={() => moverClasse(index, -1)} disabled={index === 0}
                    aria-label={`Mover ${info.label} para cima`} style={iconBtn(index === 0)}>↑</button>
                  <button type="button" onClick={() => moverClasse(index, 1)} disabled={index === classes.length - 1}
                    aria-label={`Mover ${info.label} para baixo`} style={iconBtn(index === classes.length - 1)}>↓</button>
                  <button type="button" onClick={() => onChange(classes.filter(x => x.key !== c.key))}
                    aria-label={`Remover ${info.label}`} style={{ ...iconBtn(false), color: "#ef4444" }}>🗑</button>
                </div>
              </div>

              <div style={{ marginLeft: "28px", marginTop: "8px", display: "flex", flexDirection: "column", gap: "6px" }}>
                {c.subclasses.map(s => (
                  <div key={s.key} style={{ display: "flex", gap: "8px", alignItems: "center", flexWrap: "wrap" }}>
                    <span style={{ fontSize: "12px", color: "#94a3b8" }}>↳</span>
                    <input value={s.nome} placeholder="Subclasse (ex: Bancos)" aria-label="Nome da subclasse"
                      onChange={e => atualizarSubclasse(c.key, s.key, { nome: e.target.value })}
                      style={{ ...controlStyle, minWidth: "150px" }} />
                    <div style={{ display: "flex", alignItems: "center", gap: "4px" }}>
                      <input value={s.percentual_ideal} inputMode="decimal" placeholder="0,00"
                        aria-label={`Percentual ideal da subclasse ${s.nome || "sem nome"}`}
                        onChange={e => atualizarSubclasse(c.key, s.key, { percentual_ideal: apenasNumero(e.target.value) })}
                        style={{ ...controlStyle, width: "90px", textAlign: "right" }} />
                      <span style={{ fontSize: "13px", color: "#64748b" }}>%</span>
                    </div>
                    <div style={{ display: "flex", alignItems: "center", gap: "4px" }}
                      title="Tolerância (p.p. da classe) e limite máximo de concentração desta subclasse.">
                      <span style={{ fontSize: "10px", fontWeight: 700, color: "#94a3b8" }}>±</span>
                      <input value={s.tolerancia} inputMode="decimal" placeholder="0"
                        aria-label={`Tolerância da subclasse ${s.nome || "sem nome"}`}
                        onChange={e => atualizarSubclasse(c.key, s.key, { tolerancia: apenasNumero(e.target.value) })}
                        style={{ ...controlStyle, width: "58px", textAlign: "right", fontSize: "12px" }} />
                      <span style={{ fontSize: "10px", fontWeight: 700, color: "#94a3b8" }}>máx</span>
                      <input value={s.limite_maximo} inputMode="decimal" placeholder="—"
                        aria-label={`Limite máximo da subclasse ${s.nome || "sem nome"}`}
                        onChange={e => atualizarSubclasse(c.key, s.key, { limite_maximo: apenasNumero(e.target.value) })}
                        style={{ ...controlStyle, width: "58px", textAlign: "right", fontSize: "12px" }} />
                    </div>
                    <div style={{ display: "flex", alignItems: "center", gap: "4px" }}
                      title="Tolerância (p.p. da classe) e limite máximo de concentração desta subclasse.">
                      <span style={{ fontSize: "10px", fontWeight: 700, color: "#94a3b8" }}>±</span>
                      <input value={s.tolerancia} inputMode="decimal" placeholder="0"
                        aria-label={`Tolerância da subclasse ${s.nome || "sem nome"}`}
                        onChange={e => atualizarSubclasse(c.key, s.key, { tolerancia: apenasNumero(e.target.value) })}
                        style={{ ...controlStyle, width: "58px", textAlign: "right", fontSize: "12px" }} />
                      <span style={{ fontSize: "10px", fontWeight: 700, color: "#94a3b8" }}>máx</span>
                      <input value={s.limite_maximo} inputMode="decimal" placeholder="—"
                        aria-label={`Limite máximo da subclasse ${s.nome || "sem nome"}`}
                        onChange={e => atualizarSubclasse(c.key, s.key, { limite_maximo: apenasNumero(e.target.value) })}
                        style={{ ...controlStyle, width: "58px", textAlign: "right", fontSize: "12px" }} />
                    </div>
                    <button type="button" onClick={() => removerSubclasse(c.key, s.key)}
                      aria-label="Remover subclasse" style={{ ...iconBtn(false), color: "#ef4444", marginLeft: "auto" }}>🗑</button>
                  </div>
                ))}

                <div style={{ display: "flex", alignItems: "center", gap: "10px" }}>
                  <button type="button" onClick={() => adicionarSubclasse(c.key)} style={linkBtnStyle}>
                    + Subclasse
                  </button>
                  {subExcede && (
                    <span style={{ fontSize: "11px", color: "#b45309" }}>
                      ⚠ subclasses somam {fmtPercentual(somaSub)} (acima da classe)
                    </span>
                  )}
                </div>
              </div>
            </div>
          );
        })}
      </div>

      <div style={{ marginTop: "12px" }}>
        <button type="button" disabled={disponiveis.length === 0}
          onClick={() => onChange([...classes, novaClasseDraft(disponiveis[0].key)])}
          style={{ ...linkBtnStyle, opacity: disponiveis.length === 0 ? 0.5 : 1, cursor: disponiveis.length === 0 ? "not-allowed" : "pointer" }}>
          + Adicionar classe
        </button>
        {disponiveis.length === 0 && classes.length > 0 && (
          <span style={{ fontSize: "11px", color: "#94a3b8", marginLeft: "10px" }}>
            Todas as classes já foram usadas.
          </span>
        )}
      </div>
    </div>
  );
}

/* ── Helper local ── */

/** Mantém apenas dígitos, vírgula e ponto (a vírgula é normalizada no save). */
function apenasNumero(valor: string): string {
  return valor.replace(/[^0-9.,]/g, "");
}
