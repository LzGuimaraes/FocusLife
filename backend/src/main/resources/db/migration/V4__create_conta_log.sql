-- ═══════════════════════════════════════════════════════════════════════════
-- FocusLife Hub — P9 (Logs de contas)
-- Cria a tabela de auditoria de contas (conta_log).
-- ═══════════════════════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS conta_log (
    id            BIGSERIAL    PRIMARY KEY,
    conta_id      BIGINT       NOT NULL,
    acao          VARCHAR(255) NOT NULL,
    criado_em     TIMESTAMP    NOT NULL,
    atualizado_em TIMESTAMP    NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_conta_log_conta_id ON conta_log (conta_id);
