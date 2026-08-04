package dev.LzGuimaraes.FocusLifeHub.Materia.dto;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Set;

import dev.LzGuimaraes.FocusLifeHub.Estudos.dto.EstudosResponseDTO;

public record MateriaResponseDTO(
    Long id,
    String nome,
    String descricao,
    Long userId,
    Set<DayOfWeek> diasSemana,
    List<EstudosResponseDTO> estudos,
    Long totalSegundos
) {}
