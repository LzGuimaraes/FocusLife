package dev.LzGuimaraes.FocusLifeHub.Planejamento.Aporte;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;

import dev.LzGuimaraes.FocusLifeHub.Ativo.CategoriaInvestimento;
import dev.LzGuimaraes.FocusLifeHub.Planejamento.CarteiraIdeal.dto.ComparativoResponseDTO;

class AlocacaoRegressaoTest {
    private final AlocacaoService motor = new AlocacaoService();

    @Test
    void casoInformadoDistingueLimiteDeSaldoInsuficiente() {
        var candidatos = List.of(
                AporteTestes.candidato(CategoriaInvestimento.CRIPTOMOEDAS, "BTC", 100, 0, 96.54, 419558.09),
                AporteTestes.candidato(CategoriaInvestimento.RENDA_FIXA, "PICPAY", 100, 0, 977.78, null),
                AporteTestes.candidato(CategoriaInvestimento.ACOES, "VALE3", 100, 0, 10.45, 67.16));
        var referencia = AporteTestes.comparativo(20000, List.of(
                AporteTestes.classe(CategoriaInvestimento.CRIPTOMOEDAS, 20000, 0, 96.54),
                AporteTestes.classe(CategoriaInvestimento.RENDA_FIXA, 20000, 0, 977.78),
                AporteTestes.classe(CategoriaInvestimento.ACOES, 20000, 0, 10.45)));
        var resultado = motor.ratear(candidatos, referencia, new BigDecimal("1500"));
        assertThat(resultado.alocado()).isEqualByComparingTo("1074.32");
        assertThat(resultado.troco()).isEqualByComparingTo("425.68");
        var capacidades = motor.capacidadesRestantes(candidatos, referencia, resultado);
        var servico = new AporteService(null, null, motor, new UtilizacaoAporteService(motor), null, null, null);
        assertThat(servico.explicacaoNaoAlocado(resultado.troco(), candidatos, capacidades))
                .contains("VALE3 só comporta R$ 10,45", "R$ 67,16", "ultrapassaria o limite")
                .doesNotContain("troco de arredondamento", "saldo é menor");
    }

    @Test
    void capacidadeBloqueadaNaSubclasseNaoPrendeOrcamentoDeOutraClasse() {
        var bloqueada = classeBloqueada();
        var candidatos = List.of(
                AporteTestes.candidatoNaSubclasse(CategoriaInvestimento.ACOES, "Limitado", 90, 100, 99100, 1.0, 1, "Limitada"),
                AporteTestes.candidato(CategoriaInvestimento.RENDA_FIXA, "Livre", 80, 0, 1000, null));
        var resultado = motor.ratear(candidatos, AporteTestes.comparativo(10000, List.of(bloqueada,
                AporteTestes.classe(CategoriaInvestimento.RENDA_FIXA, 10000, 0, 1000))), new BigDecimal("1000"));
        assertThat(resultado.deCandidato(0)).isZero();
        assertThat(resultado.deCandidato(1)).isEqualByComparingTo("1000");
        assertThat(resultado.troco()).isZero();
    }

    @Test
    void subclasseBloqueadaNaoImpedeQuemNaoTemSubclasse() {
        var candidatos = List.of(
                AporteTestes.candidatoNaSubclasse(CategoriaInvestimento.ACOES, "Limitado", 90, 100, 99100, 1.0, 1, "Limitada"),
                AporteTestes.candidato(CategoriaInvestimento.ACOES, "Sem subclasse", 80, 0, 1000, null));
        var resultado = motor.ratear(candidatos, AporteTestes.comparativo(10000, List.of(classeBloqueada())), new BigDecimal("1000"));
        assertThat(resultado.deCandidato(0)).isZero();
        assertThat(resultado.deCandidato(1)).isEqualByComparingTo("1000");
    }

