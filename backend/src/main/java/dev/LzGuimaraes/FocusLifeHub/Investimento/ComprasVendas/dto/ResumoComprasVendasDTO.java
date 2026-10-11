package dev.LzGuimaraes.FocusLifeHub.Investimento.ComprasVendas.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Resumo agregado das operações + série para o gráfico de andamento.
 *
 *   • {@code total_investido}/{@code total_vendido}/{@code lucro_total} são
 *     somas em R$; {@code rentabilidade_percentual} é o lucro realizado sobre o
 *     custo vendido.
 *   • {@code serie} traz, por período, o {@code valor_bruto} movimentado (R$) e
 *     o {@code valor_percentual_acumulado} da rentabilidade realizada ATÉ ali.
 */
public record ResumoComprasVendasDTO(
    BigDecimal total_investido,
    BigDecimal total_vendido,
    BigDecimal lucro_total,
    BigDecimal rentabilidade_percentual,
    long qtd_operacoes,
    long qtd_abertas,
    long qtd_fechadas,
    List<Ponto> serie
) {
    public record Ponto(
        String periodo,
        BigDecimal valor_bruto,
        BigDecimal valor_percentual_acumulado
    ) {
    }
}
