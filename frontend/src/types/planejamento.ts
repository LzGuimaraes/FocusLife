/* ══════════════════════════════════════════════════════════════════════
   Tipos do módulo de Planejamento (Fase 1 — Carteira Ideal).
   Espelham 1:1 os DTOs do backend em
   dev.LzGuimaraes.FocusLifeHub.Planejamento.*.dto
   (os nomes dos campos são exatamente os nomes JSON devolvidos pela API).
   ══════════════════════════════════════════════════════════════════════ */

export type CategoriaInvestimento =
  | "RENDA_FIXA"
  | "TESOURO_DIRETO"
  | "ACOES"
  | "FIIS"
  | "ETFS"
  | "CRIPTOMOEDAS"
  | "OUTROS";

/* ── Estratégia ── */
export interface Estrategia {
  id: number;
  nome: string;
  descricao: string | null;
  ativa: boolean;
  user_id: number | null;
}

/* ── Configuração da Carteira Ideal ── */
export interface SetorIdeal {
  id: number;
  /** Setor do CATÁLOGO global (null só em dado antigo sem vínculo). */
  setor_mercado_id: number | null;
  nome: string;
  percentual_ideal: number;
  tolerancia: number;
  limite_maximo: number | null;
  ordem: number;
}

export interface SubclasseIdeal {
  id: number;
  nome: string;
  percentual_ideal: number;
  tolerancia: number;
  limite_maximo: number | null;
  ordem: number;
  /** SETOR é um nível opcional dentro da subclasse (o % é fatia da SUBCLASSE). */
  setores: SetorIdeal[];
}
export interface ClasseIdeal {
  id: number;
  classe: CategoriaInvestimento;
  percentual_ideal: number;
  tolerancia: number;
  limite_maximo: number | null;
  ordem: number;
  subclasses: SubclasseIdeal[];
}

export interface MetaIdeal {
  id: number;
  ativo_cadastro_id: string | null;
  ticker: string | null;
  classe: CategoriaInvestimento;
  subclasse_id: number | null;
  subclasse_nome: string | null;
  setor_id: number | null;
  setor_nome: string | null;
  percentual_ideal: number;
  tolerancia: number;
  limite_maximo: number | null;
  /** Regra de compra: acima deste preço o ativo é descartado do aporte. */
  preco_maximo_compra: number | null;
  prioridade_manual: number;
  ordem: number;
}

export interface CarteiraIdeal {
  carteira_id: number;
  moeda: string;
  estrategia_id: number | null;
  estrategia_nome: string | null;
  soma_percentuais_ideal: number;
  classes: ClasseIdeal[];
  metas: MetaIdeal[];
  avisos: string[];
}

/* ── Payload de gravação (replace-all) ── */
export interface SetorIdealPayload {
  nome: string;
  /** Opcional: o backend resolve (ou cria) pelo nome quando vier nulo. */
  setor_mercado_id?: number | null;
  percentual_ideal: number;
  tolerancia?: number;
  limite_maximo?: number | null;
  ordem: number;
}

export interface SubclasseIdealPayload {
  nome: string;
  percentual_ideal: number;
  tolerancia?: number;
  limite_maximo?: number | null;
  ordem: number;
  setores?: SetorIdealPayload[];
}

export interface ClasseIdealPayload {
  classe: CategoriaInvestimento;
  percentual_ideal: number;
  tolerancia?: number;
  limite_maximo?: number | null;
  ordem: number;
  subclasses: SubclasseIdealPayload[];
}

export interface MetaIdealPayload {
  ativo_cadastro_id: string;
  classe: CategoriaInvestimento;
  subclasse_nome: string | null;
  setor_nome?: string | null;
  percentual_ideal: number;
  tolerancia?: number;
  limite_maximo?: number | null;
  /** Regra de compra: acima deste preço o ativo é descartado do aporte. */
  preco_maximo_compra?: number | null;
  prioridade_manual: number;
  ordem: number;
}

