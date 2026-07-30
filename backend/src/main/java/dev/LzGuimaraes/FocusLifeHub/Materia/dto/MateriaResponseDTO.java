package dev.LzGuimaraes.FocusLifeHub.Materia.dto;

import dev.LzGuimaraes.FocusLifeHub.Estudos.dto.EstudosResponseDTO;
import java.util.List;

public record MateriaResponseDTO(
Long id,
    String nome,
    String descricao,
    Long userId, 
    List<EstudosResponseDTO> estudos 
) {}
