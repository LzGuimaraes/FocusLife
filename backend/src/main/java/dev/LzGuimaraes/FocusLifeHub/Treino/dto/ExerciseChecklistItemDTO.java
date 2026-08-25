package dev.LzGuimaraes.FocusLifeHub.Treino.dto;

public record ExerciseChecklistItemDTO(
    Long workoutExerciseId,
    Long completionId,
    String name,
    Integer sets,
    Integer repetitions,
    Float weight,
    Integer durationMinutes,
    Float distanceKm,
    String notes,
    Boolean completed
) {}
