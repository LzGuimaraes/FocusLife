-- ═══════════════════════════════════════════════════════════════════════════
-- FocusLife Hub — Habilita geração de UUID no banco
-- ═══════════════════════════════════════════════════════════════════════════
--
-- O tipo uuid já é nativo do PostgreSQL, mas a FUNÇÃO gen_random_uuid() só
-- existe de fábrica a partir do PostgreSQL 13. Em versões anteriores ela é
-- fornecida pela extensão pgcrypto. Esta migration habilita a extensão de
-- forma idempotente, garantindo que comandos como:
--
--   INSERT INTO ativo_cadastro (id, nome, tipo, preco_atual)
--   VALUES (gen_random_uuid(), 'PETR4', 'ACAO', 41.93) ...;
--
-- funcionem em qualquer versão do banco.
--
-- ⚠️ CREATE EXTENSION exige um usuário com privilégio (superuser ou dono do
--    banco). Na maioria das VPS o usuário do app tem esse direito.
-- ═══════════════════════════════════════════════════════════════════════════

CREATE EXTENSION IF NOT EXISTS pgcrypto;
