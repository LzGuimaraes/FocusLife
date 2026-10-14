package dev.LzGuimaraes.FocusLifeHub.Planejamento.Aporte;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;

import dev.LzGuimaraes.FocusLifeHub.Ativo.CategoriaInvestimento;
import dev.LzGuimaraes.FocusLifeHub.Planejamento.CarteiraIdeal.dto.ComparativoResponseDTO;

class UtilizacaoAporteTest {
    private final AlocacaoService alocacao = new AlocacaoService();
    private final UtilizacaoAporteService utilizacao = new UtilizacaoAporteService(alocacao);

    @Test
    void sobraMaximaEhDoAporteENaoDoPatrimonioOuDaMetaDoAtivo() {
        assertThat(MetaUtilizacaoAporte.sobraMaxima(new BigDecimal("1500"))).isEqualByComparingTo("75");
        assertThat(MetaUtilizacaoAporte.minimoAlocar(new BigDecimal("1500"))).isEqualByComparingTo("1425");
        assertThat(MetaUtilizacaoAporte.sobraMaxima(new BigDecimal("0.49"))).isEqualByComparingTo("0.02");
        assertThat(MetaUtilizacaoAporte.minimoAlocar(new BigDecimal("0.49"))).isEqualByComparingTo("0.47");
    }

    @Test
    void capacidadesDoCasoInformadoPassamDeSobra425ParaMenosDe75() {
        // Valores equivalentes às capacidades enviadas pelo usuário, com R=19.800.
        var referencia = AporteTestes.comparativo(19800, List.of(
                AporteTestes.classe(CategoriaInvestimento.CRIPTOMOEDAS, 19800, 3263.12, 3199.68),
                AporteTestes.classe(CategoriaInvestimento.RENDA_FIXA, 19800, 1101.22, 1980),
                AporteTestes.classe(CategoriaInvestimento.ACOES, 19800, 201.61, 201.96)));
        var aporte = new BigDecimal("1500");
        var inicial = alocacao.ratear(casoInformado(5d), referencia, aporte);
        assertThat(inicial.alocado()).isEqualByComparingTo("1074.32");
        var resultado = utilizacao.calcular(referencia, aporte, 5d, this::casoInformado);
        // A meta percentual é guardada com quatro casas antes de virar reais.
        assertThat(resultado.orcamento().alocado()).isEqualByComparingTo("1425.49");
        assertThat(resultado.orcamento().troco()).isEqualByComparingTo("74.51");
        assertThat(resultado.margemAplicada()).isEqualByComparingTo("11.78");
        assertThat(resultado.margemConfigurada()).isEqualByComparingTo("5");
        for (int i = 0; i < inicial.porCandidato().size(); i++) {
            assertThat(resultado.orcamento().deCandidato(i)).isGreaterThanOrEqualTo(inicial.deCandidato(i));
        }
        assertThat(resultado.orcamento().deCandidato(2)).isZero(); // VALE3 ainda não comporta uma unidade.
    }

    @Test
    void planoQueJaAplica95PorCentoNaoMuda() {
        var referencia = referenciaRF(1000, 1000);
        var candidatos = List.of(AporteTestes.comMeta(CategoriaInvestimento.RENDA_FIXA, "RF", 100,
                100, 0, 1000, 5, null, null));
        var inicial = alocacao.ratear(candidatos, referencia, new BigDecimal("1000"));
        var resultado = utilizacao.calcular(referencia, new BigDecimal("1000"), 5,
                margem -> List.of(AporteTestes.comMeta(CategoriaInvestimento.RENDA_FIXA, "RF", 100,
                        100, 0, 1000, margem, null, null)));
        assertThat(resultado.ajustada()).isFalse();
        assertThat(resultado.orcamento()).isEqualTo(inicial);
    }

    @Test
    void bloqueioDoChecklistNaoEhReabilitadoPelaMargem() {
        var referencia = referenciaRF(10000, 1000);
        var resultado = utilizacao.calcular(referencia, new BigDecimal("1500"), 5,
                margem -> List.of(AporteTestes.candidatoSemCapacidade(CategoriaInvestimento.RENDA_FIXA, "Bloqueado", 100, 1000)));
        assertThat(resultado.orcamento().alocado()).isZero();
        assertThat(resultado.ajustada()).isFalse();
        assertThat(MetaUtilizacaoAporte.atingida(new BigDecimal("1500"), resultado.orcamento())).isFalse();
    }

    @Test
    void limiteMaximoCadastradoDoAtivoPermaneceAbsoluto() {
        var referencia = referenciaRF(10000, 1000);
        var resultado = utilizacao.calcular(referencia, new BigDecimal("1500"), 5,
                margem -> List.of(AporteTestes.comMeta(CategoriaInvestimento.RENDA_FIXA, "RF", 100,
                        10, 0, 10000, margem, new BigDecimal("10.5"), null)));
        assertThat(resultado.orcamento().alocado()).isEqualByComparingTo("1050");
        assertThat(resultado.orcamento().troco()).isEqualByComparingTo("450");
        assertThat(resultado.ajustada()).isFalse();
    }

