package dev.LzGuimaraes.FocusLifeHub.Investimento.ComprasVendas.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Regra de preço teto/mínimo já resolvida para a UI, com o preço atual do ativo
 * e as FLAGS de sinalização (acima do teto / abaixo do mínimo) — o front não
 * recalcula a comparação.
 */
public record AtivoPrecoAlvoResponseDTO(
    Long id,
    UUID ativo_cadastro_id,
    Long ativo_id,
    String ticker,
    BigDecimal preco_teto,
    BigDecimal preco_minimo,
    BigDecimal preco_atual,
    boolean acima_do_teto,
    boolean abaixo_do_minimo,
    LocalDateTime updated_at
) {
}
