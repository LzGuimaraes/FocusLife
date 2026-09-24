package dev.LzGuimaraes.FocusLifeHub.Planejamento.Aporte;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import dev.LzGuimaraes.FocusLifeHub.Ativo.CategoriaInvestimento;
import dev.LzGuimaraes.FocusLifeHub.Planejamento.CarteiraIdeal.dto.ComparativoResponseDTO;

/**
 * OS 14 CASOS OBRIGATÓRIOS DO MOTOR REFATORADO.
 *
 * O motor tem de responder três perguntas SEPARADAS, e o teste existe para
 * impedir que elas voltem a se misturar:
 *
 *   PERGUNTA 1  posso investir neste ativo?  → ELEGIBILIDADE
 *   PERGUNTA 2  quanto posso investir nele?  → CAPACIDADE (até o LIMITE)
 *   PERGUNTA 3  quem deve receber mais?      → NOTA (peso), nunca permissão
 *
 * O bug que estes testes eliminam: tratar "está no alvo" (ou "a classe não tem
 * déficit") como motivo para o ativo NÃO participar do aporte. A meta orienta e
 * a CAPACIDADE limita — ela sai do LIMITE OPERACIONAL
 * ({@code meta × (1 + margem)}), não do déficit, e é por isso que um ativo
 * exatamente na meta continua candidato depois de um aporte grande.
 *
 * O cenário roda o motor de ponta a ponta (menos o banco): os candidatos são
 * montados com a MESMA matemática do `AporteService` (`ReferenciaAporte.limite`)
 * e o rateio é o `AlocacaoService` de verdade.
 */
class MotorAportesRefatoradoTest {

    /** Cenário crítico do enunciado (§17): patrimônio pequeno, aporte grande. */
    private static final double T = 20000d;
    private static final double APORTE = 15000d;
    private static final double R = 35000d;
    private static final double MARGEM = 5d;

    private final AlocacaoService alocacao = new AlocacaoService();
    private final ElegibilidadeService elegibilidade = new ElegibilidadeService();

    /* ══════════════════════════════════════════════════════════════════
       TESTE 1 — ativo NA META antes do aporte continua elegível
       ══════════════════════════════════════════════════════════════════ */

    @Test
    @DisplayName("1. ativo na meta ANTES do aporte continua elegível (a meta cresce com R)")
    void ativoNaMetaAntesDoAporteContinuaElegivel() {
        // 1.000 / 20.000 = 5% hoje: exatamente na meta. O motor antigo descartava.
        Execucao e = rodar(List.of(ativo("NA_META", CategoriaInvestimento.ACOES, 5d, 1000d).comNota(80)));

        AporteCandidato c = e.candidato("NA_META");
        // Déficit (espaço até a meta) e capacidade (espaço até o limite) são
        // coisas diferentes, e o motor usa os dois para o que cada um serve.
        assertThat(c.deficit()).isEqualByComparingTo("750.00");        // 1.750,00 − 1.000,00
        assertThat(c.limiteEmReais()).isEqualByComparingTo("1837.50"); // 5% × 1,05 × 35.000
        assertThat(c.capacidade()).isEqualByComparingTo("837.50");     // 1.837,50 − 1.000,00
        assertThat(c.limiteOperacionalPercentual()).isEqualByComparingTo("5.2500");
        assertThat(c.elegivel()).isTrue();
        assertThat(c.status()).isEqualTo(StatusElegibilidade.ELEGIVEL);
        assertThat(e.recebido("NA_META")).isGreaterThan(BigDecimal.ZERO);

        imprimir("TESTE 1 — ativo na meta", e, T, APORTE);
    }

    /* ══════════════════════════════════════════════════════════════════
       TESTE 2 — ativo ACIMA do limite não recebe
       ══════════════════════════════════════════════════════════════════ */

