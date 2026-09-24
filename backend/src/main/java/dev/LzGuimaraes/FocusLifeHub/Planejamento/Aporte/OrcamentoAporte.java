package dev.LzGuimaraes.FocusLifeHub.Planejamento.Aporte;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import dev.LzGuimaraes.FocusLifeHub.Ativo.CategoriaInvestimento;

/**
 * Resultado do rateio: quanto cada NÍVEL recebeu e quanto cada candidato
 * recebeu (a lista é alinhada com a ordem dos candidatos enviada ao rateio).
 *
 * `alocado` é a soma efetivamente distribuída — o que não coube fica de fora,
 * para a tela explicar em vez de esconder (§14 do spec).
 */
public record OrcamentoAporte(
        Map<CategoriaInvestimento, BigDecimal> porClasse,
        Map<Long, BigDecimal> porSubclasse,
        Map<Long, BigDecimal> porSetor,
        List<BigDecimal> porCandidato,
        BigDecimal alocado
) {
    public BigDecimal deCandidato(int indice) {
        return porCandidato.get(indice);
    }
}
