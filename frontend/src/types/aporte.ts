import type { CategoriaInvestimento } from "./planejamento";

/* ══════════════════════════════════════════════════════════════════════
   Tipos da prioridade de aporte (Fase 3 — Módulos 6 e 9).
   Espelham os DTOs do backend em
   dev.LzGuimaraes.FocusLifeHub.Planejamento.Aporte.dto.*
   ══════════════════════════════════════════════════════════════════════ */

export type TermoScore = "QUALITY" | "DEFICIT" | "EXCESSO" | "PRIORIDADE" | "MOMENTO" | "PRECO";

/**
 * VEREDITO de elegibilidade: "esse ativo PODE receber dinheiro agora?".
 *
 * Conceito separado do déficit (quanto falta) e do Priority Score (entre os
 * que podem, qual vem primeiro). SEM_AVALIACAO continua ELEGÍVEL — só indica
 * que o termo de qualidade não entrou na conta.
 */
export type StatusElegibilidade =
  | "ELEGIVEL"
  | "SEM_AVALIACAO"
  | "PRECO_ACIMA_DO_LIMITE"
  | "CRITERIO_ELIMINATORIO"
  | "LIMITE_ATINGIDO"
  | "MOMENTO_ZERO"
  | "CLASSE_SEM_CAPACIDADE"
  | "SEM_CAPACIDADE";

/** Status de equilíbrio de classe/subclasse frente à tolerância (§18). */
export type StatusNivel = "ABAIXO" | "EQUILIBRADO" | "ACIMA" | "SEM_ALVO";

/**
 * Ação recomendada (§23). MANTER ≠ APORTAR: um ativo pode ser excelente e
 * continuar na carteira sem receber dinheiro agora.
 */
export type AcaoAtivo = "APORTAR" | "MANTER" | "NAO_APORTAR" | "AVALIAR";

/** Como o teto do ativo é calculado (§17). */
export type TetoAtivoModo = "TETO_ESTRITO" | "TETO_ATE_A_CLASSE";

/** Travas do motor, na ordem em que o usuário quer que elas sejam explicadas (§36). */
export const TRACAS_MOTOR = [
  "BLOQUEIO",
  "LIMITE",
  "PRECO",
  "CLASSE",
  "SUBCLASSE",
  "SETOR",
  "TETO_ATIVO",
  "MOMENTO",
  "SCORE",
] as const;

export type TracaMotor = (typeof TRACAS_MOTOR)[number];

export const TRACA_LABEL: Record<TracaMotor, string> = {
  BLOQUEIO: "Critério eliminatório (bloqueia)",
  LIMITE: "Limite máximo de concentração (bloqueia)",
  PRECO: "Preço máximo de compra (bloqueia)",
  CLASSE: "Déficit da CLASSE (define quanto entra no nível)",
  SUBCLASSE: "Déficit da SUBCLASSE (dentro da classe)",
  SETOR: "Déficit do SETOR (dentro da subclasse)",
  TETO_ATIVO: "Capacidade do ATIVO (déficit + tolerância)",
  MOMENTO: "Fator de momento (0 a 1)",
  SCORE: "Priority Score e estratégia (divide dentro do nível)",
};

/** Sugestão de redução (§19/§21/§37): o que passou do alvo + tolerância. */
export interface Rebalanceamento {
  nivel: "ATIVO" | "SUBCLASSE" | "SETOR" | "CLASSE";
  nome: string;
  classe: CategoriaInvestimento;
  percentual_atual: number;
  percentual_ideal: number;
  excesso: number;
  sugerido_vender: number;
  motivo: string;
}

/** Alerta do motor (§31). */
export interface Alerta {
  tipo: string;
  mensagem: string;
}

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
  peso_momento: number;
  /** Peso do termo de oportunidade de preço (0 desliga o termo). */
  peso_preco: number;
  peso_quality_efetivo: number;
  soma_pesos: number;
  /** Faixas da nota de momento → fator (0 / 0,25 / 0,50 / 0,75 / 1,00). */
  momento_faixa_1: number;
  momento_faixa_2: number;
  momento_faixa_3: number;
  momento_faixa_4: number;
  /** true = valor sem destino elegível procura outra classe com déficit. */
  redistribuir: boolean;
  /** true = o motor sugere VENDER o que passou do alvo para financiar os déficits. */
  rebalancear: boolean;
  /** TETO_ESTRITO = déficit próprio; TETO_ATE_A_CLASSE = deixar o ativo absorver o déficit da classe (§17). */
  teto_ativo_modo: TetoAtivoModo;
  /** Ordem configurada das travas do motor (§36). */
  precedencia: string;
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
  peso_momento: number;
  momento_faixa_1: number;
  momento_faixa_2: number;
  momento_faixa_3: number;
  momento_faixa_4: number;
  redistribuir: boolean;
  rebalancear: boolean;
  teto_ativo_modo: TetoAtivoModo;
  precedencia: string;
  /** Peso do termo de oportunidade de preço (0 desliga o termo). */
  peso_preco: number;
  estrategia_aporte: EstrategiaAporte;
}

