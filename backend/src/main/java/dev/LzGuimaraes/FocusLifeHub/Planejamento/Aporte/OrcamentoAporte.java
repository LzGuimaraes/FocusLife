package dev.LzGuimaraes.FocusLifeHub.Planejamento.Aporte;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Resultado do rateio.
 *
 * `porClasse` e `porSubclasse` alimentam o painel "onde entra o dinheiro";
 * `porCandidato` é indexado pela MESMA ordem da lista de candidatos recebida.
 *
 * Os valores já vêm arredondados para UNIDADES INTEIRAS quando o ativo é
 * comprado em cotas (ação, FII, ETF): ninguém compra 72,7 cotas. A sobra do
 * arredondamento é redistribuída priorizando os déficits e as notas, respeitando
 * os limites de todos os níveis. `troco` registra todo o saldo não alocado.
 */
public record OrcamentoAporte(
        Map<dev.LzGuimaraes.FocusLifeHub.Ativo.CategoriaInvestimento, BigDecimal> porClasse,
        Map<Long, BigDecimal> porSubclasse,
        List<BigDecimal> porCandidato,
        /** Unidades por candidato (null = não deu para contar). */
        List<BigDecimal> quantidades,
        BigDecimal alocado,
        /** Todo o saldo não alocado, por limites ou por unidades indivisíveis. */
        BigDecimal troco
) {

    /** Quanto o candidato da posição `i` recebeu (sempre um valor, nunca null). */
    public BigDecimal deCandidato(int i) {
        return (i >= 0 && i < porCandidato.size()) ? porCandidato.get(i) : BigDecimal.ZERO;
    }

    /** Unidades do candidato da posição `i` (null quando não há preço). */
    public BigDecimal quantidadeDe(int i) {
        return (i >= 0 && i < quantidades.size()) ? quantidades.get(i) : null;
    }
}
