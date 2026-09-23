-- ═══════════════════════════════════════════════════════════════════════════
-- FocusLife Hub — Modelos de Checklist (Módulos 2, 3 e 4 / Fase 2)
--
--   • checklist_modelo                 → modelo reutilizável criado pelo usuário
--   • checklist_modelo_pergunta        → perguntas do modelo (sem limite)
--   • checklist_modelo_pergunta_regra  → REGRA de pontuação da pergunta
--
-- O sistema NÃO tem perguntas fixas: nada de ROE/P-L/Dividend Yield no schema.
-- Os critérios vivem no texto das perguntas que o usuário criar.
--
-- REGRA de pontuação (`..._pergunta_regra`) cobre os dois jeitos de pontuar:
--   • FAIXA  (tipo NUMERO/PERCENTUAL) → valor_min / valor_max / nota
--   • OPÇÃO  (tipo MULTIPLA_ESCOLHA/LISTA) → texto / nota
-- Usar uma tabela só evita duplicar estrutura para o mesmo conceito
-- ("como esta pergunta vale ponto"), e o DTO expõe tudo como um array único.
--
-- `conta_no_score`: quando false, a pergunta é informativa (texto, lista ou
-- número sem faixas) e NÃO entra no cálculo — o sistema nunca inventa nota.
-- ═══════════════════════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS checklist_modelo (
    id         BIGSERIAL     PRIMARY KEY,
    user_id    BIGINT        NOT NULL,
    nome       VARCHAR(120)  NOT NULL,
    descricao  VARCHAR(1000),
    tipo_alvo  VARCHAR(40),
    ativa      BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_checklist_modelo_user_id
    ON checklist_modelo (user_id);

CREATE TABLE IF NOT EXISTS checklist_modelo_pergunta (
    id             BIGSERIAL    PRIMARY KEY,
    modelo_id      BIGINT       NOT NULL,
    titulo         VARCHAR(255) NOT NULL,
    descricao      VARCHAR(1000),
    tipo           VARCHAR(30)  NOT NULL,
    peso           NUMERIC(9,4) NOT NULL DEFAULT 1,
    nota_maxima    NUMERIC(9,4) NOT NULL DEFAULT 10,
    conta_no_score BOOLEAN      NOT NULL DEFAULT TRUE,
    obrigatoria    BOOLEAN      NOT NULL DEFAULT FALSE,
    ordem          INTEGER      NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_checklist_modelo_pergunta_modelo
    ON checklist_modelo_pergunta (modelo_id);

CREATE TABLE IF NOT EXISTS checklist_modelo_pergunta_regra (
    id          BIGSERIAL     PRIMARY KEY,
    pergunta_id BIGINT        NOT NULL,
    texto       VARCHAR(150),
    valor_min   NUMERIC(18,4),
    valor_max   NUMERIC(18,4),
    nota        NUMERIC(9,4)  NOT NULL DEFAULT 0,
    ordem       INTEGER       NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_checklist_modelo_regra_pergunta
    ON checklist_modelo_pergunta_regra (pergunta_id);
