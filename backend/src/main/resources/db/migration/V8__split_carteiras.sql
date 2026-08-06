-- ═══════════════════════════════════════════════════════════════════════════
-- FocusLife Hub — Divisão de tb_financas (carteiras) em duas tabelas:
--   • carteira_investimento  → antigas carteiras com tipo_carteira = 'INVESTIMENTO'
--   • carteira_dividas       → antigas carteiras com tipo_carteira = 'DESPESAS'
--
-- OBJETIVO: preservar TODOS os dados de produção (inclusive os IDs, para não
-- quebrar a FK financas_id de tb_ativos) e eliminar a coluna discriminadora
-- tipo_carteira, que deixa de existir nas novas tabelas.
--
-- IMPORTANTE (ordem de deploy):
--   1. Alterar o código (entidades/repos/serviços/controllers/frontend) para
--      usar as novas tabelas e aplicar esta migration no MESMO deploy.
--   2. Após validar os dados nas novas tabelas, remover tb_financas numa
--      migration posterior (V9) — NUNCA antes de o código novo estar no ar.
-- ═══════════════════════════════════════════════════════════════════════════

-- 1) Cria as novas tabelas (idempotente; em banco novo o Hibernate também cria)
CREATE TABLE IF NOT EXISTS carteira_investimento (
    id         BIGSERIAL    PRIMARY KEY,
    nome       VARCHAR(255),
    moeda      VARCHAR(255),
    user_id    BIGINT
);

CREATE INDEX IF NOT EXISTS idx_carteira_investimento_user_id
    ON carteira_investimento (user_id);

CREATE TABLE IF NOT EXISTS carteira_dividas (
    id         BIGSERIAL    PRIMARY KEY,
    nome       VARCHAR(255),
    moeda      VARCHAR(255),
    user_id    BIGINT
);

CREATE INDEX IF NOT EXISTS idx_carteira_dividas_user_id
    ON carteira_dividas (user_id);

-- 2) Copia os dados de tb_financas preservando os IDs.
--    Guarded: em banco NOVO a tabela tb_financas ainda não existe quando o
--    Flyway roda (ela é criada pelo Hibernate depois), então verificamos se
--    ela existe antes de copiar — mesmo padrão das migrations V3/V5/V6.
DO $$
DECLARE
    _has_old BOOLEAN;
BEGIN
    SELECT EXISTS (
        SELECT 1
        FROM information_schema.tables
        WHERE table_schema = current_schema()
          AND table_name = 'tb_financas'
    ) INTO _has_old;

    IF _has_old THEN
        -- Carteiras de investimento
        INSERT INTO carteira_investimento (id, nome, moeda, user_id)
        SELECT id, nome, moeda, user_id
        FROM tb_financas
        WHERE tipo_carteira = 'INVESTIMENTO'
        ON CONFLICT (id) DO NOTHING;

        -- Carteiras de dívidas / despesas
        INSERT INTO carteira_dividas (id, nome, moeda, user_id)
        SELECT id, nome, moeda, user_id
        FROM tb_financas
        WHERE tipo_carteira = 'DESPESAS'
        ON CONFLICT (id) DO NOTHING;
    END IF;
END $$;

-- 3) Ajusta as sequências de ID (como copiamos IDs explícitos, o nextval
--    precisa continuar de onde paramos para não gerar PK duplicada).
SELECT setval(pg_get_serial_sequence('carteira_investimento', 'id'),
              GREATEST((SELECT COALESCE(MAX(id), 1) FROM carteira_investimento), 1));
SELECT setval(pg_get_serial_sequence('carteira_dividas', 'id'),
              GREATEST((SELECT COALESCE(MAX(id), 1) FROM carteira_dividas), 1));

-- 4) tb_ativos (contas) passa a usar COLUNAS DEDICADAS:
--    carteira_investimento_id e carteira_dividas_id (estratégia escolhida).
--    Guarded: em banco NOVO a tabela tb_ativos ainda não existe quando o
--    Flyway roda (o Hibernate cria depois) — mesmo padrão das V3/V5/V6.
DO $$
DECLARE
    _has_ativos BOOLEAN;
    _has_old   BOOLEAN;
BEGIN
    SELECT EXISTS (
        SELECT 1
        FROM information_schema.tables
        WHERE table_schema = current_schema()
          AND table_name = 'tb_ativos'
    ) INTO _has_ativos;

    SELECT EXISTS (
        SELECT 1
        FROM information_schema.tables
        WHERE table_schema = current_schema()
          AND table_name = 'tb_financas'
    ) INTO _has_old;

    IF _has_ativos THEN
        -- Novas colunas de FK (nullable)
        ALTER TABLE tb_ativos ADD COLUMN IF NOT EXISTS carteira_investimento_id BIGINT;
        ALTER TABLE tb_ativos ADD COLUMN IF NOT EXISTS carteira_dividas_id BIGINT;

        -- Aponta cada conta para a carteira correta, conforme o tipo da carteira antiga
        IF _has_old THEN
            UPDATE tb_ativos a
            SET carteira_investimento_id = a.financas_id
            FROM tb_financas f
            WHERE a.financas_id IS NOT NULL
              AND a.financas_id = f.id
              AND f.tipo_carteira = 'INVESTIMENTO';

            UPDATE tb_ativos a
            SET carteira_dividas_id = a.financas_id
            FROM tb_financas f
            WHERE a.financas_id IS NOT NULL
              AND a.financas_id = f.id
              AND f.tipo_carteira = 'DESPESAS';
        END IF;

        CREATE INDEX IF NOT EXISTS idx_ativos_carteira_investimento_id
            ON tb_ativos (carteira_investimento_id);
        CREATE INDEX IF NOT EXISTS idx_ativos_carteira_dividas_id
            ON tb_ativos (carteira_dividas_id);
    END IF;
END $$;

-- 5) VERIFICAÇÃO (rodar após a migration em produção):
--    -- Carteiras:
--    SELECT 'investimento' AS tipo, COUNT(*) FROM carteira_investimento
--    UNION ALL
--    SELECT 'dividas',      COUNT(*) FROM carteira_dividas
--    UNION ALL
--    SELECT 'antiga',       COUNT(*) FROM tb_financas;
--    A soma das duas novas deve ser igual ao total da antiga.
--
--    -- Contas (todas devem ter uma carteira dedicada preenchida):
--    SELECT COUNT(*) AS total,
--           COUNT(*) FILTER (WHERE carteira_investimento_id IS NOT NULL) AS invest,
--           COUNT(*) FILTER (WHERE carteira_dividas_id IS NOT NULL) AS dividas
--    FROM tb_ativos;
--
-- 6) PRÓXIMO PASSO (só depois que o código novo estiver no ar e validado):
--    A migration V9 remove a coluna financas_id (com a FK) e a tabela tb_financas.
