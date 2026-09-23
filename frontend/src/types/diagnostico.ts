/* ══════════════════════════════════════════════════════════════════════
   Tipos do diagnóstico financeiro (recuperação de dados legados).
   Espelham o DTO do backend em
   dev.LzGuimaraes.FocusLifeHub.Carteira.dto.DiagnosticoFinanceiroDTO
   (nomes de campo iguais aos do JSON devolvido pela API).
   ══════════════════════════════════════════════════════════════════════ */

export interface CarteiraResumoDiagnostico {
  id: number;
  nome: string | null;
  moeda: string | null;
  /** Quantidade de posições (ou despesas) vinculadas à carteira. */
  itens: number;
  /** Valor somado dessas posições, já em 2 casas. */
  valor: number;
}

export interface DiagnosticoFinanceiro {
  carteiras: CarteiraResumoDiagnostico[];
  carteiras_dividas: CarteiraResumoDiagnostico[];
  total_posicoes: number;
  /** Posições de investimento sem carteira (dados legados) — globais. */
  posicoes_sem_carteira: number;
  /** Despesas sem carteira (dados legados) — globais. */
  despesas_sem_carteira: number;
  pode_reparar: boolean;
  /** Explicação exibida quando o reparo não é permitido para este usuário. */
  limitacao: string | null;
  alertas: string[];
}

export interface ReparoResultado {
  posicoes_religadas: number;
  despesas_religadas: number;
  carteira_investimento_id: number | null;
  carteira_dividas_id: number | null;
  mensagens: string[];
}
