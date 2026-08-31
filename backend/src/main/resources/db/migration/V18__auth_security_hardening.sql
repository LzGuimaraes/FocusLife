-- ═══════════════════════════════════════════════════════════════════════════
-- FocusLife Hub — Endurecimento de segurança de autenticação
--
-- 1) activation_code_expiration: o código de ativação agora expira (24h),
--    eliminando o "link eterno" de ativação.
-- 2) token_version: versão dos JWTs do usuário; é incrementada ao trocar ou
--    redefinir a senha, invalidando imediatamente todos os tokens antigos.
-- ═══════════════════════════════════════════════════════════════════════════

ALTER TABLE tb_user
    ADD COLUMN IF NOT EXISTS activation_code_expiration TIMESTAMP,
    ADD COLUMN IF NOT EXISTS token_version INTEGER DEFAULT 0;
