package dev.LzGuimaraes.FocusLifeHub.Investimento.ComprasVendas;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Agrega operações calculadas no RESUMO e na SÉRIE do gráfico — também sem
 * banco, para ser testável.
 *
 * A série responde ao gráfico de andamento: em cada período mostra o
 * {@code valorBruto} movimentado e o {@code percentual acumulado} da
 * rentabilidade realizada ATÉ aquele período. A granularidade é por DIA em
 * janelas curtas (até 31 dias) e por MÊS caso contrário.
 */
public final class ResumoComprasVendas {

    public enum Granularidade { DIA, MES }

    private ResumoComprasVendas() {
    }

    /**
     * Operação já calculada, pronta para entrar no resumo.
     *
     * @param referencia   data que coloca a operação no período (venda, ou compra
     *                     se ainda aberta).
     * @param valorInicial custo total da compra.
     * @param valorBruto   valor movimentado (venda das fechadas / custo das abertas).
     * @param lucro        lucro realizado (null/vazio na operação aberta).
     * @param custoVendido custo da parte vendida (null/vazio na operação aberta).
     * @param aberta       true quando ainda não houve venda.
     */
    public record Linha(LocalDate referencia, BigDecimal valorInicial, BigDecimal valorBruto,
                        BigDecimal lucro, BigDecimal custoVendido, boolean aberta) {
    }

    public record Ponto(String periodo, BigDecimal valorBruto, BigDecimal valorPercentualAcumulado) {
    }

    public record Resultado(BigDecimal totalInvestido, BigDecimal totalVendido, BigDecimal lucroTotal,
                            BigDecimal rentabilidadePercentual, long qtdOperacoes, long qtdAbertas,
                            long qtdFechadas, List<Ponto> serie) {
    }

    public static Granularidade decidirGranularidade(LocalDate de, LocalDate ate) {
        if (de != null && ate != null && ChronoUnit.DAYS.between(de, ate) <= 31) {
            return Granularidade.DIA;
        }
        return Granularidade.MES;
    }

    public static Resultado montar(List<Linha> linhas, Granularidade granularidade) {
        BigDecimal totalInvestido = BigDecimal.ZERO;
        BigDecimal totalVendido = BigDecimal.ZERO;
        BigDecimal lucroTotal = BigDecimal.ZERO;
        BigDecimal custoVendidoTotal = BigDecimal.ZERO;
        long abertas = 0L;
        long fechadas = 0L;

        for (Linha l : linhas) {
            totalInvestido = totalInvestido.add(nz(l.valorInicial()));
            if (l.aberta()) {
                abertas++;
                continue;
            }
            fechadas++;
            totalVendido = totalVendido.add(nz(l.valorBruto()));
            lucroTotal = lucroTotal.add(nz(l.lucro()));
            custoVendidoTotal = custoVendidoTotal.add(nz(l.custoVendido()));
        }

        // Série: valor bruto por período + rentabilidade realizada ACUMULADA.
        Map<String, BigDecimal> brutoPorPeriodo = new LinkedHashMap<>();
        Map<String, BigDecimal> lucroPorPeriodo = new LinkedHashMap<>();
        Map<String, BigDecimal> custoPorPeriodo = new LinkedHashMap<>();
        for (Linha l : linhas) {
            if (l.referencia() == null) {
                continue;
            }
            String periodo = periodo(l.referencia(), granularidade);
            brutoPorPeriodo.merge(periodo, nz(l.valorBruto()), BigDecimal::add);
            if (!l.aberta()) {
                lucroPorPeriodo.merge(periodo, nz(l.lucro()), BigDecimal::add);
                custoPorPeriodo.merge(periodo, nz(l.custoVendido()), BigDecimal::add);
            }
        }

        // yyyy-MM-dd e yyyy-MM ordenam lexicograficamente = ordem cronológica.
        List<String> periodos = new ArrayList<>(brutoPorPeriodo.keySet());
        periodos.sort(String::compareTo);

        List<Ponto> serie = new ArrayList<>();
        BigDecimal lucroAcumulado = BigDecimal.ZERO;
        BigDecimal custoAcumulado = BigDecimal.ZERO;
        for (String p : periodos) {
            lucroAcumulado = lucroAcumulado.add(lucroPorPeriodo.getOrDefault(p, BigDecimal.ZERO));
            custoAcumulado = custoAcumulado.add(custoPorPeriodo.getOrDefault(p, BigDecimal.ZERO));
            serie.add(new Ponto(p, brutoPorPeriodo.get(p).setScale(2, RoundingMode.HALF_UP),
                    CalculadoraOperacao.rentabilidade(lucroAcumulado, custoAcumulado)));
        }

        return new Resultado(
                totalInvestido.setScale(2, RoundingMode.HALF_UP),
                totalVendido.setScale(2, RoundingMode.HALF_UP),
                lucroTotal.setScale(2, RoundingMode.HALF_UP),
                CalculadoraOperacao.rentabilidade(lucroTotal, custoVendidoTotal),
                linhas.size(), abertas, fechadas, serie);
    }

    static String periodo(LocalDate data, Granularidade granularidade) {
        return granularidade == Granularidade.DIA
                ? data.toString()
                : data.format(DateTimeFormatter.ofPattern("yyyy-MM"));
    }

    private static BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }
}
