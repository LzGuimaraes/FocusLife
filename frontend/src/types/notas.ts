/* ══════════════════════════════════════════════════════════════════════
   NOTAS POR SUBCLASSE (`/notas-subclasse/{slug}`).

   Espelham dev.LzGuimaraes.FocusLifeHub.Planejamento.Avaliacao.dto.NotasSubclasseDTO.

   UM checklist por SUBCLASSE ("Financeiro", "Bens Industriais") e uma nota por
   ATIVO, tudo numa página: as listas de notas são ALINHADAS POR POSIÇÃO com a
   lista de perguntas (`notas[i]` = pergunta `i`), então o front desenha as
   colunas na ordem recebida.

   O balde (`bucket`) é ONDE se dá nota: uma subclasse ou — para o que não tem
   setor (cripto, renda fixa, Tesouro, caixinha) — a CLASSE INTEIRA, com o
   checklist padrão do TIPO do ativo.
   ══════════════════════════════════════════════════════════════════════ */

import type { CategoriaInvestimento } from "./planejamento";

export interface NotaPergunta {
  id: number;
  titulo: string;
  nota_maxima: number;
  ordem: number;
}

/** Onde dá para dar notas nesta carteira. */
export interface NotaBucket {
  slug: string;
  nome: string;
  classe: CategoriaInvestimento;
  classe_label: string;
  /** true = o balde é a classe inteira (o ativo não tem subclasse). */
  classe_inteira: boolean;
  qtd_ativos: number;
}

export interface NotaItem {
  ativo_cadastro_id: string | null;
  ativo_id: number | null;
  ticker: string | null;
  /** Nome cadastrado na posição — mostrado abaixo do ticker. */
  nome: string | null;
  checklist_id: number | null;
  /** Uma nota por pergunta (null = não respondida). */
  notas: (number | null)[];
  /** Nota final 0–100 (null enquanto nada foi respondido). */
  score: number | null;
}

export interface NotasPainel {
  subclasse_slug: string;
  subclasse_nome: string;
  classe: CategoriaInvestimento;
  classe_label: string;
  classe_inteira: boolean;
  modelo_id: number;
  modelo_nome: string;
  perguntas: NotaPergunta[];
  itens: NotaItem[];
}

export interface NotasPayload {
  subclasse_nome: string;
  itens: { ativo_cadastro_id: string | null; ativo_id: number | null; notas: (number | null)[] }[];
}

export interface NotasSalvas {
  avaliados: number;
  itens: NotaItem[];
}
