package dev.LzGuimaraes.FocusLifeHub.Planejamento.Aporte;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import dev.LzGuimaraes.FocusLifeHub.Ativo.CategoriaInvestimento;
import dev.LzGuimaraes.FocusLifeHub.Planejamento.CarteiraIdeal.dto.ComparativoResponseDTO;

/**
 * A BASE DE CÁLCULO do motor de aporte, travada por teste.
 *
 * O bug que estes testes impedem de voltar: medir o déficit contra o patrimônio
 * de HOJE (T) em vez do PROJETADO (R = T + A). Com metas que espelham a carteira,
 * "%ideal × T" é exatamente o valor atual, então o déficit dava R$ 0,00 e o
 * aporte ficava sem destino — era o "Atual = Ideal · Déficit R$ 0,00" na tela
 * mesmo depois de informar o aporte.
 *
 * As dez propriedades do enunciado estão cobertas aqui (e a de distribuição, em
 * `AlocacaoAporteTest`).
 */
class ReferenciaAporteTest {

    private static final BigDecimal PATRIMONIO_ATUAL = new BigDecimal("27500.00");
    private static final BigDecimal APORTE = new BigDecimal("10000.00");
    private static final BigDecimal PATRIMONIO_PROJETADO = new BigDecimal("37500.00");

    /* ══ 1. Sem aporte nada muda ══ */

    @Test
    @DisplayName("1. com A = 0 os percentuais continuam coerentes com T")
    void semAporteNaoMudaNada() {
        ComparativoResponseDTO comparativo = carteiraNaMeta();

        ComparativoResponseDTO resultado = ReferenciaAporte.comAporte(comparativo, BigDecimal.ZERO);

        assertThat(resultado).isSameAs(comparativo);
        assertThat(resultado.valor_total()).isEqualByComparingTo(PATRIMONIO_ATUAL);
        assertThat(primeiraClasse(resultado).percentual_atual()).isEqualByComparingTo("4.3800");
        assertThat(primeiraClasse(resultado).valor_ideal()).isEqualByComparingTo("1204.50");
        assertThat(primeiraClasse(resultado).deficit()).isEqualByComparingTo("0.00");
    }

    /* ══ 2. O patrimônio de referência aumenta ══ */

    @Test
    @DisplayName("2. com A > 0 o patrimônio de referência é T + A")
    void patrimonioProjetadoSomaOAporte() {
        ComparativoResponseDTO resultado = ReferenciaAporte.comAporte(carteiraNaMeta(), APORTE);

        assertThat(resultado.valor_total()).isEqualByComparingTo(PATRIMONIO_PROJETADO);
    }

    @Test
    @DisplayName("2b. as três grandezas ficam explícitas (T, A e R)")
    void fatoresSaoExplicitos() {
        ReferenciaAporte.Fatores fatores = ReferenciaAporte.fatores(PATRIMONIO_ATUAL, APORTE);

        assertThat(fatores.patrimonioAtual()).isEqualByComparingTo(PATRIMONIO_ATUAL);
        assertThat(fatores.aporte()).isEqualByComparingTo(APORTE);
        assertThat(fatores.patrimonioProjetado()).isEqualByComparingTo(PATRIMONIO_PROJETADO);
        assertThat(fatores.semAporte()).isFalse();
        assertThat(ReferenciaAporte.fatores(PATRIMONIO_ATUAL, null).semAporte()).isTrue();
    }

    /* ══ 3 e 9. O aporte não é incorporado nos valores atuais ══ */

    @Test
    @DisplayName("3/9. nenhum valor ATUAL muda quando o aporte é informado")
    void aporteNaoEntraNosValoresAtuais() {
        ComparativoResponseDTO antes = carteiraNaMeta();
        ComparativoResponseDTO depois = ReferenciaAporte.comAporte(antes, APORTE);

        assertThat(primeiraClasse(depois).valor_atual()).isEqualByComparingTo(primeiraClasse(antes).valor_atual());
        assertThat(primeiroAtivo(depois).valor_atual()).isEqualByComparingTo(primeiroAtivo(antes).valor_atual());
        assertThat(primeiroAtivo(depois).valor_atual()).isEqualByComparingTo("1204.50");
        // O ideal (a meta) também é intocável: nunca é recalculado da carteira.
        assertThat(primeiroAtivo(depois).percentual_ideal())
                .isEqualByComparingTo(primeiroAtivo(antes).percentual_ideal());
        assertThat(primeiraClasse(depois).percentual_ideal())
                .isEqualByComparingTo(primeiraClasse(antes).percentual_ideal());
    }

    /* ══ 4. O percentual projetado cai ══ */

