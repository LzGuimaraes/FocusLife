package dev.LzGuimaraes.FocusLifeHub.Estudos;

import org.springframework.stereotype.Component;

import dev.LzGuimaraes.FocusLifeHub.Estudos.dto.EstudosRequestDTO;
import dev.LzGuimaraes.FocusLifeHub.Estudos.dto.EstudosResponseDTO;
import dev.LzGuimaraes.FocusLifeHub.Materia.MateriaModel;

@Component
public class EstudosMapper {

    public EstudosModel toModel(EstudosRequestDTO dto, MateriaModel materia) {
        if (dto == null) {
            return null;
        }

        EstudosModel estudo = new EstudosModel();
        estudo.setNome(dto.nome());
        estudo.setDuracao_min(dto.duracao_min() != null ? dto.duracao_min() : 0); 
        estudo.setData(dto.data());
        estudo.setNotas(dto.notas());
        estudo.setMateria(materia); 

        return estudo;
    }

    public EstudosResponseDTO toResponse(EstudosModel estudo) {
        if (estudo == null) {
            return null;
        }

        Long materiaId = (estudo.getMateria() != null) ? estudo.getMateria().getId() : null;

        return new EstudosResponseDTO(
            estudo.getId(),
            estudo.getNome(),
            estudo.getDuracao_min(),
            estudo.getData(),
            estudo.getNotas(),
            materiaId 
        );
    }
}