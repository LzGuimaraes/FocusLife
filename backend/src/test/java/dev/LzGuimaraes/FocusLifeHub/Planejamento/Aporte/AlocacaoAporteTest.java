package dev.LzGuimaraes.FocusLifeHub.Planejamento.Aporte;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import dev.LzGuimaraes.FocusLifeHub.Ativo.CategoriaInvestimento;
import dev.LzGuimaraes.FocusLifeHub.Planejamento.CarteiraIdeal.dto.ComparativoResponseDTO;

/**
 * ALOCAÇÃO do aporte: quanto entra em cada ativo, em COTAS INTEIRAS.
 *
 * Aqui a referência (`ReferenciaAporte`) e o rateio (`AlocacaoService`) são
 * exercitados juntos, sem banco: é o pipeline real do `AporteService` menos a
 * leitura dos dados.
 */
class AlocacaoAporteTest {

    private static final BigDecimal PATRIMONIO_ATUAL = new BigDecimal("27500.00");
    private static final BigDecimal APORTE = new BigDecimal("10000.00");

    private final AlocacaoService alocacao = new AlocacaoService();

    /* ══ Ordem por nota ══ */

    @Test
    @DisplayName("dentro do nível, o rateio é proporcional à NOTA do checklist (90 x 30 = 3:1)")
    void rateioProporcionalANota() {
        // Preço de R$ 1,00 para o arredondamento não interferir: o que está em
        // teste aqui é a PROPORÇÃO (90 x 30 = 3:1).
        OrcamentoAporte orcamento = alocar(
                List.of(candidato("NOVE", 90, new BigDecimal("6000.00"), new BigDecimal("18750.00"), "1.00"),
                        candidato("TRES", 30, new BigDecimal("6000.00"), new BigDecimal("18750.00"), "1.00")),
                classeUnica("100.00", new BigDecimal("12000.00"), new BigDecimal("37500.00")));

        assertThat(orcamento.deCandidato(0)).isEqualByComparingTo("7500.00");
        assertThat(orcamento.deCandidato(1)).isEqualByComparingTo("2500.00");
        assertThat(orcamento.alocado()).isEqualByComparingTo(APORTE);
        assertThat(orcamento.troco()).isEqualByComparingTo("0.00");
    }

    /* ══ Cotas inteiras ══ */

    @Test
    @DisplayName("a compra é em COTAS INTEIRAS pelo preço atual (nada de valor quebrado)")
    void compraEmCotasInteiras() {
        // Sugestão de R$ 7.500 a R$ 30,00 a cota = 250 cotas exatas;
        // R$ 2.500 a R$ 40,00 = 62,5 → 62 cotas (R$ 2.480,00), sobrando R$ 20,00.
        OrcamentoAporte orcamento = alocar(
                List.of(candidato("CARA", 90, new BigDecimal("6000.00"), new BigDecimal("18750.00"), "30.00"),
                        candidato("MEIO", 30, new BigDecimal("6000.00"), new BigDecimal("18750.00"), "40.00")),
                classeUnica("100.00", new BigDecimal("12000.00"), new BigDecimal("37500.00")));

        assertThat(orcamento.quantidadeDe(0)).isEqualByComparingTo("250");
        assertThat(orcamento.deCandidato(0)).isEqualByComparingTo("7500.00");
        assertThat(orcamento.quantidadeDe(1)).isEqualByComparingTo("62");
        assertThat(orcamento.deCandidato(1)).isEqualByComparingTo("2480.00");
        // O que não fecha uma cota fica como troco (R$ 20,00 não dá uma cota de R$ 30).
        assertThat(orcamento.troco()).isEqualByComparingTo("20.00");
    }

