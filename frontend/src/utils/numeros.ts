/* ══════════════════════════════════════════════════════════════════════
   Conversão de número ↔ texto para campos editáveis em pt-BR
   (a vírgula é aceita na digitação e normalizada antes de enviar).
   ══════════════════════════════════════════════════════════════════════ */

/** 40.0000 (JSON) → "40" · 4.5 → "4,5" */
export const numParaTexto = (v: number | null | undefined): string =>
  v == null ? "" : String(v).replace(".", ",");

/** "4,5" → 4.5 (0 quando não é número) */
export const textoParaNum = (v: string): number => {
  const n = parseFloat(v.replace(",", "."));
  return Number.isFinite(n) ? n : 0;
};

/** "4,5" → 4.5 · "" → null (para campos opcionais) */
export const textoParaNumOuNull = (v: string): number | null =>
  (v == null || v.trim() === "") ? null : textoParaNum(v);

/** Mantém apenas dígitos, vírgula, ponto e sinal negativo. */
export const apenasNumero = (v: string): string => v.replace(/[^0-9.,-]/g, "");
