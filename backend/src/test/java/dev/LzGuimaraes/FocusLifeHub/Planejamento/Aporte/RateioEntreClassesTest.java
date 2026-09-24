package dev.LzGuimaraes.FocusLifeHub.Planejamento.Aporte;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import dev.LzGuimaraes.FocusLifeHub.Ativo.CategoriaInvestimento;
import dev.LzGuimaraes.FocusLifeHub.Planejamento.CarteiraIdeal.dto.ComparativoResponseDTO;

/**
 * O APORTE TEM DE PASSAR DE UMA CLASSE PARA OUTRA.
 *
 * Cenário do bug relatado (T = R$ 21.006,68, A = R$ 1.000,00): Ações recebeu
 * R$ 178,51 e R$ 821,49 ficaram "sem destino" — mesmo existindo outras classes
 * com déficit e ativos elegíveis.
 *
 * Causa: o orçamento de cada classe era proporcional ao DÉFICIT DA CLASSE, não à
 * CAPACIDADE ELEGÍVEL dela. Classe com déficit grande e ativos já no teto (ou sem
 * ativo elegível) continuava "reservando" a sua fatia do fator em TODAS as
 * rodadas; como o dinheiro só é debitado do que foi de fato distribuído, o valor
 * reservado a quem não podia absorver ia sendo diluído rodada a rodada e, com o
 * limite de 6 rodadas, sobrava dinheiro que ninguém "enxergava" — enquanto as
 * classes líquidas ainda tinham capacidade sobrando.
 *
 * A regra agora: para o rateio, vale `min(déficit do nível, capacidade elegível
 * do nível)`. Déficit da classe ≠ capacidade elegível da classe (§ do pedido).
 */
class RateioEntreClassesTest {

    private static final double PATRIMONIO_ATUAL = 21006.68;
    private static final BigDecimal APORTE = new BigDecimal("1000.00");
    /** R = T + A: é sobre ele que os alvos do comparativo estão calculados. */
    private static final double R = 22006.68;

    private final AlocacaoService alocacao = new AlocacaoService();

    /* ══ O cenário do relato, com o diagnóstico impresso ══ */

    @Test
    @DisplayName("o valor não fica preso na primeira classe: todas as classes elegíveis participam")
    void aportePassaDeUmaClasseParaOutra() {
        List<AporteCandidato> candidatos = AporteTestes.ordenar(List.of(
                // AÇÕES: déficit grande, mas só UM ativo elegível com R$ 178,00 de espaço.
                AporteTestes.candidato(CategoriaInvestimento.ACOES, "BBAS3", 90, 6822.00, 7000.00, 1.0),
                AporteTestes.candidatoSemCapacidade(CategoriaInvestimento.ACOES, "TAEE3", 30, 5000.00),
                // FIIs: tem déficit, mas NENHUM ativo elegível (todos no próprio alvo).
                AporteTestes.candidatoSemCapacidade(CategoriaInvestimento.FIIS, "MXRF11", 80, 2801.00),
                // Renda Fixa: déficit com espaço de sobra.
                AporteTestes.candidato(CategoriaInvestimento.RENDA_FIXA, "PICPAY", 70, 3801.34, 4401.34, 1.0)));

        ComparativoResponseDTO comparativo = cenarios();

        OrcamentoAporte orcamento = alocacao.ratear(candidatos, comparativo, APORTE);

        imprimirDiagnostico(candidatos, comparativo, orcamento);

        assertThat(orcamento).isNotNull();
        // Ações recebe o que o ativo elegível comporta; FIIs, nada; RF, o resto do espaço.
        assertThat(orcamento.porClasse().get(CategoriaInvestimento.ACOES)).isEqualByComparingTo("178.00");
        // FIIs tem déficit, mas não tem ativo elegível: não recebe (e nem aparece).
        assertThat(orcamento.porClasse()).doesNotContainKey(CategoriaInvestimento.FIIS);
        assertThat(orcamento.porClasse().get(CategoriaInvestimento.RENDA_FIXA)).isEqualByComparingTo("600.00");
        // Σ capacidades elegíveis = 778,00 < aporte: só AQUI o não alocado é legítimo.
        assertThat(orcamento.alocado()).isEqualByComparingTo("778.00");
        assertThat(APORTE.subtract(orcamento.alocado())).isEqualByComparingTo("222.00");
    }

