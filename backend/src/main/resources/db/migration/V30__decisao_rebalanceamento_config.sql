-- ═══════════════════════════════════════════════════════════════════════════
-- FocusLife Hub — Decisão: rebalanceamento, teto configurável e precedência
--
-- Três escolhas que estavam FIXAS no motor e o spec (§17, §23, §36, §37) pede
-- como configuráveis. Todas com o default igual ao comportamento de hoje, para
-- nada mudar sozinho:
--
--   1. `rebalancear` — quando ligado, o motor também sugere REDUZIR o que está
--      acima do alvo (dentro da faixa de tolerância) e soma esse valor ao
--      orçamento do aporte. Continua sendo SUGESTÃO: o usuário decide.
--
--   2. `teto_ativo_modo` — TETO_ESTRITO (padrão): o ativo recebe no máximo o
--      déficit dele + tolerância. TETO_ATE_A_CLASSE: o limite do ativo passa a
--      ser o déficit da classe/subclasse a que ele pertence — útil quando as
--      metas por ticker espelham a carteira e nenhum ativo teria espaço. O
--      limite máximo de concentração continua valendo nos dois modos.
--
--   3. `precedencia` — ordem das travas, em CSV de identificadores:
--      BLOQUEIO,LIMITE,CLASSE,SUBCLASSE,SETOR,TETO_ATIVO,MOMENTO,SCORE.
--      A ordem define (a) como a tela explica o motivo do bloqueio e (b) a lista
--      de precedência mostrada ao usuário. As travas de coerência continuam
--      sendo aplicadas sempre (nenhum ajuste faz o motor ultrapassar um teto).
-- ═══════════════════════════════════════════════════════════════════════════

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables
               WHERE table_schema = current_schema() AND table_name = 'score_config') THEN
        ALTER TABLE score_config ADD COLUMN IF NOT EXISTS rebalancear BOOLEAN NOT NULL DEFAULT FALSE;
        ALTER TABLE score_config ADD COLUMN IF NOT EXISTS teto_ativo_modo VARCHAR(30) NOT NULL DEFAULT 'TETO_ESTRITO';
        ALTER TABLE score_config ADD COLUMN IF NOT EXISTS precedencia VARCHAR(300)
            NOT NULL DEFAULT 'BLOQUEIO,LIMITE,CLASSE,SUBCLASSE,SETOR,TETO_ATIVO,MOMENTO,SCORE';
    END IF;
END $$;

-- ═══════════════════════════════════════════════════════════════════════════
-- VERIFICAÇÃO (rodar após aplicar):
--   SELECT rebalancear, teto_ativo_modo, precedencia FROM score_config;
-- ═══════════════════════════════════════════════════════════════════════════
