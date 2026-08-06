package dev.LzGuimaraes.FocusLifeHub.Despesa.dto;

import java.time.LocalDateTime;

public record DespesaLogResponseDTO(
    Long id,
    String acao,
    LocalDateTime criadoEm
) {}
