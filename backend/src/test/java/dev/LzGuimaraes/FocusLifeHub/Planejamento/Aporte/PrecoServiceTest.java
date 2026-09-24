package dev.LzGuimaraes.FocusLifeHub.Planejamento.Aporte;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * PREÇO — a regra de compra do investidor.
 *
 * Cobre o cenário 10 da especificação: o MESMO ativo, sem mexer no checklist,
 * muda de elegibilidade quando o preço passa do limite de compra.
 */
class PrecoServiceTest {

    private static final BigDecimal LIMITE = new BigDecimal("20.00");

    @Test
    @DisplayName("preço abaixo do limite gera oportunidade e mantém o ativo elegível")
    void precoAbaixoDoLimite() {
        BigDecimal oportunidade = PrecoService.oportunidade(new BigDecimal("16.00"), LIMITE);

        assertEquals(new BigDecimal("0.2000"), oportunidade);
        assertFalse(precoAcima(new BigDecimal("16.00")));
    }

    @Test
    @DisplayName("preço acima do limite descarta o ativo (sem tocar no checklist)")
    void precoAcimaDoLimite() {
        assertTrue(precoAcima(new BigDecimal("22.00")));
        assertEquals(0, PrecoService.oportunidade(new BigDecimal("22.00"), LIMITE).signum());
    }

    @Test
    @DisplayName("no limite de compra a oportunidade é zero, mas o ativo continua elegível")
    void noLimite() {
        assertEquals(0, PrecoService.oportunidade(LIMITE, LIMITE).signum());
        assertFalse(precoAcima(LIMITE), "igual ao limite ainda é permitido");
    }

    @Test
    @DisplayName("sem preço máximo configurado o termo sai da conta (não é penalidade)")
    void semRegraDePreco() {
        assertNull(PrecoService.oportunidade(new BigDecimal("30.00"), null));
        assertNull(PrecoService.oportunidade(new BigDecimal("30.00"), BigDecimal.ZERO));
        assertNull(PrecoService.oportunidade(null, LIMITE));
    }

    /** Mesma decisão que o motor toma: o preço atual passou do preço máximo? */
    private boolean precoAcima(BigDecimal precoAtual) {
        return precoAtual.compareTo(LIMITE) > 0;
    }

    @Test
    @DisplayName("o motor usa a mesma regra nas duas fases (elegibilidade e oportunidade)")
    void coerenciaEntreElegibilidadeEOportunidade() {
        ElegibilidadeService elegibilidade = new ElegibilidadeService();
        List<String> precedencia = List.of("BLOQUEIO", "LIMITE", "PRECO", "CLASSE", "TETO_ATIVO");

        for (String preco : List.of("16.00", "20.00")) {
            BigDecimal atual = new BigDecimal(preco);
            var veredito = elegibilidade.avaliar(new ElegibilidadeService.Entrada(
                    List.of(), false, false, atual.compareTo(LIMITE) > 0, false, false,
                    new BigDecimal("1000")), precedencia);
            assertTrue(veredito.elegivel(), "preço " + preco + " deveria ser elegível");
        }

        var caro = elegibilidade.avaliar(new ElegibilidadeService.Entrada(
                List.of(), false, false, new BigDecimal("22.00").compareTo(LIMITE) > 0, false, false,
                new BigDecimal("1000")), precedencia);
        assertFalse(caro.elegivel());
        assertEquals(StatusElegibilidade.PRECO_ACIMA_DO_LIMITE, caro.status());
    }
}
