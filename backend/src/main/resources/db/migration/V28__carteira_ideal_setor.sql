-- ═══════════════════════════════════════════════════════════════════════════
-- FocusLife Hub — SETOR como nível da Carteira Ideal (Fase 2 da decisão)
--
-- A hierarquia passa a ser configurável em um nível a mais:
--
--     CARTEIRA → CLASSE → SUBCLASSE → SETOR → ATIVO
--
-- O setor pertence a uma SUBCLASSE (ex.: Ações → Bancos → "Bancos grandes"),
-- tem meta própria (fatia da SUBCLASSE), tolerância e limite de concentração.
-- É OPCIONAL: subclasse sem setor com alvo continua sendo o "bucket" do rateio,
-- então nada do que já existe muda de comportamento.
--
-- Posse dos ativos no setor (mesmo padrão da V26, que liga a posição à
-- subclasse):
--   • `meta_ativo.setor_id`  → setor de um ticker do catálogo (planejamento)
--   • `ativo.setor_id`       → setor de uma POSIÇÃO sem ticker (renda fixa)
-- ═══════════════════════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS carteira_ideal_setor (
    id               BIGSERIAL    PRIMARY KEY,
    nome             VARCHAR(80)  NOT NULL,
    percentual_ideal NUMERIC(9,4) NOT NULL DEFAULT 0,
    tolerancia       NUMERIC(9,4) NOT NULL DEFAULT 0,
    limite_maximo    NUMERIC(9,4),
    ordem            INTEGER      NOT NULL DEFAULT 0,
    subclasse_id     BIGINT
);

CREATE INDEX IF NOT EXISTS idx_carteira_ideal_setor_subclasse_id
    ON carteira_ideal_setor (subclasse_id);

-- ── Vínculo do setor com o ticker (meta) e com a posição (sem ticker) ──────
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables
               WHERE table_schema = current_schema() AND table_name = 'meta_ativo') THEN
        ALTER TABLE meta_ativo ADD COLUMN IF NOT EXISTS setor_id BIGINT;
        CREATE INDEX IF NOT EXISTS idx_meta_ativo_setor_id ON meta_ativo (setor_id);
    END IF;

    IF EXISTS (SELECT 1 FROM information_schema.tables
               WHERE table_schema = current_schema() AND table_name = 'ativo') THEN
        ALTER TABLE ativo ADD COLUMN IF NOT EXISTS setor_id BIGINT;
        CREATE INDEX IF NOT EXISTS idx_ativo_setor_id ON ativo (setor_id);
    END IF;
END $$;

-- ═══════════════════════════════════════════════════════════════════════════
-- VERIFICAÇÃO (rodar após aplicar):
--   SELECT count(*) FROM carteira_ideal_setor;
--   SELECT count(*) FILTER (WHERE setor_id IS NOT NULL) AS com_setor, count(*)
--   FROM meta_ativo;
-- ═══════════════════════════════════════════════════════════════════════════
