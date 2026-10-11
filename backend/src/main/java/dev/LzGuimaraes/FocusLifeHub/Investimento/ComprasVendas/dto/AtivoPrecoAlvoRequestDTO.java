package dev.LzGuimaraes.FocusLifeHub.Investimento.ComprasVendas.dto;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Payload da regra de preço teto/mínimo. A regra é por CATÁLOGO
 * ({@code ativo_cadastro_id}) OU por POSIÇÃO ({@code ativo_id}) — informar os
 * dois (ou nenhum) é rejeitado com 400.
 */
public record AtivoPrecoAlvoRequestDTO(
    UUID ativo_cadastro_id,
    Long ativo_id,
    String ticker,
    BigDecimal preco_teto,
    BigDecimal preco_minimo
) {
}