    @Test
    void redistribuicaoDoSaldoRespeitaLimitesDeClasse() {
        var candidatos = List.of(
                AporteTestes.candidato(CategoriaInvestimento.ACOES, "Acao", 90, 0, 1000, 100.0),
                AporteTestes.candidato(CategoriaInvestimento.RENDA_FIXA, "RF", 80, 0, 1000, null));
        var resultado = motor.ratear(candidatos, AporteTestes.comparativo(10000, List.of(
                AporteTestes.classeComLimite(CategoriaInvestimento.ACOES, 10000, 0, 1000, 1.5),
                AporteTestes.classeComLimite(CategoriaInvestimento.RENDA_FIXA, 10000, 0, 1000, 0.1))), new BigDecimal("500"));
        assertThat(resultado.deCandidato(0)).isEqualByComparingTo("100");
        assertThat(resultado.deCandidato(1)).isEqualByComparingTo("10");
        assertThat(resultado.troco()).isEqualByComparingTo("390");
    }

    @Test
    void redistribuicaoRespeitaLimitesCompartilhadosDeSubclasse() {
        var candidatos = List.of(
                AporteTestes.candidatoNaSubclasse(CategoriaInvestimento.ACOES, "A", 90, 0, 1000, 100.0, 1, "Limitada"),
                AporteTestes.candidatoNaSubclasse(CategoriaInvestimento.ACOES, "B", 80, 0, 1000, 60.0, 1, "Limitada"),
                AporteTestes.candidato(CategoriaInvestimento.ACOES, "Livre", 50, 0, 1000, 1.0));
        var sub = comLimite(AporteTestes.subclasse(1, "Limitada", 50, 0, 1000), 1.5);
        var referencia = AporteTestes.comparativo(10000, List.of(
                AporteTestes.classe(CategoriaInvestimento.ACOES, 10000, 0, 2000, List.of(sub))));
        var resultado = motor.ratear(candidatos, referencia, new BigDecimal("1000"));
        assertThat(resultado.porSubclasse().getOrDefault(1L, BigDecimal.ZERO)).isLessThanOrEqualTo(new BigDecimal("150"));
        assertThat(resultado.alocado()).isEqualByComparingTo("1000");
    }

    @Test
    void dinheiroPequenoContinuaAplicavelEmPatrimonioGrande() {
        var resultado = motor.ratear(List.of(AporteTestes.candidato(CategoriaInvestimento.RENDA_FIXA,
                "RF", 80, 0, 1000, null)), AporteTestes.comparativo(1000000, List.of(
                AporteTestes.classe(CategoriaInvestimento.RENDA_FIXA, 1000000, 0, 1000))), new BigDecimal("0.51"));
        assertThat(resultado.alocado()).isEqualByComparingTo("0.51");
        assertThat(resultado.troco()).isZero();
    }

    @Test
    void precoFloatNaoFazUmaCotaInteiraDesaparecer() {
        var candidato = AporteTestes.candidato(CategoriaInvestimento.ACOES, "VALE3", 100, 0, 1000, (double) 67.16f);
        var resultado = motor.ratear(List.of(candidato), AporteTestes.comparativo(10000, List.of(
                AporteTestes.classe(CategoriaInvestimento.ACOES, 10000, 0, 1000))), new BigDecimal("268.64"));
        assertThat(candidato.precoUnitario()).isEqualByComparingTo("67.16");
        assertThat(resultado.quantidadeDe(0)).isEqualByComparingTo("4");
        assertThat(resultado.alocado()).isEqualByComparingTo("268.64");
        assertThat(resultado.troco()).isZero();
    }

