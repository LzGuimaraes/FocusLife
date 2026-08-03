package dev.LzGuimaraes.FocusLifeHub.Estudos.dto;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonFormat;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record EstudosRequestDTO(
@NotBlank(message = "O nome é obrigatório")
    String nome,
    @Positive(message = "A duração deve ser um número positivo")
    Integer duracao_min,
    @NotNull(message = "A data é obrigatória")
    @JsonFormat(pattern = "yyyy-MM-dd")
    LocalDate data,
    String notas,
    @NotNull(message = "O ID da matéria é obrigatório")
    Long materia_id
) {}
