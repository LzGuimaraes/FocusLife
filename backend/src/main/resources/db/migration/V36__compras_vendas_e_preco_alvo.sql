-- ═══════════════════════════════════════════════════════════════════════════
-- FocusLife Hub — Compras/Vendas (lucro/prejuízo por operação) + Preço alvo
-- ═══════════════════════════════════════════════════════════════════════════
-- Duas tabelas novas, criadas de forma idempotente (CREATE ... IF NOT EXISTS):
--
--   • operacao_compra_venda — cada linha é uma operação: a COMPRA (custo total,
--     data, quantidade) e, quando encerrada, a VENDA (valor, data, quantidade
--     vendida). Lucro/rentabilidade NÃO ficam gravados: são calculados no
--     backend (CalculadoraOperacao), para o front nunca recalcular dinheiro.
--     `ativo_cadastro_id` (ticker do catálogo) e `ativo_id` (posição) são
--     informativos e opcionais; `ticker`/`nome` ficam em SNAPSHOT para a
--     operação continuar legível se o catálogo/posição forem excluídos.
--
--   • ativo_preco_alvo — regra de preço teto/mínimo por usuário+ativo, para a
--     UI sinalizar quando o preço atual está acima do teto ou abaixo do mínimo.
--
-- GUARDED (padrão de V29/V35): em banco NOVO o Hibernate (ddl-auto=update) cria
-- o schema DEPOIS do Flyway. As tabelas são criadas pela PRÓPRIA migration, sem
-- depender de nenhuma tabela criada pelo Hibernate — por isso não é preciso
-- tocar no DatabaseBootstrapConfig.
--
-- SEM FK de banco (mesmo padrão de `aporte_registro`, V29): user_id,
-- carteira_investimento_id, ativo_cadastro_id e ativo_id são referências
-- informativas validadas na aplicação. Isso evita depender da ordem de criação
-- das tabelas do Hibernate em banco novo.
-- ═══════════════════════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS operacao_compra_venda (
    id                       BIGSERIAL      PRIMARY KEY,
    user_id                  BIGINT         NOT NULL,
    carteira_investimento_id BIGINT,
    ativo_cadastro_id        UUID,
    ativo_id                 BIGINT,
    ticker                   VARCHAR(255),
    nome                     VARCHAR(255),
    classe                   VARCHAR(40),
    data_compra              DATE           NOT NULL,
    data_venda               DATE,
    quantidade               NUMERIC(18,8),
    quantidade_vendida       NUMERIC(18,8),
    valor_inicial            NUMERIC(14,2)  NOT NULL DEFAULT 0,
    valor_venda              NUMERIC(14,2),
    created_at               TIMESTAMP,
    updated_at               TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_operacao_compra_venda_user
    ON operacao_compra_venda (user_id);

CREATE INDEX IF NOT EXISTS idx_operacao_compra_venda_carteira
    ON operacao_compra_venda (carteira_investimento_id);

CREATE TABLE IF NOT EXISTS ativo_preco_alvo (
    id                BIGSERIAL     PRIMARY KEY,
    user_id           BIGINT        NOT NULL,
    ativo_cadastro_id UUID,
    ativo_id          BIGINT,
    ticker            VARCHAR(255),
    preco_teto        NUMERIC(14,2),
    preco_minimo      NUMERIC(14,2),
    created_at        TIMESTAMP,
    updated_at        TIMESTAMP
);

-- Unicidade por usuário+ativo em DOIS ÍNDICES ÚNICOS PARCIAIS: um unique comum
-- no Postgres trata cada NULL como distinto, então não deduplicaria nem a regra
-- por catálogo nem a por posição (a coluna "do outro tipo" é sempre NULL).
CREATE UNIQUE INDEX IF NOT EXISTS uq_ativo_preco_alvo_catalogo
    ON ativo_preco_alvo (user_id, ativo_cadastro_id)
    WHERE ativo_cadastro_id IS NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uq_ativo_preco_alvo_posicao
    ON ativo_preco_alvo (user_id, ativo_id)
    WHERE ativo_id IS NOT NULL;