    @Test
    @DisplayName("o troco do arredondamento vai para o ativo de MELHOR NOTA que ainda tem espaço")
    void trocoVaiParaAMelhorNota() {
        // 7.500 / 1.000 = 7 cotas (troco 500) e 2.500 / 1.000 = 2 cotas (troco 500):
        // sobram R$ 1.000, que fecham uma cota do melhor avaliado.
        OrcamentoAporte orcamento = alocar(
                List.of(candidato("MELHOR", 90, new BigDecimal("6000.00"), new BigDecimal("18750.00"), "1000.00"),
                        candidato("PIOR", 30, new BigDecimal("6000.00"), new BigDecimal("18750.00"), "1000.00")),
                classeUnica("100.00", new BigDecimal("12000.00"), new BigDecimal("37500.00")));

        assertThat(orcamento.quantidadeDe(0)).isEqualByComparingTo("8");   // 7.500 → 8.000
        assertThat(orcamento.deCandidato(0)).isEqualByComparingTo("8000.00");
        assertThat(orcamento.deCandidato(1)).isEqualByComparingTo("2000.00");
        assertThat(orcamento.troco()).isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("ativo sem preço conhecido não é arredondado (renda fixa, caixinha)")
    void semPrecoNaoArredonda() {
        OrcamentoAporte orcamento = alocar(
                List.of(candidato("CAIXINHA", 90, new BigDecimal("6000.00"), new BigDecimal("18750.00"), null)),
                classeUnica("100.00", new BigDecimal("12000.00"), new BigDecimal("37500.00")));

        // Sem preço não há unidade para contar: o valor entra cheio e sem troco.
        assertThat(orcamento.quantidadeDe(0)).isNull();
        assertThat(orcamento.deCandidato(0)).isEqualByComparingTo(APORTE);
        assertThat(orcamento.troco()).isEqualByComparingTo("0.00");
    }

    /* ══ O aporte não altera o valor atual; o final é atual + aporte ══ */

    @Test
    @DisplayName("8/9. valor final = valorAtual + valorAporte e a soma final é T + alocado")
    void valorFinalEhAtualMaisAporte() {
        List<AporteCandidato> candidatos = List.of(
                candidato("UM", 90, new BigDecimal("6000.00"), new BigDecimal("18750.00"), "1000.00"),
                candidato("DOIS", 30, new BigDecimal("6000.00"), new BigDecimal("18750.00"), "1000.00"));
        OrcamentoAporte orcamento = alocar(candidatos,
                classeUnica("100.00", new BigDecimal("12000.00"), new BigDecimal("37500.00")));

        BigDecimal somaAtual = BigDecimal.ZERO;
        BigDecimal somaFinal = BigDecimal.ZERO;
        for (int i = 0; i < candidatos.size(); i++) {
            BigDecimal valorAtual = candidatos.get(i).valorAtual();
            BigDecimal valorFinal = valorAtual.add(orcamento.deCandidato(i));   // valorFinal = atual + aporte
            assertThat(valorFinal).isGreaterThanOrEqualTo(valorAtual);
            somaAtual = somaAtual.add(valorAtual);
            somaFinal = somaFinal.add(valorFinal);
        }

        assertThat(somaAtual).isEqualByComparingTo("12000.00");                      // nada foi incorporado antes
        assertThat(somaFinal).isEqualByComparingTo(somaAtual.add(orcamento.alocado()));
        assertThat(somaFinal).isEqualByComparingTo("22000.00");                      // 12.000 + 10.000
    }

    @Test
    @DisplayName("sem nenhuma capacidade o aporte não é distribuído (alocado 0, não alocado = aporte)")
    void semCapacidadeNadaEhAlocado() {
        ComparativoResponseDTO semEspaco = classeUnica("100.00",
                new BigDecimal("37500.00"), new BigDecimal("37500.00"));

        // Ninguém absorve: o aporte volta inteiro como não alocado. O rateio NÃO
        // devolve nulo (o valor foi informado e a resposta tem de dizer o que
        // aconteceu com ele).
        OrcamentoAporte nada = alocacao.ratear(
                List.of(candidato("CHEIO", 90, new BigDecimal("18750.00"), new BigDecimal("18750.00"), "1000.00")),
                semEspaco, APORTE);
        assertThat(nada).isNotNull();
        assertThat(nada.alocado()).isEqualByComparingTo("0.00");
        assertThat(APORTE.subtract(nada.alocado())).isEqualByComparingTo(APORTE);

        // Com aporte zero (ou nulo) não há o que distribuir: aí sim é nulo.
        assertThat(alocacao.ratear(
                List.of(candidato("UM", 90, new BigDecimal("6000.00"), new BigDecimal("18750.00"), "1000.00")),
                classeUnica("100.00", new BigDecimal("12000.00"), new BigDecimal("37500.00")),
                BigDecimal.ZERO)).isNull();
    }

    /* ══ Integração com a referência: o ativo na meta passa a receber ══ */

    @Test
    @DisplayName("6b. ativo que estava NA META (capacidade 0 em T) fica elegível depois do aporte")
    void ativoNaMetaPassaAReceberComOAporte() {
        // Hoje: 4,38% de 27.500 = 1.204,50 = alvo ⇒ déficit 0 ⇒ não recebe.
        ComparativoResponseDTO hoje = comparativo("4.38", new BigDecimal("1204.50"),
                new BigDecimal("1204.50"), new BigDecimal("27500.00"));
        assertThat(hoje.classes().get(0).deficit()).isEqualByComparingTo("0.00");

        // Depois do aporte: alvo = 4,38% × 37.500 = 1.642,50 ⇒ déficit 438,00.
        ComparativoResponseDTO projetado = ReferenciaAporte.comAporte(hoje, APORTE);
        BigDecimal deficitProjetado = projetado.classes().get(0).deficit();
        assertThat(deficitProjetado).isEqualByComparingTo("438.00");

        // Com essa capacidade, o ativo recebe o déficit (4 cotas de R$ 100 = R$ 400,
        // com R$ 38 de troco: não fecha a quinta cota).
        OrcamentoAporte orcamento = alocacao.ratear(
                List.of(candidato("PKIN11", 100, new BigDecimal("1204.50"),
                        projetado.classes().get(0).valor_ideal(), "100.00")),
                projetado, APORTE);

        assertThat(orcamento).isNotNull();
        assertThat(orcamento.quantidadeDe(0)).isEqualByComparingTo("4");
        assertThat(orcamento.deCandidato(0)).isEqualByComparingTo("400.00");
        assertThat(orcamento.alocado()).isEqualByComparingTo("400.00");
    }

    /* ══ Onde a nota não existe, o peso cai para o espaço ══ */

    @Test
    @DisplayName("sem nota em nenhum ativo do nível, o rateio é proporcional ao espaço (déficit)")
    void semNotaRateiaPeloEspaco() {
        OrcamentoAporte orcamento = alocar(
                List.of(candidato("GRANDE", null, new BigDecimal("3000.00"), new BigDecimal("27500.00"), "1000.00"),
                        candidato("PEQUENO", null, new BigDecimal("3000.00"), new BigDecimal("17500.00"), "1000.00")),
                classeUnica("100.00", new BigDecimal("6000.00"), new BigDecimal("45000.00")));

        assertThat(orcamento.alocado()).isEqualByComparingTo(APORTE);
        // Espaço 24.500 x 14.500 → proporção 62,82% / 37,18% (em cotas de R$ 1.000).
        assertThat(orcamento.deCandidato(0)).isGreaterThan(orcamento.deCandidato(1));
    }

    /* ── Apoio ── */

    /** Aloca como o `AporteService` faz: elegível primeiro, depois a maior NOTA. */
    private OrcamentoAporte alocar(List<AporteCandidato> candidatos, ComparativoResponseDTO comparativo) {
        List<AporteCandidato> ordenados = new ArrayList<>(candidatos);
        ordenados.sort(Comparator
                .comparing(AporteCandidato::elegivel, Comparator.reverseOrder())
                .thenComparing(AporteCandidato::nota, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(AporteCandidato::nome, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));
        return alocacao.ratear(ordenados, comparativo, APORTE);
    }

    /** Uma classe com um ativo; `valorAlvo` é o alvo do patrimônio projetado. */
    private static ComparativoResponseDTO classeUnica(String percentualIdeal, BigDecimal valorAtual,
                                                      BigDecimal valorAlvo) {
        return comparativo(percentualIdeal, valorAtual, valorAlvo, new BigDecimal("37500.00"));
    }

    private static ComparativoResponseDTO comparativo(String percentualIdeal, BigDecimal valorAtual,
                                                      BigDecimal valorAlvo, BigDecimal patrimonio) {
        BigDecimal ideal = new BigDecimal(percentualIdeal);
        BigDecimal deficit = valorAlvo.subtract(valorAtual).max(BigDecimal.ZERO);
        BigDecimal excesso = valorAtual.subtract(valorAlvo).max(BigDecimal.ZERO);
        ComparativoResponseDTO.AtivoComparativoDTO ativo = new ComparativoResponseDTO.AtivoComparativoDTO(
                1L, null, "ATIVO", null, ideal, BigDecimal.ZERO, valorAlvo, valorAtual,
                deficit, excesso, BigDecimal.ZERO, null, true);
        ComparativoResponseDTO.ClasseComparativoDTO classe = new ComparativoResponseDTO.ClasseComparativoDTO(
                CategoriaInvestimento.ACOES, ideal,
                BigDecimal.valueOf(valorAtual.doubleValue() / patrimonio.doubleValue() * 100d),
                valorAlvo, valorAtual, deficit, excesso, BigDecimal.ZERO, null, List.of(), List.of(ativo));
        return new ComparativoResponseDTO(1L, "BRL", patrimonio, ideal, List.of(classe), List.of());
    }

    /**
     * Candidato com a capacidade JÁ resolvida: {@code limiteEmReais − valorAtual}.
     * O nome `valorAlvo` do parâmetro é histórico — o que o motor usa é o TETO.
     */
    private static AporteCandidato candidato(String nome, Integer nota, BigDecimal valorAtual,
                                             BigDecimal limiteEmReais, String preco) {
        double capacidade = Math.max(0d, limiteEmReais.doubleValue() - valorAtual.doubleValue());
        return new AporteCandidato(
                null, 1L, nome, true, null, null, CategoriaInvestimento.ACOES,
                (nota != null) ? BigDecimal.valueOf(nota) : null,
                nota != null, 5, 5, List.of(),
                true, StatusElegibilidade.ELEGIVEL, List.of(),
                null, false, BigDecimal.ZERO, BigDecimal.ZERO, limiteEmReais,
                ReferenciaAporte.moeda(capacidade),
                BigDecimal.ZERO, BigDecimal.ZERO,
                valorAtual, limiteEmReais,
                ReferenciaAporte.moeda(capacidade), BigDecimal.ZERO, BigDecimal.ZERO,
                (preco != null) ? new BigDecimal(preco) : null);
    }
}
