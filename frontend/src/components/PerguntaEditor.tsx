import type { CSSProperties } from "react";
import type { Pergunta, PerguntaPayload, TipoPergunta } from "../types/checklist";
import { TIPOS_PERGUNTA, ehInformativo, tipoInfo, usaFaixas, usaOpcoes } from "../utils/avaliacao";
import { novaChave } from "../utils/chaves";
import { apenasNumero, numParaTexto, textoParaNum, textoParaNumOuNull } from "../utils/numeros";
import { controlStyle, iconBtn, linkBtnStyle } from "./FormStyles";

/* ══════════════════════════════════════════════════════════════════════
   Editor de UMA pergunta (Módulo 3) + conversão draft ⇄ payload.

   O "draft" mantém tudo como string (digitação livre); a conversão para o
   payload do backend acontece no salvar, em `draftParaPayload`.
   Usado tanto pelo modelo de checklist quanto pelo checklist do ativo.
   ══════════════════════════════════════════════════════════════════════ */

export interface RegraDraft {
  key: string;
  texto: string;
  valor_min: string;
  valor_max: string;
  nota: string;
}

export interface PerguntaDraft {
  key: string;
  titulo: string;
  descricao: string;
  tipo: TipoPergunta;
  peso: string;
  nota_maxima: string;
  conta_no_score: boolean;
  /** Critério eliminatório: reprovada, o ativo fica em NÃO APORTAR. */
  bloqueadora: boolean;
  /** Nota mínima para aprovar (vazio = qualquer nota > 0). */
  nota_minima: string;
  regras: RegraDraft[];
}

export const novaRegraDraft = (): RegraDraft => ({
  key: novaChave(), texto: "", valor_min: "", valor_max: "", nota: "",
});

export const novaPerguntaDraft = (tipo: TipoPergunta = "SIM_NAO"): PerguntaDraft => ({
  key: novaChave(), titulo: "", descricao: "", tipo,
  peso: "1", nota_maxima: "10", conta_no_score: !ehInformativo(tipo),
  bloqueadora: false, nota_minima: "", regras: [],
});

export const perguntaParaDraft = (p: Pergunta): PerguntaDraft => ({
  key: novaChave(),
  titulo: p.titulo,
  descricao: p.descricao ?? "",
  tipo: p.tipo,
  peso: numParaTexto(p.peso),
  nota_maxima: numParaTexto(p.nota_maxima),
  conta_no_score: p.conta_no_score,
  bloqueadora: Boolean(p.bloqueadora),
  nota_minima: numParaTexto(p.nota_minima ?? null),
  regras: (p.regras ?? []).map(r => ({
    key: novaChave(),
    texto: r.texto ?? "",
    valor_min: numParaTexto(r.valor_min),
    valor_max: numParaTexto(r.valor_max),
    nota: numParaTexto(r.nota),
  })),
});

/** O tipo usa alguma lista de regras (faixa, opção ou item de lista)? */
export const tipoUsaRegras = (tipo: TipoPergunta): boolean =>
  usaFaixas(tipo) || usaOpcoes(tipo) || tipo === "LISTA";

/** Converte o draft no payload aceito pelo backend. */
export function draftParaPayload(d: PerguntaDraft, ordem: number): PerguntaPayload {
  const tipo = d.tipo;

  const regras = tipoUsaRegras(tipo)
    ? d.regras
        .filter(r => usaFaixas(tipo)
          ? (r.valor_min.trim() !== "" || r.valor_max.trim() !== "" || r.nota.trim() !== "")
          : r.texto.trim() !== "")
        .map((r, i) => ({
          texto: r.texto.trim() === "" ? null : r.texto.trim(),
          valor_min: usaFaixas(tipo) ? textoParaNumOuNull(r.valor_min) : null,
          valor_max: usaFaixas(tipo) ? textoParaNumOuNull(r.valor_max) : null,
          nota: tipo === "LISTA" ? 0 : textoParaNum(r.nota),
          ordem: i,
        }))
    : [];

  return {
    titulo: d.titulo.trim(),
    descricao: d.descricao.trim() === "" ? null : d.descricao.trim(),
    tipo,
    peso: textoParaNum(d.peso) || 1,
    nota_maxima: textoParaNum(d.nota_maxima) || 10,
    conta_no_score: ehInformativo(tipo) ? false : d.conta_no_score,
    // Critério eliminatório só faz sentido em pergunta que pontua: em pergunta
    // informativa não existe "reprovar" (não há nota).
    bloqueadora: !ehInformativo(tipo) && d.bloqueadora,
    nota_minima: (!ehInformativo(tipo) && d.bloqueadora)
      ? textoParaNumOuNull(d.nota_minima)
      : null,
    ordem,
    regras,
  };
}

