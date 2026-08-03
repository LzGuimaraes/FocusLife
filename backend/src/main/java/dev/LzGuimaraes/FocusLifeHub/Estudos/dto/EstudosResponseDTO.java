package dev.LzGuimaraes.FocusLifeHub.Estudos.dto;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonFormat;

public record EstudosResponseDTO(
    Long id,
    String nome,
    int duracao_min,
    @JsonFormat(pattern = "yyyy-MM-dd")
    LocalDate data,
    String notas,
    Long materia_id
)  {}
