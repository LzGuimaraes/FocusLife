package dev.LzGuimaraes.FocusLifeHub.Tarefas.dto;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonFormat;

import dev.LzGuimaraes.FocusLifeHub.Tarefas.Enum.Prioridade;
import dev.LzGuimaraes.FocusLifeHub.Tarefas.Enum.TarefaStatus;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;

public record TarefasRequestDTO(

    @NotBlank(message = "O título é obrigatório")
    String titulo,
    TarefaStatus status, 
    Prioridade prioridade, 
    @FutureOrPresent(message = "O prazo não pode ser uma data passada")
    @JsonFormat(pattern = "yyyy-MM-dd")
    LocalDate prazo
) {}