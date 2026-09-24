/* ══════════════════════════════════════════════════════════════════════
   CATÁLOGO GLOBAL DE SETORES (`/setores-mercado`).

   É tabela de REFERÊNCIA, como o catálogo de ativos: a mesma lista serve para
   qualquer carteira e pode ser mantida direto no banco. No app ela alimenta as
   sugestões do campo de setor da Carteira Ideal — o que o usuário digitar ali
   passa a existir para as próximas carteiras e para os tickers classificados.
   ══════════════════════════════════════════════════════════════════════ */

export interface SetorMercado {
  id: number;
  nome: string;
  slug: string;
  segmento: string | null;
  ativo: boolean;
  /** Quantos tickers do catálogo estão neste setor. */
  ativos: number;
  /** Quantos alvos de Carteira Ideal usam este setor. */
  alvos_carteira: number;
}
