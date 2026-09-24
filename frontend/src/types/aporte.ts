/* ══════════════════════════════════════════════════════════════════════
   Tipos da prioridade de aporte — versão do motor separado por PERGUNTAS.

   Espelham dev.LzGuimaraes.FocusLifeHub.Planejamento.Aporte.dto.RankingAportesDTO.

   O motor responde a TRÊS perguntas diferentes, e não as mistura:
     PERGUNTA 1  posso investir neste ativo?  → ELEGIBILIDADE
     PERGUNTA 2  quanto posso investir nele?  → CAPACIDADE (até o LIMITE)
     PERGUNTA 3  quem deve receber mais?      → NOTA (peso), nunca permissão

   A meta NÃO é bloqueio, a nota NÃO é bloqueio, o déficit NÃO é bloqueio. O que
   bloqueia é uma regra real de inelegibilidade ou a inexistência de espaço até
   o LIMITE OPERACIONAL = meta × (1 + margem). É por isso que um ativo
   exatamente na meta continua candidato: o alvo dele cresce com R = T + A.
   ══════════════════════════════════════════════════════════════════════ */

import type { CategoriaInvestimento } from "./planejamento";

/**
 * VEREDITO de elegibilidade.
 *
 * NÃO existe mais `CLASSE_SEM_CAPACIDADE`: classe e subclasse distribuem o
 * orçamento e não vetam ativos. Um nível sem espaço simplesmente não recebe.
 */
export type StatusElegibilidade =
  | "ELEGIVEL"
  | "SEM_AVALIACAO"
  | "CRITERIO_ELIMINATORIO"
  | "LIMITE_ATINGIDO"
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
  /**
   * PESO no rateio: a própria nota. Sem nota, o motor rateia pelo ESPAÇO
   * (capacidade) do ativo — nunca pela ausência de avaliação como exclusão.
   */
  peso: number | null;
  /** Critérios eliminatórios reprovados (texto pronto). */
  bloqueios: string[];

  /* ── Elegibilidade ── */
  /** false = descartado: NÃO participa do ranking nem do rateio. */
  elegivel: boolean;
  status: StatusElegibilidade;
  motivos_inelegibilidade: string[];
  /** Limite de concentração cadastrado pelo usuário (null = não há). */
  limite_maximo: number | null;
  limite_atingido: boolean;
  /** Limite OPERACIONAL em % = meta × (1 + margem). Ex.: meta 5% → 5,25%. */
  limite_operacional_percentual: number | null;
  /** Limite que realmente vale = min(operacional, cadastrado). null = sem meta própria. */
  limite_percentual: number | null;
  /** Limite final em reais = `limite_percentual × R`. */
  limite_em_reais: number | null;
  /**
   * Quanto ainda cabe NELE até o limite — o teto do aporte. NÃO é o déficit:
   * um ativo exatamente na meta continua com capacidade.
   */
  capacidade_aporte: number;

  /* ── Situação na carteira ── */
  /** % do patrimônio PROJETADO sem o dinheiro deste ativo (cai quando o aporte entra). */
  percentual_atual: number;
  /** Meta do usuário (null quando a posição não tem meta própria). */
  percentual_ideal: number | null;
  /** Alvo projetado em reais = meta × R (a META, não o limite). */
  valor_ideal: number;
  valor_atual: number;
  /** max(0, alvo − atual): o espaço DESEJÁVEL (diferente da capacidade). */
  deficit: number;
  excesso: number;
  tolerancia: number;

  /** Quanto deste aporte o item recebeu (null quando nenhum valor foi informado). */
  sugestao_aporte: number | null;
  /** Preço atual da cota (null = desconhecido). */
  preco_unitario: number | null;
  /**
   * QUANTAS UNIDADES comprar: cotas INTEIRAS para ação/FII/ETF e fração
   * (8 casas) para cripto, renda fixa e Tesouro. null quando não há preço.
   */
  quantidade: number | null;
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
  /** Limite operacional em % DO PATRIMÔNIO (o % da subclasse é fatia da classe). */
  limite_operacional_percentual: number | null;
  /** Soma das capacidades dos ativos elegíveis dela. */
  capacidade_elegivel: number;
  status: StatusNivel;
  sugerido: number;
  motivo: string | null;
}

/**
 * Onde o dinheiro entra, no nível da decisão: classe → subclasse.
 *
 * O ORÇAMENTO do nível é limitado pela CAPACIDADE ELEGÍVEL (o que os ativos dele
 * absorvem) e, quando houver, pelo limite máximo cadastrado. O DÉFICIT é
 * informativo: ele NÃO reserva dinheiro.
 */
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
  /** Limite operacional da classe = meta × (1 + margem). */
  limite_operacional_percentual: number | null;
  /** Soma das capacidades dos ativos elegíveis da classe. */
  capacidade_elegivel: number;
  status: StatusNivel;
  /** Quanto deste aporte a classe recebe (0 = nada entrou aqui). */
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
  /** Margem operacional usada no cálculo (%, relativa à meta). */
  margem_percentual: number;
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
