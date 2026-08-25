package dev.LzGuimaraes.FocusLifeHub.Treino.dto;

import java.time.LocalDate;
import java.util.List;

public record WeekResponseDTO(
    LocalDate start,
    LocalDate end,
    List<WorkoutOccurrenceDTO> occurrences,
    WeekSummaryDTO summary
) {}