    @Test
    @DisplayName("2. ativo acima do limite: LIMITE_ATINGIDO e capacidade 0 (a nota não o reabilita)")
    void ativoAcimaDoLimiteFicaDeFora() {
        // 4.000 / 35.000 = 11,43% — muito acima do limite de 5,25%.
        Execucao e = rodar(List.of(ativo("CHEIO", CategoriaInvestimento.ACOES, 5d, 4000d).comNota(100)));

        AporteCandidato c = e.candidato("CHEIO");
        assertThat(c.capacidade()).isEqualByComparingTo("0.00");
        assertThat(c.limiteAtingido()).isTrue();
        assertThat(c.elegivel()).isFalse();
        assertThat(c.status()).isEqualTo(StatusElegibilidade.LIMITE_ATINGIDO);
        assertThat(e.recebido("CHEIO")).isEqualByComparingTo("0.00");
    }

    /* ══════════════════════════════════════════════════════════════════
       TESTE 3 — aporte GRANDE (o caso do enunciado)
       ══════════════════════════════════════════════════════════════════ */

    @Test
    @DisplayName("3. aporte de R$ 15.000 sobre R$ 20.000: quem estava na meta continua candidato")
    void aporteGrandeNaoDescartaQuemEstavaNaMeta() {
        // Cada ativo está EXATAMENTE na própria meta hoje (5% × 20.000 = 1.000).
        Execucao e = rodar(List.of(
                ativo("A", CategoriaInvestimento.ACOES, 5d, 1000d).comNota(100),
                ativo("B", CategoriaInvestimento.ACOES, 5d, 1000d).comNota(80),
                ativo("C", CategoriaInvestimento.FIIS, 5d, 1000d).comNota(60),
                ativo("D", CategoriaInvestimento.FIIS, 5d, 1000d).comNota(40)));

        // Ninguém foi descartado por "estar no alvo": o patrimônio projetado (R)
        // criou espaço em TODOS eles.
        assertThat(e.candidatos()).allMatch(AporteCandidato::elegivel);
        assertThat(e.candidatos()).allMatch(c -> c.capacidade().compareTo(BigDecimal.ZERO) > 0);
        assertThat(e.recebidos()).allMatch(v -> v.compareTo(BigDecimal.ZERO) > 0);
        assertThat(e.alocado()).isGreaterThan(BigDecimal.ZERO);

        imprimir("TESTE 3 — aporte grande", e, T, APORTE);
    }

    /* ══════════════════════════════════════════════════════════════════
       TESTE 4 — nota alta = peso maior
       ══════════════════════════════════════════════════════════════════ */

    @Test
    @DisplayName("4. nota 100 recebe o dobro de nota 50 quando os dois têm capacidade")
    void notaAltaPesaMais() {
        Execucao e = rodar(List.of(
                ativo("N100", CategoriaInvestimento.ACOES, 50d, 0d).comNota(100),
                ativo("N50", CategoriaInvestimento.ACOES, 50d, 0d).comNota(50)),
                T, 3000d, MARGEM);

        // Os dois têm a MESMA capacidade (17.500 − 0), então o peso é só a nota.
        assertThat(e.recebido("N100")).isEqualByComparingTo("2000.00");
        assertThat(e.recebido("N50")).isEqualByComparingTo("1000.00");
    }

    /* ══════════════════════════════════════════════════════════════════
       TESTE 5 — nota alta NÃO fura o teto
       ══════════════════════════════════════════════════════════════════ */

    @Test
    @DisplayName("5. nota 100 com capacidade pequena recebe a capacidade; o resto vai para a nota 50")
    void notaAltaNaoFuraOCapacidade() {
        // R = 21.000. CARO tem meta de 0,4% → limite de 0,42% × 21.000 = R$ 88,20.
        Execucao e = rodar(List.of(
                ativo("CARO", CategoriaInvestimento.ACOES, 0.4d, 0d).comNota(100),
                ativo("LIVRE", CategoriaInvestimento.ACOES, 20d, 0d).comNota(50)),
                T, 1000d, MARGEM);

        assertThat(e.candidato("CARO").capacidade()).isEqualByComparingTo("88.20");
        assertThat(e.recebido("CARO")).isEqualByComparingTo("88.20");
        assertThat(e.recebido("CARO")).isEqualTo(e.candidato("CARO").capacidade());
        assertThat(e.recebido("LIVRE")).isEqualByComparingTo("911.80");
    }

