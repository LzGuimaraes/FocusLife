import type { CategoriaInvestimento } from "./planejamento";

/* ══════════════════════════════════════════════════════════════════════
   Tipos de Compras/Vendas — espelham 1:1 os DTOs snake_case do backend em
   dev.LzGuimaraes.FocusLifeHub.Investimento.ComprasVendas.dto
   ══════════════════════════════════════════════════════════════════════ */

/** Uma operação de compra e (opcional) venda, já com os valores CALCULADOS. */
export interface OperacaoCompraVenda {
  id: number;
  carteira_investimento_id: number | null;
  ativo_cadastro_id: string | null;
  ativo_id: number | null;
  ticker: string;
  nome: string;
  classe: CategoriaInvestimento | null;
  data_compra: string;
  data_venda: string | null;
  quantidade: number | null;
  quantidade_vendida: number | null;
  valor_inicial: number;
  valor_venda: number | null;
  valor_unitario_compra: number | null;
  valor_unitario_venda: number | null;
  custo_vendido: number | null;
  lucro: number | null;
  rentabilidade_percentual: number | null;
  prazo_dias: number | null;
  aberta: boolean;
  preco_atual: number | null;
  created_at: string | null;
  updated_at: string | null;
}

/** Ponto do gráfico: valor bruto do período + percentual realizado acumulado. */
export interface SeriePonto {
  periodo: string;
  valor_bruto: number;
  valor_percentual_acumulado: number | null;
}

export interface ResumoComprasVendas {
  total_investido: number;
  total_vendido: number;
  lucro_total: number;
  rentabilidade_percentual: number | null;
  qtd_operacoes: number;
  qtd_abertas: number;
  qtd_fechadas: number;
  serie: SeriePonto[];
}

/** Regra de preço teto/mínimo já resolvida com o preço atual e as flags. */
export interface AtivoPrecoAlvo {
  id: number;
  ativo_cadastro_id: string | null;
  ativo_id: number | null;
  ticker: string;
  preco_teto: number | null;
  preco_minimo: number | null;
  preco_atual: number | null;
  acima_do_teto: boolean;
  abaixo_do_minimo: boolean;
  updated_at: string | null;
}

/** Payload de criação/edição de operação. */
export interface OperacaoFormPayload {
  carteira_investimento_id: number | null;
  ativo_cadastro_id: string | null;
  ativo_id: number | null;
  ticker: string;
  nome: string;
  classe: CategoriaInvestimento | null;
  data_compra: string;
  data_venda: string | null;
  quantidade: number | null;
  quantidade_vendida: number | null;
  valor_inicial: number;
  valor_venda: number | null;
}
