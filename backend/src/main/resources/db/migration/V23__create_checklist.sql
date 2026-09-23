-- ═══════════════════════════════════════════════════════════════════════════
-- FocusLife Hub — Checklist do ativo (Módulos 2, 3 e 4 / Fase 2)
--
--   • checklist_ativo                 → INSTÂNCIA de checklist de um ativo
--   • checklist_ativo_pergunta        → perguntas da instância (+ resposta)
--   • checklist_ativo_pergunta_regra  → regra de pontuação copiada do modelo
--
-- SNAPSHOT (mesmo padrão do V17 em workout_exercise_completion): ao criar um
-- checklist a partir de um modelo, as perguntas e suas regras são COPIADAS.
-- Editar o modelo depois NÃO altera checklists existentes, e editar um
-- checklist não altera o modelo. Por isso não há FK para checklist_modelo —
-- `modelo_origem_id` é apenas uma referência informativa.
--
-- ÂNCORA (decisão do usuário): o checklist aponta para o ATIVO do catálogo
-- (`ativo_cadastro_id`) ou, quando o ativo não existe no catálogo (ex.: renda
-- fixa/posição específica), para a POSIÇÃO (`ativo_id`). Um dos dois é
-- obrigatório (CHECK).
--
-- Respostas: `nota_atribuida` é o que o usuário atribuiu (ou o que foi DERIVADO
-- da faixa, para tipos numéricos) e `valor_numerico`/`resposta_texto` guardam a
-- resposta crua, para que o histórico continue legível.
-- ═══════════════════════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS checklist_ativo (
    id                BIGSERIAL    PRIMARY KEY,
    user_id           BIGINT       NOT NULL,
    modelo_origem_id  BIGINT,
    ativo_cadastro_id UUID,
    ativo_id          BIGINT,
    nome              VARCHAR(120) NOT NULL,
    peso              NUMERIC(9,4) NOT NULL DEFAULT 1,
    ordem             INTEGER      NOT NULL DEFAULT 0,
    ativa             BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at        TIMESTAMP,
    updated_at        TIMESTAMP,
    CONSTRAINT ck_checklist_ativo_alvo
        CHECK (ativo_cadastro_id IS NOT NULL OR ativo_id IS NOT NULL)
);

CREATE INDEX IF NOT EXISTS idx_checklist_ativo_user
    ON checklist_ativo (user_id);

CREATE INDEX IF NOT EXISTS idx_checklist_ativo_cadastro
    ON checklist_ativo (user_id, ativo_cadastro_id);

CREATE INDEX IF NOT EXISTS idx_checklist_ativo_posicao
    ON checklist_ativo (user_id, ativo_id);

CREATE TABLE IF NOT EXISTS checklist_ativo_pergunta (
    id             BIGSERIAL     PRIMARY KEY,
    checklist_id   BIGINT        NOT NULL,
    titulo         VARCHAR(255)  NOT NULL,
    descricao      VARCHAR(1000),
    tipo           VARCHAR(30)   NOT NULL,
    peso           NUMERIC(9,4)  NOT NULL DEFAULT 1,
    nota_maxima    NUMERIC(9,4)  NOT NULL DEFAULT 10,
    conta_no_score BOOLEAN       NOT NULL DEFAULT TRUE,
    ordem          INTEGER       NOT NULL DEFAULT 0,
    nota_atribuida NUMERIC(9,4),
    valor_numerico NUMERIC(18,4),
    resposta_texto VARCHAR(1000),
    observacao     VARCHAR(1000),
    respondido_em  TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_checklist_ativo_pergunta_checklist
    ON checklist_ativo_pergunta (checklist_id);

CREATE TABLE IF NOT EXISTS checklist_ativo_pergunta_regra (
    id          BIGSERIAL     PRIMARY KEY,
    pergunta_id BIGINT        NOT NULL,
    texto       VARCHAR(150),
    valor_min   NUMERIC(18,4),
    valor_max   NUMERIC(18,4),
    nota        NUMERIC(9,4)  NOT NULL DEFAULT 0,
    ordem       INTEGER       NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_checklist_ativo_regra_pergunta
    ON checklist_ativo_pergunta_regra (pergunta_id);