    /* ══════════════════════════════════════════════════════════════════
       TESTE 6 — redistribuição da sobra
       ══════════════════════════════════════════════════════════════════ */

    @Test
    @DisplayName("6. quando o de maior nota bate o limite, a sobra vai para os próximos")
    void sobraDoLimiteVaiParaOsProximos() {
        // R = 25.000. TOPO tem meta de 1% → limite de 1,05% × 25.000 = R$ 262,50.
        Execucao e = rodar(List.of(
                ativo("TOPO", CategoriaInvestimento.ACOES, 1d, 0d).comNota(100),
                ativo("SEGUE", CategoriaInvestimento.ACOES, 30d, 0d).comNota(50)),
                T, 5000d, MARGEM);

        assertThat(e.recebido("TOPO")).isEqualByComparingTo("262.50");
        assertThat(e.recebido("SEGUE")).isEqualByComparingTo("4737.50");
        assertThat(e.alocado()).isEqualByComparingTo("5000.00");
    }

    /* ══════════════════════════════════════════════════════════════════
       TESTE 7 — cotas inteiras: o que não fecha cota volta para o pool
       ══════════════════════════════════════════════════════════════════ */

    @Test
    @DisplayName("7. sugestão de R$ 250 a R$ 120 a cota → 2 cotas (R$ 240) e R$ 10 voltam ao pool")
    void sobraDeCotaVoltaParaOPool() {
        // R = 20.500; a divisão é igual (nota 100 x 100) → R$ 250 para cada.
        Execucao e = rodar(List.of(
                ativo("CARA", CategoriaInvestimento.ACOES, 5d, 0d).comNota(100).comPreco(120d),
                ativo("OUTRA", CategoriaInvestimento.ACOES, 5d, 0d).comNota(100).comPreco(1d)),
                T, 500d, MARGEM);

        assertThat(e.quantidade("CARA")).isEqualByComparingTo("2");        // floor(250 / 120)
        assertThat(e.recebido("CARA")).isEqualByComparingTo("240.00");
        // Os R$ 10 que não fecham uma cota de R$ 120 NÃO viram "não alocado":
        // eles voltam para quem ainda consegue comprar uma unidade.
        assertThat(e.recebido("OUTRA")).isEqualByComparingTo("260.00");
        assertThat(e.alocado()).isEqualByComparingTo("500.00");
        assertThat(e.naoAlocado()).isEqualByComparingTo("0.00");
    }

    /* ══════════════════════════════════════════════════════════════════
       TESTE 8 — ativo fracionável absorve o restante
       ══════════════════════════════════════════════════════════════════ */

    @Test
    @DisplayName("8. renda fixa/cripto/Tesouro absorvem o que não fecha cota inteira")
    void ativoFracionavelAbsorveOTroco() {
        Execucao e = rodar(List.of(
                // Ação: R$ 220,50 de sugestão, cota de R$ 300 → não fecha UMA cota.
                ativo("ACAO", CategoriaInvestimento.ACOES, 1d, 0d).comNota(100).comPreco(300d),
                ativo("CDB", CategoriaInvestimento.RENDA_FIXA, 20d, 0d).comNota(1).comPreco(1d)),
                T, 1000d, MARGEM);

        assertThat(e.quantidade("ACAO")).isEqualByComparingTo("0");
        assertThat(e.recebido("ACAO")).isEqualByComparingTo("0.00");
        assertThat(e.recebido("CDB")).isEqualByComparingTo("1000.00");   // absorveu os R$ 220,50
        assertThat(e.naoAlocado()).isEqualByComparingTo("0.00");
    }

    /* ══════════════════════════════════════════════════════════════════
       TESTE 9 — classe SEM DÉFICIT não elimina os ativos dela
       ══════════════════════════════════════════════════════════════════ */

