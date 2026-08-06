-- ═══════════════════════════════════════════════════════════════════════════
-- FocusLife Hub — Remoção da estrutura antiga de carteiras
-- Remove a coluna financas_id (e a FK associada) de tb_ativos e a tabela
-- antiga tb_financas. Os dados já foram copiados para carteira_investimento
-- e carteira_dividas na migration V8.
--
-- ⚠️ Aplicar SOMENTE depois que o código novo (V8 + refactor) estiver no ar
--    e os dados forem validados em produção.
-- ═══════════════════════════════════════════════════════════════════════════

-- Remove a coluna financas_id de tb_ativos (o PostgreSQL remove a FK junto).
-- Guarded: em banco novo essa coluna nunca existiu.
DO $$
DECLARE
    _has_col BOOLEAN;
BEGIN
    SELECT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = current_schema()
          AND table_name = 'tb_ativos'
          AND column_name = 'financas_id'
    ) INTO _has_col;

    IF _has_col THEN
        ALTER TABLE tb_ativos DROP COLUMN financas_id;
    END IF;
END $$;

-- Remove a tabela antiga, se ainda existir (em banco novo ela nunca existiu).
DROP TABLE IF EXISTS tb_financas;
