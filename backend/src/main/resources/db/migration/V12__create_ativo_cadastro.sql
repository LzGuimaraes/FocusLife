-- ═══════════════════════════════════════════════════════════════════════════
-- FocusLife Hub — Catálogo de ativos cadastrados (renda variável)
-- Tabela usada no autocomplete do cadastro de investimentos. Os ativos são
-- cadastrados manualmente (INSERT) — aqui a tabela é apenas criada.
-- ═══════════════════════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS ativo_cadastro (
    id           UUID         PRIMARY KEY,
    nome         VARCHAR(255) NOT NULL UNIQUE,
    tipo         VARCHAR(255),
    preco_atual  REAL
);

CREATE INDEX IF NOT EXISTS idx_ativo_cadastro_nome ON ativo_cadastro (nome);

-- ─────────────────────────────────────────────────────────────────────────────
-- EXEMPLO de cadastro manual (rodar no banco de produção quando quiser):
--
-- INSERT INTO ativo_cadastro (id, nome, tipo, preco_atual) VALUES
--   (gen_random_uuid(), 'PETR4',  'ACAO',        24.51),
--   (gen_random_uuid(), 'WEGE3',  'ACAO',        54.82),
--   (gen_random_uuid(), 'XPLG11', 'FII',         104.20),
--   (gen_random_uuid(), 'BOVA11', 'ETF',         13.90),
--   (gen_random_uuid(), 'W1LD34', 'BDR',         45.30),
--   (gen_random_uuid(), 'BTC',    'CRIPTOMOEDA', 350000.00),
--   (gen_random_uuid(), 'ETH',    'CRIPTOMOEDA', 18000.00);
-- ─────────────────────────────────────────────────────────────────────────────