    @Test
    @DisplayName("9. classe sem déficit NÃO elimina os ativos (cada um é avaliado pelo próprio limite)")
    void classeSemDeficitNaoEliminaOsAtivos() {
        // Cada ativo está EXATAMENTE no próprio alvo (2,5% × 35.000 = 875), então a
        // classe soma 1.750 = 5% de R — DÉFICIT ZERO. Pela regra antiga, nada era
        // distribuído; agora cada ativo ainda tem espaço até o limite operacional.
        Execucao e = rodar(List.of(
                ativo("UM", CategoriaInvestimento.ACOES, 2.5d, 875d).comNota(90),
                ativo("DOIS", CategoriaInvestimento.ACOES, 2.5d, 875d).comNota(80)));

        assertThat(e.classe(CategoriaInvestimento.ACOES).deficit()).isEqualByComparingTo("0.00");
        assertThat(e.candidatos()).allMatch(AporteCandidato::elegivel);
        assertThat(e.candidato("UM").capacidade()).isEqualByComparingTo("43.75");   // 2,625% × 35.000 − 875
        assertThat(e.recebido("UM")).isGreaterThan(BigDecimal.ZERO);
        assertThat(e.recebido("DOIS")).isGreaterThan(BigDecimal.ZERO);
        assertThat(e.alocado()).isGreaterThan(BigDecimal.ZERO);

        imprimir("TESTE 9 — classe sem déficit", e, T, APORTE);
    }

    /* ══════════════════════════════════════════════════════════════════
       TESTE 10 — subclasse SEM DÉFICIT também não elimina
       ══════════════════════════════════════════════════════════════════ */

    @Test
    @DisplayName("10. subclasse sem déficit NÃO elimina os ativos dela")
    void subclasseSemDeficitNaoEliminaOsAtivos() {
        // Classe no alvo E as duas subclasses no alvo. Nenhum nível tem déficit,
        // e mesmo assim os ativos têm espaço até o limite operacional (§16).
        Execucao e = rodar(List.of(
                ativo("S1A", CategoriaInvestimento.ACOES, 10d, 3500d).comNota(90).naSubclasse(1L, "Financeiro"),
                ativo("S2A", CategoriaInvestimento.ACOES, 10d, 3500d).comNota(80).naSubclasse(2L, "Energia")));

        ComparativoResponseDTO.ClasseComparativoDTO classe = e.classe(CategoriaInvestimento.ACOES);
        assertThat(classe.deficit()).isEqualByComparingTo("0.00");
        assertThat(classe.subclasses()).allMatch(s -> s.deficit().signum() == 0);

        assertThat(e.candidatos()).allMatch(AporteCandidato::elegivel);
        assertThat(e.recebido("S1A")).isGreaterThan(BigDecimal.ZERO);
        assertThat(e.recebido("S2A")).isGreaterThan(BigDecimal.ZERO);

        imprimir("TESTE 10 — subclasse sem déficit", e, T, APORTE);
    }

    /* ══════════════════════════════════════════════════════════════════
       TESTE 11 — critério eliminatório bloqueia mesmo com nota 100
       ══════════════════════════════════════════════════════════════════ */

    @Test
    @DisplayName("11. critério eliminatório do checklist bloqueia mesmo com nota 100")
    void criterioEliminatorioBloqueiaMesmoComNota100() {
        Execucao e = rodar(List.of(
                ativo("BLOQUEADO", CategoriaInvestimento.ACOES, 5d, 0d).comNota(100)
                        .bloqueado("Endividamento: critério eliminatório reprovado"),
                ativo("LIVRE", CategoriaInvestimento.ACOES, 5d, 0d).comNota(10)));

        AporteCandidato bloqueado = e.candidato("BLOQUEADO");
        assertThat(bloqueado.elegivel()).isFalse();
        assertThat(bloqueado.status()).isEqualTo(StatusElegibilidade.CRITERIO_ELIMINATORIO);
        assertThat(e.recebido("BLOQUEADO")).isEqualByComparingTo("0.00");
        // O livre recebe tudo o que a própria capacidade permite (5% × 1,05 × 35.000).
        assertThat(e.recebido("LIVRE")).isEqualByComparingTo("1837.50");
        assertThat(e.naoAlocado()).isEqualByComparingTo("13162.50");   // não há mais ninguém
    }

