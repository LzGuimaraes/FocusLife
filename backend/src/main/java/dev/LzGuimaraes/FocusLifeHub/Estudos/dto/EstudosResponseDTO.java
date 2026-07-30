package dev.LzGuimaraes.FocusLifeHub.Estudos.dto;

import java.util.Date;

public record EstudosResponseDTO(
    Long id,
    String nome,
    int duracao_min,
    Date data,
    String notas,
    Long materia_id
)  {}
