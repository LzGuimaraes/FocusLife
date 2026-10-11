package dev.LzGuimaraes.FocusLifeHub.Investimento.ComprasVendas;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Regras de cálculo de uma operação de compra/venda (SEPARADAS do service para
 * serem testáveis sem banco).
 *
 *   • Operação FECHADA (tem {@code valorVenda}): o lucro é realizado.
 *       custo vendido = valor inicial × (quantidade vendida ÷ quantidade comprada)
 *       lucro         = valor venda − custo vendido
 *       rentabilidade = lucro ÷ custo vendido × 100   (null se custo = 0)
 *       prazo (dias)  = data venda − data compra
 *   • Operação ABERTA (só compra): o resultado é NÃO REALIZADO, marcado com a
 *     flag {@code aberta}, usando o preço atual do catálogo × quantidade.
 *
 * Nunca divide por zero: base nula/zerada devolve rentabilidade {@code null}.
 */
public final class CalculadoraOperacao {

    /** Prazo sentinela quando a venda é anterior à compra (dado inconsistente). */
    public static final long PRAZO_INCONSISTENTE_DIAS = 400L;

    private CalculadoraOperacao() {
    }

    /**
     * Resultado calculado de uma operação.
     *
     * @param aberta       true quando só há compra (resultado não realizado).
     * @param custoVendido custo da parte vendida (null na operação aberta).
     * @param valorBruto   valor movimentado: venda (fechada) ou custo (aberta).
     * @param lucro        lucro/prejuízo em R$ (null na aberta, é não realizado).
     * @param rentabilidadePercentual % sobre o custo (null se não houver base).
     * @param prazoDias    dias entre compra e venda (null na operação aberta).
     */
    public record Resultado(
            boolean aberta,
            BigDecimal custoVendido,
            BigDecimal valorBruto,
            BigDecimal lucro,
            BigDecimal rentabilidadePercentual,
            Long prazoDias) {
    }

    public static Resultado calcular(OperacaoCompraVendaModel op, BigDecimal precoAtual) {
        BigDecimal valorInicial = nz(op.getValorInicial());

        if (op.getValorVenda() == null) {
            // Aberta: resultado não realizado com o preço ATUAL do catálogo.
            BigDecimal quantidade = nz(op.getQuantidade());
            BigDecimal valorAtual = nz(precoAtual).multiply(quantidade);
            BigDecimal lucro = valorAtual.subtract(valorInicial).setScale(2, RoundingMode.HALF_UP);
            return new Resultado(true, null, valorInicial.setScale(2, RoundingMode.HALF_UP),
                    lucro, rentabilidade(lucro, valorInicial), null);
        }

        BigDecimal valorVenda = nz(op.getValorVenda());
        BigDecimal custoVendido = ratearCusto(valorInicial, op.getQuantidade(), op.getQuantidadeVendida());
        BigDecimal lucro = valorVenda.subtract(custoVendido).setScale(2, RoundingMode.HALF_UP);
        return new Resultado(false, custoVendido, valorVenda.setScale(2, RoundingMode.HALF_UP),
                lucro, rentabilidade(lucro, custoVendido), prazoDias(op.getDataCompra(), op.getDataVenda()));
    }

    /**
     * Custo da parte vendida. Sem quantidade (ou quantidade não positiva) não há
     * como ratear — assume-se o custo integral. Vender tudo (ou "mais" que tudo)
     * também devolve o custo integral.
     */
    static BigDecimal ratearCusto(BigDecimal valorInicial, BigDecimal qtdComprada, BigDecimal qtdVendida) {
        BigDecimal valor = nz(valorInicial);
        if (qtdComprada == null || qtdComprada.signum() <= 0 || qtdVendida == null) {
            return valor.setScale(2, RoundingMode.HALF_UP);
        }
        if (qtdVendida.signum() <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        if (qtdVendida.compareTo(qtdComprada) >= 0) {
            return valor.setScale(2, RoundingMode.HALF_UP);
        }
        return valor.multiply(qtdVendida).divide(qtdComprada, 2, RoundingMode.HALF_UP);
    }

    static long prazoDias(LocalDate compra, LocalDate venda) {
        if (compra == null || venda == null) {
            return 0L;
        }
        if (venda.isBefore(compra)) {
            return PRAZO_INCONSISTENTE_DIAS;
        }
        return ChronoUnit.DAYS.between(compra, venda);
    }

    /** Rentabilidade % = lucro / base × 100; {@code null} quando a base é 0. */
    static BigDecimal rentabilidade(BigDecimal lucro, BigDecimal base) {
        if (base == null || base.signum() == 0) {
            return null;
        }
        return nz(lucro).multiply(BigDecimal.valueOf(100))
                .divide(base, 2, RoundingMode.HALF_UP);
    }

    private static BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }
}
