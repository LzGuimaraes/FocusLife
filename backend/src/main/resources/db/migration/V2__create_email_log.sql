-- ═══════════════════════════════════════════════════════════════════════════
-- FocusLife Hub — P3 (Infra de e-mail)
-- Cria a tabela de log de e-mails com índice único para evitar duplicidade.
--
-- O índice único em (usuario_id, tipo, referencia_id, data_referencia)
-- garante que um mesmo e-mail não seja enviado duas vezes para o mesmo
-- usuário/referência no mesmo dia (as colunas do índice são NOT NULL para
-- evitar que NULLs burlem a unicidade no PostgreSQL).
-- ═══════════════════════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS email_log (
    id              BIGSERIAL    PRIMARY KEY,
    usuario_id      BIGINT       NOT NULL,
    tipo            VARCHAR(50)  NOT NULL,
    referencia_id   BIGINT       NOT NULL,
    data_referencia DATE         NOT NULL,
    enviado_em      TIMESTAMP    NOT NULL,
    CONSTRAINT uk_email_log_usuario_tipo_ref_data
        UNIQUE (usuario_id, tipo, referencia_id, data_referencia)
);
