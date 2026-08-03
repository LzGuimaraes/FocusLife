package dev.LzGuimaraes.FocusLifeHub.Metas.dto;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonFormat;

import dev.LzGuimaraes.FocusLifeHub.Metas.Enum.MetaStatus;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record MetasRequestDTO(
    
    @NotBlank(message = "O título é obrigatório")
    String titulo,

    String descricao,

    @PositiveOrZero(message = "O progresso não pode ser negativo")
    Float prograsso,
    @NotNull(message = "O prazo é obrigatório")
    @Future(message = "O prazo deve ser uma data futura")
    @JsonFormat(pattern = "yyyy-MM-dd")
    LocalDate prazo,
    MetaStatus status
) {}