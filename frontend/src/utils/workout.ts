/* ══════════════════════════════════════════
   Treinos — helpers de formatação
   ══════════════════════════════════════════ */

interface ExerciseMetrics {
  sets?: number | null;
  repetitions?: number | null;
  weight?: number | null;
  durationMinutes?: number | null;
  distanceKm?: number | null;
}

/** Formata número sem zeros decimais desnecessários (20 → "20", 22.5 → "22.5"). */
const fmtNum = (n: number): string => {
  const rounded = Math.round(n * 100) / 100;
  return Number.isInteger(rounded) ? String(rounded) : String(rounded);
};

/**
 * Formata as métricas de um exercício usando APENAS os dados cadastrados pelo
 * usuário. Campos não preenchidos são omitidos — nunca exibe "0 kg",
 * "0 repetições" ou "0 min".
 *
 *   Flexão  (4 séries, 15 reps)         → "4 séries × 15 repetições"
 *   Supino  (4 séries, 10 reps, 20 kg)  → "4 séries × 10 repetições • 20 kg"
 *   Corrida (5 km, 30 min)              → "5 km • 30 min"
 *   Formas  (45 min)                    → "45 min"
 */
export function formatExerciseMetrics(e: ExerciseMetrics): string {
  const main: string[] = [];
  if (e.sets != null) main.push(`${fmtNum(e.sets)} séries`);
  if (e.repetitions != null) main.push(`${fmtNum(e.repetitions)} repetições`);
  const mainStr = main.join(" × ");

  const extra: string[] = [];
  if (e.weight != null) extra.push(`${fmtNum(e.weight)} kg`);
  if (e.durationMinutes != null) extra.push(`${fmtNum(e.durationMinutes)} min`);
  if (e.distanceKm != null) extra.push(`${fmtNum(e.distanceKm)} km`);

  return [mainStr, ...extra].filter(Boolean).join(" • ");
}
