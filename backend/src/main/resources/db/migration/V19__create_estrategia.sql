-- ═══════════════════════════════════════════════════════════════════════════
-- FocusLife Hub — Estratégia de Investimentos (Módulo Planejamento / Fase 1)
--
-- A estratégia é OPCIONAL e pertence ao USUÁRIO. Ela serve apenas como
-- container/agrupador da metodologia (nome + descrição). Uma carteira de
-- investimento pode apontar para uma estratégia (FK nullable), o que mantém
-- total compatibilidade com as carteiras já existentes (estrategia_id NULL).
--
-- SEM FK no SQL (mesmo padrão V16): o Hibernate (ddl-auto=update) cria as
-- relações a partir do mapeamento JPA quando a aplicação sobe após o Flyway.
-- ═══════════════════════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS estrategia_investimento (
    id         BIGSERIAL     PRIMARY KEY,
    user_id    BIGINT        NOT NULL,
    nome       VARCHAR(120)  NOT NULL,
    descricao  VARCHAR(1000),
    ativa      BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_estrategia_investimento_user_id
    ON estrategia_investimento (user_id);

-- Vínculo opcional carteira → estratégia (nullable: carteiras antigas seguem
-- funcionando sem estratégia definida).
ALTER TABLE carteira_investimento
    ADD COLUMN IF NOT EXISTS estrategia_id BIGINT;

CREATE INDEX IF NOT EXISTS idx_carteira_investimento_estrategia_id
    ON carteira_investimento (estrategia_id);
