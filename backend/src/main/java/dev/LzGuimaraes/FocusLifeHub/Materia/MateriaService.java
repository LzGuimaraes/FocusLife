package dev.LzGuimaraes.FocusLifeHub.Materia;

import java.time.Duration;
import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication; 
import org.springframework.security.core.context.SecurityContextHolder; 
import org.springframework.stereotype.Service;

import dev.LzGuimaraes.FocusLifeHub.Exceptions.ResourceNotFoundException;
import dev.LzGuimaraes.FocusLifeHub.Materia.dto.MateriaRequestDTO;
import dev.LzGuimaraes.FocusLifeHub.Materia.dto.MateriaResponseDTO;
import dev.LzGuimaraes.FocusLifeHub.Materia.dto.SessaoIniciadaResponseDTO;
import dev.LzGuimaraes.FocusLifeHub.Materia.dto.SessaoPausadaResponseDTO;
import dev.LzGuimaraes.FocusLifeHub.Materia.dto.TempoTotalResponseDTO;
import dev.LzGuimaraes.FocusLifeHub.config.JWTUserData;
import dev.LzGuimaraes.FocusLifeHub.User.UserModel;
import dev.LzGuimaraes.FocusLifeHub.User.UserRepository;

@Service
public class MateriaService {
    private final MateriaRepository materiaRepository; 
    private final UserRepository userRepository; 
    private final SessaoEstudoRepository sessaoEstudoRepository; 
    private final MateriaMapper materiaMapper; 

    public MateriaService(MateriaRepository materiaRepository, UserRepository userRepository, SessaoEstudoRepository sessaoEstudoRepository, MateriaMapper materiaMapper) {
        this.materiaMapper = materiaMapper;
        this.materiaRepository = materiaRepository;
        this.userRepository = userRepository;
        this.sessaoEstudoRepository = sessaoEstudoRepository;
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

    public SessaoIniciadaResponseDTO iniciarSessao(Long materiaId) {
        Long userId = getAuthenticatedUserId();
        validarMateriaDoUsuario(materiaId, userId);

        if (sessaoEstudoRepository.findFirstByMateriaIdAndFimIsNull(materiaId).isPresent()) {
            throw new IllegalStateException("Já existe uma sessão de estudo em andamento para esta matéria.");
        }

        SessaoEstudo sessao = new SessaoEstudo();
        sessao.setMateriaId(materiaId);
        sessao.setInicio(LocalDateTime.now());
        SessaoEstudo saved = sessaoEstudoRepository.save(sessao);

        return new SessaoIniciadaResponseDTO(saved.getId(), saved.getInicio());
    }

    public SessaoPausadaResponseDTO pausarSessao(Long materiaId, Long sessaoId) {
        Long userId = getAuthenticatedUserId();
        validarMateriaDoUsuario(materiaId, userId);

        SessaoEstudo sessao = sessaoEstudoRepository.findByIdAndMateriaId(sessaoId, materiaId)
                .orElseThrow(() -> new ResourceNotFoundException("Sessão de estudo não encontrada"));

        if (sessao.getFim() != null) {
            throw new IllegalStateException("Esta sessão já foi pausada.");
        }

        LocalDateTime fim = LocalDateTime.now();
        sessao.setFim(fim);
        sessao.setDuracaoSegundos(Duration.between(sessao.getInicio(), fim).getSeconds());
        sessaoEstudoRepository.save(sessao);

        return new SessaoPausadaResponseDTO(sessao.getDuracaoSegundos(), totalSegundosMateria(materiaId));
    }

    public TempoTotalResponseDTO getTempoTotal(Long materiaId) {
        Long userId = getAuthenticatedUserId();
        validarMateriaDoUsuario(materiaId, userId);
        return new TempoTotalResponseDTO(totalSegundosMateria(materiaId));
    }

    private long totalSegundosMateria(Long materiaId) {
        return sessaoEstudoRepository.findByMateriaId(materiaId)
                .stream()
                .mapToLong(SessaoEstudo::getDuracaoSegundos)
                .sum();
    }

    private MateriaModel validarMateriaDoUsuario(Long materiaId, Long userId) {
        MateriaModel materia = materiaRepository.findById(materiaId)
                .orElseThrow(() -> new ResourceNotFoundException("Matéria com ID " + materiaId + " não encontrada"));
        if (!materia.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("Matéria com ID " + materiaId + " não encontrada");
        }
        return materia;
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

        if (dto.diasSemana() != null) {
            materia.setDiasSemana(dto.diasSemana());
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