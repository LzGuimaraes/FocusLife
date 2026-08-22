-- ═══════════════════════════════════════════════════════════════════════════
-- FocusLife Hub — Backfill do vínculo posição → catálogo
-- ═══════════════════════════════════════════════════════════════════════════
-- Preenche ativo.ativo_cadastro_id para as posições (cards) que ainda não têm
-- vínculo com o catálogo, casando pelo nome do ticker (case-insensitive e sem
-- espaços nas pontas).
--
-- Só afeta posições com ativo_cadastro_id IS NULL cujo nome bate exatamente
-- com um ativo do catálogo (ativo_cadastro.nome é UNIQUE). Operação única e
-- barata (UPDATE em massa, sem loop em aplicação).
-- ═══════════════════════════════════════════════════════════════════════════

UPDATE ativo a
SET ativo_cadastro_id = c.id
FROM ativo_cadastro c
WHERE a.ativo_cadastro_id IS NULL
  AND a.nome IS NOT NULL
  AND upper(trim(a.nome)) = upper(trim(c.nome));
