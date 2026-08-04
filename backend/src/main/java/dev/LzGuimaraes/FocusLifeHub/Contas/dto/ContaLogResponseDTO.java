package dev.LzGuimaraes.FocusLifeHub.Contas.dto;

import java.time.LocalDateTime;

public record ContaLogResponseDTO(
    Long id,
    String acao,
    LocalDateTime criadoEm
) {}
