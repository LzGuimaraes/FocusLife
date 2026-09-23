-- ═══════════════════════════════════════════════════════════════════════════
-- FocusLife Hub — Subclasse da POSIÇÃO (renda fixa / ativos sem ticker)
--
-- PROBLEMA: a meta da Carteira Ideal era sempre amarrada a um ativo do
-- catálogo (`meta_ativo.ativo_cadastro_id`). Renda fixa, Tesouro e caixinhas
-- (ex.: "Caixa PICPAY") NÃO têm ticker — sem ticker não havia meta, e sem meta
-- a posição ficava fora do comparativo por subclasse e da prioridade de aporte,
-- mesmo representando 21% da carteira.
--
-- SOLUÇÃO: a POSIÇÃO pode pertencer a uma subclasse da Carteira Ideal
-- (`carteira_ideal_subclasse`). A meta continua sendo o percentual da
-- SUBCLASSE, e qualquer posição atribuída a ela passa a contar para o alvo —
-- com ou sem ticker.
--
-- Convenção do projeto: nullable, sem FK declarada (o Hibernate cria) e
-- idempotente com IF NOT EXISTS, para rodar tanto em banco novo quanto
-- existente. Posições já vinculadas por `meta_ativo` continuam funcionando
-- (a meta é o fallback quando `subclasse_id` está vazio).
-- ═══════════════════════════════════════════════════════════════════════════

DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.tables
        WHERE table_schema = current_schema() AND table_name = 'ativo'
    ) THEN
        ALTER TABLE ativo ADD COLUMN IF NOT EXISTS subclasse_id BIGINT;

        CREATE INDEX IF NOT EXISTS idx_ativo_subclasse_id
            ON ativo (subclasse_id);
    END IF;
END $$;

-- ═══════════════════════════════════════════════════════════════════════════
-- VERIFICAÇÃO (rodar após aplicar):
--   SELECT count(*) FILTER (WHERE subclasse_id IS NOT NULL) AS com_subclasse,
--          count(*) AS total
--   FROM ativo;
-- ═══════════════════════════════════════════════════════════════════════════
