/* ══════════════════════════════════════════════════════════════════════
   Tipos do módulo de avaliação de ativos (Fase 2 — Módulos 2, 3, 4 e 5).
   Espelham os DTOs do backend em
   dev.LzGuimaraes.FocusLifeHub.Planejamento.Avaliacao.dto.*
   ══════════════════════════════════════════════════════════════════════ */

export type TipoPergunta =
  | "SIM_NAO"
  | "NOTA"
  | "NUMERO"
  | "PERCENTUAL"
  | "TEXTO"
  | "LISTA"
  | "MULTIPLA_ESCOLHA";

/** Regra de pontuação: faixa (valor_min/max → nota) ou opção (texto → nota). */
export interface Regra {
  id?: number;
  texto: string | null;
  valor_min: number | null;
  valor_max: number | null;
  nota: number;
  ordem: number;
}

export interface Pergunta {
  id?: number;
  titulo: string;
  descricao: string | null;
  tipo: TipoPergunta;
  peso: number;
  nota_maxima: number;
  conta_no_score: boolean;
  obrigatoria?: boolean | null;
  ordem: number;
  regras: Regra[];

  /* ── Resposta (só no checklist do ativo) ── */
  nota_atribuida?: number | null;
  valor_numerico?: number | null;
  resposta_texto?: string | null;
  observacao?: string | null;
  respondido_em?: string | null;
}

/* ── Modelo de checklist ── */
export interface ModeloChecklist {
  id: number;
  nome: string;
  descricao: string | null;
  tipo_alvo: string | null;
  ativa: boolean;
  user_id: number | null;
  perguntas: Pergunta[];
}

export interface ModeloChecklistResumo {
  id: number;
  nome: string;
  descricao: string | null;
  tipo_alvo: string | null;
  ativa: boolean;
  total_perguntas: number;
}

/* ── Checklist do ativo ── */
export interface Checklist {
  id: number;
  ativo_cadastro_id: string | null;
  ativo_id: number | null;
  ticker: string | null;
  nome: string;
  modelo_origem_id: number | null;
  peso: number;
  ordem: number;
  ativa: boolean;
  score: number | null;
  total_perguntas: number;
  total_respondidas: number;
  total_pontuadas_respondidas: number;
  perguntas: Pergunta[];
}

export interface AtivoAvaliado {
  ativo_cadastro_id: string | null;
  ativo_id: number | null;
  ticker: string | null;
  total_checklists: number;
  total_perguntas: number;
  total_respondidas: number;
  quality_score: number | null;
}

/* ── Payloads ── */
export interface RegraPayload {
  texto: string | null;
  valor_min: number | null;
  valor_max: number | null;
  nota: number;
  ordem: number;
}

export interface PerguntaPayload {
  titulo: string;
  descricao: string | null;
  tipo: TipoPergunta;
  peso: number;
  nota_maxima: number;
  conta_no_score: boolean;
  obrigatoria?: boolean;
  ordem: number;
  regras: RegraPayload[];
}

export interface ModeloChecklistPayload {
  nome: string;
  descricao: string | null;
  tipo_alvo: string | null;
  ativa: boolean;
  perguntas: PerguntaPayload[];
}

export interface ChecklistCriarPayload {
  ativo_cadastro_id: string | null;
  ativo_id: number | null;
  nome: string | null;
  modelo_id: number | null;
  peso: number;
  ordem: number | null;
  perguntas?: PerguntaPayload[];
}

export interface RespostaItemPayload {
  pergunta_id: number;
  nota: number | null;
  valor: number | null;
  texto: string | null;
  observacao: string | null;
}
