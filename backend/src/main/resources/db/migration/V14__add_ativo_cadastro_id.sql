-- ═══════════════════════════════════════════════════════════════════════════
-- FocusLife Hub — Relaciona a posição (ativo) com o ativo do catálogo
-- ═══════════════════════════════════════════════════════════════════════════
-- A tabela ativo (posições do usuário) passa a guardar o ID do ativo do
-- catálogo (ativo_cadastro), para que a posição saiba a qual ativo cadastrado
-- ela se refere. O catálogo continua global (sem dono); quem passa a ter o ID
-- do ativo é a posição do usuário.
--
-- A coluna é apenas adicionada aqui (idempotente); a restrição de FK é criada
-- pelo Hibernate (ddl-auto=update) quando o mapeamento JPA subir.

ALTER TABLE ativo ADD COLUMN IF NOT EXISTS ativo_cadastro_id UUID;

CREATE INDEX IF NOT EXISTS idx_ativo_ativo_cadastro_id
    ON ativo (ativo_cadastro_id);
