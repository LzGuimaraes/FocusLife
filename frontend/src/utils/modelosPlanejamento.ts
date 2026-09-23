import type { CategoriaInvestimento } from "../types/planejamento";

/* ══════════════════════════════════════════════════════════════════════
   Modelos prontos de planejamento (ponto de partida, NUNCA imposição).

   O sistema não impõe metodologia: estes modelos só preenchem as classes com
   percentuais que somam 100% para o usuário não começar de uma tela vazia.
   Ele aplica, ajusta o que quiser e salva — ou simplesmente ignora e monta do
   zero. Vale a mesma régua para tudo no módulo: a metodologia é do usuário.
   ══════════════════════════════════════════════════════════════════════ */

export interface ModeloPlanejamento {
  nome: string;
  icon: string;
  descricao: string;
  classes: { classe: CategoriaInvestimento; percentual: number }[];
}

export const MODELOS_PLANEJAMENTO: ModeloPlanejamento[] = [
  {
    nome: "Conservador",
    icon: "🛡️",
    descricao: "Prioriza proteção e previsibilidade (60% renda fixa).",
    classes: [
      { classe: "RENDA_FIXA", percentual: 45 },
      { classe: "TESOURO_DIRETO", percentual: 15 },
      { classe: "ACOES", percentual: 15 },
      { classe: "FIIS", percentual: 15 },
      { classe: "CRIPTOMOEDAS", percentual: 5 },
      { classe: "OUTROS", percentual: 5 },
    ],
  },
  {
    nome: "Moderado",
    icon: "⚖️",
    descricao: "Equilibra renda fixa e variável (40% fixa).",
    classes: [
      { classe: "RENDA_FIXA", percentual: 30 },
      { classe: "TESOURO_DIRETO", percentual: 10 },
      { classe: "ACOES", percentual: 25 },
      { classe: "FIIS", percentual: 20 },
      { classe: "ETFS", percentual: 10 },
      { classe: "CRIPTOMOEDAS", percentual: 5 },
    ],
  },
  {
    nome: "Arrojado",
    icon: "🚀",
    descricao: "Foco em crescimento de longo prazo (75% variável).",
    classes: [
      { classe: "ACOES", percentual: 45 },
      { classe: "FIIS", percentual: 20 },
      { classe: "ETFS", percentual: 10 },
      { classe: "CRIPTOMOEDAS", percentual: 10 },
      { classe: "RENDA_FIXA", percentual: 15 },
    ],
  },
  {
    nome: "Dividendos",
    icon: "💸",
    descricao: "Foco em renda recorrente (ações + FIIs).",
    classes: [
      { classe: "ACOES", percentual: 40 },
      { classe: "FIIS", percentual: 40 },
      { classe: "RENDA_FIXA", percentual: 15 },
      { classe: "TESOURO_DIRETO", percentual: 5 },
    ],
  },
];
