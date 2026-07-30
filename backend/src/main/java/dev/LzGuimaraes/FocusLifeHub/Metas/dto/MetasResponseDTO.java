package dev.LzGuimaraes.FocusLifeHub.Metas.dto;

import java.util.Date;

import dev.LzGuimaraes.FocusLifeHub.Metas.Enum.MetaStatus;

public record MetasResponseDTO(
    Long id,
    String titulo,
    String descricao,
    Float prograsso,
    Date prazo,
    MetaStatus status,
    Long user_id
) {}