    /* ══════════════════════════════════════════════════════════════════
       TESTES 12, 13 e 14 — INVARIANTES
       ══════════════════════════════════════════════════════════════════ */

    @Test
    @DisplayName("12. alocado ≤ aporte e alocado + não alocado = aporte (tolerância de centavo)")
    void somaSempreFechaComOAporte() {
        for (Execucao e : List.of(cenarioCompleto(T, APORTE), cenarioCompleto(T, 1000d),
                cenarioCompleto(T, 100000d))) {
            assertThat(e.alocado()).isLessThanOrEqualTo(e.aporte());
            assertThat(e.alocado().add(e.naoAlocado())).isEqualByComparingTo(e.aporte());
        }
    }

    @Test
    @DisplayName("13. nenhum ativo recebe mais que a própria capacidade")
    void ninguemRecebeMaisQueACapacidade() {
        Execucao e = cenarioCompleto(T, APORTE);
        for (AporteCandidato c : e.candidatos()) {
            assertThat(e.recebido(c.nome())).isLessThanOrEqualTo(c.capacidade());
        }
    }

    @Test
    @DisplayName("14. depois do aporte, nenhum ativo que recebeu passa do limite operacional")
    void ninguemPassaDoLimiteOperacional() {
        for (Execucao e : List.of(cenarioCompleto(T, APORTE), cenarioCompleto(T, 1000d))) {
            for (AporteCandidato c : e.candidatos()) {
                if (c.limitePercentual() == null || c.limitePercentual().signum() <= 0) {
                    continue;
                }
                if (!c.elegivel()) {
                    // Quem JÁ estava acima do limite recebe zero (sair do excesso exige
                    // VENDER, não aportar) — o que se garante é que o aporte não piora.
                    assertThat(e.recebido(c.nome())).isEqualByComparingTo("0.00");
                    continue;
                }
                double valorFinal = c.valorAtual().doubleValue() + e.recebido(c.nome()).doubleValue();
                double percentualFinal = valorFinal / e.r() * 100d;
                assertThat(percentualFinal)
                        .as("ativo %s depois do aporte", c.nome())
                        .isLessThanOrEqualTo(c.limitePercentual().doubleValue() + 1e-9);
            }
        }
    }

    /* ══════════════════════════════════════════════════════════════════
       DIAGNÓSTICO OBRIGATÓRIO (§30)
       ══════════════════════════════════════════════════════════════════ */

    @Test
    @DisplayName("30. diagnóstico completo: aporte de R$ 1.000 e de R$ 15.000 sobre R$ 20.000")
    void diagnosticoCompleto() {
        Execucao mil = cenarioCompleto(T, 1000d);
        imprimir("DIAGNÓSTICO — aporte de R$ 1.000", mil, T, 1000d);
        assertThat(mil.alocado().add(mil.naoAlocado())).isEqualByComparingTo("1000.00");

        Execucao quinze = cenarioCompleto(T, APORTE);
        imprimir("DIAGNÓSTICO — aporte de R$ 15.000", quinze, T, APORTE);
        assertThat(quinze.alocado().add(quinze.naoAlocado())).isEqualByComparingTo("15000.00");
    }

    /**
     * Carteira do diagnóstico: ações (duas na meta), FIIs (uma acima do limite,
     * uma na meta), renda fixa sem meta (posição herdando o alvo da subclasse) e
     * uma ação sem nota. Cobre classe, subclasse, nota, limite e herança.
     */
    private Execucao cenarioCompleto(double patrimonioAtual, double aporte) {
        return rodar(List.of(
                ativo("BBAS3", CategoriaInvestimento.ACOES, 6d, 1200d).comNota(100).naSubclasse(1L, "Bancos"),
                ativo("TAEE3", CategoriaInvestimento.ACOES, 4d, 800d).comNota(80).naSubclasse(2L, "Energia"),
                ativo("MXRF11", CategoriaInvestimento.FIIS, 8d, 2100d).comNota(70),
                ativo("KNRI11", CategoriaInvestimento.FIIS, 6d, 1200d).comNota(60),
                ativo("PICPAY", CategoriaInvestimento.RENDA_FIXA, 2d, 400d).naSubclasse(3L, "Liquidez"),
                ativo("SEM_NOTA", CategoriaInvestimento.ACOES, 2d, 400d)),
                patrimonioAtual, aporte, MARGEM);
    }

