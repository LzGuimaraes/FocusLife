import type { CSSProperties } from "react";

/* ══════════════════════════════════════════════════════════════════════
   Estilos compartilhados pelos editores "inline" do módulo de Planejamento
   (classes da Carteira Ideal, metas, perguntas de checklist).

   O projeto usa estilos inline; estes são os que se repetiam em mais de um
   editor, então ficam em um só lugar.
   ══════════════════════════════════════════════════════════════════════ */

/** Cartão branco que envolve um bloco de edição. */
export const boxStyle: CSSProperties = {
  background: "white", borderRadius: "12px", padding: "18px",
  boxShadow: "0 1px 3px rgba(0,0,0,0.06)", border: "1px solid #f1f5f9",
};

/** Input/select compacto para linhas editáveis. */
export const controlStyle: CSSProperties = {
  padding: "8px 10px", fontSize: "13px", borderRadius: "8px",
  border: "1.5px solid #e2e8f0", background: "white", color: "#0f172a", outline: "none",
};

/** Botão "adicionar linha" (tracejado). */
export const linkBtnStyle: CSSProperties = {
  background: "transparent", border: "1.5px dashed #cbd5e1", borderRadius: "8px",
  padding: "6px 12px", fontSize: "12px", fontWeight: 600, color: "#6366f1", cursor: "pointer",
};

/** Rótulo pequeno acima de um campo inline. */
export const miniLabel: CSSProperties = {
  display: "block", fontSize: "11px", fontWeight: 600, color: "#64748b", marginBottom: "3px",
};

/** Botão de ação pequeno (↑ ↓ 🗑). */
export const iconBtn = (disabled = false): CSSProperties => ({
  background: "#f1f5f9", border: "none", borderRadius: "6px", width: "28px", height: "28px",
  cursor: disabled ? "not-allowed" : "pointer", fontSize: "13px", color: "#475569",
  opacity: disabled ? 0.4 : 1, lineHeight: 1,
});
