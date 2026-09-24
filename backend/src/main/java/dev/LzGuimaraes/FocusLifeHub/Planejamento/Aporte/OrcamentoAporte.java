package dev.LzGuimaraes.FocusLifeHub.Planejamento.Aporte;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Resultado do rateio.
 *
 * `porClasse` e `porSubclasse` alimentam o painel "onde entra o dinheiro";
 * `porCandidato` é indexado pela MESMA ordem da lista de candidatos recebida.
 */
public record OrcamentoAporte(
        Map<dev.LzGuimaraes.FocusLifeHub.Ativo.CategoriaInvestimento, BigDecimal> porClasse,
        Map<Long, BigDecimal> porSubclasse,
        List<BigDecimal> porCandidato,
        BigDecimal alocado
) {

    /** Quanto o candidato da posição `i` recebeu (sempre um valor, nunca null). */
    public BigDecimal deCandidato(int i) {
        return (i >= 0 && i < porCandidato.size()) ? porCandidato.get(i) : BigDecimal.ZERO;
    }
}
