-- ═══════════════════════════════════════════════════════════════════════════
-- FocusLife Hub — P6 (Data de vencimento)
-- Adiciona a coluna data_vencimento em tb_ativos.
--
-- Guarded: em banco NOVO a tabela tb_ativos ainda não existe (o Hibernate
-- cria as tabelas DEPOIS do Flyway), então o ALTER é pulado sem erro.
--
-- Default temporário: como não há coluna de "data de criação" na tabela,
-- usa-se CURRENT_DATE (a data em que a migration roda) para preencher as
-- linhas existentes; logo após, o default é removido para que o app passe
-- a fornecer o valor (o DTO exige @NotNull).
-- ═══════════════════════════════════════════════════════════════════════════

DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.tables
        WHERE table_schema = current_schema() AND table_name = 'tb_ativos'
    ) AND NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = current_schema()
          AND table_name = 'tb_ativos'
          AND column_name = 'data_vencimento'
    ) THEN
        ALTER TABLE tb_ativos ADD COLUMN data_vencimento DATE NOT NULL DEFAULT CURRENT_DATE;
        ALTER TABLE tb_ativos ALTER COLUMN data_vencimento DROP DEFAULT;
    END IF;
END
$$;
