-- ═══════════════════════════════════════════════════════════════════════════
-- FocusLife Hub — Histórico de APORTES (§24 do spec de decisão)
--
-- Guarda o aporte que o usuário EXECUTOU (não o sugerido): data, valor, ativo,
-- preço, quantidade e o percentual do ativo na carteira ANTES e DEPOIS. Serve
-- para o motor avisar quando um ativo já recebeu muito dinheiro em pouco tempo
-- — o spec pede o histórico justamente para "evitar concentração excessiva de
-- novos aportes".
--
-- `ativo_cadastro_id` (ticker do catálogo) e `ativo_id` (posição, para renda
-- fixa/caixinha) são informativos e mutuamente opcionais: se a posição for
-- excluída depois, o histórico continua legível pelo `ticker`.
-- ═══════════════════════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS aporte_registro (
    id                       BIGSERIAL    PRIMARY KEY,
    user_id                  BIGINT,
    carteira_investimento_id BIGINT,
    data                     DATE         NOT NULL,
    ativo_cadastro_id        UUID,
    ativo_id                 BIGINT,
    ticker                   VARCHAR(255),
    classe                   VARCHAR(40),
    valor                    NUMERIC(18,4) NOT NULL DEFAULT 0,
    preco                    NUMERIC(18,4),
    quantidade               NUMERIC(18,8),
    percentual_antes         NUMERIC(9,4),
    percentual_depois        NUMERIC(9,4),
    observacao               VARCHAR(1000),
    created_at               TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_aporte_registro_carteira_data
    ON aporte_registro (carteira_investimento_id, data DESC);

CREATE INDEX IF NOT EXISTS idx_aporte_registro_user_id
    ON aporte_registro (user_id);

-- ═══════════════════════════════════════════════════════════════════════════
-- VERIFICAÇÃO (rodar após aplicar):
--   SELECT data, ticker, valor, percentual_antes, percentual_depois
--   FROM aporte_registro ORDER BY data DESC LIMIT 20;
-- ═══════════════════════════════════════════════════════════════════════════
