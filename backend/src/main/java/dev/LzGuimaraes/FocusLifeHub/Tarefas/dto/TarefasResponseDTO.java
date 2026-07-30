package dev.LzGuimaraes.FocusLifeHub.Tarefas.dto;

import java.util.Date;

import dev.LzGuimaraes.FocusLifeHub.Tarefas.Enum.Prioridade;
import dev.LzGuimaraes.FocusLifeHub.Tarefas.Enum.TarefaStatus;

public record TarefasResponseDTO(
    Long id,
    String titulo,
    TarefaStatus status,
    Prioridade prioridade,
    Date prazo,
    Long user_id
) {}