    /* ══ Os 10 casos exigidos ══ */

    @Test
    @DisplayName("1. classe com déficit menor que o aporte não encerra a distribuição")
    void classeComDeficitPequenoNaoEncerra() {
        // Σ capacidades elegíveis (178,51 + 900 = 1.078,51) > aporte → a divisão é
        // PROPORCIONAL (fator = aporte / Σ capacidades elegíveis = 0,9272), e o
        // troco das cotas é absorvido por quem aceita fração (renda fixa).
        OrcamentoAporte orcamento = alocacao.ratear(
                AporteTestes.ordenar(List.of(
                        AporteTestes.candidato(CategoriaInvestimento.ACOES, "A", 90, 0.0, 178.51, 1.0),
                        AporteTestes.candidato(CategoriaInvestimento.RENDA_FIXA, "B", 50, 0.0, 900.0, 1.0))),
                AporteTestes.comparativo(28500, List.of(
                        AporteTestes.classe(CategoriaInvestimento.ACOES, 28500, 0, 178.51),
                        AporteTestes.classe(CategoriaInvestimento.RENDA_FIXA, 28500, 0, 900.0))),
                APORTE);

        assertThat(orcamento.alocado()).isEqualByComparingTo("1000.00");   // nada fica preso
        assertThat(orcamento.porClasse().get(CategoriaInvestimento.ACOES)).isEqualByComparingTo("165.00");
        assertThat(orcamento.porClasse().get(CategoriaInvestimento.RENDA_FIXA)).isEqualByComparingTo("835.00");
    }

    @Test
    @DisplayName("2. o restante continua para a segunda classe")
    void restanteVaiParaASegundaClasse() {
        OrcamentoAporte orcamento = alocacao.ratear(
                AporteTestes.ordenar(List.of(
                        AporteTestes.candidato(CategoriaInvestimento.ACOES, "A", 90, 0.0, 100.0, 1.0),
                        AporteTestes.candidato(CategoriaInvestimento.FIIS, "B", 50, 0.0, 900.0, 1.0))),
                AporteTestes.comparativo(28500, List.of(
                        AporteTestes.classe(CategoriaInvestimento.ACOES, 28500, 0, 100.0),
                        AporteTestes.classe(CategoriaInvestimento.FIIS, 28500, 0, 900.0))),
                APORTE);

        assertThat(orcamento.porClasse().get(CategoriaInvestimento.ACOES)).isEqualByComparingTo("100.00");
        assertThat(orcamento.porClasse().get(CategoriaInvestimento.FIIS)).isEqualByComparingTo("900.00");
        assertThat(orcamento.alocado()).isEqualByComparingTo("1000.00");
    }

    @Test
    @DisplayName("3. classe com déficit e SEM ativo elegível não bloqueia as demais")
    void classeSemAtivoElegivelNaoBloqueia() {
        OrcamentoAporte orcamento = alocacao.ratear(
                AporteTestes.ordenar(List.of(
                        // FIIs: déficit de R$ 5.000 e nenhum ativo elegível.
                        AporteTestes.candidatoSemCapacidade(CategoriaInvestimento.FIIS, "PARADO", 80, 1000.0),
                        AporteTestes.candidato(CategoriaInvestimento.RENDA_FIXA, "LIVRE", 50, 0.0, 3000.0, 1.0))),
                AporteTestes.comparativo(28500, List.of(
                        AporteTestes.classe(CategoriaInvestimento.FIIS, 28500, 1000.0, 6000.0),
                        AporteTestes.classe(CategoriaInvestimento.RENDA_FIXA, 28500, 0, 3000.0))),
                APORTE);

        assertThat(orcamento.porClasse()).doesNotContainKey(CategoriaInvestimento.FIIS);
        assertThat(orcamento.alocado()).isEqualByComparingTo("1000.00");   // tudo em RF
    }

