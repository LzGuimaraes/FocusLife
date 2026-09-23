import { boxStyle, linkBtnStyle } from "./FormStyles";
import PerguntaEditor, {
  novaPerguntaDraft, validarPerguntaDraft, type PerguntaDraft,
} from "./PerguntaEditor";

/* ══════════════════════════════════════════════════════════════════════
   Builder de perguntas de um checklist (Módulo 3): adiciona, reordena (↑↓)
   e remove perguntas, delegando a edição de cada uma ao PerguntaEditor.
   ══════════════════════════════════════════════════════════════════════ */

interface Props {
  perguntas: PerguntaDraft[];
  onChange: (perguntas: PerguntaDraft[]) => void;
  titulo?: string;
  descricao?: string;
}

export default function ChecklistBuilder({ perguntas, onChange, titulo, descricao }: Props) {
  const mover = (index: number, delta: number) => {
    const destino = index + delta;
    if (destino < 0 || destino >= perguntas.length) return;
    const copia = [...perguntas];
    [copia[index], copia[destino]] = [copia[destino], copia[index]];
    onChange(copia);
  };

  const pontuadas = perguntas.filter(p => {
    const v = validarPerguntaDraft(p);
    return v === null;
  }).length;

  return (
    <div style={boxStyle}>
      <div style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-start", flexWrap: "wrap", gap: "8px", marginBottom: "12px" }}>
        <div>
          <h3 style={{ fontSize: "15px", fontWeight: 700, color: "#0f172a", margin: 0 }}>
            {titulo ?? "Perguntas"}
          </h3>
          <p style={{ fontSize: "12px", color: "#64748b", margin: "2px 0 0" }}>
            {descricao ?? "Não há limite de perguntas. Os critérios e os pesos são seus."}
          </p>
        </div>
        <span style={{ fontSize: "12px", fontWeight: 600, color: pontuadas === perguntas.length ? "#047857" : "#b45309" }}>
          {perguntas.length} pergunta(s)
        </span>
      </div>

      {perguntas.length === 0 && (
        <p style={{ fontSize: "13px", color: "#94a3b8", margin: "0 0 12px" }}>
          Nenhuma pergunta ainda. Adicione a primeira para montar seu critério de avaliação.
        </p>
      )}

      <div style={{ display: "flex", flexDirection: "column", gap: "10px" }}>
        {perguntas.map((p, i) => (
          <PerguntaEditor
            key={p.key}
            pergunta={p}
            index={i}
            total={perguntas.length}
            onChange={nova => onChange(perguntas.map(x => (x.key === nova.key ? nova : x)))}
            onRemove={() => onChange(perguntas.filter(x => x.key !== p.key))}
            onMove={delta => mover(i, delta)}
          />
        ))}
      </div>

      <div style={{ marginTop: "12px" }}>
        <button type="button" style={linkBtnStyle}
          onClick={() => onChange([...perguntas, novaPerguntaDraft()])}>
          + Adicionar pergunta
        </button>
      </div>
    </div>
  );
}
