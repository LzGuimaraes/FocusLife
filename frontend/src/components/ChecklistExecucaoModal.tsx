import { useEffect, useState } from "react";
import { toast } from "sonner";
import api from "../api/api";
import Modal from "./Modal";
import ScoreBadge from "./ScoreBadge";
import type { Checklist, Pergunta, RespostaItemPayload } from "../types/checklist";
import { ehInformativo, fmtFaixa, tipoInfo, usaFaixas, usaOpcoes } from "../utils/avaliacao";
import { apenasNumero, numParaTexto, textoParaNum } from "../utils/numeros";
import { controlStyle, miniLabel } from "./FormStyles";

/* ══════════════════════════════════════════════════════════════════════
   Execução de um checklist: responder as perguntas (Módulos 2, 3 e 5).

   O sistema apenas registra a avaliação do usuário. Para perguntas com
   faixa, a nota é DERIVADA da faixa no backend — aqui mostramos a prévia
   para o usuário entender o que será somado.
   ══════════════════════════════════════════════════════════════════════ */

interface CampoResposta {
  nota: string;
  valor: string;
  texto: string;
  observacao: string;
}

const vazio = (): CampoResposta => ({ nota: "", valor: "", texto: "", observacao: "" });

interface Props {
  open: boolean;
  checklist: Checklist | null;
  onClose: () => void;
  onSaved: (checklist: Checklist) => void;
}

export default function ChecklistExecucaoModal({ open, checklist, onClose, onSaved }: Props) {
  const [campos, setCampos] = useState<Record<number, CampoResposta>>({});
  const [salvando, setSalvando] = useState(false);

  useEffect(() => {
    if (!open || !checklist) return;
    const inicial: Record<number, CampoResposta> = {};
    for (const p of checklist.perguntas) {
      if (p.id == null) continue;
      inicial[p.id] = {
        nota: numParaTexto(p.nota_atribuida),
        valor: numParaTexto(p.valor_numerico),
        texto: p.resposta_texto ?? "",
        observacao: p.observacao ?? "",
      };
    }
    setCampos(inicial);
  }, [open, checklist]);

  if (!checklist) return null;

  const patch = (id: number, p: Partial<CampoResposta>) =>
    setCampos(prev => ({ ...prev, [id]: { ...(prev[id] ?? vazio()), ...p } }));

  const campo = (id: number): CampoResposta => campos[id] ?? vazio();

  const handleSalvar = async () => {
    const respostas: RespostaItemPayload[] = [];
    for (const p of checklist.perguntas) {
      if (p.id == null) continue;
      const c = campo(p.id);
      respostas.push({
        pergunta_id: p.id,
        nota: p.tipo === "SIM_NAO" || p.tipo === "NOTA" ? (c.nota === "" ? null : textoParaNum(c.nota)) : null,
        valor: usaFaixas(p.tipo) ? (c.valor === "" ? null : textoParaNum(c.valor)) : null,
        texto: (usaOpcoes(p.tipo) || ehInformativo(p.tipo)) ? (c.texto.trim() === "" ? null : c.texto.trim()) : null,
        observacao: c.observacao.trim() === "" ? null : c.observacao.trim(),
      });
    }

    setSalvando(true);
    try {
      const { data } = await api.put<Checklist>(`/checklists/${checklist.id}/responder`, { respostas });
      onSaved(data);
      toast.success("Avaliação salva!");
      onClose();
    } catch (err: any) {
      toast.error(err?.response?.data?.message || "Erro ao salvar a avaliação");
    } finally {
      setSalvando(false);
    }
  };

  const respondidas = checklist.perguntas.filter(p => {
    if (p.id == null) return false;
    const c = campo(p.id);
    return c.nota !== "" || c.valor !== "" || c.texto.trim() !== "";
  }).length;

  return (
    <Modal open={open} onClose={onClose} title={`Avaliar — ${checklist.nome}`} width="680px"
      onSubmit={handleSalvar} submitLabel={salvando ? "Salvando..." : "Salvar avaliação"}>
      <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", flexWrap: "wrap", gap: "8px" }}>
        <span style={{ fontSize: "12px", color: "#64748b" }}>
          {checklist.ticker ? `${checklist.ticker} · ` : ""}{respondidas} de {checklist.perguntas.length} respondida(s)
        </span>
        <ScoreBadge score={checklist.score} label="Score atual" size="sm" />
      </div>

      {checklist.perguntas.length === 0 && (
        <p style={{ fontSize: "13px", color: "#94a3b8" }}>
          Este checklist não tem perguntas. Adicione perguntas para poder avaliar.
        </p>
      )}

      {checklist.perguntas.map((p, i) => (
        <PerguntaResposta key={p.id ?? i} pergunta={p} indice={i}
          campo={campo(p.id ?? -1)} onChange={patchCampo => patch(p.id ?? -1, patchCampo)} />
      ))}
    </Modal>
  );
}

