package dev.LzGuimaraes.FocusLifeHub.Estudos;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder; 
import org.springframework.stereotype.Service;

import dev.LzGuimaraes.FocusLifeHub.Exceptions.ResourceNotFoundException;
import dev.LzGuimaraes.FocusLifeHub.Estudos.dto.EstudosRequestDTO;
import dev.LzGuimaraes.FocusLifeHub.Estudos.dto.EstudosResponseDTO;
import dev.LzGuimaraes.FocusLifeHub.Materia.MateriaModel;
import dev.LzGuimaraes.FocusLifeHub.Materia.MateriaRepository;
import dev.LzGuimaraes.FocusLifeHub.config.JWTUserData;

@Service
public class EstudosService {

    private final EstudosRepository estudosRepository;
    private final MateriaRepository materiaRepository;
    private final EstudosMapper estudosMapper;

    public EstudosService(
            EstudosRepository estudosRepository,
            MateriaRepository materiaRepository,
            EstudosMapper estudosMapper) {
        this.estudosRepository = estudosRepository;
        this.materiaRepository = materiaRepository;
        this.estudosMapper = estudosMapper;
    }

    private Long getAuthenticatedUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        JWTUserData jwtData = (JWTUserData) authentication.getPrincipal();
        return jwtData.userId();
    }

    public Page<EstudosResponseDTO> getAllEstudos(Pageable pageable) {
        Long userId = getAuthenticatedUserId();
        return estudosRepository.findByMateria_UserId(userId, pageable)
                .map(estudosMapper::toResponse);
    }

    public EstudosResponseDTO getEstudosById(Long id) {
        Long userId = getAuthenticatedUserId();
        EstudosModel estudo = estudosRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sessão de estudo com ID " + id + " não encontrada"));

        if (!estudo.getMateria().getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("Sessão de estudo com ID " + id + " não encontrada");
        }

        return estudosMapper.toResponse(estudo);
    }

    public EstudosResponseDTO createEstudos(EstudosRequestDTO dto) {
        Long userId = getAuthenticatedUserId();
        MateriaModel materia = materiaRepository.findById(dto.materia_id())
                .orElseThrow(() -> new ResourceNotFoundException("Matéria com ID " + dto.materia_id() + " não encontrada"));

        if (!materia.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("Matéria com ID " + dto.materia_id() + " não encontrada");
        }

        EstudosModel estudo = estudosMapper.toModel(dto, materia);
        EstudosModel savedEstudo = estudosRepository.save(estudo);
        return estudosMapper.toResponse(savedEstudo);
    }

    public EstudosResponseDTO updateEstudos(Long id, EstudosRequestDTO dto) {
        Long userId = getAuthenticatedUserId();
        EstudosModel estudo = estudosRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sessão de estudo com ID " + id + " não encontrada para alteração"));

        if (!estudo.getMateria().getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("Sessão de estudo com ID " + id + " não encontrada para alteração");
        }

        if (dto.nome() != null && !dto.nome().isBlank()) {
            estudo.setNome(dto.nome());
        }
        if (dto.duracao_min() != null && dto.duracao_min() > 0) {
            estudo.setDuracao_min(dto.duracao_min());
        }
        if (dto.data() != null) {
            estudo.setData(dto.data());
        }
        if (dto.notas() != null) {
            estudo.setNotas(dto.notas());
        }

        if (dto.materia_id() != null && !dto.materia_id().equals(estudo.getMateria().getId())) {
            MateriaModel novaMateria = materiaRepository.findById(dto.materia_id())
                    .orElseThrow(() -> new ResourceNotFoundException("Nova matéria com ID " + dto.materia_id() + " não encontrada"));

            if (!novaMateria.getUser().getId().equals(userId)) {
                throw new ResourceNotFoundException("Nova matéria com ID " + dto.materia_id() + " não encontrada");
            }
            estudo.setMateria(novaMateria);
        }

        EstudosModel updatedEstudo = estudosRepository.save(estudo);
        return estudosMapper.toResponse(updatedEstudo);
    }

    public void deleteEstudos(Long id) {
        Long userId = getAuthenticatedUserId();
        EstudosModel estudo = estudosRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sessão de estudo com ID " + id + " não encontrada para exclusão"));

        if (!estudo.getMateria().getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("Sessão de estudo com ID " + id + " não encontrada para exclusão");
        }
        estudosRepository.deleteById(id);
    }
}