    /* ══════════════════════════════════════════════════════════════════
       HARNESS — roda o motor de ponta a ponta (menos a leitura do banco)
       ══════════════════════════════════════════════════════════════════ */

    private static AtivoDef ativo(String ticker, CategoriaInvestimento classe, double metaPercentual,
                                  double valorAtual) {
        return new AtivoDef(ticker, classe, null, null, metaPercentual, valorAtual, null, null, null, List.of());
    }

    /** Definição do ativo: tudo o que o motor lê do banco, num só lugar. */
    private record AtivoDef(String ticker, CategoriaInvestimento classe, Long subclasseId, String subclasseNome,
                            double metaPercentual, double valorAtual, Integer nota, Double preco,
                            BigDecimal limiteCadastrado, List<String> bloqueios) {

        AtivoDef comNota(int valor) {
            return new AtivoDef(ticker, classe, subclasseId, subclasseNome, metaPercentual, valorAtual,
                    valor, preco, limiteCadastrado, bloqueios);
        }

        AtivoDef comPreco(double valor) {
            return new AtivoDef(ticker, classe, subclasseId, subclasseNome, metaPercentual, valorAtual,
                    nota, valor, limiteCadastrado, bloqueios);
        }

        /** Limite de concentração CADASTRADO (% do patrimônio projetado). */
        AtivoDef comLimite(double percentual) {
            return new AtivoDef(ticker, classe, subclasseId, subclasseNome, metaPercentual, valorAtual,
                    nota, preco, BigDecimal.valueOf(percentual), bloqueios);
        }

        AtivoDef naSubclasse(Long id, String nome) {
            return new AtivoDef(ticker, classe, id, nome, metaPercentual, valorAtual, nota, preco,
                    limiteCadastrado, bloqueios);
        }

        AtivoDef bloqueado(String motivo) {
            List<String> novos = new ArrayList<>(bloqueios);
            novos.add(motivo);
            return new AtivoDef(ticker, classe, subclasseId, subclasseNome, metaPercentual, valorAtual,
                    nota, preco, limiteCadastrado, List.copyOf(novos));
        }
    }

    /** Resultado do cenário: candidatos (na ordem do motor), comparativo e orçamento. */
    private record Execucao(List<AporteCandidato> candidatos, ComparativoResponseDTO comparativo,
                            OrcamentoAporte orcamento, double r, BigDecimal aporte) {

        AporteCandidato candidato(String ticker) {
            return candidatos.stream().filter(c -> ticker.equals(c.nome())).findFirst()
                    .orElseThrow(() -> new AssertionError("ativo inexistente no cenário: " + ticker));
        }

        BigDecimal recebido(String ticker) {
            return orcamento.deCandidato(candidatos.indexOf(candidato(ticker)));
        }

        /** Quantas cotas/unidades o ativo comprou (null quando não há preço). */
        BigDecimal quantidade(String ticker) {
            return orcamento.quantidadeDe(candidatos.indexOf(candidato(ticker)));
        }

        List<BigDecimal> recebidos() {
            return java.util.stream.IntStream.range(0, candidatos.size())
                    .mapToObj(orcamento::deCandidato)
                    .toList();
        }

        BigDecimal alocado() {
            return orcamento.alocado();
        }

        BigDecimal naoAlocado() {
            return aporte.subtract(orcamento.alocado());
        }

        ComparativoResponseDTO.ClasseComparativoDTO classe(CategoriaInvestimento classe) {
            return comparativo.classes().stream().filter(c -> c.classe() == classe).findFirst().orElseThrow();
        }
    }

    /**
     * Monta os candidatos com a MESMA matemática do `AporteService` (Limite →
     * capacidade), roda a mesma elegibilidade e o mesmo rateio. O que fica de
     * fora é só a leitura do banco (posições, metas e notas).
     */
    private Execucao rodar(List<AtivoDef> ativos) {
        return rodar(ativos, T, APORTE, MARGEM);
    }

