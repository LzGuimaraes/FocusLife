-- ═══════════════════════════════════════════════════════════════════════════
-- FocusLife Hub — Treinos (Workout)
--   • workout                      → treino recorrente do usuário (planejamento)
--   • workout_exercise             → exercícios do treino (definição)
--   • workout_session              → ocorrência semanal (execução)
--   • workout_exercise_completion  → checklist de exercícios da ocorrência
--
-- SEM FKs no banco (mesmo padrão V6/V7): o Hibernate gerencia as relações via
-- JPA. As sessions guardam snapshots (title / exercise_name) para que o
-- histórico sobreviva à edição/exclusão do treino de origem.
-- Ocorrências são criadas sob demanda (datas <= hoje), nunca em massa no futuro.
-- ═══════════════════════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS workout (
    id          BIGSERIAL    PRIMARY KEY,
    title       VARCHAR(255) NOT NULL,
    description TEXT,
    day_of_week VARCHAR(10)  NOT NULL,
    is_active   BOOLEAN      NOT NULL DEFAULT TRUE,
    user_id     BIGINT       NOT NULL,
    created_at  TIMESTAMP,
    updated_at  TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_workout_user_id
    ON workout (user_id);

CREATE TABLE IF NOT EXISTS workout_exercise (
    id               BIGSERIAL    PRIMARY KEY,
    workout_id       BIGINT       NOT NULL,
    name             VARCHAR(255) NOT NULL,
    sets             INTEGER,
    repetitions      INTEGER,
    weight           REAL,
    duration_minutes INTEGER,
    distance_km      REAL,
    notes            TEXT,
    sort_order       INTEGER      NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_workout_exercise_workout_id
    ON workout_exercise (workout_id);

CREATE TABLE IF NOT EXISTS workout_session (
    id                    BIGSERIAL    PRIMARY KEY,
    workout_id            BIGINT       NOT NULL,
    user_id               BIGINT       NOT NULL,
    title                 VARCHAR(255) NOT NULL,
    scheduled_date        DATE         NOT NULL,
    status                VARCHAR(10)  NOT NULL DEFAULT 'PENDING',
    completion_percentage INTEGER      NOT NULL DEFAULT 0,
    completed_at          TIMESTAMP,
    notes                 TEXT,
    created_at            TIMESTAMP,
    updated_at            TIMESTAMP,
    CONSTRAINT uq_workout_session UNIQUE (workout_id, scheduled_date)
);

CREATE INDEX IF NOT EXISTS idx_workout_session_user_date
    ON workout_session (user_id, scheduled_date);

CREATE TABLE IF NOT EXISTS workout_exercise_completion (
    id                   BIGSERIAL    PRIMARY KEY,
    workout_session_id   BIGINT       NOT NULL,
    workout_exercise_id  BIGINT,
    exercise_name        VARCHAR(255) NOT NULL,
    completed            BOOLEAN      NOT NULL DEFAULT FALSE,
    notes                TEXT
);

CREATE INDEX IF NOT EXISTS idx_workout_ex_completion_session
    ON workout_exercise_completion (workout_session_id);
