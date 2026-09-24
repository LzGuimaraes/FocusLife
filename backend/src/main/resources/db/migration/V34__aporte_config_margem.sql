-- ═══════════════════════════════════════════════════════════════════════════
-- FocusLife Hub — Margem operacional do motor de aportes
--
-- O motor de aporte NÃO trava um ativo porque ele está "no alvo": a meta orienta
-- e o LIMITE OPERACIONAL é que impede a concentração. Esse limite é
--
--     limiteOperacional = meta × (1 + margem)
--
-- e a margem é do USUÁRIO (não da carteira, não da classe): ela descreve o
-- quanto ele aceita passar da meta antes de considerar que o ativo está cheio.
-- Ex.: meta 5% com margem 5% → limite 5,25% (RELATIVO à meta, nunca
-- "5% + 5 pontos percentuais").
--
-- Por que no banco e não no código: 3% a 5% é uma decisão de risco do usuário,
-- e a mesma metodologia vale para todas as carteiras dele. Sem linha gravada o
-- sistema usa 5% e NÃO cria nada (só o PUT grava), igual ao padrão do projeto.
--
-- user_id UNIQUE = um registro por usuário.
-- ═══════════════════════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS aporte_config (
    id                BIGSERIAL   PRIMARY KEY,
    user_id           BIGINT      NOT NULL,
    margem_percentual NUMERIC(5,2) NOT NULL DEFAULT 5.00,
    updated_at        TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_aporte_config_user
    ON aporte_config (user_id);

-- ═══════════════════════════════════════════════════════════════════════════
-- VERIFICAÇÃO (rodar após aplicar):
--   SELECT user_id, margem_percentual FROM aporte_config;
-- ═══════════════════════════════════════════════════════════════════════════
