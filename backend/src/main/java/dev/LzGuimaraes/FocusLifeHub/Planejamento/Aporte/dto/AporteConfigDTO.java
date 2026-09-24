package dev.LzGuimaraes.FocusLifeHub.Planejamento.Aporte.dto;

import java.math.BigDecimal;

/**
 * Configuração do motor de aporte (uma por usuário).
 *
 * A MARGEM OPERACIONAL é relativa à meta: com margem de 5% e meta de 5%, o
 * limite do ativo é 5,25% — e não 10%. Ela cria uma zona operacional em volta
 * da meta para o motor não tratar diferença de centavos como excesso.
 */
public record AporteConfigDTO(
        BigDecimal margem_percentual,
        /** Faixa aceita pela API (3% a 5%). */
        BigDecimal margem_minima,
        BigDecimal margem_maxima,
        /** Valor usado quando o usuário nunca salvou. */
        BigDecimal margem_padrao,
        /** false = o usuário ainda não personalizou (a API devolve o padrão). */
        boolean personalizada
) {}
