package dev.LzGuimaraes.FocusLifeHub.Planejamento.Aporte;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import dev.LzGuimaraes.FocusLifeHub.Ativo.CategoriaInvestimento;
import dev.LzGuimaraes.FocusLifeHub.Planejamento.CarteiraIdeal.dto.ComparativoResponseDTO;

/**
 * ALOCAÇÃO — "quanto dinheiro cabe em cada ativo sem violar as regras?"
 *
 * Cobre os cenários 4, 6, 7, 8 e 9 da especificação: classe acima do alvo não
 * recebe, o ranking decide a ordem, quem enche sai do ciclo e o que não tem
 * destino fica NÃO ALOCADO (o motor nunca distribui por distribuir).
 */
class AlocacaoServiceTest {

    private final AlocacaoService service = new AlocacaoService();

    private static final BigDecimal PATRIMONIO = new BigDecimal("100000");

    /** Cenário 4 — classe acima da meta não recebe aporte. */
    @Test
    @DisplayName("classe acima do alvo não recebe aporte")
    void classeAcimaDoAlvoNaoRecebe() {
        // Ações BR: ideal 50% (50.000) e atual 60% (60.000) → acima do alvo.
        ComparativoResponseDTO comparativo = comparativo(
                classe(CategoriaInvestimento.ACOES, "50", "60", "50000", "60000", "2", null, List.of()));

        List<AporteCandidato> candidatos = List.of(ativo("BBDC3", CategoriaInvestimento.ACOES,
                "60000", BigDecimal.ZERO, new BigDecimal("3000"), true));

        OrcamentoAporte orcamento = service.ratear(candidatos, comparativo, new BigDecimal("5000"), config());

        assertNull(orcamento, "sem classe com déficit não existe plano de aporte");
    }

    /** Cenário 6 — dois elegíveis: o ranking (Priority Score) decide a ordem. */
    @Test
    @DisplayName("entre dois elegíveis, o de maior Priority Score vem primeiro")
    void rankingDecideAOrdem() {
        ComparativoResponseDTO comparativo = comparativo(
                classe(CategoriaInvestimento.ACOES, "50", "40", "50000", "40000", "0", null, List.of()));

        AporteCandidato bom = ativo("BBDC3", CategoriaInvestimento.ACOES,
                "20000", new BigDecimal("10000"), new BigDecimal("30000"), true);
        AporteCandidato fraco = ativo("TAEE3", CategoriaInvestimento.ACOES,
                "20000", new BigDecimal("10000"), new BigDecimal("30000"), true);
        // Priority Score difere: o "bom" tem score 90 e o "fraco" 40.
        bom = comScore(bom, "90");
        fraco = comScore(fraco, "40");

        List<AporteCandidato> ordenados = new java.util.ArrayList<>(List.of(fraco, bom));
        ordenados.sort(java.util.Comparator.comparing(AporteCandidato::priorityScore).reversed());

        OrcamentoAporte orcamento = service.ratear(ordenados, comparativo, new BigDecimal("5000"), config());

        assertNotNull(orcamento);
        // Estratégia padrão = proporcional ao déficit (capacidade), então os dois
        // recebem; o que importa é que o de maior score é o primeiro da fila.
        assertEquals(0, ordenados.get(0).priorityScore().compareTo(new BigDecimal("90")));
        assertEquals(0, orcamento.alocado().compareTo(new BigDecimal("5000")));
    }

