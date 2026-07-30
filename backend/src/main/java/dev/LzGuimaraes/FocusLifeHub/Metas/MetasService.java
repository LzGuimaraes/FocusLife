package dev.LzGuimaraes.FocusLifeHub.Metas;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder; 
import org.springframework.stereotype.Service;

import dev.LzGuimaraes.FocusLifeHub.Exceptions.ResourceNotFoundException;
import dev.LzGuimaraes.FocusLifeHub.Metas.Enum.MetaStatus;
import dev.LzGuimaraes.FocusLifeHub.Metas.dto.MetasRequestDTO;
import dev.LzGuimaraes.FocusLifeHub.Metas.dto.MetasResponseDTO;
import dev.LzGuimaraes.FocusLifeHub.config.JWTUserData; 
import dev.LzGuimaraes.FocusLifeHub.User.UserModel;
import dev.LzGuimaraes.FocusLifeHub.User.UserRepository;

@Service
public class MetasService {

    private final MetasRepository metasRepository;
    private final UserRepository userRepository;
    private final MetasMapper metasMapper;

    public MetasService(MetasRepository metasRepository, UserRepository userRepository, MetasMapper metasMapper) {
        this.metasRepository = metasRepository;
        this.userRepository = userRepository;
        this.metasMapper = metasMapper;
    }

    private Long getAuthenticatedUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        JWTUserData jwtData = (JWTUserData) authentication.getPrincipal();
        return jwtData.userId();
    }

    public Page<MetasResponseDTO> getAllMetas(Pageable pageable) {
        Long userId = getAuthenticatedUserId();
        return metasRepository.findByUserId(userId, pageable)
                .map(metasMapper::toResponse);
    }

    public MetasResponseDTO getMetaById(Long id) {
        Long userId = getAuthenticatedUserId();
        MetasModel meta = metasRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Meta com ID " + id + " não encontrada"));

        if (!meta.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("Meta com ID " + id + " não encontrada");
        }

        return metasMapper.toResponse(meta);
    }

    public MetasResponseDTO createMeta(MetasRequestDTO dto) {
        Long authenticatedUserId = getAuthenticatedUserId(); 
        
        UserModel user = userRepository.findById(authenticatedUserId)
        .orElseThrow(() -> new ResourceNotFoundException("Usuário autenticado com ID " + authenticatedUserId + " não encontrado"));

        MetasModel meta = metasMapper.toModel(dto, user);

        meta.setStatus(MetaStatus.PENDENTE);
        meta.setPrograsso(0.0f);

        MetasModel savedMeta = metasRepository.save(meta);

        return metasMapper.toResponse(savedMeta);
    }

    public MetasResponseDTO updateMeta(Long id, MetasRequestDTO dto) {
        Long userId = getAuthenticatedUserId();
        MetasModel meta = metasRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Meta com ID " + id + " não encontrada para alteração"));

        if (!meta.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("Meta com ID " + id + " não encontrada");
        }
        if (dto.titulo() != null && !dto.titulo().isBlank()) {
            meta.setTitulo(dto.titulo());
        }
        if (dto.descricao() != null) {
            meta.setDescricao(dto.descricao());
        }
        if (dto.prograsso() != null) {
            meta.setPrograsso(dto.prograsso());
        }
        if (dto.prazo() != null) {
            meta.setPrazo(dto.prazo());
        }
        if (dto.status() != null) {
            meta.setStatus(dto.status());
        }

        MetasModel updatedMeta = metasRepository.save(meta);

        return metasMapper.toResponse(updatedMeta);
    }

    public void deleteMeta(Long id) {
        Long userId = getAuthenticatedUserId();
        MetasModel meta = metasRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Meta com ID " + id + " não encontrada para exclusão"));

        if (!meta.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("Meta com ID " + id + " não encontrada para exclusão");
        }

        metasRepository.deleteById(id);
    }
}