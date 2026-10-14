package dev.LzGuimaraes.FocusLifeHub.Planejamento.Aporte;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Meta de uso do dinheiro novo; independente da margem sobre metas da carteira. */
public final class MetaUtilizacaoAporte {
    public static final BigDecimal SOBRA_MAXIMA_PERCENTUAL = new BigDecimal("5.00");

    private MetaUtilizacaoAporte() {}

    public static BigDecimal sobraMaxima(BigDecimal aporte) {
        return aporte.multiply(SOBRA_MAXIMA_PERCENTUAL).movePointLeft(2).setScale(2, RoundingMode.DOWN);
    }

    public static BigDecimal minimoAlocar(BigDecimal aporte) {
        return aporte.subtract(sobraMaxima(aporte));
    }

    public static boolean atingida(BigDecimal aporte, OrcamentoAporte plano) {
        return plano != null && plano.alocado().compareTo(minimoAlocar(aporte)) >= 0;
    }
}