export interface ItemRanking {
  posicao: number;
  ativo_cadastro_id: string | null;
  meta_id: number | null;
  ticker: string | null;
  classe: CategoriaInvestimento;
  /** false = posição sem ticker de catálogo (renda fixa, Tesouro, caixinha). */
  vinculado: boolean;
  subclasse_id: number | null;
  subclasse_nome: string | null;
  /** Setor dentro da subclasse (opcional). */
  setor_id: number | null;
  setor_nome: string | null;

  /** Quality Score do ativo (Módulo 5) — null = ainda sem avaliação. */
  quality_score: number | null;
  qualidade_avaliada: boolean;
  /** α aplicado: 0 quando o ativo não tem Quality Score. */
  peso_quality_aplicado: number;

  /* ── Momento / valuation (§10–§12) ── */
  momento_score: number | null;
  momento_avaliado: boolean;
  /** Fator 0 a 1 (1 = neutro). 0 = não aportar por momento. */
  fator_momento: number;

  /* ── ELEGIBILIDADE ── */
  /** false = descartado: NÃO participa do ranking nem do rateio. */
  elegivel: boolean;
  status: StatusElegibilidade;
  /** Explicação de cada motivo de descarte (vazio quando elegível). */
  motivos_inelegibilidade: string[];
  /** Critérios eliminatórios reprovados no checklist (texto pronto). */
  bloqueios: string[];
  limite_maximo: number | null;
  limite_atingido: boolean;
  /** Quanto o ativo ainda pode receber (déficit + tolerância, respeitando o limite). */
  capacidade_aporte: number;

  /* ── PREÇO ── */
  preco_atual: number | null;
  /** Preço médio pago nas compras registradas — INFORMATIVO, não é regra. */
  preco_medio: number | null;
  /** Regra de compra: acima deste preço o ativo é descartado. */
  preco_maximo_compra: number | null;
  /** 0..1 = (máximo − atual) / máximo. Null = sem regra de preço. */
  oportunidade_preco: number | null;

  /* ── RANKING ── */
  /** Priority Score (0–100): prioridade ENTRE os elegíveis. */
  priority_score: number;

  percentual_atual: number;
  percentual_ideal: number;
  valor_atual: number;
  valor_ideal: number;
  deficit: number;
  excesso: number;
  tolerancia: number;
  prioridade_manual: number;

  /** Quanto deste aporte o item recebeu (null quando nenhum valor foi informado). */
  sugestao_aporte: number | null;

  /** Explicação objetiva da decisão (§29). */
  motivo: string | null;

  /** Quanto este item já recebeu de aporte nos últimos 30 dias (§24). */
  aportes_recentes: number;
  aportes_recentes_qtd: number;

  /** Cálculo aberto do Priority Score (§34) — a conta exata, passo a passo. */
  formula: string | null;

  /** Ação recomendada (§23): aportar, manter, não aportar ou avaliar. */
  acao: AcaoAtivo;
}

/** Comparação de cenários (§33): mesmos dados, pesos de decisão diferentes. */
export interface ItemCenario {
  ticker: string;
  valor: number;
}

export interface Cenario {
  nome: string;
  descricao: string;
  pesos: string;
  valor_alocado: number;
  valor_nao_alocado: number;
  itens: ItemCenario[];
}

/** Setor dentro da subclasse (nível opcional): % é fatia da SUBCLASSE. */
export interface SetorAporte {
  id: number;
  nome: string;
  percentual_atual: number;
  percentual_ideal: number;
  valor_atual: number;
  valor_ideal: number;
  deficit: number;
  excesso: number;
  tolerancia: number;
  limite_maximo: number | null;
  status: StatusNivel;
  sugerido: number;
}

