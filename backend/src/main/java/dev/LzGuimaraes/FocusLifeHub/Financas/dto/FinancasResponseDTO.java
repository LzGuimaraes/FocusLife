package dev.LzGuimaraes.FocusLifeHub.Financas.dto;

import dev.LzGuimaraes.FocusLifeHub.Financas.TipoCarteira;

public record FinancasResponseDTO(
    Long id,
    String nome,
    String moeda,
    TipoCarteira tipoCarteira,
    Long user_id
) {}