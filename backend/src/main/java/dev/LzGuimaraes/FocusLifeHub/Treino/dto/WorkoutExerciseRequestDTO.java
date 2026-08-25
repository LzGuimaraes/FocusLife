package dev.LzGuimaraes.FocusLifeHub.Treino.dto;

import jakarta.validation.constraints.NotBlank;

public record WorkoutExerciseRequestDTO(
    Long id,
    @NotBlank(message = "O nome do exercício é obrigatório")
    String name,
    Integer sets,
    Integer repetitions,
    Float weight,
    Integer durationMinutes,
    Float distanceKm,
    String notes,
    Integer order
) {}
