-- ═══════════════════════════════════════════════════════════════════════════
-- FocusLife Hub — Divisão de tb_ativos (contas) em duas tabelas:
--   • ativo    → antigas linhas com categoria = 'INVESTIMENTO'
--   • despesa  → antigas linhas com categoria = 'CONTA'
--
-- OBJETIVO: preservar TODOS os dados de produção (inclusive os IDs, para que
-- conta_log continue apontando certo) e eliminar a coluna discriminadora
-- categoria, que deixa de existir nas novas tabelas.
--
-- IMPORTANTE (ordem de deploy): aplicar junto com o código novo (V10 no mesmo
-- deploy); o V11 remove a tabela antiga SOMENTE depois de validar os dados.
-- ═══════════════════════════════════════════════════════════════════════════

-- 1) Cria as novas tabelas (idempotente; em banco novo o Hibernate também cria)
CREATE TABLE IF NOT EXISTS ativo (
    id                        BIGSERIAL    PRIMARY KEY,
    nome                      VARCHAR(255),
    saldo                     REAL,
    data_vencimento           DATE,
    categoria_investimento    VARCHAR(255),
    quantidade                REAL,
    valor_unitario            REAL,
    preco_atual               REAL,
    instituicao               VARCHAR(255),
    data_aplicacao            VARCHAR(255),
    vencimento                VARCHAR(255),
    rentabilidade             REAL,
    carteira_investimento_id  BIGINT
);

CREATE INDEX IF NOT EXISTS idx_ativo_carteira_investimento_id
    ON ativo (carteira_investimento_id);

CREATE TABLE IF NOT EXISTS despesa (
    id                    BIGSERIAL    PRIMARY KEY,
    nome                  VARCHAR(255),
    saldo                 REAL,
    data_vencimento       DATE,
    pago                  BOOLEAN,
    carteira_dividas_id   BIGINT
);

CREATE INDEX IF NOT EXISTS idx_despesa_carteira_dividas_id
    ON despesa (carteira_dividas_id);

-- 2) Copia os dados de tb_ativos preservando os IDs.
--    Guarded: em banco NOVO a tabela tb_ativos ainda não existe quando o
--    Flyway roda (o Hibernate cria depois) — mesmo padrão das V3/V5/V6/V8.
DO $$
DECLARE
    _has_old BOOLEAN;
BEGIN
    SELECT EXISTS (
        SELECT 1
        FROM information_schema.tables
        WHERE table_schema = current_schema()
          AND table_name = 'tb_ativos'
    ) INTO _has_old;

    IF _has_old THEN
        -- Ativos (investimentos)
        INSERT INTO ativo (id, nome, saldo, data_vencimento, categoria_investimento,
                           quantidade, valor_unitario, preco_atual, instituicao,
                           data_aplicacao, vencimento, rentabilidade, carteira_investimento_id)
        SELECT id, nome, saldo, data_vencimento, categoria_investimento,
               quantidade, valor_unitario, preco_atual, instituicao,
               data_aplicacao, vencimento, rentabilidade, carteira_investimento_id
        FROM tb_ativos
        WHERE categoria = 'INVESTIMENTO'
        ON CONFLICT (id) DO NOTHING;

        -- Despesas (contas a pagar)
        INSERT INTO despesa (id, nome, saldo, data_vencimento, pago, carteira_dividas_id)
        SELECT id, nome, saldo, data_vencimento, pago, carteira_dividas_id
        FROM tb_ativos
        WHERE categoria = 'CONTA'
        ON CONFLICT (id) DO NOTHING;
    END IF;
END $$;

-- 3) Ajusta as sequências de ID (como copiamos IDs explícitos, o nextval
--    precisa continuar de onde paramos para não gerar PK duplicada).
SELECT setval(pg_get_serial_sequence('ativo', 'id'),
              GREATEST((SELECT COALESCE(MAX(id), 1) FROM ativo), 1));
SELECT setval(pg_get_serial_sequence('despesa', 'id'),
              GREATEST((SELECT COALESCE(MAX(id), 1) FROM despesa), 1));

-- 4) VERIFICAÇÃO (rodar após a migration em produção):
--    -- Totais:
--    SELECT 'ativo'   AS tipo, COUNT(*) FROM ativo
--    UNION ALL
--    SELECT 'despesa',        COUNT(*) FROM despesa
--    UNION ALL
--    SELECT 'antiga',         COUNT(*) FROM tb_ativos;
--    A soma das duas novas deve ser igual ao total da antiga.
--
--    -- Checar se há linhas que NÃO seriam copiadas (categoria NULL/inválida):
--    SELECT categoria, COUNT(*) FROM tb_ativos GROUP BY categoria;
--
-- 5) PRÓXIMO PASSO (só depois que o código novo estiver no ar e validado):
--    A migration V11 remove a tabela tb_ativos.
