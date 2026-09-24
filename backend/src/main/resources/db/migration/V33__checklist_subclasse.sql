-- V33 — CHECKLIST POR SUBCLASSE.
--
-- O checklist nasceu ancorado no ATIVO (um por empresa) e o usuário pediu o
-- contrário: UM conjunto de perguntas por SUBCLASSE ("Financeiro", "Bens
-- Industriais"), com a NOTA dada a cada empresa que cai nessa subclasse, tudo
-- numa página só.
--
-- Como a subclasse da Carteira Ideal é recriada a cada save (replace-all), o
-- vínculo NÃO pode ser o id: vai por SLUG do nome da subclasse (sem acento,
-- minúsculo, com hífen) — a mesma convenção usada no setor. Assim o checklist
-- sobrevive ao save e o mesmo nome em outra carteira cai no mesmo checklist.
--
--   • checklist_modelo.subclasse_slug — o "checklist padrão da subclasse"
--     (as perguntas). Único por usuário+subclasse: não existe dois padrões para
--     a mesma subclasse.
--   • checklist_ativo.subclasse_slug — de qual subclasse aquele checklist do
--     ativo veio (para a página saber o que já foi respondido).
--
-- Nenhuma coluna antiga é removida (as tabelas continuam como estão).

ALTER TABLE checklist_modelo ADD COLUMN IF NOT EXISTS subclasse_slug VARCHAR(120);
ALTER TABLE checklist_ativo  ADD COLUMN IF NOT EXISTS subclasse_slug VARCHAR(120);

CREATE UNIQUE INDEX IF NOT EXISTS uk_checklist_modelo_subclasse
    ON checklist_modelo (user_id, subclasse_slug)
    WHERE subclasse_slug IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_checklist_ativo_subclasse
    ON checklist_ativo (user_id, subclasse_slug);
