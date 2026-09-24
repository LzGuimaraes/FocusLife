-- V32 — CATÁLOGO GLOBAL DE SETORES (setor_mercado).
--
-- Até aqui "setor" era só um balde DENTRO de uma carteira
-- (carteira_ideal_subclasse → carteira_ideal_setor, com % alvo): o mesmo setor
-- tinha ids diferentes em cada carteira e o TICKER não carregava setor nenhum.
-- Isso impedia responder "quanto eu tenho no setor Bancos?" e impedia o motor de
-- decidir aporte POR SETOR (setor + preço + checklist).
--
-- A partir daqui o setor é uma entidade de REFERÊNCIA, como `ativo_cadastro`:
--
--   • setor_mercado              — catálogo global (nome, slug, segmento, ativo)
--   • ativo_cadastro.setor_mercado_id  — setor DO TICKER (sobrevive à carteira e
--     vale para qualquer carteira/usuário). É o que o sync do catálogo alimenta
--     quando a classificação é feita manualmente no banco.
--   • carteira_ideal_setor.setor_mercado_id — liga o balde da carteira (que tem
--     o % alvo) ao setor global (que tem a identidade). O `nome` continua na
--     tabela para exibição e para os setores criados antes desta migração.
--
-- As FKs NÃO são declaradas aqui de propósito: quem manda no esquema é o
-- Hibernate (mesma convenção das migrações anteriores).

CREATE TABLE IF NOT EXISTS setor_mercado (
    id         BIGSERIAL PRIMARY KEY,
    nome       VARCHAR(120) NOT NULL,
    slug       VARCHAR(120) NOT NULL,
    segmento   VARCHAR(120),
    ativo      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP(6) WITHOUT TIME ZONE
);

-- Nome e slug são a chave de reuso: o mesmo setor digitado como "Bancos",
-- "bancos" ou "BANCOS" tem de cair na MESMA linha do catálogo.
CREATE UNIQUE INDEX IF NOT EXISTS uk_setor_mercado_nome ON setor_mercado (lower(nome));
CREATE UNIQUE INDEX IF NOT EXISTS uk_setor_mercado_slug ON setor_mercado (slug);

ALTER TABLE ativo_cadastro
    ADD COLUMN IF NOT EXISTS setor_mercado_id BIGINT;

ALTER TABLE carteira_ideal_setor
    ADD COLUMN IF NOT EXISTS setor_mercado_id BIGINT;

-- Os setores que o usuário já tinha cadastrado nas carteiras viram linhas do
-- catálogo (um por NOME normalizado), preservando o trabalho já feito. O vínculo
-- é feito por nome, ignorando acento e caixa.
INSERT INTO setor_mercado (nome, slug, created_at)
SELECT DISTINCT
       s.nome,
       lower(regexp_replace(translate(s.nome,
             'áàâãäéèêëíìîïóòôõöúùûüçÁÀÂÃÄÉÈÊËÍÌÎÏÓÒÔÕÖÚÙÛÜÇ',
             'aaaaaeeeeiiiiooooouuuucAAAAAEEEEIIIIOOOOOUUUUC'),
             '[^a-zA-Z0-9]+', '-', 'g')),
       now()
FROM carteira_ideal_setor s
WHERE s.nome IS NOT NULL
  AND trim(s.nome) <> ''
ON CONFLICT DO NOTHING;

UPDATE carteira_ideal_setor s
SET setor_mercado_id = m.id
FROM setor_mercado m
WHERE s.setor_mercado_id IS NULL
  AND lower(trim(s.nome)) = lower(trim(m.nome));
