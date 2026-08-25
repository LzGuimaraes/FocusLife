package dev.LzGuimaraes.FocusLifeHub.Treino.dto;

import java.time.LocalDateTime;

public record WorkoutSessionDTO(
    Long id,
    String status,
    Integer completionPercentage,
    LocalDateTime completedAt,
    String notes
) {}
