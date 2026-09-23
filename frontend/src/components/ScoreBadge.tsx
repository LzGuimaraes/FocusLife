import { fmtScore, scoreBg, scoreColor } from "../utils/avaliacao";

/**
 * Badge de Quality Score (Módulo 5).
 * Score nulo significa "nada pontuado foi respondido ainda" — nunca 0.
 */
export default function ScoreBadge({ score, label, size = "md" }: {
  score: number | null | undefined;
  label?: string;
  size?: "sm" | "md";
}) {
  const nulo = score == null;
  return (
    <span
      title={nulo ? "Nenhuma pergunta que pontua foi respondida ainda" : "Soma ponderada das suas notas"}
      style={{
        display: "inline-flex", alignItems: "center", gap: "4px",
        padding: size === "sm" ? "2px 8px" : "4px 12px",
        borderRadius: "9999px", fontWeight: 700,
        fontSize: size === "sm" ? "11px" : "13px",
        background: scoreBg(score), color: scoreColor(score),
        whiteSpace: "nowrap",
      }}
    >
      {label && <span style={{ fontWeight: 600, opacity: 0.85 }}>{label}</span>}
      {fmtScore(score)}
    </span>
  );
}