    @Test
    void abaixoDaMetaTemPreferenciaSobreNotaMaiorNaMargem() {
        var candidatos = List.of(
                AporteTestes.comMeta(CategoriaInvestimento.RENDA_FIXA, "Acima", 100, 10, 1020, 10000, 5, null, null),
                AporteTestes.comMeta(CategoriaInvestimento.RENDA_FIXA, "Abaixo", 50, 10, 0, 10000, 5, null, null));
        var resultado = motor.ratear(candidatos, referenciaRF(), new BigDecimal("500"));
        assertThat(resultado.deCandidato(0)).isZero();
        assertThat(resultado.deCandidato(1)).isEqualByComparingTo("500");
    }

    @Test
    void deficitSemNotaTemPreferenciaSobreAtivoAvaliadoAcimaDaMeta() {
        var candidatos = List.of(
                AporteTestes.comMeta(CategoriaInvestimento.RENDA_FIXA, "Acima", 100, 10, 1020, 10000, 5, null, null),
                AporteTestes.comMeta(CategoriaInvestimento.RENDA_FIXA, "Abaixo", null, 10, 0, 10000, 5, null, null));
        var resultado = motor.ratear(candidatos, referenciaRF(), new BigDecimal("500"));
        assertThat(resultado.deCandidato(0)).isZero();
        assertThat(resultado.deCandidato(1)).isEqualByComparingTo("500");
    }

    @Test
    void depoisDePreencherMetaUsaMargemAteLimite() {
        var candidatos = List.of(AporteTestes.comMeta(CategoriaInvestimento.RENDA_FIXA, "RF", 100,
                10, 0, 10000, 5, null, null));
        var resultado = motor.ratear(candidatos, referenciaRF(), new BigDecimal("1100"));
        assertThat(resultado.alocado()).isEqualByComparingTo("1050");
        assertThat(resultado.troco()).isEqualByComparingTo("50");
    }

    @Test
    void semNotaENotaZeroRecebemDepoisQueNotasPositivasAtingemMeta() {
        var candidatos = List.of(
                AporteTestes.candidato(CategoriaInvestimento.RENDA_FIXA, "Avaliado", 100, 0, 100, null),
                AporteTestes.candidato(CategoriaInvestimento.RENDA_FIXA, "Sem nota", null, 0, 600, null),
                AporteTestes.candidato(CategoriaInvestimento.RENDA_FIXA, "Zero", 0, 0, 300, null));
        var resultado = motor.ratear(candidatos, referenciaRF(), new BigDecimal("1000"));
        assertThat(resultado.deCandidato(0)).isEqualByComparingTo("100");
        assertThat(resultado.deCandidato(1)).isEqualByComparingTo("600");
        assertThat(resultado.deCandidato(2)).isEqualByComparingTo("300");
    }

    @Test
    void menorPrecoDaExplicacaoConsideraSomenteCotasQueCabemNaCapacidade() {
        var candidatos = List.of(
                AporteTestes.candidato(CategoriaInvestimento.ACOES, "Cara", 100, 0, 500, 100.0),
                AporteTestes.candidato(CategoriaInvestimento.ACOES, "Barata", 50, 0, 500, 50.0));
        var servico = new AporteService(null, null, motor, new UtilizacaoAporteService(motor), null, null, null);
        assertThat(servico.explicacaoNaoAlocado(new BigDecimal("20"), candidatos,
                List.of(new BigDecimal("500"), new BigDecimal("500"))))
                .contains("cota mais barata", "R$ 50,00").doesNotContain("R$ 100,00");
    }