    @Test
    @DisplayName("4. o percentual atual projetado CAI quando o ativo não recebeu o aporte")
    void percentualProjetadoCai() {
        ComparativoResponseDTO depois = ReferenciaAporte.comAporte(carteiraNaMeta(), APORTE);

        // 1.204,50 ÷ 37.500 = 3,2120% (antes era 4,38% de 27.500)
        assertThat(primeiroAtivo(depois).percentual_atual()).isEqualByComparingTo("3.2120");
        assertThat(primeiraClasse(depois).percentual_atual()).isEqualByComparingTo("3.2120");
        assertThat(primeiroAtivo(depois).percentual_atual())
                .isLessThan(primeiroAtivo(carteiraNaMeta()).percentual_atual());
    }

    /* ══ 5 e 6. O déficit aparece ══ */

    @Test
    @DisplayName("5. o déficit aumenta quando o alvo cresce e o ativo não recebeu")
    void deficitAumentaComOAlvoMaior() {
        ComparativoResponseDTO antes = carteiraNaMeta();
        ComparativoResponseDTO depois = ReferenciaAporte.comAporte(antes, APORTE);

        assertThat(primeiroAtivo(antes).deficit()).isEqualByComparingTo("0.00");
        assertThat(primeiroAtivo(depois).deficit()).isEqualByComparingTo("438.00");
        assertThat(primeiroAtivo(depois).deficit()).isGreaterThan(primeiroAtivo(antes).deficit());
    }

    @Test
    @DisplayName("6. o ativo EXATAMENTE na meta passa a ser deficitário depois do aporte")
    void ativoNaMetaViraDeficitario() {
        // Caso relatado: ativo com 4,38% de uma carteira de R$ 27.500 (R$ 1.204,50)
        // e meta de 4,38% — aparecia "Atual 4,38% · Ideal 4,38% · Déficit R$ 0,00"
        // mesmo com aporte informado.
        ComparativoResponseDTO depois = ReferenciaAporte.comAporte(carteiraNaMeta(), APORTE);

        BigDecimal alvoProjetado = ReferenciaAporte.valorAlvoProjetado(
                new BigDecimal("4.38"), PATRIMONIO_PROJETADO);

        assertThat(alvoProjetado).isEqualByComparingTo("1642.50");              // 4,38% × 37.500
        assertThat(primeiroAtivo(depois).valor_ideal()).isEqualByComparingTo("1642.50");
        assertThat(primeiroAtivo(depois).deficit()).isEqualByComparingTo("438.00"); // 1.642,50 − 1.204,50
        assertThat(primeiroAtivo(depois).percentual_atual()).isLessThan(new BigDecimal("4.38"));
    }

    /* ══ 7. A soma dos valores atuais continua sendo T ══ */

