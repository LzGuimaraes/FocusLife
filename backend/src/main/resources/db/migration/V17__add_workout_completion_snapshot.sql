-- ═══════════════════════════════════════════════════════════════════════════
-- FocusLife Hub — Treinos: snapshot das métricas do exercício na ocorrência
--
-- Regra crítica (histórico imutável): quando o usuário edita um treino
-- (ex.: "Flexão 4×15 → 5×20"), as execuções anteriores DEVEM continuar
-- mostrando o que realmente foi planejado na época. Por isso cada
-- workout_exercise_completion passa a guardar uma cópia (snapshot) das
-- métricas no momento em que a ocorrência foi materializada.
--
-- Registros já existentes ficam com snapshot NULL e o backend faz fallback
-- para a definição atual (comportamento antigo) até a ocorrência seguinte.
-- ═══════════════════════════════════════════════════════════════════════════

ALTER TABLE workout_exercise_completion
    ADD COLUMN IF NOT EXISTS sets             INTEGER,
    ADD COLUMN IF NOT EXISTS repetitions      INTEGER,
    ADD COLUMN IF NOT EXISTS weight           REAL,
    ADD COLUMN IF NOT EXISTS duration_minutes INTEGER,
    ADD COLUMN IF NOT EXISTS distance_km      REAL;
