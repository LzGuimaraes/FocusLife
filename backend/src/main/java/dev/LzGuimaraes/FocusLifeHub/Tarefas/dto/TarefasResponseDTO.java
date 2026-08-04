package dev.LzGuimaraes.FocusLifeHub.Tarefas.dto;

import java.time.LocalDate;
import java.time.LocalTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import dev.LzGuimaraes.FocusLifeHub.Tarefas.Enum.Prioridade;
import dev.LzGuimaraes.FocusLifeHub.Tarefas.Enum.TarefaStatus;

public record TarefasResponseDTO(
    Long id,
    String titulo,
    TarefaStatus status,
    Prioridade prioridade,
    @JsonFormat(pattern = "yyyy-MM-dd")
    LocalDate prazo,
    @JsonFormat(pattern = "HH:mm")
    LocalTime horario,
    Long user_id
) {}