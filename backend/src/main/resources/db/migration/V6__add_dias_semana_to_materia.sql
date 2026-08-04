-- ═══════════════════════════════════════════════════════════════════════════
-- FocusLife Hub — P14 (Dias de estudo da matéria)
-- Tabela de coleção de @ElementCollection (materia.diasSemana).
-- Sem FK para funcionar também em banco novo (tb_materia ainda não existe
-- quando o Flyway roda, antes do Hibernate).
-- ═══════════════════════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS materia_dias_semana (
    materia_id BIGINT       NOT NULL,
    dia_semana VARCHAR(20)  NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_materia_dias_semana_materia_id
    ON materia_dias_semana (materia_id);
