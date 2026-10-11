-- ═══════════════════════════════════════════════════════════════════════════
-- FocusLife Hub — Categoria opcional das despesas (gráfico por categoria)
-- ═══════════════════════════════════════════════════════════════════════════
-- A tabela despesa ganha a coluna `categoria` (VARCHAR), que guarda a
-- categoria do gasto (lista FIXA do enum CategoriaDespesa: MORADIA,
-- ALIMENTACAO, TRANSPORTE, SAUDE, EDUCACAO, LAZER, CONTAS, OUTROS).
--
-- É NULLABLE de propósito: o campo é OPCIONAL no cadastro — a despesa pode
-- ficar "sem categoria" e o gráfico a agrupa em "Sem categoria". Nenhum
-- default é aplicado às linhas existentes (elas ficam sem categoria).
--
-- "Guarded" (padrão de V29/V35): em banco NOVO a tabela despesa ainda não
-- existe quando o Flyway roda (o Hibernate a cria depois) — nesse caso o
-- bloco é no-op e a coluna é criada pelo próprio Hibernate (ddl-auto=update).
-- ═══════════════════════════════════════════════════════════════════════════

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'despesa') THEN
        ALTER TABLE despesa
            ADD COLUMN IF NOT EXISTS categoria VARCHAR(30);
    END IF;
END $$;
