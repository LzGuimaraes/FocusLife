package dev.LzGuimaraes.FocusLifeHub.Planejamento.Estrategia.dto;

public record EstrategiaResponseDTO(
        Long id,
        String nome,
        String descricao,
        Boolean ativa,
        Long user_id
) {}
