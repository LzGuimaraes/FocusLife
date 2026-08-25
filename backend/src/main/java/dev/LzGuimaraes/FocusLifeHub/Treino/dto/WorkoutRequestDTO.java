package dev.LzGuimaraes.FocusLifeHub.Treino.dto;

import java.time.DayOfWeek;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record WorkoutRequestDTO(
    @NotBlank(message = "O título do treino é obrigatório")
    String title,
    String description,
    @NotNull(message = "O dia da semana é obrigatório")
    DayOfWeek dayOfWeek,
    Boolean isActive,
    List<@Valid WorkoutExerciseRequestDTO> exercises
) {}
