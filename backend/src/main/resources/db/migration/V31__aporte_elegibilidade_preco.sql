-- V31 — Elegibilidade por PREÇO e o novo peso do Priority Score.
--
-- A partir daqui o motor de aporte tem uma fase explícita de ELEGIBILIDADE antes
-- do ranking: o ativo pode ser descartado (e o motivo fica visível) sem que um
-- Quality Score alto compense uma regra de compra violada.
--
--   • meta_ativo.preco_maximo_compra — regra de compra do próprio investidor
--     (por carteira + ativo). NULL/vazio = sem regra de preço: o ativo concorre
--     normalmente. Só se aplica a ativos com ticker (cotação).
--   • score_config.peso_preco — peso configurável do termo "Oportunidade de
--     preço" no Priority Score. DEFAULT 2.0 para as configurações que já
--     existiam (sem isso o termo entraria desligado para quem já usava o app).

ALTER TABLE meta_ativo
    ADD COLUMN IF NOT EXISTS preco_maximo_compra NUMERIC(14, 2);

ALTER TABLE score_config
    ADD COLUMN IF NOT EXISTS peso_preco NUMERIC(9, 4) NOT NULL DEFAULT 2.0;
