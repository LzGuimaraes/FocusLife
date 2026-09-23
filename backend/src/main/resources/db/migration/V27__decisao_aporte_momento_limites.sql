-- ═══════════════════════════════════════════════════════════════════════════
-- FocusLife Hub — Motor de decisão de aporte (Fase 1)
--
-- Cinco blocos, todos ADITIVOS (nada é removido nem reinterpretado):
--
--   1. TIPO DO CHECKLIST: separa QUALIDADE (o ativo é bom?) de MOMENTO
--      (é bom aportar agora?). São perguntas diferentes e não podem se
--      substituir — reusa toda a infraestrutura de perguntas, pesos e faixas.
--
--   2. CRITÉRIO ELIMINATÓRIO: pergunta marcada como bloqueadora. Se ela for
--      reprovada (nota abaixo de `nota_minima`, ou zero quando não definida),
--      o ativo NÃO recebe aporte, independentemente do déficit.
--
--   3. TOLERÂNCIA e LIMITE MÁXIMO por nível da Carteira Ideal
--      (classe, subclasse e ativo). Tolerância = faixa em que o item é
--      considerado EQUILIBRADO; limite máximo = teto de concentração que
--      bloqueia novos aportes mesmo com déficit.
--
--   4. TETO DO ATIVO: o valor que o ativo pode receber é o déficit DELE
--      (percentual ideal + tolerância), então a tolerância por nível é o que
--      dá espaço quando a meta já foi atingida.
--
--   5. FATOR DE MOMENTO E REDISTRIBUIÇÃO (score_config): o fator (0 a 1) sai
--      de faixas de nota configuráveis e NÃO altera a qualidade; a
--      redistribuição decide se o dinheiro que não achou destino elegível vai
--      para outras classes ou fica não alocado.
--
-- Convenção do projeto: ADD COLUMN IF NOT EXISTS, com NOT NULL + DEFAULT para
-- não quebrar as linhas existentes (o Hibernate só cria colunas novas em banco
-- vazio, nunca altera as existentes).
-- ═══════════════════════════════════════════════════════════════════════════

-- ── 1. Tipo do checklist (modelo e instância) ──────────────────────────────
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables
               WHERE table_schema = current_schema() AND table_name = 'checklist_modelo') THEN
        ALTER TABLE checklist_modelo ADD COLUMN IF NOT EXISTS tipo VARCHAR(20) NOT NULL DEFAULT 'QUALIDADE';
    END IF;

    IF EXISTS (SELECT 1 FROM information_schema.tables
               WHERE table_schema = current_schema() AND table_name = 'checklist_ativo') THEN
        ALTER TABLE checklist_ativo ADD COLUMN IF NOT EXISTS tipo VARCHAR(20) NOT NULL DEFAULT 'QUALIDADE';
    END IF;
END $$;

-- ── 2. Critério eliminatório (modelo e instância) ─────────────────────────
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables
               WHERE table_schema = current_schema() AND table_name = 'checklist_modelo_pergunta') THEN
        ALTER TABLE checklist_modelo_pergunta ADD COLUMN IF NOT EXISTS bloqueadora BOOLEAN NOT NULL DEFAULT FALSE;
        ALTER TABLE checklist_modelo_pergunta ADD COLUMN IF NOT EXISTS nota_minima NUMERIC(9,4);
    END IF;

    IF EXISTS (SELECT 1 FROM information_schema.tables
               WHERE table_schema = current_schema() AND table_name = 'checklist_ativo_pergunta') THEN
        ALTER TABLE checklist_ativo_pergunta ADD COLUMN IF NOT EXISTS bloqueadora BOOLEAN NOT NULL DEFAULT FALSE;
        ALTER TABLE checklist_ativo_pergunta ADD COLUMN IF NOT EXISTS nota_minima NUMERIC(9,4);
    END IF;
END $$;

-- ── 3. Tolerância e limite máximo de concentração por nível ───────────────
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables
               WHERE table_schema = current_schema() AND table_name = 'carteira_ideal_classe') THEN
        ALTER TABLE carteira_ideal_classe ADD COLUMN IF NOT EXISTS tolerancia NUMERIC(9,4) NOT NULL DEFAULT 0;
        ALTER TABLE carteira_ideal_classe ADD COLUMN IF NOT EXISTS limite_maximo NUMERIC(9,4);
    END IF;

    IF EXISTS (SELECT 1 FROM information_schema.tables
               WHERE table_schema = current_schema() AND table_name = 'carteira_ideal_subclasse') THEN
        ALTER TABLE carteira_ideal_subclasse ADD COLUMN IF NOT EXISTS tolerancia NUMERIC(9,4) NOT NULL DEFAULT 0;
        ALTER TABLE carteira_ideal_subclasse ADD COLUMN IF NOT EXISTS limite_maximo NUMERIC(9,4);
    END IF;

    IF EXISTS (SELECT 1 FROM information_schema.tables
               WHERE table_schema = current_schema() AND table_name = 'meta_ativo') THEN
        ALTER TABLE meta_ativo ADD COLUMN IF NOT EXISTS tolerancia NUMERIC(9,4) NOT NULL DEFAULT 0;
        ALTER TABLE meta_ativo ADD COLUMN IF NOT EXISTS limite_maximo NUMERIC(9,4);
    END IF;
END $$;

-- ── 4. Fator de momento e redistribuição ─────────────────────────────────
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables
               WHERE table_schema = current_schema() AND table_name = 'score_config') THEN
        -- Peso do termo MOMENTO no Contribution Score (0 desliga o termo).
        ALTER TABLE score_config ADD COLUMN IF NOT EXISTS peso_momento NUMERIC(9,4) NOT NULL DEFAULT 3;

        -- Faixas da NOTA DE MOMENTO → FATOR (não altera a qualidade, só a
        -- prioridade de aporte): até 25 → 0 | até 50 → 0,25 | até 75 → 0,50 |
        -- até 90 → 0,75 | acima → 1,00.
        ALTER TABLE score_config ADD COLUMN IF NOT EXISTS momento_faixa_1 NUMERIC(9,4) NOT NULL DEFAULT 25;
        ALTER TABLE score_config ADD COLUMN IF NOT EXISTS momento_faixa_2 NUMERIC(9,4) NOT NULL DEFAULT 50;
        ALTER TABLE score_config ADD COLUMN IF NOT EXISTS momento_faixa_3 NUMERIC(9,4) NOT NULL DEFAULT 75;
        ALTER TABLE score_config ADD COLUMN IF NOT EXISTS momento_faixa_4 NUMERIC(9,4) NOT NULL DEFAULT 90;

        -- true = o valor que não achou destino elegível procura outra classe
        -- com déficit; false = fica não alocado (com o motivo explicado).
        ALTER TABLE score_config ADD COLUMN IF NOT EXISTS redistribuir BOOLEAN NOT NULL DEFAULT TRUE;
    END IF;
END $$;

-- ═══════════════════════════════════════════════════════════════════════════
-- VERIFICAÇÃO (rodar após aplicar):
--   SELECT tipo, count(*) FROM checklist_modelo GROUP BY tipo;
--   SELECT count(*) FILTER (WHERE bloqueadora) AS bloqueadoras
--   FROM checklist_ativo_pergunta;
--   SELECT redistribuir, peso_momento, momento_faixa_1, momento_faixa_4
--   FROM score_config;
-- ═══════════════════════════════════════════════════════════════════════════
