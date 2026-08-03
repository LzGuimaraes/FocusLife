-- ═══════════════════════════════════════════════════════════════════════════
-- FocusLife Hub — P1 (Timezone)
-- Converte colunas que representam APENAS data (sem hora) de TIMESTAMP para DATE.
--
-- Aplicada AUTOMATICAMENTE pelo Flyway no startup do backend
-- (localização padrão: classpath:db/migration).
--
-- Cada ALTER é guardado por um IF: só executa se a coluna existir e ainda
-- for do tipo timestamp. Isso permite funcionar em:
--   • banco existente  → converte timestamp -> date;
--   • banco novo       → tabelas ainda não criadas pelo Hibernate, nada a
--                        fazer (sem erro).
--
-- Observação: a conversão `timestamp -> date` trunca a parte da hora.
-- ═══════════════════════════════════════════════════════════════════════════

DO $$
BEGIN
    -- Tarefas: prazo (era java.util.Date -> timestamp; agora LocalDate -> date)
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = current_schema()
          AND table_name = 'tb_tarefas'
          AND column_name = 'prazo'
          AND data_type IN ('timestamp without time zone', 'timestamp with time zone')
    ) THEN
        ALTER TABLE tb_tarefas ALTER COLUMN prazo TYPE DATE USING prazo::date;
    END IF;

    -- Estudos: data (era java.util.Date -> timestamp; agora LocalDate -> date)
    -- O nome da tabela contém hífen e por isso é citado entre aspas duplas.
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = current_schema()
          AND table_name = 'tb-estudos'
          AND column_name = 'data'
          AND data_type IN ('timestamp without time zone', 'timestamp with time zone')
    ) THEN
        ALTER TABLE "tb-estudos" ALTER COLUMN data TYPE DATE USING data::date;
    END IF;

    -- Metas: prazo (era java.util.Date -> timestamp; agora LocalDate -> date)
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = current_schema()
          AND table_name = 'tb_metas'
          AND column_name = 'prazo'
          AND data_type IN ('timestamp without time zone', 'timestamp with time zone')
    ) THEN
        ALTER TABLE tb_metas ALTER COLUMN prazo TYPE DATE USING prazo::date;
    END IF;
END
$$;
