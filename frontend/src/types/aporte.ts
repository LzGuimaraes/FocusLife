import type { CategoriaInvestimento } from "./planejamento";

/* ══════════════════════════════════════════════════════════════════════
   Tipos da prioridade de aporte (Fase 3 — Módulos 6 e 9).
   Espelham os DTOs do backend em
   dev.LzGuimaraes.FocusLifeHub.Planejamento.Aporte.dto.*
   ══════════════════════════════════════════════════════════════════════ */

export type TermoScore = "QUALITY" | "DEFICIT" | "EXCESSO" | "PRIORIDADE";

export type EstrategiaAporte =
  | "DEFICIT_PROPORCIONAL"
  | "SCORE_PROPORCIONAL"
  | "DEFICIT_COM_PRIORIDADE";

export interface Termo {
  termo: TermoScore;
  label: string;
  descricao: string;
  peso: number;
  peso_padrao: number;
}

export interface ScoreConfig {
  id: number | null;
  peso_quality: number;
  peso_deficit: number;
  peso_excesso: number;
  peso_prioridade: number;
  peso_quality_efetivo: number;
  soma_pesos: number;
  estrategia_aporte: EstrategiaAporte;
  /** false = o usuário nunca personalizou (os pesos são os padrões do sistema). */
  personalizada: boolean;
  termos: Termo[];
}

export interface ScoreConfigPayload {
  peso_quality: number;
  peso_deficit: number;
  peso_excesso: number;
  peso_prioridade: number;
  estrategia_aporte: EstrategiaAporte;
}

export interface ItemRanking {
  posicao: number;
  ativo_cadastro_id: string | null;
  meta_id: number;
  ticker: string | null;
  classe: CategoriaInvestimento;

  /** Quality Score do ativo (Módulo 5) — null = ainda sem avaliação. */
  quality_score: number | null;
  qualidade_avaliada: boolean;
  /** α aplicado: 0 quando o ativo não tem Quality Score. */
  peso_quality_aplicado: number;

  /** Prioridade de aporte (Módulo 6). */
  contribution_score: number;

  percentual_atual: number;
  percentual_ideal: number;
  valor_atual: number;
  valor_ideal: number;
  deficit: number;
  excesso: number;
  prioridade_manual: number;

  /** Quanto deste aporte o ativo receberia (null quando nenhum valor foi informado). */
  sugestao_aporte: number | null;
}

export interface RankingAportes {
  carteira_id: number;
  moeda: string;
  valor_total: number;
  valor_aporte: number | null;
  valor_alocado: number | null;
  valor_nao_alocado: number | null;
  estrategia_aporte: EstrategiaAporte;
  termos: Termo[];
  avisos: string[];
  itens: ItemRanking[];
}

export const ESTRATEGIAS_APORTE: { value: EstrategiaAporte; label: string; descricao: string }[] = [
  {
    value: "DEFICIT_PROPORCIONAL",
    label: "Proporcional ao déficit",
    descricao: "Divide o aporte na proporção do déficit de cada ativo.",
  },
  {
    value: "SCORE_PROPORCIONAL",
    label: "Proporcional ao Contribution Score",
    descricao: "Ativos com Contribution Score maior recebem uma fatia maior do aporte.",
  },
  {
    value: "DEFICIT_COM_PRIORIDADE",
    label: "Déficit com prioridade",
    descricao: "Como o proporcional, mas a prioridade manual aumenta o peso de cada ativo.",
  },
];