    @Test
    @DisplayName("4. duas classes elegíveis dividem o aporte proporcionalmente ao déficit elegível")
    void duasClassesDividemProporcionalmente() {
        OrcamentoAporte orcamento = alocacao.ratear(
                AporteTestes.ordenar(List.of(
                        AporteTestes.candidato(CategoriaInvestimento.ACOES, "A", 90, 0.0, 1000.0, 1.0),
                        AporteTestes.candidato(CategoriaInvestimento.FIIS, "B", 50, 0.0, 1000.0, 1.0))),
                AporteTestes.comparativo(28500, List.of(
                        AporteTestes.classe(CategoriaInvestimento.ACOES, 28500, 0, 1000.0),
                        AporteTestes.classe(CategoriaInvestimento.FIIS, 28500, 0, 1000.0))),
                APORTE);

        assertThat(orcamento.porClasse().get(CategoriaInvestimento.ACOES)).isEqualByComparingTo("500.00");
        assertThat(orcamento.porClasse().get(CategoriaInvestimento.FIIS)).isEqualByComparingTo("500.00");
    }

    @Test
    @DisplayName("5. Σ capacidades elegíveis < aporte → o não alocado é legítimo")
    void naoAlocadoSoQuandoNaoHaCapacidade() {
        OrcamentoAporte orcamento = alocacao.ratear(
                AporteTestes.ordenar(List.of(
                        AporteTestes.candidato(CategoriaInvestimento.ACOES, "A", 90, 0.0, 300.0, 1.0))),
                AporteTestes.comparativo(28500, List.of(
                        AporteTestes.classe(CategoriaInvestimento.ACOES, 28500, 0, 300.0))),
                APORTE);

        assertThat(orcamento.alocado()).isEqualByComparingTo("300.00");
        // Só aqui o não alocado é legítimo: nenhuma capacidade elegível sobrou.
        assertThat(APORTE.subtract(orcamento.alocado())).isEqualByComparingTo("700.00");
    }