    @Test
    @DisplayName("7. a soma dos valores atuais continua sendo o patrimônio de hoje (T)")
    void somaDosValoresAtuaisContinuaT() {
        ComparativoResponseDTO depois = ReferenciaAporte.comAporte(carteiraDuasClasses(), APORTE);

        BigDecimal somaClasses = depois.classes().stream()
                .map(ComparativoResponseDTO.ClasseComparativoDTO::valor_atual)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        assertThat(somaClasses).isEqualByComparingTo(PATRIMONIO_ATUAL);
        // E o alvo projetado é o patrimônio projetado inteiro nas classes que somam 100%.
        BigDecimal somaAlvos = depois.classes().stream()
                .map(ComparativoResponseDTO.ClasseComparativoDTO::valor_ideal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(somaAlvos).isEqualByComparingTo(PATRIMONIO_PROJETADO);
    }

    /* ══ 8. valorFinal = valorAtual + valorAporte ══ */

    @Test
    @DisplayName("8. valor final = valorAtual + valorAporte (a soma vira T + alocado)")
    void valorFinalEhAtualMaisAporte() {
        BigDecimal valorAtual = new BigDecimal("1204.50");
        BigDecimal valorAporte = new BigDecimal("438.00");

        BigDecimal valorFinal = valorAtual.add(valorAporte);

        assertThat(valorFinal).isEqualByComparingTo("1642.50");
        // Comprando exatamente o déficit, o ativo chega no alvo projetado.
        assertThat(valorFinal).isEqualByComparingTo(
                ReferenciaAporte.valorAlvoProjetado(new BigDecimal("4.38"), PATRIMONIO_PROJETADO));
    }

    /* ══ 10. Metas somando 100% → o déficit total comporta o aporte ══ */

    @Test
    @DisplayName("10. com metas somando 100% o déficit total é compatível com o aporte")
    void deficitTotalCompativelComOAporte() {
        ComparativoResponseDTO depois = ReferenciaAporte.comAporte(carteiraDuasClasses(), APORTE);

        BigDecimal somaPercentuais = depois.classes().stream()
                .map(ComparativoResponseDTO.ClasseComparativoDTO::percentual_ideal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal deficitTotal = depois.classes().stream()
                .map(ComparativoResponseDTO.ClasseComparativoDTO::deficit)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal somaAtuais = depois.classes().stream()
                .map(ComparativoResponseDTO.ClasseComparativoDTO::valor_atual)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        assertThat(somaPercentuais).isEqualByComparingTo("100.00");
        assertThat(somaAtuais).isEqualByComparingTo(PATRIMONIO_ATUAL);
        // Σ alvos − Σ atuais = R − T = A: é o que o aporte tem para preencher.
        assertThat(deficitTotal).isEqualByComparingTo(APORTE);
    }

    /* ══ Tolerância ══ */

    @Test
    @DisplayName("a tolerância não entra no déficit financeiro (ela é só rótulo)")
    void toleranciaNaoEntraNoCalculoFinanceiro() {
        ComparativoResponseDTO comTolerancia = ReferenciaAporte.comAporte(carteiraNaMeta(3), APORTE);
        ComparativoResponseDTO semTolerancia = ReferenciaAporte.comAporte(carteiraNaMeta(0), APORTE);

        assertThat(comTolerancia.classes().get(0).tolerancia()).isEqualByComparingTo("3.00");
        assertThat(comTolerancia.classes().get(0).deficit())
                .isEqualByComparingTo(semTolerancia.classes().get(0).deficit());
        assertThat(comTolerancia.classes().get(0).deficit()).isEqualByComparingTo("438.00");
    }

    /* ── Cenários ── */

    /**
     * UMA classe com UM ativo de 4,38% (R$ 1.204,50), os dois NA META: 4,38% de
     * 27.500 é exatamente 1.204,50, então o déficit de hoje é zero. É o cenário do
     * relato (T = 27.500, A = 10.000).
     */
    private static ComparativoResponseDTO carteiraNaMeta() {
        return carteiraNaMeta(0);
    }

    private static ComparativoResponseDTO carteiraNaMeta(double tolerancia) {
        return new ComparativoResponseDTO(1L, "BRL", PATRIMONIO_ATUAL, new BigDecimal("4.38"),
                List.of(classe(CategoriaInvestimento.ETFS, "4.38", "4.3800",
                        new BigDecimal("1204.50"), new BigDecimal("1204.50"), tolerancia)),
                List.of());
    }

    /**
     * Carteira de duas classes que somam 100% e R$ 27.500, cada uma com um ativo
     * dentro. As duas estão ABAIXO do alvo, então a soma dos déficits projetados
     * tem de fechar exatamente no aporte.
     */
    private static ComparativoResponseDTO carteiraDuasClasses() {
        return new ComparativoResponseDTO(1L, "BRL", PATRIMONIO_ATUAL, new BigDecimal("100.00"),
                List.of(
                        classe(CategoriaInvestimento.ACOES, "50.00", "43.6364",
                                new BigDecimal("12000.00"), new BigDecimal("13750.00"), 0),
                        classe(CategoriaInvestimento.FIIS, "50.00", "56.3636",
                                new BigDecimal("15500.00"), new BigDecimal("13750.00"), 0)),
                List.of());
    }

    /**
     * Uma classe com um ativo dentro. `valorAlvo` é o alvo de HOJE
     * (percentualIdeal × T) — é o que o comparativo real devolve antes do aporte.
     */
    private static ComparativoResponseDTO.ClasseComparativoDTO classe(CategoriaInvestimento categoria,
                                                                     String percentualIdeal,
                                                                     String percentualAtual,
                                                                     BigDecimal valorAtual,
                                                                     BigDecimal valorAlvo,
                                                                     double tolerancia) {
        BigDecimal ideal = new BigDecimal(percentualIdeal);
        BigDecimal deficit = valorAlvo.subtract(valorAtual).max(BigDecimal.ZERO);
        BigDecimal excesso = valorAtual.subtract(valorAlvo).max(BigDecimal.ZERO);
        ComparativoResponseDTO.AtivoComparativoDTO ativo = new ComparativoResponseDTO.AtivoComparativoDTO(
                1L, null, "ATIVO11", null, ideal, new BigDecimal(percentualAtual),
                valorAlvo, valorAtual, deficit, excesso, BigDecimal.ZERO, null, true);
        return new ComparativoResponseDTO.ClasseComparativoDTO(
                categoria, ideal, new BigDecimal(percentualAtual), valorAlvo, valorAtual,
                deficit, excesso, BigDecimal.valueOf(tolerancia), null, List.of(), List.of(ativo));
    }

    private static ComparativoResponseDTO.ClasseComparativoDTO primeiraClasse(ComparativoResponseDTO c) {
        return c.classes().get(0);
    }

    private static ComparativoResponseDTO.AtivoComparativoDTO primeiroAtivo(ComparativoResponseDTO c) {
        return c.classes().get(0).ativos().get(0);
    }
}
