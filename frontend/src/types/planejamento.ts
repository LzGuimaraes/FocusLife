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
export interface SubclasseIdeal {
  id: number;
  nome: string;
  percentual_ideal: number;
  ordem: number;
}

export interface ClasseIdeal {
  id: number;
  classe: CategoriaInvestimento;
  percentual_ideal: number;
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
  percentual_ideal: number;
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
export interface SubclasseIdealPayload {
  nome: string;
  percentual_ideal: number;
  ordem: number;
}

export interface ClasseIdealPayload {
  classe: CategoriaInvestimento;
  percentual_ideal: number;
  ordem: number;
  subclasses: SubclasseIdealPayload[];
}

export interface MetaIdealPayload {
  ativo_cadastro_id: string;
  classe: CategoriaInvestimento;
  subclasse_nome: string | null;
  percentual_ideal: number;
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
}

export interface AtivoComparativo {
  meta_id: number;
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

/* ── Carteira (reuso do CRUD existente) ── */
export interface CarteiraResumo {
  id: number;
  nome: string;
  moeda: string;
}
