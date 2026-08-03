/* ══════════════════════════════════════════
   Datas — helpers de data LOCAL (yyyy-MM-dd)

   Motivo: `new Date("yyyy-MM-dd")` interpreta a string como meia-noite em
   UTC, o que causa deslocamento de 1 dia em fusos com offset negativo
   (ex.: America/Cuiaba, UTC-4). Estes helpers sempre usam os valores
   locais do navegador (getFullYear/getMonth/getDate).
   ══════════════════════════════════════════ */

/** Converte um Date local em string "yyyy-MM-dd" (sem shift de timezone). */
export function formatLocalDate(date: Date): string {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");
  return `${year}-${month}-${day}`;
}

/**
 * Converte string "yyyy-MM-dd" em Date local.
 * NUNCA use `new Date("yyyy-MM-dd")` direto: ele interpreta em UTC e
 * desloca a data em 1 dia em fusos negativos.
 */
export function parseLocalDate(str: string): Date {
  const [year, month, day] = str.slice(0, 10).split("-").map(Number);
  return new Date(year, month - 1, day);
}
