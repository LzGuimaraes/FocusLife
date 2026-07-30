package dev.LzGuimaraes.FocusLifeHub.Materia;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication; 
import org.springframework.security.core.context.SecurityContextHolder; 
import org.springframework.stereotype.Service;

import dev.LzGuimaraes.FocusLifeHub.Exceptions.ResourceNotFoundException;
import dev.LzGuimaraes.FocusLifeHub.Materia.dto.MateriaRequestDTO;
import dev.LzGuimaraes.FocusLifeHub.Materia.dto.MateriaResponseDTO;
import dev.LzGuimaraes.FocusLifeHub.config.JWTUserData;
import dev.LzGuimaraes.FocusLifeHub.User.UserModel;
import dev.LzGuimaraes.FocusLifeHub.User.UserRepository;

@Service
public class MateriaService {
    private final MateriaRepository materiaRepository; 
    private final UserRepository userRepository; 
    private final MateriaMapper materiaMapper; 

    public MateriaService(MateriaRepository materiaRepository, UserRepository userRepository, MateriaMapper materiaMapper) {
        this.materiaMapper = materiaMapper;
        this.materiaRepository = materiaRepository;
        this.userRepository = userRepository;
    }

    private Long getAuthenticatedUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        JWTUserData jwtData = (JWTUserData) authentication.getPrincipal();
        return jwtData.userId();
    }

    public Page<MateriaResponseDTO> getAllMaterias(Pageable pageable) {
        Long userId = getAuthenticatedUserId();
        return materiaRepository.findByUserId(userId, pageable)
                .map(materiaMapper::toResponse);
    }

    public MateriaResponseDTO getMateriaById(Long id) {
        Long userId = getAuthenticatedUserId();
        MateriaModel materia = materiaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Matéria com ID " + id + " não encontrada")); 

        if (!materia.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("Matéria com ID " + id + " não encontrada");
        }

        return materiaMapper.toResponse(materia);
    }

    public MateriaResponseDTO createMateria(MateriaRequestDTO dto) {
        Long authenticatedUserId = getAuthenticatedUserId();
        UserModel user = userRepository.findById(authenticatedUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário com ID " + authenticatedUserId + " não encontrado"));

        MateriaModel materia = materiaMapper.toModel(dto, user);
        MateriaModel saved = materiaRepository.save(materia);

        return materiaMapper.toResponse(saved);
    }

    public MateriaResponseDTO alterarMateria(Long id, MateriaRequestDTO dto) {
        Long userId = getAuthenticatedUserId();
        MateriaModel materia = materiaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Matéria com ID " + id + " não encontrada para alteração"));

        if (!materia.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("Matéria com ID " + id + " não encontrada para alteração");
        }

        if (dto.nome() != null && !dto.nome().isBlank()) {
            materia.setNome(dto.nome());
        }

        if (dto.descricao() != null) {
            materia.setDescricao(dto.descricao());
        }

        MateriaModel alterarMateria = materiaRepository.save(materia);
        return materiaMapper.toResponse(alterarMateria);
    }

    public void deleteMateria(Long id) {
        Long userId = getAuthenticatedUserId();

        MateriaModel materia = materiaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Matéria com ID " + id + " não encontrada para exclusão"));

        if (!materia.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("Matéria com ID " + id + " não encontrada para exclusão");
        }

        materiaRepository.deleteById(id);
    }
}