package dev.LzGuimaraes.FocusLifeHub.Treino.dto;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.List;

public record WorkoutResponseDTO(
    Long id,
    String title,
    String description,
    DayOfWeek dayOfWeek,
    Boolean isActive,
    List<WorkoutExerciseResponseDTO> exercises,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