    @Test
    void cenariosVariadosPreservamSaldoCotasELimitesSemDeixarDinheiroExecutavel() {
        Random random = new Random(42);
        for (int rodada = 0; rodada < 200; rodada++) {
            List<AporteCandidato> candidatos = new ArrayList<>();
            for (int i = 0; i < 12; i++) {
                CategoriaInvestimento classe = i < 6 ? CategoriaInvestimento.ACOES : CategoriaInvestimento.RENDA_FIXA;
                candidatos.add(AporteTestes.candidatoNaSubclasse(classe, "Ativo " + i,
                        i % 3 == 0 ? null : random.nextInt(101), 0, random.nextInt(100000) / 100.0,
                        classe == CategoriaInvestimento.ACOES ? 1.0 + random.nextInt(10000) / 100.0 : null,
                        i < 6 ? 1 : 2, "Subclasse"));
            }
            var referencia = AporteTestes.comparativo(10000, List.of(
                    classeComTeto(CategoriaInvestimento.ACOES, 1, 10, 8),
                    classeComTeto(CategoriaInvestimento.RENDA_FIXA, 2, 12, 9)));
            BigDecimal aporte = BigDecimal.valueOf(random.nextInt(500000), 2);
            var resultado = motor.ratear(candidatos, referencia, aporte);
            if (aporte.signum() == 0) continue;
            assertThat(resultado.alocado().add(resultado.troco())).isEqualByComparingTo(aporte);
            assertThat(resultado.alocado()).isLessThanOrEqualTo(aporte);
            assertThat(resultado.porClasse().getOrDefault(CategoriaInvestimento.ACOES, BigDecimal.ZERO)).isLessThanOrEqualTo(new BigDecimal("1000"));
            assertThat(resultado.porSubclasse().getOrDefault(1L, BigDecimal.ZERO)).isLessThanOrEqualTo(new BigDecimal("800"));
            assertThat(resultado.porSubclasse().getOrDefault(2L, BigDecimal.ZERO)).isLessThanOrEqualTo(new BigDecimal("900"));
            var capacidades = motor.capacidadesRestantes(candidatos, referencia, resultado);
            for (int i = 0; i < candidatos.size(); i++) {
                var c = candidatos.get(i);
                assertThat(resultado.deCandidato(i)).isBetween(BigDecimal.ZERO, c.capacidade());
                if (c.classe() == CategoriaInvestimento.ACOES) {
                    assertThat(resultado.deCandidato(i).remainder(c.precoUnitario())).isZero();
                    if (capacidades.get(i).compareTo(c.precoUnitario()) >= 0) {
                        assertThat(resultado.troco()).isLessThan(c.precoUnitario());
                    }
                } else if (capacidades.get(i).signum() > 0) {
                    assertThat(resultado.troco()).isZero();
                }
            }
        }
    }

    private ComparativoResponseDTO referenciaRF() {
        return AporteTestes.comparativo(10000, List.of(AporteTestes.classe(CategoriaInvestimento.RENDA_FIXA, 10000, 0, 2000)));
    }

    private ComparativoResponseDTO.ClasseComparativoDTO classeBloqueada() {
        return AporteTestes.classe(CategoriaInvestimento.ACOES, 10000, 100, 1000,
                List.of(comLimite(AporteTestes.subclasse(1, "Limitada", 100, 100, 1000), 1)));
    }

    private ComparativoResponseDTO.ClasseComparativoDTO classeComTeto(CategoriaInvestimento classe, long id,
                                                                    double limiteClasse, double limiteSub) {
        var c = AporteTestes.classeComLimite(classe, 10000, 0, 2000, limiteClasse);
        return new ComparativoResponseDTO.ClasseComparativoDTO(c.classe(), c.percentual_ideal(), c.percentual_atual(),
                c.valor_ideal(), c.valor_atual(), c.deficit(), c.excesso(), c.tolerancia(), c.limite_maximo(),
                List.of(comLimite(AporteTestes.subclasse(id, "Subclasse", 100, 0, 2000), limiteSub)), List.of());
    }

    private ComparativoResponseDTO.SubclasseComparativoDTO comLimite(ComparativoResponseDTO.SubclasseComparativoDTO s, double limite) {
        return new ComparativoResponseDTO.SubclasseComparativoDTO(s.id(), s.nome(), s.percentual_ideal(), s.percentual_atual(),
                s.valor_ideal(), s.valor_atual(), s.deficit(), s.excesso(), s.tolerancia(), BigDecimal.valueOf(limite));
    }
}
