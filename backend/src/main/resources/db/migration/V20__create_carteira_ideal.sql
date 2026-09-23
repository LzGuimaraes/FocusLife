-- ═══════════════════════════════════════════════════════════════════════════
-- FocusLife Hub — Carteira Ideal (Módulo 1 / Fase 1)
--
--   • carteira_ideal_classe     → % ideal por CLASSE (soma = 100%)
--   • carteira_ideal_subclasse  → % ideal por SUBCLASSE dentro de uma classe
--                                 (soma das subclasses ≤ % da classe)
--
-- A classe é o enum CategoriaInvestimento (persistido como STRING), o que
-- permite comparar diretamente o ideal com as posições reais da tabela
-- `ativo` (que já usam esse mesmo enum). "OUTROS" funciona como bucket.
--
-- Percentuais em NUMERIC(9,4) (nunca REAL) para evitar erro de arredondamento
-- na validação da soma = 100%.
-- ═══════════════════════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS carteira_ideal_classe (
    id                       BIGSERIAL    PRIMARY KEY,
    carteira_investimento_id BIGINT       NOT NULL,
    classe                   VARCHAR(40)  NOT NULL,
    percentual_ideal         NUMERIC(9,4) NOT NULL DEFAULT 0,
    ordem                    INTEGER      NOT NULL DEFAULT 0
);

-- Uma classe só pode aparecer uma vez por carteira.
CREATE UNIQUE INDEX IF NOT EXISTS uq_carteira_ideal_classe
    ON carteira_ideal_classe (carteira_investimento_id, classe);

CREATE INDEX IF NOT EXISTS idx_carteira_ideal_classe_carteira
    ON carteira_ideal_classe (carteira_investimento_id);

CREATE TABLE IF NOT EXISTS carteira_ideal_subclasse (
    id               BIGSERIAL    PRIMARY KEY,
    classe_id        BIGINT       NOT NULL,
    nome             VARCHAR(80)  NOT NULL,
    percentual_ideal NUMERIC(9,4) NOT NULL DEFAULT 0,
    ordem            INTEGER      NOT NULL DEFAULT 0
);

-- O nome da subclasse é único dentro da classe (o frontend referencia
-- subclasses por nome ao salvar as metas de ativos).
CREATE UNIQUE INDEX IF NOT EXISTS uq_carteira_ideal_subclasse
    ON carteira_ideal_subclasse (classe_id, nome);

CREATE INDEX IF NOT EXISTS idx_carteira_ideal_subclasse_classe
    ON carteira_ideal_subclasse (classe_id);
