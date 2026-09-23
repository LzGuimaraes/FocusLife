/* ══════════════════════════════════════════════════════════════════════
   Tipos do histórico de avaliações (Fase 4 — Módulo 8).
   Espelham dev.LzGuimaraes.FocusLifeHub.Planejamento.Historico.dto.HistoricoDTO
   ══════════════════════════════════════════════════════════════════════ */

export interface PontoHistorico {
  data: string;
  quality_score: number | null;
  contribution_score: number | null;
  percentual_atual: number | null;
  percentual_ideal: number | null;
  deficit: number | null;
  excesso: number | null;
  prioridade_manual: number | null;
}

export interface SerieAtivo {
  ativo_cadastro_id: string | null;
  ativo_id: number | null;
  ticker: string | null;
  pontos: PontoHistorico[];
  primeiro_score: number | null;
  ultimo_score: number | null;
  /** ultimo − primeiro, em pontos percentuais. */
  variacao: number | null;
}

export interface PontoCarteira {
  data: string;
  quality_medio: number | null;
  ativos_avaliados: number;
  deficit_total: number | null;
  excesso_total: number | null;
  valor_carteira_total: number | null;
}

export interface SerieCarteira {
  carteira_id: number;
  moeda: string;
  dias_registrados: number;
  pontos: PontoCarteira[];
}

export interface AtivoComHistorico {
  ativo_cadastro_id: string | null;
  ativo_id: number | null;
  ticker: string | null;
  registros: number;
  ultimo_registro: string;
  ultimo_score: number | null;
}

export interface RegistrarResponse {
  data_referencia: string;
  registrados: number;
  atualizados: number;
  tickers: string[];
}
