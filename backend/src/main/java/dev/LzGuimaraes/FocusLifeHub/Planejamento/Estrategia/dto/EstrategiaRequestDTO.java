package dev.LzGuimaraes.FocusLifeHub.Planejamento.Estrategia.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EstrategiaRequestDTO(
        @NotBlank(message = "O nome da estratégia é obrigatório")
        @Size(max = 120, message = "O nome da estratégia deve ter no máximo 120 caracteres")
        String nome,

        @Size(max = 1000, message = "A descrição deve ter no máximo 1000 caracteres")
        String descricao,

        Boolean ativa
) {}
