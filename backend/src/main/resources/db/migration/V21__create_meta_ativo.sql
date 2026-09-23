-- ═══════════════════════════════════════════════════════════════════════════
-- FocusLife Hub — Meta individual de ativo (Módulos 1 e 7 / Fase 1)
--
--   • percentual_ideal   → quanto este ativo deveria representar na carteira
--   • prioridade_manual  → 0 a 10, definida pelo usuário (Módulo 7)
--
-- O ativo alvo é o do CATÁLOGO (ativo_cadastro / ticker), pois a meta é sobre
-- o ativo, não sobre a linha de posição. A classe fica denormalizada para
-- permitir validar (Σ metas da classe ≤ % da classe) sem JOIN obrigatório;
-- a subclasse é opcional (deriva da classe quando informada).
--
-- SEM FK no SQL (padrão V16): o Hibernate cria as relações via JPA.
-- ═══════════════════════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS meta_ativo (
    id                       BIGSERIAL    PRIMARY KEY,
    carteira_investimento_id BIGINT       NOT NULL,
    ativo_cadastro_id        UUID         NOT NULL,
    classe                   VARCHAR(40)  NOT NULL,
    subclasse_id             BIGINT,
    percentual_ideal         NUMERIC(9,4) NOT NULL DEFAULT 0,
    prioridade_manual        INTEGER      NOT NULL DEFAULT 0,
    ordem                    INTEGER      NOT NULL DEFAULT 0
);

-- Um ativo tem no máximo uma meta por carteira.
CREATE UNIQUE INDEX IF NOT EXISTS uq_meta_ativo
    ON meta_ativo (carteira_investimento_id, ativo_cadastro_id);

CREATE INDEX IF NOT EXISTS idx_meta_ativo_carteira
    ON meta_ativo (carteira_investimento_id);

CREATE INDEX IF NOT EXISTS idx_meta_ativo_subclasse
    ON meta_ativo (subclasse_id);

CREATE INDEX IF NOT EXISTS idx_meta_ativo_ativo_cadastro
    ON meta_ativo (ativo_cadastro_id);