export interface CarteiraIdealPayload {
  estrategia_id: number | null;
  classes: ClasseIdealPayload[];
  metas: MetaIdealPayload[];
}

/* ── Comparativo Atual × Ideal ── */
export interface SubclasseComparativo {
  id: number;
  nome: string;
  percentual_ideal: number;
  percentual_atual: number;
  valor_ideal: number;
  valor_atual: number;
  deficit: number;
  excesso: number;
  /** Setores da subclasse (o % do setor é fatia DESTA subclasse). */
  setores: SetorComparativo[];
}

export interface SetorComparativo {
  id: number;
  /** Setor do catálogo global (identidade estável). */
  setor_mercado_id: number | null;
  nome: string;
  percentual_ideal: number;
  percentual_atual: number;
  valor_ideal: number;
  valor_atual: number;
  deficit: number;
  excesso: number;
  tolerancia: number;
  limite_maximo: number | null;
}

export interface AtivoComparativo {
  meta_id: number | null;
  ativo_cadastro_id: string;
  ticker: string;
  subclasse_id: number | null;
  percentual_ideal: number;
  percentual_atual: number;
  valor_ideal: number;
  valor_atual: number;
  deficit: number;
  excesso: number;
  prioridade_manual: number;
  /**
   * false = o ativo está na carteira mas ainda não tem meta individual.
   * O comparativo mostra a carteira real, não só o que já foi planejado.
   */
  possui_meta: boolean;
}

export interface ClasseComparativo {
  classe: CategoriaInvestimento;
  percentual_ideal: number;
  percentual_atual: number;
  valor_ideal: number;
  valor_atual: number;
  deficit: number;
  excesso: number;
  subclasses: SubclasseComparativo[];
  ativos: AtivoComparativo[];
}

export interface Comparativo {
  carteira_id: number;
  moeda: string;
  valor_total: number;
  soma_percentuais_ideal: number;
  classes: ClasseComparativo[];
  avisos: string[];
}

/* ── Resumo (widget do Dashboard) ── */
export interface ClasseResumo {
  classe: CategoriaInvestimento;
  percentual_ideal: number;
  percentual_atual: number;
  valor_ideal: number;
  valor_atual: number;
  deficit: number;
  excesso: number;
}

export interface ResumoIdeal {
  carteira_id: number;
  moeda: string;
  valor_total: number;
  soma_percentuais_ideal: number;
  total_classes: number;
  total_ativos_com_meta: number;
  classes: ClasseResumo[];
  avisos: string[];
}

/* ── Ativos que o usuário já tem (base da tela de metas) ── */
export interface MeuAtivo {
  /** null quando a posição não está vinculada ao catálogo. */
  ativo_cadastro_id: string | null;
  /** false = posição sem vínculo (renda fixa, ou ativo que ficou sem catálogo). */
  vinculado: boolean;
  ticker: string;
  /** Posições agrupadas nesta linha (permite vincular todas de uma vez). */
  ativo_ids: number[];
  /** Catálogo com o MESMO nome — permite vincular com um clique. */
  sugestao_catalogo_id: string | null;
  sugestao_catalogo_nome: string | null;
  classe: CategoriaInvestimento;
  quantidade: number | null;
  preco_atual: number | null;
  valor_atual: number;
  percentual_atual: number;
  meta_id: number | null;
  percentual_ideal: number | null;
  tolerancia: number | null;
  limite_maximo: number | null;
  /** Regra de compra vinda da meta (null = sem regra de preço). */
  preco_maximo_compra: number | null;
  prioridade_manual: number | null;
  subclasse_id: number | null;
  subclasse_nome: string | null;
  setor_id: number | null;
  setor_nome: string | null;
}

export interface MeusAtivos {
  carteira_id: number;
  moeda: string;
  valor_total: number;
  /** Quantidade de posições sem vínculo com o catálogo. */
  posicoes_sem_catalogo: number;
  ativos: MeuAtivo[];
}

/* ── Carteira (reuso do CRUD existente) ── */
export interface CarteiraResumo {
  id: number;
  nome: string;
  moeda: string;
}