export function draftsParaPayloads(drafts: PerguntaDraft[]): PerguntaPayload[] {
  return drafts.map((d, i) => draftParaPayload(d, i));
}

/** Validação local (o backend valida de novo — ele é a fonte da verdade). */
export function validarPerguntaDraft(d: PerguntaDraft): string | null {
  if (!d.titulo.trim()) return "Toda pergunta precisa de um título.";
  if (textoParaNum(d.peso) <= 0) return `O peso da pergunta "${d.titulo}" deve ser maior que zero.`;
  const notaMaxima = textoParaNum(d.nota_maxima);
  if (notaMaxima <= 0) return `A nota máxima de "${d.titulo}" deve ser maior que zero.`;

  const tipo = d.tipo;
  const pontua = !ehInformativo(tipo) && d.conta_no_score;

  if (pontua && usaFaixas(tipo) && d.regras.length === 0) {
    return `A pergunta "${d.titulo}" pontua pelo valor informado, então precisa de ao menos uma faixa.`;
  }
  if (pontua && usaOpcoes(tipo) && d.regras.length === 0) {
    return `A pergunta "${d.titulo}" é de múltipla escolha e precisa de ao menos uma opção.`;
  }

  for (const r of d.regras) {
    if (usaOpcoes(tipo) && r.texto.trim() === "") {
      return `Toda opção de "${d.titulo}" precisa de um rótulo.`;
    }
    if (tipo === "LISTA") continue;
    if (r.nota.trim() === "") {
      return `Informe a nota de todas as regras de "${d.titulo}".`;
    }
    const nota = textoParaNum(r.nota);
    if (nota < 0 || nota > notaMaxima) {
      return `A nota das regras de "${d.titulo}" deve ficar entre 0 e ${notaMaxima}.`;
    }
    if (usaFaixas(tipo)) {
      const min = textoParaNumOuNull(r.valor_min);
      const max = textoParaNumOuNull(r.valor_max);
      if (min != null && max != null && min > max) {
        return `Em "${d.titulo}" o valor mínimo de uma faixa é maior que o máximo.`;
      }
    }
  }
  return null;
}

/* ══════════════════════════════════════════════════════════════════════
   Componente
   ══════════════════════════════════════════════════════════════════════ */

interface Props {
  pergunta: PerguntaDraft;
  index: number;
  total: number;
  onChange: (p: PerguntaDraft) => void;
  onRemove: () => void;
  onMove: (delta: number) => void;
}