    private Execucao rodar(List<AtivoDef> ativos, double patrimonioAtual, double aporte, double margem) {
        double r = patrimonioAtual + aporte;
        List<AporteCandidato> candidatos = new ArrayList<>();
        for (AtivoDef a : ativos) {
            ReferenciaAporte.Limite limite = ReferenciaAporte.limite(
                    BigDecimal.valueOf(a.metaPercentual()),
                    a.limiteCadastrado(), BigDecimal.valueOf(a.valorAtual()), BigDecimal.valueOf(r), margem);
            double percentualProjetado = percentual(a.valorAtual(), r);
            boolean limiteAtingido = limite.limitePercentual() != null
                    && limite.limitePercentual().signum() > 0
                    && percentualProjetado >= limite.limitePercentual().doubleValue();
            ElegibilidadeService.Veredito veredito = elegibilidade.avaliar(
                    new ElegibilidadeService.Entrada(a.bloqueios(), a.nota() == null, limiteAtingido,
                            limite.capacidade()));

            BigDecimal excesso = ReferenciaAporte.moeda(
                    Math.max(0d, a.valorAtual() - limite.alvoEmReais().doubleValue()));
            candidatos.add(new AporteCandidato(
                    null, 1L, a.ticker(), true, a.subclasseId(), a.subclasseNome(), a.classe(),
                    (a.nota() != null) ? BigDecimal.valueOf(a.nota()) : null,
                    a.nota() != null, 5, (a.nota() != null) ? 5 : 0, a.bloqueios(),
                    veredito.elegivel(), veredito.status(), veredito.motivos(),
                    a.limiteCadastrado(), limiteAtingido,
                    limite.limiteOperacionalPercentual(), limite.limitePercentual(), limite.limiteEmReais(),
                    limite.capacidade(),
                    BigDecimal.valueOf(percentualProjetado).setScale(4, RoundingMode.HALF_UP),
                    BigDecimal.valueOf(a.metaPercentual()),
                    ReferenciaAporte.moeda(a.valorAtual()), limite.alvoEmReais(),
                    limite.deficitAteMeta(), excesso, BigDecimal.ZERO,
                    (a.preco() != null) ? BigDecimal.valueOf(a.preco()) : null));
        }

        List<AporteCandidato> ordenados = AporteTestes.ordenar(candidatos);
        ComparativoResponseDTO comparativo = comparativo(ativos, r);
        OrcamentoAporte orcamento = alocacao.ratear(ordenados, comparativo, BigDecimal.valueOf(aporte));
        return new Execucao(ordenados, comparativo, orcamento, r, BigDecimal.valueOf(aporte));
    }

    /** O comparativo como o `CarteiraIdealService` monta: alvos sobre R. */
    private static ComparativoResponseDTO comparativo(List<AtivoDef> ativos, double r) {
        List<CategoriaInvestimento> classes = ativos.stream().map(AtivoDef::classe).distinct().toList();
        List<ComparativoResponseDTO.ClasseComparativoDTO> classesDto = new ArrayList<>();
        for (CategoriaInvestimento classe : classes) {
            List<AtivoDef> daClasse = ativos.stream().filter(a -> a.classe() == classe).toList();
            double atualClasse = daClasse.stream().mapToDouble(AtivoDef::valorAtual).sum();
            double idealClasse = daClasse.stream().mapToDouble(a -> a.metaPercentual() / 100d * r).sum();

            List<ComparativoResponseDTO.SubclasseComparativoDTO> subs = new ArrayList<>();
            List<Long> ids = daClasse.stream().map(AtivoDef::subclasseId).distinct()
                    .filter(Objects::nonNull).toList();
            for (Long id : ids) {
                List<AtivoDef> daSub = daClasse.stream().filter(a -> id.equals(a.subclasseId())).toList();
                double atualSub = daSub.stream().mapToDouble(AtivoDef::valorAtual).sum();
                double idealSub = daSub.stream().mapToDouble(a -> a.metaPercentual() / 100d * r).sum();
                // O percentual da subclasse é FATIA DA CLASSE (soma 100% da classe).
                double pctDaClasse = (idealClasse > 0d) ? idealSub / idealClasse * 100d : 0d;
                subs.add(AporteTestes.subclasse(id, daSub.get(0).subclasseNome(), pctDaClasse, atualSub, idealSub));
            }
            classesDto.add(AporteTestes.classe(classe, r, atualClasse, idealClasse, subs));
        }
        return AporteTestes.comparativo(r, classesDto);
    }

