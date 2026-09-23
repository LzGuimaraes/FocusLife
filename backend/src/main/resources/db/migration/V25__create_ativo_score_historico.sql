-- ═══════════════════════════════════════════════════════════════════════════
-- FocusLife Hub — Histórico de avaliações (Módulo 8 / Fase 4)
--
-- Uma linha por ATIVO por DIA (não por checklist): o histórico do Módulo 8 é
-- "BBAS3 23/09 → 83%, 15/10 → 88%", ou seja, o score consolidado do ativo ao
-- longo do tempo.
--
-- POR QUE NÃO TEM FK
--   • carteira_investimento_id e ativo_id são referências informativas: o
--     histórico é um retrato do passado e precisa sobreviver à exclusão da
--     carteira ou da posição (mesmo critério do V16/V17 em workout_session).
--   • user_id TEM FK (o histórico pertence a alguém; sem dono não faz sentido).
--
-- SNAPSHOTS: ticker é copiado na gravação para que o histórico continue
-- legível se o ativo for renomeado ou sair do catálogo.
--
-- IDEMPOTÊNCIA POR DIA: dois índices únicos PARCIAIS (um para ativo do
-- catálogo, outro para posição) porque no Postgres um índice único comum com
-- coluna NULL não impede duplicatas. Registrar duas vezes no mesmo dia apenas
-- ATUALIZA a linha do dia.
-- ═══════════════════════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS ativo_score_historico (
    id                       BIGSERIAL     PRIMARY KEY,
    user_id                  BIGINT        NOT NULL,
    carteira_investimento_id BIGINT,
    ativo_cadastro_id        UUID,
    ativo_id                 BIGINT,
    ticker                   VARCHAR(120),
    data_referencia          DATE          NOT NULL,

    -- Qualidade (Módulo 5, consolidado dos checklists do ativo)
    quality_score            NUMERIC(9,4),
    total_checklists         INTEGER       NOT NULL DEFAULT 0,
    total_perguntas          INTEGER       NOT NULL DEFAULT 0,
    total_respondidas        INTEGER       NOT NULL DEFAULT 0,

    -- Prioridade de aporte e situação na carteira naquele dia (Módulos 1/6)
    contribution_score       NUMERIC(9,4),
    percentual_atual         NUMERIC(9,4),
    percentual_ideal         NUMERIC(9,4),
    deficit                  NUMERIC(18,2),
    excesso                  NUMERIC(18,2),
    valor_carteira_total     NUMERIC(18,2),
    prioridade_manual        INTEGER,

    observacao               VARCHAR(1000),
    created_at               TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_ativo_score_hist_data
    ON ativo_score_historico (user_id, data_referencia);

CREATE INDEX IF NOT EXISTS idx_ativo_score_hist_cadastro
    ON ativo_score_historico (user_id, ativo_cadastro_id, data_referencia);

CREATE INDEX IF NOT EXISTS idx_ativo_score_hist_carteira
    ON ativo_score_historico (user_id, carteira_investimento_id, data_referencia);

CREATE UNIQUE INDEX IF NOT EXISTS uq_ativo_score_hist_cadastro_dia
    ON ativo_score_historico (user_id, ativo_cadastro_id, data_referencia)
    WHERE ativo_cadastro_id IS NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uq_ativo_score_hist_posicao_dia
    ON ativo_score_historico (user_id, ativo_id, data_referencia)
    WHERE ativo_id IS NOT NULL;
