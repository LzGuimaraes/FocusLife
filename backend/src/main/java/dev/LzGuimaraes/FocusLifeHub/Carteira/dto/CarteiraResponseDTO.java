package dev.LzGuimaraes.FocusLifeHub.Carteira.dto;

public record CarteiraResponseDTO(
    Long id,
    String nome,
    String moeda,
    Long user_id
) {}
