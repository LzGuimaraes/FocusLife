-- ═══════════════════════════════════════════════════════════════════════════
-- FocusLife Hub — Configuração de pontuação (Módulos 6 e 9 / Fase 3)
--
--   • peso_quality    → quanto o Quality Score pesa na prioridade de aporte
--   • peso_deficit    → quanto a distância até a meta pesa
--   • peso_excesso    → quanto o excesso sobre a meta REDUZ a prioridade
--   • peso_prioridade → quanto a prioridade manual (0-10) pesa
--   • estrategia_aporte → como ratear o valor do aporte entre os ativos
--
-- A fórmula NÃO é hardcoded: o usuário define os pesos e o sistema apenas
-- aplica. Por isso os pesos ficam no banco (por USUÁRIO) e não no código.
--
-- Um registro por usuário (user_id UNIQUE). Sem registro, o sistema usa os
-- pesos padrão (definidos no enum TermoScore) sem criar linha — só o PUT
-- grava. A metodologia pertence ao usuário, não à carteira.
-- ═══════════════════════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS score_config (
    id                BIGSERIAL    PRIMARY KEY,
    user_id           BIGINT       NOT NULL,
    peso_quality      NUMERIC(9,4) NOT NULL,
    peso_deficit      NUMERIC(9,4) NOT NULL,
    peso_excesso      NUMERIC(9,4) NOT NULL,
    peso_prioridade   NUMERIC(9,4) NOT NULL,
    estrategia_aporte VARCHAR(30)  NOT NULL,
    updated_at        TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_score_config_user
    ON score_config (user_id);