/** Onde o dinheiro entra, no nível da decisão: classe → subclasse. */
export interface SubclasseAporte {
  id: number;
  nome: string;
  percentual_atual: number;
  percentual_ideal: number;
  valor_atual: number;
  valor_ideal: number;
  deficit: number;
  excesso: number;
  tolerancia: number;
  limite_maximo: number | null;
  status: StatusNivel;
  sugerido: number;
  motivo: string | null;
  setores: SetorAporte[];
}

export interface ClasseAporte {
  classe: CategoriaInvestimento;
  percentual_atual: number;
  percentual_ideal: number;
  valor_atual: number;
  valor_ideal: number;
  deficit: number;
  excesso: number;
  tolerancia: number;
  limite_maximo: number | null;
  status: StatusNivel;
  /** Quanto deste aporte a classe recebe (0 = já no alvo ou acima). */
  sugerido: number;
  motivo: string | null;
  subclasses: SubclasseAporte[];
}

export interface RankingAportes {
  carteira_id: number;
  moeda: string;
  valor_total: number;
  valor_aporte: number | null;
  valor_alocado: number | null;
  valor_nao_alocado: number | null;
  /** Valor sugerido de VENDA (rebalanceamento) — 0 quando desligado. */
  valor_vendas: number | null;
  /** Orçamento usado no plano = aporte + (vendas, quando ligado). */
  valor_orcamento: number | null;
  rebalancear: boolean;
  /** TETO_ESTRITO | TETO_ATE_A_CLASSE. */
  teto_ativo_modo: TetoAtivoModo;
  /** Quantos ativos disputaram o aporte (elegíveis) e quantos foram descartados. */
  total_elegiveis: number;
  total_descartados: number;
  /** Explicação legível do valor não alocado (§32). */
  nao_alocado_explicacao: string | null;
  redistribuir: boolean;
  estrategia_aporte: EstrategiaAporte;
  termos: Termo[];
  avisos: string[];
  alertas: Alerta[];
  /** Ordem em que o motor aplica as travas (§36) — fixa e visível de propósito. */
  precedencia: string[];
  /** Cenários comparativos (§33) — vazio quando nenhum valor foi informado. */
  cenarios: Cenario[];
  /** Sugestões de redução (§19/§21) — vazio quando o rebalanceamento está desligado. */
  rebalanceamento: Rebalanceamento[];
  classes: ClasseAporte[];
  itens: ItemRanking[];
}

/* ══════════════════════════════════════════════════════════════════════
   Histórico de aportes executados (§24).
   ══════════════════════════════════════════════════════════════════════ */
export interface AporteRegistro {
  id: number;
  data: string;
  ativo_cadastro_id: string | null;
  ativo_id: number | null;
  ticker: string | null;
  classe: string | null;
  valor: number;
  preco: number | null;
  quantidade: number | null;
  percentual_antes: number | null;
  percentual_depois: number | null;
  observacao: string | null;
  created_at: string;
}

export interface AporteRegistroItemPayload {
  ativo_cadastro_id: string | null;
  ativo_id: number | null;
  ticker: string | null;
  classe: string | null;
  valor: number;
  preco?: number | null;
  quantidade?: number | null;
}

export interface AporteRegistroPayload {
  carteira_investimento_id: number;
  data?: string | null;
  observacao?: string | null;
  itens: AporteRegistroItemPayload[];
}

export interface AporteRegistroResultado {
  registrados: number;
  valor_total: number;
  data: string;
  itens: AporteRegistro[];
}

export const ESTRATEGIAS_APORTE: { value: EstrategiaAporte; label: string; descricao: string }[] = [
  {
    value: "DEFICIT_PROPORCIONAL",
    label: "Proporcional ao déficit",
    descricao: "Divide o aporte na proporção do déficit de cada ativo.",
  },
  {
    value: "SCORE_PROPORCIONAL",
    label: "Proporcional ao Priority Score",
    descricao: "Entre os ativos ELEGÍVEIS, os de Priority Score maior recebem uma fatia maior do aporte.",
  },
  {
    value: "DEFICIT_COM_PRIORIDADE",
    label: "Déficit com prioridade",
    descricao: "Como o proporcional, mas a prioridade manual aumenta o peso de cada ativo.",
  },
];