    private static double percentual(double valor, double total) {
        return (total > 0d) ? valor / total * 100d : 0d;
    }

    /* ── Diagnóstico (§30) ── */

    private void imprimir(String titulo, Execucao e, double patrimonioAtual, double aporte) {
        double r = e.r();
        System.out.println("\n══════════════════════════════════════════════════════════════════");
        System.out.println("  " + titulo);
        System.out.printf("  T = %.2f   A = %.2f   R = %.2f   margem = %.1f%%%n",
                patrimonioAtual, aporte, r, MARGEM);
        System.out.println("──────────────────────────────────────────────────────────────────");
        System.out.println("  CLASSE        meta      atual       alvo    déficit    cap.elegível   distribuído");
        for (ComparativoResponseDTO.ClasseComparativoDTO c : e.comparativo().classes()) {
            double capElegivel = e.candidatos().stream()
                    .filter(x -> x.classe() == c.classe() && x.elegivel())
                    .mapToDouble(x -> x.capacidade().doubleValue()).sum();
            System.out.printf("  %-12s %6.2f%% %10.2f %10.2f %10.2f %14.2f %14.2f%n",
                    c.classe(), c.percentual_ideal().doubleValue(), c.valor_atual().doubleValue(),
                    c.valor_ideal().doubleValue(), c.deficit().doubleValue(), capElegivel,
                    e.orcamento().porClasse().getOrDefault(c.classe(), BigDecimal.ZERO).doubleValue());
        }
        System.out.println("──────────────────────────────────────────────────────────────────");
        System.out.println("  ATIVO          classe      subclasse   nota  atual   %atual  meta%  limite%"
                + "      limite       capacidade   sugerido      final  status");
        for (AporteCandidato c : e.candidatos()) {
            BigDecimal recebido = e.recebido(c.nome());
            System.out.printf("  %-12s %-11s %-10s %5s %7.2f %7.2f%% %5.2f%% %6.2f%% %11.2f %12.2f %11.2f %10.2f  %s%n",
                    c.nome(), c.classe(),
                    (c.subclasseNome() != null) ? c.subclasseNome() : "—",
                    (c.nota() != null) ? c.nota().stripTrailingZeros().toPlainString() : "—",
                    c.valorAtual().doubleValue(), c.percentualAtual().doubleValue(),
                    c.percentualIdeal().doubleValue(),
                    (c.limitePercentual() != null) ? c.limitePercentual().doubleValue() : 0d,
                    c.limiteEmReais().doubleValue(), c.capacidade().doubleValue(),
                    recebido.doubleValue(), c.valorAtual().add(recebido).doubleValue(), c.status());
        }
        System.out.println("──────────────────────────────────────────────────────────────────");
        System.out.printf("  aporte inicial = %.2f   total alocado = %.2f   não alocado = %.2f%n",
                aporte, e.alocado().doubleValue(), e.naoAlocado().doubleValue());
        System.out.printf("  soma alocado + não alocado = %.2f  (= aporte: %s)%n",
                e.alocado().add(e.naoAlocado()).doubleValue(),
                e.alocado().add(e.naoAlocado()).compareTo(e.aporte()) == 0);
        long descartados = e.candidatos().stream().filter(c -> !c.elegivel()).count();
        if (descartados > 0) {
            System.out.println("  motivo do não alocado: " + e.candidatos().stream()
                    .filter(c -> !c.elegivel())
                    .map(c -> c.nome() + " [" + c.status() + "]")
                    .reduce((a, b) -> a + ", " + b).orElse(""));
        }
        System.out.println("══════════════════════════════════════════════════════════════════\n");
    }
}
