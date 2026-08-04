package dev.LzGuimaraes.FocusLifeHub.Materia.dto;

import java.time.DayOfWeek;
import java.util.Set;

public record MateriaRequestDTO(  
    String nome,
    String descricao,
    Set<DayOfWeek> diasSemana
    ) {}
