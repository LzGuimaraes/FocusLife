package dev.LzGuimaraes.FocusLifeHub.Investimento.ComprasVendas;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Agregação do resumo e da série do gráfico (totais + percentual acumulado),
 * sem banco.
 */
class ResumoComprasVendasTest {

    @Test
    @DisplayName("totais: investido, vendido, lucro e rentabilidade realizada")
    void totaisDoResumo() {
        ResumoComprasVendas.Resultado r = ResumoComprasVendas.montar(
                List.of(
                        linha("2024-01-10", "1000.00", "1200.00", "200.00", "1000.00", false),
                        linha("2024-02-05", "500.00", "400.00", "-100.00", "500.00", false),
                        linha("2024-02-20", "300.00", "300.00", null, null, true)),
                ResumoComprasVendas.Granularidade.MES);

        assertThat(r.totalInvestido()).isEqualByComparingTo("1800.00");
        assertThat(r.totalVendido()).isEqualByComparingTo("1600.00");
        assertThat(r.lucroTotal()).isEqualByComparingTo("100.00");
        // 100 / (1000 + 500) × 100 = 6,67%
        assertThat(r.rentabilidadePercentual()).isEqualByComparingTo("6.67");
        assertThat(r.qtdOperacoes()).isEqualTo(3);
        assertThat(r.qtdAbertas()).isEqualTo(1);
        assertThat(r.qtdFechadas()).isEqualTo(2);
    }

    @Test
    @DisplayName("série por mês: valor bruto do período e percentual realizado ACUMULADO")
    void seriePorMes() {
        ResumoComprasVendas.Resultado r = ResumoComprasVendas.montar(
                List.of(
                        linha("2024-01-10", "1000.00", "1200.00", "200.00", "1000.00", false),
                        linha("2024-02-05", "500.00", "400.00", "-100.00", "500.00", false),
                        linha("2024-02-20", "300.00", "300.00", null, null, true)),
                ResumoComprasVendas.Granularidade.MES);

        assertThat(r.serie()).hasSize(2);

        assertThat(r.serie().get(0).periodo()).isEqualTo("2024-01");
        assertThat(r.serie().get(0).valorBruto()).isEqualByComparingTo("1200.00");
        assertThat(r.serie().get(0).valorPercentualAcumulado()).isEqualByComparingTo("20.00");

        // Fevereiro soma venda (400) + custo da aberta (300) = 700 no valor bruto.
        assertThat(r.serie().get(1).periodo()).isEqualTo("2024-02");
        assertThat(r.serie().get(1).valorBruto()).isEqualByComparingTo("700.00");
        // Acumulado: (200 − 100) / (1000 + 500) × 100 = 6,67%.
        assertThat(r.serie().get(1).valorPercentualAcumulado()).isEqualByComparingTo("6.67");
    }

    @Test
    @DisplayName("sem venda ainda: percentual acumulado é nulo (nunca divide por zero)")
    void serieSemVendaNaoDivide() {
        ResumoComprasVendas.Resultado r = ResumoComprasVendas.montar(
                List.of(linha("2024-03-01", "500.00", "500.00", null, null, true)),
                ResumoComprasVendas.Granularidade.MES);

        assertThat(r.rentabilidadePercentual()).isNull();
        assertThat(r.serie()).hasSize(1);
        assertThat(r.serie().get(0).valorPercentualAcumulado()).isNull();
    }

    @Test
    @DisplayName("granularidade: dia em janelas curtas (≤31 dias), mês caso contrário")
    void granularidadePorJanela() {
        assertThat(ResumoComprasVendas.decidirGranularidade(
                LocalDate.parse("2024-01-01"), LocalDate.parse("2024-01-20")))
                .isEqualTo(ResumoComprasVendas.Granularidade.DIA);
        assertThat(ResumoComprasVendas.decidirGranularidade(
                LocalDate.parse("2024-01-01"), LocalDate.parse("2024-03-01")))
                .isEqualTo(ResumoComprasVendas.Granularidade.MES);
        assertThat(ResumoComprasVendas.decidirGranularidade(null, null))
                .isEqualTo(ResumoComprasVendas.Granularidade.MES);
    }

    private static ResumoComprasVendas.Linha linha(String referencia, String investido, String bruto,
                                                   String lucro, String custo, boolean aberta) {
        return new ResumoComprasVendas.Linha(
                LocalDate.parse(referencia),
                new BigDecimal(investido),
                new BigDecimal(bruto),
                lucro != null ? new BigDecimal(lucro) : null,
                custo != null ? new BigDecimal(custo) : null,
                aberta);
    }
}
