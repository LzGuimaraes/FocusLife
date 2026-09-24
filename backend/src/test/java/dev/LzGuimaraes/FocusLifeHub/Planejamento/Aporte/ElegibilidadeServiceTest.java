package dev.LzGuimaraes.FocusLifeHub.Planejamento.Aporte;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * ELEGIBILIDADE — "esse ativo PODE receber dinheiro agora?"
 *
 * Cobre os cenários 1, 2, 3, 4 e 5 da especificação:
 *   1. ativo bom e barato  → elegível
 *   2. ativo excelente mas caro → INELEGÍVEL (score não compensa)
 *   3. ativo com critério eliminatório reprovado → INELEGÍVEL
 *   4. classe sem déficit → INELEGÍVEL
 *   5. sem capacidade (já no alvo/limite) → INELEGÍVEL
 */
class ElegibilidadeServiceTest {

    private final ElegibilidadeService service = new ElegibilidadeService();

    private static final List<String> PRECEDENCIA_PADRAO =
            List.of("BLOQUEIO", "LIMITE", "PRECO", "CLASSE", "SUBCLASSE", "SETOR", "TETO_ATIVO", "MOMENTO", "SCORE");

    private static final BigDecimal CAPACIDADE = new BigDecimal("1500.00");

    /** Cenário 1 — Quality alto, preço dentro do limite, déficit existente. */
    @Test
    @DisplayName("ativo bom e barato é ELEGÍVEL")
    void ativoBomEBarato() {
        var veredito = service.avaliar(
                new ElegibilidadeService.Entrada(List.of(), false, false, false, false, false, CAPACIDADE),
                PRECEDENCIA_PADRAO);

        assertTrue(veredito.elegivel());
        assertEquals(StatusElegibilidade.ELEGIVEL, veredito.status());
        assertTrue(veredito.motivos().isEmpty());
        assertEquals(CAPACIDADE, veredito.capacidade());
    }

    /** Cenário 2 — Quality 98, mas o preço passou do limite de compra. */
    @Test
    @DisplayName("ativo excelente e caro é INELEGÍVEL pelo preço")
    void ativoExcelenteECaro() {
        var veredito = service.avaliar(
                new ElegibilidadeService.Entrada(List.of(), false, false, true, false, false, CAPACIDADE),
                PRECEDENCIA_PADRAO);

        assertFalse(veredito.elegivel());
        assertEquals(StatusElegibilidade.PRECO_ACIMA_DO_LIMITE, veredito.status());
        assertTrue(veredito.motivos().contains("PRECO"), veredito.motivos().toString());
        // A capacidade continua sendo um FATO do ativo (ela é exibida na tela);
        // quem garante aporte R$ 0 é a alocação, que só distribui entre elegíveis.
        assertEquals(CAPACIDADE, veredito.capacidade());
        assertTrue(veredito.explicacoes().get(0).contains("Preço acima do limite"));
    }

    /** Cenário 3 — checklist com critério eliminatório reprovado. */
    @Test
    @DisplayName("critério eliminatório reprovado torna o ativo INELEGÍVEL")
    void criterioEliminatorio() {
        var veredito = service.avaliar(
                new ElegibilidadeService.Entrada(List.of("ROE abaixo do mínimo"), false, false, false, false, false,
                        CAPACIDADE),
                PRECEDENCIA_PADRAO);

        assertFalse(veredito.elegivel());
        assertEquals(StatusElegibilidade.CRITERIO_ELIMINATORIO, veredito.status());
        assertTrue(veredito.motivos().contains("BLOQUEIO"));
    }

    /** Cenário 4 — a classe está acima do alvo: não recebe aporte novo. */
    @Test
    @DisplayName("classe sem déficit descarta os ativos dela")
    void classeSemCapacidade() {
        var veredito = service.avaliar(
                new ElegibilidadeService.Entrada(List.of(), false, false, false, false, true, CAPACIDADE),
                PRECEDENCIA_PADRAO);

        assertFalse(veredito.elegivel());
        assertEquals(StatusElegibilidade.CLASSE_SEM_CAPACIDADE, veredito.status());
        assertTrue(veredito.motivos().contains("NIVEL"));
    }

    /** Cenário 5 — o ativo já está no alvo / atingiu o limite máximo. */
    @Test
    @DisplayName("ativo sem capacidade é INELEGÍVEL")
    void semCapacidade() {
        var semEspaco = service.avaliar(
                new ElegibilidadeService.Entrada(List.of(), false, false, false, false, false, BigDecimal.ZERO),
                PRECEDENCIA_PADRAO);
        assertFalse(semEspaco.elegivel());
        assertEquals(StatusElegibilidade.SEM_CAPACIDADE, semEspaco.status());

        var noLimite = service.avaliar(
                new ElegibilidadeService.Entrada(List.of(), false, true, false, false, false, CAPACIDADE),
                PRECEDENCIA_PADRAO);
        assertFalse(noLimite.elegivel());
        assertEquals(StatusElegibilidade.LIMITE_ATINGIDO, noLimite.status());
    }

    /** O status é a PRIMEIRA trava violada na ordem configurada (explicação previsível). */
    @Test
    @DisplayName("status segue a ordem configurada das travas")
    void statusSeguePrecedencia() {
        var entrada = new ElegibilidadeService.Entrada(List.of(), false, false, true, false, false,
                BigDecimal.ZERO);

        var precoPrimeiro = service.avaliar(entrada, PRECEDENCIA_PADRAO);
        assertEquals(StatusElegibilidade.PRECO_ACIMA_DO_LIMITE, precoPrimeiro.status());

        var capacidadePrimeiro = service.avaliar(entrada,
                List.of("TETO_ATIVO", "PRECO", "BLOQUEIO", "LIMITE"));
        assertEquals(StatusElegibilidade.SEM_CAPACIDADE, capacidadePrimeiro.status());
        // A ordem muda a EXPLICAÇÃO — nunca deixa de descartar.
        assertFalse(capacidadePrimeiro.elegivel());
    }

    /** Sem avaliação não é descarte: só sai o termo de qualidade do score. */
    @Test
    @DisplayName("sem avaliação continua elegível")
    void semAvaliacaoNaoDescarta() {
        var veredito = service.avaliar(
                new ElegibilidadeService.Entrada(List.of(), true, false, false, false, false, CAPACIDADE),
                PRECEDENCIA_PADRAO);

        assertTrue(veredito.elegivel());
        assertEquals(StatusElegibilidade.SEM_AVALIACAO, veredito.status());
    }
}