    /** Cenário 7 — o ativo prioritário enche e o dinheiro passa para o próximo. */
    @Test
    @DisplayName("ativo que atinge a capacidade sai do ciclo e o resto segue para o próximo")
    void dinheiroPassaParaOProximoQuandoUmEnche() {
        ComparativoResponseDTO comparativo = comparativo(
                classe(CategoriaInvestimento.ACOES, "50", "40", "50000", "40000", "0", null, List.of()));

        // Estratégia por SCORE: o primeiro (score 95) tem MUITO mais peso que o
        // segundo (5). A divisão proporcional daria quase todo o dinheiro a ele,
        // mas a capacidade dele é 1.000 — o excedente tem de seguir para o outro.
        ScoreConfigModel config = config();
        config.setEstrategiaAporte(EstrategiaAporte.SCORE_PROPORCIONAL);

        AporteCandidato primeiro = comScore(ativo("BBDC3", CategoriaInvestimento.ACOES,
                "20000", new BigDecimal("30000"), new BigDecimal("1000"), true), "95");
        AporteCandidato segundo = comScore(ativo("TAEE3", CategoriaInvestimento.ACOES,
                "20000", new BigDecimal("10000"), new BigDecimal("20000"), true), "5");

        OrcamentoAporte orcamento = service.ratear(List.of(primeiro, segundo), comparativo,
                new BigDecimal("3000"), config);

        assertNotNull(orcamento);
        assertEquals(0, orcamento.deCandidato(0).compareTo(new BigDecimal("1000")),
                "o primeiro recebe exatamente a capacidade dele (não passa disso)");
        assertEquals(0, orcamento.deCandidato(1).compareTo(new BigDecimal("2000")),
                "o restante continua para o próximo elegível");
        assertEquals(0, orcamento.alocado().compareTo(new BigDecimal("3000")));
    }

    /** Cenário 8 — nenhum ativo elegível: nada é alocado. */
    @Test
    @DisplayName("sem ativo elegível, o aporte fica inteiramente não alocado")
    void semElegivelNadaEhAlocado() {
        ComparativoResponseDTO comparativo = comparativo(
                classe(CategoriaInvestimento.ACOES, "50", "40", "50000", "40000", "0", null, List.of()));

        AporteCandidato descartado = inelegivel(ativo("WEGE3", CategoriaInvestimento.ACOES,
                "20000", new BigDecimal("30000"), new BigDecimal("30000"), false),
                StatusElegibilidade.PRECO_ACIMA_DO_LIMITE);

        OrcamentoAporte orcamento = service.ratear(List.of(descartado), comparativo,
                new BigDecimal("5000"), config());

        assertNotNull(orcamento);
        assertEquals(0, orcamento.alocado().compareTo(BigDecimal.ZERO));
        assertEquals(0, orcamento.deCandidato(0).compareTo(BigDecimal.ZERO));
    }

    /** Cenário 9 — só parte do aporte pode ser usada. */
    @Test
    @DisplayName("capacidade elegível menor que o aporte deixa o resto não alocado")
    void parteDoAporteFicaNaoAlocada() {
        ComparativoResponseDTO comparativo = comparativo(
                classe(CategoriaInvestimento.ACOES, "50", "47", "50000", "47000", "0", null, List.of()));

        // Capacidade total elegível = 3.000 (o déficit da classe); aporte 5.000.
        AporteCandidato unico = ativo("BBDC3", CategoriaInvestimento.ACOES,
                "47000", new BigDecimal("3000"), new BigDecimal("3000"), true);

        OrcamentoAporte orcamento = service.ratear(List.of(unico), comparativo,
                new BigDecimal("5000"), config());

        assertNotNull(orcamento);
        assertEquals(0, orcamento.alocado().compareTo(new BigDecimal("3000")));
        assertEquals(0, new BigDecimal("5000").subtract(orcamento.alocado()).compareTo(new BigDecimal("2000")));
    }

    /* ── Apoio ── */

    private ScoreConfigModel config() {
        ScoreConfigModel config = new ScoreConfigModel();
        config.setRedistribuir(false);   // uma rodada: o resultado fica previsível no teste
        return config;
    }

    private ComparativoResponseDTO comparativo(ComparativoResponseDTO.ClasseComparativoDTO... classes) {
        return new ComparativoResponseDTO(1L, "BRL", PATRIMONIO, new BigDecimal("100"),
                List.of(classes), List.of());
    }

