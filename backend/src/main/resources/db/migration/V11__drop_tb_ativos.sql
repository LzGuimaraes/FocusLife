-- ═══════════════════════════════════════════════════════════════════════════
-- FocusLife Hub — Remoção da estrutura antiga de ativos/contas
-- Remove a tabela antiga tb_ativos. Os dados já foram copiados para as
-- tabelas ativo e despesa na migration V10.
--
-- ⚠️ Aplicar SOMENTE depois que o código novo (V10 + refactor) estiver no ar
--    e os dados forem validados em produção.
-- ═══════════════════════════════════════════════════════════════════════════

DROP TABLE IF EXISTS tb_ativos;
