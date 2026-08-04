-- ═══════════════════════════════════════════════════════════════════════════
-- FocusLife Hub — P11 (Horário de tarefas)
-- Adiciona a coluna horario em tb_tarefas (TIME, nullable).
-- Guarded: em banco novo o tb_tarefas ainda não existe quando o Flyway roda.
-- ═══════════════════════════════════════════════════════════════════════════

DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.tables
        WHERE table_schema = current_schema() AND table_name = 'tb_tarefas'
    ) AND NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = current_schema()
          AND table_name = 'tb_tarefas'
          AND column_name = 'horario'
    ) THEN
        ALTER TABLE tb_tarefas ADD COLUMN horario TIME;
    END IF;
END
$$;