    @Test
    void limiteMaximoDaClasseNaoEhAlteradoParaCumprirUtilizacao() {
        var referencia = AporteTestes.comparativo(10000, List.of(
                AporteTestes.classeComLimite(CategoriaInvestimento.RENDA_FIXA, 10000, 0, 1000, 11)));
        var resultado = utilizacao.calcular(referencia, new BigDecimal("1500"), 5,
                margem -> List.of(AporteTestes.comMeta(CategoriaInvestimento.RENDA_FIXA, "RF", 100,
                        10, 0, 10000, margem, null, null)));
        assertThat(resultado.orcamento().alocado()).isEqualByComparingTo("1100");
        assertThat(resultado.orcamento().troco()).isEqualByComparingTo("400");
        assertThat(resultado.margemAplicada()).isEqualByComparingTo("10");
    }

    @Test
    void limiteMaximoDaSubclasseNaoEhAmpliado() {
        var sub = new ComparativoResponseDTO.SubclasseComparativoDTO(1L, "Subclasse", new BigDecimal("100"),
                BigDecimal.ZERO, new BigDecimal("1000"), BigDecimal.ZERO, new BigDecimal("1000"), BigDecimal.ZERO,
                BigDecimal.ZERO, new BigDecimal("8"));
        var referencia = AporteTestes.comparativo(10000, List.of(AporteTestes.classe(CategoriaInvestimento.RENDA_FIXA,
                10000, 0, 1000, List.of(sub))));
        var resultado = utilizacao.calcular(referencia, new BigDecimal("1500"), 5, margem -> {
            var limite = ReferenciaAporte.limite(new BigDecimal("10"), BigDecimal.ZERO, new BigDecimal("10000"), margem);
            return List.of(AporteTestes.candidatoNaSubclasse(CategoriaInvestimento.RENDA_FIXA, "RF", 100,
                    0, limite.limiteEmReais().doubleValue(), null, 1L, "Subclasse"));
        });
        assertThat(resultado.orcamento().alocado()).isEqualByComparingTo("800");
        assertThat(resultado.orcamento().porSubclasse().get(1L)).isEqualByComparingTo("800");
        assertThat(resultado.ajustada()).isFalse();
    }

    @Test
    void metaPositivaMenorQueUmCentavoNaoZeraBuscaDeCapacidade() {
        var referencia = referenciaRF(300, 0.0003);
        var resultado = utilizacao.calcular(referencia, new BigDecimal("10"), 5, margem -> List.of(
                AporteTestes.comMeta(CategoriaInvestimento.RENDA_FIXA, "RF", 100, 0.0001,
                        0, 300, margem, null, null)));
        assertThat(resultado.orcamento().alocado()).isGreaterThanOrEqualTo(new BigDecimal("9.50"));
        assertThat(resultado.orcamento().troco()).isLessThanOrEqualTo(new BigDecimal("0.50"));
    }

    @Test
    void quandoCotasImpedem95PorCentoEncontraMelhorUsoComMargemContida() {
        var referencia = AporteTestes.comparativo(10000, List.of(
                AporteTestes.classe(CategoriaInvestimento.ACOES, 10000, 0, 500)));
        var resultado = utilizacao.calcular(referencia, new BigDecimal("1500"), 5,
                margem -> List.of(AporteTestes.comMeta(CategoriaInvestimento.ACOES, "Acao", 100,
                        5, 0, 10000, margem, null, 800d)));
        assertThat(resultado.orcamento().alocado()).isEqualByComparingTo("800");
        assertThat(resultado.margemAplicada()).isEqualByComparingTo("60");
        assertThat(resultado.orcamento().quantidadeDe(0)).isEqualByComparingTo("1");
        assertThat(MetaUtilizacaoAporte.atingida(new BigDecimal("1500"), resultado.orcamento())).isFalse();
    }

    @Test
    void semAporteNaoAjustaMargemENaoCriaPlano() {
        var resultado = utilizacao.calcular(referenciaRF(10000, 1000), null, 5,
                margem -> List.of(AporteTestes.comMeta(CategoriaInvestimento.RENDA_FIXA, "RF", 100,
                        10, 0, 10000, margem, null, null)));
        assertThat(resultado.orcamento()).isNull();
        assertThat(resultado.ajustada()).isFalse();
    }

    private List<AporteCandidato> casoInformado(double margem) {
        return List.of(
                AporteTestes.comMeta(CategoriaInvestimento.CRIPTOMOEDAS, "BTC", 100, 16.16, 3263.12, 19800, margem, null, 419558.09),
                AporteTestes.comMeta(CategoriaInvestimento.RENDA_FIXA, "PICPAY", 100, 10, 1101.22, 19800, margem, null, null),
                AporteTestes.comMeta(CategoriaInvestimento.ACOES, "VALE3", 100, 1.02, 201.61, 19800, margem, null, 67.16));
    }

    private ComparativoResponseDTO referenciaRF(double patrimonio, double alvo) {
        return AporteTestes.comparativo(patrimonio, List.of(AporteTestes.classe(CategoriaInvestimento.RENDA_FIXA, patrimonio, 0, alvo)));
    }
}
