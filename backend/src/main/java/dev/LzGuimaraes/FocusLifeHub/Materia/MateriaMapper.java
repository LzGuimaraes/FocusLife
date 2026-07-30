package dev.LzGuimaraes.FocusLifeHub.Materia;

import org.springframework.stereotype.Component;

import dev.LzGuimaraes.FocusLifeHub.Estudos.EstudosModel;
import dev.LzGuimaraes.FocusLifeHub.Estudos.dto.EstudosResponseDTO;
import dev.LzGuimaraes.FocusLifeHub.Materia.dto.MateriaRequestDTO;
import dev.LzGuimaraes.FocusLifeHub.Materia.dto.MateriaResponseDTO;
import dev.LzGuimaraes.FocusLifeHub.User.UserModel;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class MateriaMapper {

    public MateriaModel toModel(MateriaRequestDTO dto, UserModel user) {
        MateriaModel materia = new MateriaModel();
        materia.setNome(dto.nome());
        materia.setDescricao(dto.descricao());
        materia.setUser(user);
        return materia;
    }

    public MateriaResponseDTO toResponse(MateriaModel model) {
        
        List<EstudosResponseDTO> estudosDTOs = model.getEstudos()
                .stream()
                .map(this::toEstudosDTO)
                .collect(Collectors.toList());

        return new MateriaResponseDTO(
            model.getId(),
            model.getNome(),
            model.getDescricao(),
            model.getUser().getId(),
            estudosDTOs 
        );
    }
    
private EstudosResponseDTO toEstudosDTO(EstudosModel estudo) {
        
        MateriaModel materia = estudo.getMateria();

        Long materiaId = (materia != null) ? materia.getId() : null;

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