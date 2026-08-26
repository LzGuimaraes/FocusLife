package dev.LzGuimaraes.FocusLifeHub.Treino.dto;

import java.time.LocalDate;

public record WeekHistoryDTO(
    LocalDate start,
    LocalDate end,
    Integer plannedCount,
    Integer completedCount,
    Integer executedCount,
    Integer consistencyPercent,
    Integer executionAveragePercent,
    String evaluationKey,
    String evaluationLabel,
    String evaluationMessage
) {}
