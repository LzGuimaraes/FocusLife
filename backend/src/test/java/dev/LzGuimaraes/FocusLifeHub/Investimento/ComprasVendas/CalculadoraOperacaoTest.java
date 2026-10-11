package dev.LzGuimaraes.FocusLifeHub.Investimento.ComprasVendas;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Regras de cálculo de uma operação (lucro, rentabilidade, prazo, venda
 * parcial e operação aberta) — sem banco.
 */
class CalculadoraOperacaoTest {

    /* ══ Operação fechada ══ */

    @Test
    @DisplayName("venda com lucro: lucro = venda − custo e rentabilidade = lucro/custo")
    void vendaComLucro() {
        OperacaoCompraVendaModel op = operacao(new BigDecimal("1000.00"), new BigDecimal("1200.00"),
                BigDecimal.valueOf(100), null, "2024-01-01", "2024-01-11");

        CalculadoraOperacao.Resultado r = CalculadoraOperacao.calcular(op, null);

        assertThat(r.aberta()).isFalse();
        assertThat(r.custoVendido()).isEqualByComparingTo("1000.00");
        assertThat(r.lucro()).isEqualByComparingTo("200.00");
        assertThat(r.rentabilidadePercentual()).isEqualByComparingTo("20.00");
        assertThat(r.prazoDias()).isEqualTo(10L);
    }

    @Test
    @DisplayName("venda com prejuízo: rentabilidade negativa")
    void vendaComPrejuizo() {
        OperacaoCompraVendaModel op = operacao(new BigDecimal("1000.00"), new BigDecimal("700.00"),
                BigDecimal.valueOf(100), null, "2024-01-01", "2024-03-01");

        CalculadoraOperacao.Resultado r = CalculadoraOperacao.calcular(op, null);

        assertThat(r.lucro()).isEqualByComparingTo("-300.00");
        assertThat(r.rentabilidadePercentual()).isEqualByComparingTo("-30.00");
        assertThat(r.prazoDias()).isEqualTo(60L);
    }

    @Test
    @DisplayName("venda PARCIAL: o custo é rateado pela quantidade vendida")
    void vendaParcialRateiaCusto() {
        // Comprou 100 por R$ 1.000 (R$ 10/cota) e vendeu 40 por R$ 500.
        // Custo da parte vendida = 1.000 × 40/100 = 400 ⇒ lucro 100 ⇒ 25%.
        OperacaoCompraVendaModel op = operacao(new BigDecimal("1000.00"), new BigDecimal("500.00"),
                BigDecimal.valueOf(100), BigDecimal.valueOf(40), "2024-01-01", "2024-02-01");

        CalculadoraOperacao.Resultado r = CalculadoraOperacao.calcular(op, null);

        assertThat(r.custoVendido()).isEqualByComparingTo("400.00");
        assertThat(r.lucro()).isEqualByComparingTo("100.00");
        assertThat(r.rentabilidadePercentual()).isEqualByComparingTo("25.00");
    }

    @Test
    @DisplayName("custo zero não divide por zero: rentabilidade fica nula")
    void custoZeroNaoDivide() {
        OperacaoCompraVendaModel op = operacao(BigDecimal.ZERO, new BigDecimal("100.00"),
                null, null, "2024-01-01", "2024-01-02");

        CalculadoraOperacao.Resultado r = CalculadoraOperacao.calcular(op, null);

        assertThat(r.lucro()).isEqualByComparingTo("100.00");
        assertThat(r.rentabilidadePercentual()).isNull();
    }

    @Test
    @DisplayName("data de venda anterior à compra usa o prazo sentinela de 400 dias")
    void prazoInconsistente() {
        OperacaoCompraVendaModel op = operacao(new BigDecimal("1000.00"), new BigDecimal("1100.00"),
                BigDecimal.valueOf(10), null, "2024-02-01", "2024-01-01");

        CalculadoraOperacao.Resultado r = CalculadoraOperacao.calcular(op, null);

        assertThat(r.prazoDias()).isEqualTo(CalculadoraOperacao.PRAZO_INCONSISTENTE_DIAS);
    }

    /* ══ Operação aberta ══ */

    @Test
    @DisplayName("operação aberta: resultado NÃO realizado com o preço atual do catálogo")
    void operacaoAbertaUsaPrecoAtual() {
        // Comprou 10 cotas por R$ 1.000; preço atual R$ 150 ⇒ valor atual 1.500.
        OperacaoCompraVendaModel op = operacao(new BigDecimal("1000.00"), null,
                BigDecimal.valueOf(10), null, "2024-01-01", null);

        CalculadoraOperacao.Resultado r = CalculadoraOperacao.calcular(op, new BigDecimal("150.00"));

        assertThat(r.aberta()).isTrue();
        assertThat(r.custoVendido()).isNull();
        assertThat(r.lucro()).isEqualByComparingTo("500.00");
        assertThat(r.rentabilidadePercentual()).isEqualByComparingTo("50.00");
        assertThat(r.prazoDias()).isNull();
    }

    /* ── Apoio ── */

    private static OperacaoCompraVendaModel operacao(BigDecimal valorInicial, BigDecimal valorVenda,
                                                     BigDecimal quantidade, BigDecimal quantidadeVendida,
                                                     String compra, String venda) {
        OperacaoCompraVendaModel op = new OperacaoCompraVendaModel();
        op.setValorInicial(valorInicial);
        op.setValorVenda(valorVenda);
        op.setQuantidade(quantidade);
        op.setQuantidadeVendida(quantidadeVendida);
        op.setDataCompra(LocalDate.parse(compra));
        op.setDataVenda(venda != null ? LocalDate.parse(venda) : null);
        return op;
    }
}
