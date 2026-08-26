package dev.LzGuimaraes.FocusLifeHub.Treino.dto;

import java.time.LocalDate;

public record WeekSummaryDTO(
    LocalDate start,
    LocalDate end,
    Integer plannedCount,
    Integer completedCount,
    Integer executedCount,
    Integer consistencyPercent,
    Integer executionAveragePercent,
    Integer currentStreak,
    Integer bestStreak,
    Boolean hasPlannedWorkouts,
    String evaluationKey,
    String evaluationLabel,
    String evaluationMessage
) {}
