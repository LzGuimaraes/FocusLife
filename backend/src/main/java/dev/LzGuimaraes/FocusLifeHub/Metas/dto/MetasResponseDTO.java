package dev.LzGuimaraes.FocusLifeHub.Metas.dto;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonFormat;

import dev.LzGuimaraes.FocusLifeHub.Metas.Enum.MetaStatus;

public record MetasResponseDTO(
    Long id,
    String titulo,
    String descricao,
    Float prograsso,
    @JsonFormat(pattern = "yyyy-MM-dd")
    LocalDate prazo,
    MetaStatus status,
    Long user_id
) {}