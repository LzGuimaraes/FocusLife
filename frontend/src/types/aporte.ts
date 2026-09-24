/* ══════════════════════════════════════════════════════════════════════
   Tipos da prioridade de aporte — versão CURTA.

   Espelham dev.LzGuimaraes.FocusLifeHub.Planejamento.Aporte.dto.RankingAportesDTO.

   A decisão tem quatro perguntas, e só elas:
     DÉFICIT       → quanto falta (classe/subclasse)
     ELEGIBILIDADE → esse ativo pode receber agora?
     NOTA          → entre os que podem, qual vem primeiro (nota do checklist)
     ALOCAÇÃO      → quanto cabe em cada um

   Saíram: pesos configuráveis, termos de score, cenários, rebalanceamento,
   precedência de travas, preço (bloqueio/oportunidade), momento e prioridade
   manual.
   ══════════════════════════════════════════════════════════════════════ */

import type { CategoriaInvestimento } from "./planejamento";

/**
 * VEREDITO de elegibilidade. Todos os motivos são estruturais — nenhum depende
 * de preço.
 */
export type StatusElegibilidade =
  | "ELEGIVEL"
  | "SEM_AVALIACAO"
  | "CRITERIO_ELIMINATORIO"
  | "LIMITE_ATINGIDO"
  | "CLASSE_SEM_CAPACIDADE"
  | "SEM_CAPACIDADE";

/** Status de equilíbrio de classe/subclasse frente à tolerância. */
export type StatusNivel = "ABAIXO" | "EQUILIBRADO" | "ACIMA" | "SEM_ALVO";

/**
 * Ação recomendada. MANTER ≠ APORTAR: um ativo pode ser bom e continuar na
 * carteira sem receber dinheiro agora.
 */
export type AcaoAtivo = "APORTAR" | "MANTER" | "NAO_APORTAR" | "AVALIAR";

export interface Alerta {
  tipo: string;
  mensagem: string;
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

  /* ── NOTA do checklist: é o critério de ordem ── */
  /** 0–100 (null = sem checklist respondido). */
  nota: number | null;
  avaliada: boolean;
  perguntas: number;
  respondidas: number;
  /** Critérios eliminatórios reprovados (texto pronto). */
  bloqueios: string[];

  /* ── Elegibilidade ── */
  /** false = descartado: NÃO participa do ranking nem do rateio. */
  elegivel: boolean;
  status: StatusElegibilidade;
  motivos_inelegibilidade: string[];
  limite_maximo: number | null;
  limite_atingido: boolean;
  /** Quanto o ativo ainda pode receber (déficit + tolerância). */
  capacidade_aporte: number;

  /* ── Situação na carteira ── */
  percentual_atual: number;
  percentual_ideal: number;
  valor_atual: number;
  valor_ideal: number;
  deficit: number;
  excesso: number;
  tolerancia: number;

  /** Quanto deste aporte o item recebeu (null quando nenhum valor foi informado). */
  sugestao_aporte: number | null;
  /** Explicação objetiva da decisão. */
  motivo: string | null;
  acao: AcaoAtivo;
}

/** Subclasse dentro da classe: o % é fatia da CLASSE. */
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
}

/** Onde o dinheiro entra, no nível da decisão: classe → subclasse. */
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
  /** Quanto deste aporte a classe recebe (0 = já está no alvo ou acima). */
  sugerido: number;
  motivo: string | null;
  subclasses: SubclasseAporte[];
}

export interface RankingAportes {
  carteira_id: number;
  moeda: string;
  /** Patrimônio de HOJE (sem o aporte). */
  valor_total: number;
  valor_aporte: number | null;
  /**
   * Patrimônio DEPOIS do aporte: é a referência dos alvos. O alvo de uma classe
   * é um % do total, então o dinheiro novo aumenta o próprio alvo — sem isso um
   * aporte numa carteira já equilibrada não teria para onde ir.
   */
  valor_total_com_aporte: number | null;
  valor_alocado: number | null;
  valor_nao_alocado: number | null;
  /** Quantos ativos disputaram o aporte (elegíveis) e quantos foram descartados. */
  total_elegiveis: number;
  total_descartados: number;
  nao_alocado_explicacao: string | null;
  avisos: string[];
  alertas: Alerta[];
  classes: ClasseAporte[];
  /** TODOS os ativos analisados: elegíveis e descartados (com o motivo). */
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
