-- ═══════════════════════════════════════════════════════════════════════════
-- FocusLife Hub — P14 (Sessões de estudo / tempo acumulado)
-- ═══════════════════════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS sessao_estudo (
    id               BIGSERIAL   PRIMARY KEY,
    materia_id       BIGINT      NOT NULL,
    inicio           TIMESTAMP   NOT NULL,
    fim              TIMESTAMP,
    duracao_segundos BIGINT      NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_sessao_estudo_materia_id
    ON sessao_estudo (materia_id);