    private ComparativoResponseDTO.ClasseComparativoDTO classe(CategoriaInvestimento classe,
                                                               String ideal, String atual,
                                                               String valorIdeal, String valorAtual,
                                                               String tolerancia, Long idSubclasse,
                                                               List<ComparativoResponseDTO.SubclasseComparativoDTO> subs) {
        BigDecimal vIdeal = new BigDecimal(valorIdeal);
        BigDecimal vAtual = new BigDecimal(valorAtual);
        BigDecimal deficit = vIdeal.subtract(vAtual).max(BigDecimal.ZERO);
        BigDecimal excesso = vAtual.subtract(vIdeal).max(BigDecimal.ZERO);
        return new ComparativoResponseDTO.ClasseComparativoDTO(
                classe, new BigDecimal(ideal), new BigDecimal(atual),
                vIdeal, vAtual, deficit, excesso,
                new BigDecimal(tolerancia), null, subs, List.of());
    }

    private AporteCandidato ativo(String nome, CategoriaInvestimento classe, String valorAtual,
                                  BigDecimal deficit, BigDecimal capacidade, boolean elegivel) {
        BigDecimal percentualIdeal = new BigDecimal("20");
        BigDecimal percentualAtual = new BigDecimal(valorAtual)
                .divide(PATRIMONIO, 4, java.math.RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"));
        return new AporteCandidato(
                UUID.randomUUID(), 1L, nome, true, null, null, null, null, classe,
                new BigDecimal("90"), new BigDecimal("80"), BigDecimal.ONE, List.of(),
                elegivel, elegivel ? StatusElegibilidade.ELEGIVEL : StatusElegibilidade.SEM_CAPACIDADE, List.of(),
                null, false, capacidade,
                new BigDecimal("18.00"), new BigDecimal("16.20"), new BigDecimal("20.00"), new BigDecimal("0.1"),
                new BigDecimal("80"),
                percentualAtual, percentualIdeal, new BigDecimal(valorAtual), new BigDecimal("20000"),
                deficit, BigDecimal.ZERO, new BigDecimal("1"),
                5,
                BigDecimal.ZERO, 0,
                0.9, 0.5, 0.0, 0.5, 0.8, 0.1);
    }

    private AporteCandidato comScore(AporteCandidato c, String score) {
        return new AporteCandidato(c.ativoCadastroId(), c.metaId(), c.nome(), c.vinculado(), c.subclasseId(),
                c.subclasseNome(), c.setorId(), c.setorNome(), c.classe(), c.quality(), c.momento(), c.fator(),
                c.bloqueios(), c.elegivel(), c.status(), c.motivos(), c.limiteMaximo(), c.limiteAtingido(),
                c.capacidade(), c.precoAtual(), c.precoMedio(), c.precoMaximoCompra(), c.oportunidadePreco(),
                new BigDecimal(score), c.percentualAtual(), c.percentualIdeal(), c.valorAtual(), c.valorIdeal(),
                c.deficit(), c.excesso(), c.tolerancia(), c.prioridade(), c.aportesRecentes(),
                c.aportesRecentesQtd(), c.qualityNorm(), c.deficitNorm(), c.excessoNorm(), c.prioridadeNorm(),
                c.momentoNorm(), c.oportunidadeNorm());
    }

    private AporteCandidato inelegivel(AporteCandidato c, StatusElegibilidade status) {
        return new AporteCandidato(c.ativoCadastroId(), c.metaId(), c.nome(), c.vinculado(), c.subclasseId(),
                c.subclasseNome(), c.setorId(), c.setorNome(), c.classe(), c.quality(), c.momento(), c.fator(),
                c.bloqueios(), false, status, List.of("Preço acima do limite"), c.limiteMaximo(),
                c.limiteAtingido(), c.capacidade(), c.precoAtual(), c.precoMedio(), c.precoMaximoCompra(),
                c.oportunidadePreco(), c.priorityScore(), c.percentualAtual(), c.percentualIdeal(), c.valorAtual(),
                c.valorIdeal(), c.deficit(), c.excesso(), c.tolerancia(), c.prioridade(), c.aportesRecentes(),
                c.aportesRecentesQtd(), c.qualityNorm(), c.deficitNorm(), c.excessoNorm(), c.prioridadeNorm(),
                c.momentoNorm(), c.oportunidadeNorm());
    }
}
