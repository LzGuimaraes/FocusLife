package dev.LzGuimaraes.FocusLifeHub.Tarefas;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import dev.LzGuimaraes.FocusLifeHub.Exceptions.ResourceNotFoundException;
import dev.LzGuimaraes.FocusLifeHub.Tarefas.Enum.Prioridade;
import dev.LzGuimaraes.FocusLifeHub.Tarefas.Enum.TarefaStatus;
import dev.LzGuimaraes.FocusLifeHub.Tarefas.dto.TarefasRequestDTO;
import dev.LzGuimaraes.FocusLifeHub.Tarefas.dto.TarefasResponseDTO;
import dev.LzGuimaraes.FocusLifeHub.User.UserModel;
import dev.LzGuimaraes.FocusLifeHub.User.UserRepository;
import dev.LzGuimaraes.FocusLifeHub.config.JWTUserData;

@Service
public class TarefasService {

    private final TarefasRepository tarefasRepository;
    private final UserRepository userRepository;
    private final TarefasMapper tarefasMapper;

    public TarefasService( TarefasRepository tarefasRepository, UserRepository userRepository, TarefasMapper tarefasMapper) {

                this.tarefasRepository = tarefasRepository;
                this.userRepository = userRepository;
                this.tarefasMapper = tarefasMapper;
            }

    private Long getAuthenticatedUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        JWTUserData jwtData = (JWTUserData) authentication.getPrincipal();
        return jwtData.userId();
    }

    public Page<TarefasResponseDTO> getAllTarefas(Pageable pageable) {
        Long userId = getAuthenticatedUserId();
        return tarefasRepository.findByUserId(userId, pageable) 
                .map(tarefasMapper::toResponse);
    }

    public TarefasResponseDTO getTarefaById(Long id) {
        Long userId = getAuthenticatedUserId();
        TarefasModel tarefa = tarefasRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tarefa com ID " + id + " não encontrada"));
        
        if (!tarefa.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("Tarefa com ID " + id + " não encontrada");
        }
        
        return tarefasMapper.toResponse(tarefa);
    }

    public TarefasResponseDTO createTarefa(TarefasRequestDTO dto) {
        Long userId = getAuthenticatedUserId();
        
        UserModel user = userRepository.findById(userId) 
                .orElseThrow(() -> new ResourceNotFoundException("Usuário com ID " + userId + " não encontrado"));

        TarefasModel tarefa = tarefasMapper.toModel(dto, user);
        tarefa.setStatus(TarefaStatus.PENDENTE);
        
        if (dto.prioridade() == null) {
            tarefa.setPrioridade(Prioridade.media); 
        }

        TarefasModel savedTarefa = tarefasRepository.save(tarefa);
        return tarefasMapper.toResponse(savedTarefa);
    }

    public TarefasResponseDTO updateTarefa(Long id, TarefasRequestDTO dto) {
        Long userId = getAuthenticatedUserId();
        TarefasModel tarefa = tarefasRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tarefa com ID " + id + " não encontrada"));

        if (!tarefa.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("Tarefa com ID " + id + " não encontrada");
        }

        if (dto.titulo() != null && !dto.titulo().isBlank()) {
            tarefa.setTitulo(dto.titulo());
        }
        if (dto.status() != null) {
            tarefa.setStatus(dto.status());
        }
        if (dto.prioridade() != null) {
            tarefa.setPrioridade(dto.prioridade());
        }
        if (dto.prazo() != null) {
            tarefa.setPrazo(dto.prazo());
        }

        TarefasModel updatedTarefa = tarefasRepository.save(tarefa);
        return tarefasMapper.toResponse(updatedTarefa);
    }

    public void deleteTarefa(Long id) {
        Long userId = getAuthenticatedUserId();
        TarefasModel tarefa = tarefasRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tarefa com ID " + id + " não encontrada"));

        if (!tarefa.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("Tarefa com ID " + id + " não encontrada");
        }

        tarefasRepository.deleteById(id);
    }
}
