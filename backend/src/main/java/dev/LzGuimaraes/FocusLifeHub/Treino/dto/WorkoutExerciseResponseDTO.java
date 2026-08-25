package dev.LzGuimaraes.FocusLifeHub.Treino.dto;

public record WorkoutExerciseResponseDTO(
    Long id,
    String name,
    Integer sets,
    Integer repetitions,
    Float weight,
    Integer durationMinutes,
    Float distanceKm,
    String notes,
    Integer order
) {}
