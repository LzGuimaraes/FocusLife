package dev.LzGuimaraes.FocusLifeHub.Treino.dto;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

public record WorkoutOccurrenceDTO(
    Long workoutId,
    String title,
    String description,
    LocalDate date,
    DayOfWeek dayOfWeek,
    WorkoutSessionDTO session,
    List<ExerciseChecklistItemDTO> exercises
) {}
