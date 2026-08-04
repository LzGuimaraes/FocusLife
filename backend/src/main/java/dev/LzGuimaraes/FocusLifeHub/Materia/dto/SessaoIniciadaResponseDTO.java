package dev.LzGuimaraes.FocusLifeHub.Materia.dto;

import java.time.LocalDateTime;

public record SessaoIniciadaResponseDTO(
    Long sessaoId,
    LocalDateTime inicio
) {}