/* ── Uma pergunta com o campo de resposta adequado ao tipo ── */
function PerguntaResposta({ pergunta, indice, campo, onChange }: {
  pergunta: Pergunta;
  indice: number;
  campo: CampoResposta;
  onChange: (p: Partial<CampoResposta>) => void;
}) {
  const info = tipoInfo(pergunta.tipo);
  const notaMaxima = pergunta.nota_maxima;
  const valor = campo.valor === "" ? null : textoParaNum(campo.valor);

  const faixa = usaFaixas(pergunta.tipo) && valor != null
    ? pergunta.regras.find(r =>
        (r.valor_min == null || valor >= r.valor_min) && (r.valor_max == null || valor <= r.valor_max)) ?? null
    : null;

  return (
    <div style={{
      border: "1px solid #e2e8f0", borderRadius: "10px", padding: "12px",
      background: pergunta.conta_no_score ? "#fcfdff" : "#f8fafc",
    }}>
      <div style={{ display: "flex", justifyContent: "space-between", gap: "10px", flexWrap: "wrap", marginBottom: "8px" }}>
        <div>
          <p style={{ fontSize: "13px", fontWeight: 700, color: "#0f172a", margin: 0 }}>
            {indice + 1}. {pergunta.titulo}
          </p>
          {pergunta.descricao && (
            <p style={{ fontSize: "12px", color: "#64748b", margin: "2px 0 0" }}>{pergunta.descricao}</p>
          )}
        </div>
        <span style={{ fontSize: "11px", color: "#94a3b8", whiteSpace: "nowrap" }}>
          {info.icon} {info.label} · máx {notaMaxima}
          {!pergunta.conta_no_score && " · não pontua"}
        </span>
      </div>

      <div style={{ display: "flex", gap: "10px", alignItems: "flex-end", flexWrap: "wrap" }}>
        {pergunta.tipo === "SIM_NAO" && (
          <div style={{ minWidth: "160px" }}>
            <label style={miniLabel}>Resposta</label>
            <select value={campo.nota === "" ? "" : (textoParaNum(campo.nota) > 0 ? "sim" : "nao")}
              onChange={e => onChange({ nota: e.target.value === "sim" ? String(notaMaxima) : e.target.value === "nao" ? "0" : "" })}
              style={{ ...controlStyle, width: "100%" }}>
              <option value="">— não respondida —</option>
              <option value="sim">✅ Sim ({notaMaxima})</option>
              <option value="nao">❌ Não (0)</option>
            </select>
          </div>
        )}

        {pergunta.tipo === "NOTA" && (
          <div>
            <label style={miniLabel}>Nota (0 a {notaMaxima})</label>
            <input value={campo.nota} inputMode="decimal" aria-label="Nota"
              onChange={e => onChange({ nota: apenasNumero(e.target.value) })}
              style={{ ...controlStyle, width: "110px", textAlign: "right" }} />
          </div>
        )}

        {usaFaixas(pergunta.tipo) && (
          <>
            <div>
              <label style={miniLabel}>{pergunta.tipo === "PERCENTUAL" ? "Valor (%)" : "Valor"}</label>
              <input value={campo.valor} inputMode="decimal" aria-label="Valor"
                onChange={e => onChange({ valor: apenasNumero(e.target.value) })}
                style={{ ...controlStyle, width: "120px", textAlign: "right" }} />
            </div>
            <div style={{ paddingBottom: "9px" }}>
              {campo.valor === "" ? (
                <span style={{ fontSize: "11px", color: "#94a3b8" }}>
                  {pergunta.regras.length} faixa(s) cadastrada(s)
                </span>
              ) : faixa ? (
                <span style={{ fontSize: "11px", fontWeight: 700, color: "#047857" }}>
                  → nota {numParaTexto(faixa.nota)}
                </span>
              ) : (
                <span style={{ fontSize: "11px", fontWeight: 700, color: "#b45309" }}>
                  ⚠ nenhuma faixa cobre este valor
                </span>
              )}
            </div>
            <div style={{ paddingBottom: "9px", fontSize: "11px", color: "#94a3b8" }}>
              {pergunta.regras.map(r => (
                <div key={r.id}>{fmtFaixa(r.valor_min, r.valor_max, pergunta.tipo === "PERCENTUAL")} → {r.nota}</div>
              ))}
            </div>
          </>
        )}

        {usaOpcoes(pergunta.tipo) && (
          <div style={{ minWidth: "220px", flex: "1 1 220px" }}>
            <label style={miniLabel}>Opção</label>
            <select value={campo.texto} onChange={e => onChange({ texto: e.target.value })}
              style={{ ...controlStyle, width: "100%" }}>
              <option value="">— não respondida —</option>
              {pergunta.regras.map(r => (
                <option key={r.id} value={r.texto ?? ""}>
                  {r.texto}{pergunta.conta_no_score ? ` (nota ${r.nota})` : ""}
                </option>
              ))}
            </select>
          </div>
        )}

        {ehInformativo(pergunta.tipo) && (
          <div style={{ flex: "1 1 240px" }}>
            <label style={miniLabel}>{pergunta.tipo === "LISTA" ? "Itens" : "Resposta"}</label>
            <textarea value={campo.texto} rows={2} aria-label="Resposta"
              onChange={e => onChange({ texto: e.target.value })}
              placeholder={pergunta.tipo === "LISTA" && pergunta.regras.length > 0
                ? `Sugestões: ${pergunta.regras.map(r => r.texto).filter(Boolean).join(", ")}`
                : ""}
              style={{ ...controlStyle, width: "100%", resize: "vertical", fontFamily: "inherit" }} />
          </div>
        )}
      </div>

      <div style={{ marginTop: "8px" }}>
        <label style={miniLabel}>Observação (opcional)</label>
        <input value={campo.observacao} onChange={e => onChange({ observacao: e.target.value })}
          placeholder="Anote a justificativa desta avaliação"
          style={{ ...controlStyle, width: "100%" }} />
      </div>
    </div>
  );
}
