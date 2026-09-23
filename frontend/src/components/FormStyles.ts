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

/* ══════════════════════════════════════════════════════════════════════
   Tokens de layout das telas densas de configuração (Carteira Ideal).

   Existem para dar HIERARQUIA e RESPIRO: poucos níveis tipográficos, todos
   com o mesmo significado em qualquer bloco da tela — número grande é sempre
   o valor que o usuário decide; rótulo pequeno é sempre contexto.
   ══════════════════════════════════════════════════════════════════════ */

/** Cartão de seção com respiro maior (mais padding que `boxStyle`). */
export const sectionCard: CSSProperties = {
  background: "white", borderRadius: "16px", padding: "22px 24px",
  boxShadow: "0 1px 3px rgba(15,23,42,0.05)", border: "1px solid #eef2f7",
};

/** Título de seção. */
export const sectionTitle: CSSProperties = {
  fontSize: "16px", fontWeight: 800, color: "#0f172a", margin: 0, letterSpacing: "-0.01em",
};

/** Texto de apoio logo abaixo do título. */
export const sectionSubtitle: CSSProperties = {
  fontSize: "12.5px", color: "#64748b", margin: "5px 0 0", lineHeight: 1.55,
};

/** Rótulo miúdo em caixa alta (contexto, nunca o dado). */
export const overline: CSSProperties = {
  fontSize: "10px", fontWeight: 800, color: "#94a3b8",
  textTransform: "uppercase", letterSpacing: "0.6px",
};

/** Número de destaque — o dado que o usuário olha primeiro. */
export const numGrande: CSSProperties = {
  fontSize: "24px", fontWeight: 800, color: "#0f172a",
  letterSpacing: "-0.02em", fontVariantNumeric: "tabular-nums", lineHeight: 1.1,
};

/** Número intermediário (linhas e cards). */
export const numMedio: CSSProperties = {
  fontSize: "15px", fontWeight: 700, color: "#0f172a",
  fontVariantNumeric: "tabular-nums", lineHeight: 1.2,
};

/** Entrada numérica grande: digitação confortável e alinhada à direita. */
export const numInput: CSSProperties = {
  ...controlStyle, width: "88px", textAlign: "right", fontSize: "16px", fontWeight: 700,
  fontVariantNumeric: "tabular-nums", padding: "9px 10px",
};

/** Botão +/- do seletor numérico. */
export const stepperBtn = (disabled = false): CSSProperties => ({
  width: "32px", height: "32px", borderRadius: "9px", border: "1.5px solid #e2e8f0",
  background: "white", color: "#475569", fontSize: "16px", fontWeight: 700, lineHeight: 1,
  cursor: disabled ? "not-allowed" : "pointer", opacity: disabled ? 0.4 : 1,
  display: "inline-flex", alignItems: "center", justifyContent: "center", flexShrink: 0,
});

/** Pílula de status (compara valores sem precisar de texto corrido). */
export const chip = (cor: string, bg: string): CSSProperties => ({
  display: "inline-flex", alignItems: "center", gap: "6px", fontSize: "11.5px", fontWeight: 700,
  padding: "4px 10px", borderRadius: "9999px", color: cor, background: bg, whiteSpace: "nowrap",
});

/** Card de notificação do sistema (aviso que pede uma ação). */
export const noticeCard = (cor: string, bg: string, borda: string): CSSProperties => ({
  background: bg, border: `1px solid ${borda}`, borderLeft: `4px solid ${cor}`,
  borderRadius: "12px", padding: "14px 16px",
});

/** Botão principal (ação que fecha o fluxo). */
export const primaryBtn: CSSProperties = {
  background: "#6366f1", color: "white", border: "none", borderRadius: "10px",
  padding: "10px 18px", fontSize: "13.5px", fontWeight: 700, cursor: "pointer",
  boxShadow: "0 1px 2px rgba(99,102,241,0.35)",
};

/** Botão secundário (ação alternativa, sem competir com a principal). */
export const secondaryBtn: CSSProperties = {
  background: "white", color: "#4338ca", border: "1.5px solid #c7d2fe",
  borderRadius: "10px", padding: "9px 16px", fontSize: "13px", fontWeight: 700, cursor: "pointer",
};

/** Controle segmentado (abas e alternâncias de visualização). */
export const segmented: CSSProperties = {
  display: "inline-flex", gap: "3px", background: "#eef2f7", padding: "3px", borderRadius: "11px",
};

export const segmentBtn = (ativo: boolean): CSSProperties => ({
  padding: "7px 13px", borderRadius: "8px", border: "none", cursor: "pointer",
  fontSize: "12.5px", fontWeight: 700, whiteSpace: "nowrap",
  background: ativo ? "white" : "transparent",
  color: ativo ? "#0f172a" : "#64748b",
  boxShadow: ativo ? "0 1px 2px rgba(15,23,42,0.08)" : "none",
});
