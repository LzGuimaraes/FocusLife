-- ═══════════════════════════════════════════════════════════════════════════
-- FocusLife Hub — Tipo de lançamento na carteira de dívidas (DESPESA | SALDO)
-- ═══════════════════════════════════════════════════════════════════════════
-- A tabela despesa passa a guardar também "saldos" (créditos): o usuário
-- lança tanto débitos (despesas) quanto créditos (saldos) e a carteira
-- mostra o saldo final = Σ(saldos) − Σ(despesas).
--
-- A coluna é adicionada de forma idempotente e COM DEFAULT, para que as
-- linhas existentes continuem sendo despesas (nada muda no histórico).
-- O bloco é "guarded" porque, em banco NOVO, a tabela despesa ainda não
-- existe quando o Flyway roda (o Hibernate a cria depois) — ver
-- DatabaseBootstrapConfig / migrations V3/V5/V6.
--
-- A restrição/FK, se necessária, fica a cargo do Hibernate (ddl-auto=update).

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'despesa') THEN
        ALTER TABLE despesa
            ADD COLUMN IF NOT EXISTS tipo VARCHAR(20) NOT NULL DEFAULT 'DESPESA';
    END IF;
END $$;