    @Test
    @DisplayName("6. Σ capacidades elegíveis = aporte → não alocado zero")
    void capacidadesIguaisAoAporteZeramONaoAlocado() {
        OrcamentoAporte orcamento = alocacao.ratear(
                AporteTestes.ordenar(List.of(
                        AporteTestes.candidato(CategoriaInvestimento.ACOES, "A", 90, 0.0, 600.0, 1.0),
                        AporteTestes.candidato(CategoriaInvestimento.FIIS, "B", 50, 0.0, 400.0, 1.0))),
                AporteTestes.comparativo(28500, List.of(
                        AporteTestes.classe(CategoriaInvestimento.ACOES, 28500, 0, 800.0),
                        AporteTestes.classe(CategoriaInvestimento.FIIS, 28500, 0, 600.0))),
                APORTE);

        assertThat(orcamento.alocado()).isEqualByComparingTo("1000.00");
        assertThat(orcamento.troco()).isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("7. o orçamento de uma classe nunca supera o LIMITE cadastrado dela")
    void orcamentoNuncaSuperaOLimiteDaClasse() {
        // Limite explícito de 1% de R$ 28.500 = R$ 285,00; o ativo comporta R$ 900.
        // O que limita a classe é o LIMITE dela (não o déficit, não a capacidade).
        OrcamentoAporte orcamento = alocacao.ratear(
                AporteTestes.ordenar(List.of(
                        AporteTestes.candidato(CategoriaInvestimento.ACOES, "A", 90, 0.0, 900.0, 1.0))),
                AporteTestes.comparativo(28500, List.of(
                        AporteTestes.classeComLimite(CategoriaInvestimento.ACOES, 28500, 0, 900.0, 1.0))),
                APORTE);

        assertThat(orcamento.porClasse().get(CategoriaInvestimento.ACOES)).isEqualByComparingTo("285.00");
        assertThat(orcamento.alocado()).isEqualByComparingTo("285.00");
    }

    @Test
    @DisplayName("7b. a CLASSE acima da meta não zera os ativos dela (déficit não é trava)")
    void classeSemDeficitNaoZeraOsAtivos() {
        // Classe com alvo JÁ atingido (déficit 0) e sem limite máximo cadastrado.
        // Pela regra antiga, `deficitClasse.isEmpty()` encerrava o rateio e devolvia
        // nulo — o ativo nunca recebia, mesmo tendo espaço até o limite operacional.
        OrcamentoAporte orcamento = alocacao.ratear(
                AporteTestes.ordenar(List.of(
                        AporteTestes.candidato(CategoriaInvestimento.ACOES, "A", 90, 1000.0, 1500.0, 1.0))),
                AporteTestes.comparativo(28500, List.of(
                        AporteTestes.classe(CategoriaInvestimento.ACOES, 28500, 1000.0, 1000.0))),
                APORTE);

        assertThat(orcamento.alocado()).isEqualByComparingTo("500.00");
        assertThat(orcamento.porClasse().get(CategoriaInvestimento.ACOES)).isEqualByComparingTo("500.00");
    }

    @Test
    @DisplayName("8. o orçamento de uma classe nunca supera a capacidade elegível dela")
    void orcamentoNuncaSuperaACapacidadeElegivel() {
        // Classe com déficit de R$ 900, mas os ativos elegíveis só comportam R$ 150.
        OrcamentoAporte orcamento = alocacao.ratear(
                AporteTestes.ordenar(List.of(
                        AporteTestes.candidato(CategoriaInvestimento.ACOES, "A", 90, 0.0, 150.0, 1.0),
                        AporteTestes.candidatoSemCapacidade(CategoriaInvestimento.ACOES, "B", 80, 750.0))),
                AporteTestes.comparativo(28500, List.of(
                        AporteTestes.classe(CategoriaInvestimento.ACOES, 28500, 0, 900.0))),
                APORTE);

        assertThat(orcamento.porClasse().get(CategoriaInvestimento.ACOES)).isEqualByComparingTo("150.00");
        assertThat(orcamento.alocado()).isEqualByComparingTo("150.00");
    }

    @Test
    @DisplayName("9. o total alocado nunca supera o aporte")
    void totalAlocadoNuncaSuperaOAporte() {
        OrcamentoAporte orcamento = alocacao.ratear(
                AporteTestes.ordenar(List.of(
                        AporteTestes.candidato(CategoriaInvestimento.ACOES, "A", 90, 0.0, 5000.0, 1.0),
                        AporteTestes.candidato(CategoriaInvestimento.FIIS, "B", 50, 0.0, 5000.0, 1.0))),
                AporteTestes.comparativo(28500, List.of(
                        AporteTestes.classe(CategoriaInvestimento.ACOES, 28500, 0, 11000.0),
                        AporteTestes.classe(CategoriaInvestimento.FIIS, 28500, 0, 11000.0))),
                APORTE);

        assertThat(orcamento.alocado()).isLessThanOrEqualTo(APORTE);
        assertThat(orcamento.alocado().add(orcamento.troco())).isEqualByComparingTo(APORTE);
    }

    @Test
    @DisplayName("10. quem bate o teto sai da rodada e a sobra volta para os demais")
    void quemBateOTetoDevolveASobra() {
        // Ações tem só R$ 100 de espaço; o resto (R$ 900) tem de ir para FIIs.
        OrcamentoAporte orcamento = alocacao.ratear(
                AporteTestes.ordenar(List.of(
                        AporteTestes.candidato(CategoriaInvestimento.ACOES, "A", 90, 0.0, 100.0, 1.0),
                        AporteTestes.candidato(CategoriaInvestimento.FIIS, "B", 10, 0.0, 900.0, 1.0))),
                AporteTestes.comparativo(28500, List.of(
                        AporteTestes.classe(CategoriaInvestimento.ACOES, 28500, 0, 100.0),
                        AporteTestes.classe(CategoriaInvestimento.FIIS, 28500, 0, 900.0))),
                APORTE);

        assertThat(orcamento.porClasse().get(CategoriaInvestimento.ACOES)).isEqualByComparingTo("100.00");
        assertThat(orcamento.porClasse().get(CategoriaInvestimento.FIIS)).isEqualByComparingTo("900.00");
        assertThat(orcamento.alocado()).isEqualByComparingTo("1000.00");
    }

    @Test
    @DisplayName("o mesmo vale no nível da SUBCLASSE: subclasse sem espaço devolve para as outras")
    void subclasseSemEspacoDevolveParaAsOutras() {
        OrcamentoAporte orcamento = alocacao.ratear(
                AporteTestes.ordenar(List.of(
                        AporteTestes.candidatoNaSubclasse(CategoriaInvestimento.ACOES, "PARADA", 90,
                                1000.0, 1000.0, 1.0, 1L, "Parada"),
                        AporteTestes.candidatoNaSubclasse(CategoriaInvestimento.ACOES, "LIVRE", 50,
                                0.0, 900.0, 1.0, 2L, "Livre"))),
                AporteTestes.comparativo(28500, List.of(
                        AporteTestes.classe(CategoriaInvestimento.ACOES, 28500, 1000.0, 1900.0, List.of(
                                AporteTestes.subclasse(1L, "Parada", 50, 1000.0, 1000.0),
                                AporteTestes.subclasse(2L, "Livre", 50, 0.0, 900.0))))),
                APORTE);

        assertThat(orcamento.porSubclasse()).doesNotContainKey(1L);   // subclasse sem espaço
        assertThat(orcamento.porSubclasse().get(2L)).isEqualByComparingTo("900.00");
        assertThat(orcamento.alocado()).isEqualByComparingTo("900.00");
    }

    /* ── Cenário e diagnóstico ── */

    /** Ações com déficit grande, FIIs sem ativo elegível e Renda Fixa com espaço. */
    private static ComparativoResponseDTO cenarios() {
        return AporteTestes.comparativo(R, List.of(
                AporteTestes.classe(CategoriaInvestimento.ACOES, R, 11822.00, 14622.00),
                AporteTestes.classe(CategoriaInvestimento.FIIS, R, 2801.00, 3301.00),
                AporteTestes.classe(CategoriaInvestimento.RENDA_FIXA, R, 3801.34, 4401.34)));
    }

    /** Tabela do diagnóstico pedido: T, A, R e, por classe, tudo o que decide a fatia. */
    private void imprimirDiagnostico(List<AporteCandidato> candidatos, ComparativoResponseDTO comparativo,
                                     OrcamentoAporte orcamento) {
        double patrimonioProjetado = comparativo.valor_total().doubleValue();
        System.out.println("\n===== DIAGNÓSTICO DO RATEIO =====");
        System.out.printf("T (patrimônio atual) = %.2f   A (aporte) = %.2f   R = %.2f%n",
                PATRIMONIO_ATUAL, APORTE.doubleValue(), patrimonioProjetado);
        for (ComparativoResponseDTO.ClasseComparativoDTO c : comparativo.classes()) {
            List<AporteCandidato> daClasse = candidatos.stream().filter(x -> x.classe() == c.classe()).toList();
            long elegiveis = daClasse.stream().filter(AporteCandidato::elegivel).count();
            double capacidadeElegivel = daClasse.stream().filter(AporteCandidato::elegivel)
                    .mapToDouble(x -> x.capacidade().doubleValue()).sum();
            BigDecimal recebido = orcamento.porClasse().getOrDefault(c.classe(), BigDecimal.ZERO);
            System.out.printf(
                    "%-12s ideal=%6.2f%%  alvo=%10.2f  atual=%10.2f  déficit=%9.2f  "
                            + "elegíveis=%d  cap.elegível=%9.2f  recebido=%9.2f%n",
                    c.classe(), c.percentual_ideal().doubleValue(), c.valor_ideal().doubleValue(),
                    c.valor_atual().doubleValue(), c.deficit().doubleValue(),
                    elegiveis, Math.min(capacidadeElegivel, c.deficit().doubleValue()), recebido.doubleValue());
        }
        System.out.printf("aporte=%.2f  alocado=%.2f  restante=%.2f%n",
                APORTE.doubleValue(), orcamento.alocado().doubleValue(), orcamento.troco().doubleValue());
        System.out.println("=================================\n");
    }
}