export default function PerguntaEditor({ pergunta, index, total, onChange, onRemove, onMove }: Props) {
  const info = tipoInfo(pergunta.tipo);
  const informativo = ehInformativo(pergunta.tipo);
  const comRegras = tipoUsaRegras(pergunta.tipo);

  const patch = (p: Partial<PerguntaDraft>) => onChange({ ...pergunta, ...p });

  const trocarTipo = (tipo: TipoPergunta) => {
    const proximo: Partial<PerguntaDraft> = {
      tipo,
      conta_no_score: !ehInformativo(tipo),
    };
    if (!tipoUsaRegras(tipo)) proximo.regras = [];
    else if (pergunta.regras.length === 0) proximo.regras = [novaRegraDraft()];
    onChange({ ...pergunta, ...proximo });
  };

  const patchRegra = (key: string, p: Partial<RegraDraft>) =>
    patch({ regras: pergunta.regras.map(r => (r.key === key ? { ...r, ...p } : r)) });

  const rotuloRegra = usaFaixas(pergunta.tipo) ? "faixa" : usaOpcoes(pergunta.tipo) ? "opção" : "item";

  return (
    <div style={cardStyle}>
      <div style={{ display: "flex", gap: "8px", alignItems: "flex-end", flexWrap: "wrap" }}>
        <span style={{ fontSize: "12px", fontWeight: 700, color: "#94a3b8", minWidth: "22px" }}>{index + 1}.</span>

        <div style={{ flex: "1 1 220px", minWidth: "180px" }}>
          <label style={labelStyle}>Pergunta</label>
          <input value={pergunta.titulo} placeholder="Ex: ROE está acima de 15%?"
            onChange={e => patch({ titulo: e.target.value })} style={controlStyleFull} />
        </div>

        <div>
          <label style={labelStyle}>Tipo</label>
          <select value={pergunta.tipo} onChange={e => trocarTipo(e.target.value as TipoPergunta)}
            style={{ ...controlStyle, minWidth: "150px" }}>
            {TIPOS_PERGUNTA.map(t => <option key={t.key} value={t.key}>{t.icon} {t.label}</option>)}
          </select>
        </div>

        <div>
          <label style={labelStyle}>Peso</label>
          <input value={pergunta.peso} inputMode="decimal" aria-label="Peso da pergunta"
            onChange={e => patch({ peso: apenasNumero(e.target.value) })}
            style={{ ...controlStyle, width: "70px", textAlign: "right" }} />
        </div>

        <div>
          <label style={labelStyle}>Nota máx.</label>
          <input value={pergunta.nota_maxima} inputMode="decimal" aria-label="Nota máxima"
            onChange={e => patch({ nota_maxima: apenasNumero(e.target.value) })}
            style={{ ...controlStyle, width: "75px", textAlign: "right" }} />
        </div>

        <label style={{
          display: "flex", alignItems: "center", gap: "6px", fontSize: "12px", fontWeight: 600,
          color: informativo ? "#94a3b8" : "#374151", paddingBottom: "9px",
        }}>
          <input type="checkbox" checked={!informativo && pergunta.conta_no_score} disabled={informativo}
            onChange={e => patch({ conta_no_score: e.target.checked })}
            style={{ width: "16px", height: "16px", accentColor: "#6366f1", cursor: informativo ? "not-allowed" : "pointer" }} />
          Entra no score
        </label>

        {/* ── Critério eliminatório (§8): reprovado, o ativo fica em NÃO APORTAR ── */}
        <label title="Se esta pergunta for reprovada, o ativo não recebe novos aportes — o déficit continua existindo e sendo mostrado."
          style={{
            display: "flex", alignItems: "center", gap: "6px", fontSize: "12px", fontWeight: 600,
            color: informativo ? "#94a3b8" : (pergunta.bloqueadora ? "#b91c1c" : "#374151"), paddingBottom: "9px",
          }}>
          <input type="checkbox" checked={!informativo && pergunta.bloqueadora} disabled={informativo}
            onChange={e => patch({ bloqueadora: e.target.checked, nota_minima: e.target.checked ? pergunta.nota_minima : "" })}
            style={{ width: "16px", height: "16px", accentColor: "#dc2626", cursor: informativo ? "not-allowed" : "pointer" }} />
          🚫 Critério eliminatório
        </label>

        {!informativo && pergunta.bloqueadora && (
          <div>
            <label style={labelStyle}>Nota mínima</label>
            <input value={pergunta.nota_minima} inputMode="decimal" aria-label="Nota mínima para aprovar"
              placeholder="0"
              onChange={e => patch({ nota_minima: apenasNumero(e.target.value) })}
              style={{ ...controlStyle, width: "80px", textAlign: "right" }} />
          </div>
        )}

        <div style={{ display: "flex", gap: "2px", marginLeft: "auto", paddingBottom: "4px" }}>
          <button type="button" onClick={() => onMove(-1)} disabled={index === 0}
            aria-label="Mover pergunta para cima" style={iconBtn(index === 0)}>↑</button>
          <button type="button" onClick={() => onMove(1)} disabled={index === total - 1}
            aria-label="Mover pergunta para baixo" style={iconBtn(index === total - 1)}>↓</button>
          <button type="button" onClick={onRemove} aria-label="Remover pergunta"
            style={{ ...iconBtn(false), color: "#ef4444" }}>🗑</button>
        </div>
      </div>

      <p style={{ fontSize: "11px", color: "#94a3b8", margin: "6px 0 0", paddingLeft: "30px" }}>
        {info.icon} {info.hint}{informativo && " · não entra no score"}
      </p>

      <div style={{ paddingLeft: "30px", marginTop: "10px" }}>
        <input value={pergunta.descricao} placeholder="Descrição / o que você avalia nesta pergunta (opcional)"
          onChange={e => patch({ descricao: e.target.value })}
          style={{ ...controlStyleFull, marginBottom: comRegras ? "10px" : "0" }} />

        {comRegras && (
          <div style={{ display: "flex", flexDirection: "column", gap: "6px" }}>
            {pergunta.regras.map(r => (
              <div key={r.key} style={{ display: "flex", gap: "8px", alignItems: "center", flexWrap: "wrap" }}>
                <input value={r.texto}
                  placeholder={usaFaixas(pergunta.tipo) ? "Rótulo da faixa (opcional)" : `Rótulo da ${rotuloRegra}`}
                  aria-label={`Rótulo da ${rotuloRegra}`}
                  onChange={e => patchRegra(r.key, { texto: e.target.value })}
                  style={{ ...controlStyle, minWidth: "160px", flex: "1 1 160px" }} />

                {usaFaixas(pergunta.tipo) && (
                  <>
                    <input value={r.valor_min} inputMode="decimal" placeholder="mín." aria-label="Valor mínimo"
                      onChange={e => patchRegra(r.key, { valor_min: apenasNumero(e.target.value) })}
                      style={{ ...controlStyle, width: "80px", textAlign: "right" }} />
                    <span style={{ fontSize: "12px", color: "#94a3b8" }}>a</span>
                    <input value={r.valor_max} inputMode="decimal" placeholder="máx." aria-label="Valor máximo"
                      onChange={e => patchRegra(r.key, { valor_max: apenasNumero(e.target.value) })}
                      style={{ ...controlStyle, width: "80px", textAlign: "right" }} />
                  </>
                )}

                {pergunta.tipo !== "LISTA" && (
                  <div style={{ display: "flex", alignItems: "center", gap: "4px" }}>
                    <span style={{ fontSize: "12px", color: "#64748b" }}>nota</span>
                    <input value={r.nota} inputMode="decimal" placeholder="0" aria-label="Nota da regra"
                      onChange={e => patchRegra(r.key, { nota: apenasNumero(e.target.value) })}
                      style={{ ...controlStyle, width: "70px", textAlign: "right" }} />
                  </div>
                )}

                <button type="button" aria-label={`Remover ${rotuloRegra}`}
                  onClick={() => patch({ regras: pergunta.regras.filter(x => x.key !== r.key) })}
                  style={{ ...iconBtn(false), color: "#ef4444" }}>🗑</button>
              </div>
            ))}

            <div>
              <button type="button" onClick={() => patch({ regras: [...pergunta.regras, novaRegraDraft()] })} style={linkBtnStyle}>
                + Adicionar {rotuloRegra}
              </button>
              {usaFaixas(pergunta.tipo) && (
                <span style={{ fontSize: "11px", color: "#94a3b8", marginLeft: "10px" }}>
                  Deixe o máximo vazio para "acima de"; deixe os dois vazios para "qualquer valor".
                </span>
              )}
            </div>
          </div>
        )}
      </div>
    </div>
  );
}

const cardStyle: CSSProperties = {
  border: "1px solid #e2e8f0", borderRadius: "10px", padding: "12px", background: "#fcfdff",
};

const labelStyle: CSSProperties = {
  display: "block", fontSize: "11px", fontWeight: 600, color: "#64748b", marginBottom: "3px",
};

const controlStyleFull: CSSProperties = { ...controlStyle, width: "100%" };
