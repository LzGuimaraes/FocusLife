package dev.LzGuimaraes.FocusLifeHub.Planejamento.Estrategia;

import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.LzGuimaraes.FocusLifeHub.Carteira.CarteiraInvestimentoModel;
import dev.LzGuimaraes.FocusLifeHub.Carteira.CarteiraInvestimentoRepository;
import dev.LzGuimaraes.FocusLifeHub.Exceptions.ResourceNotFoundException;
import dev.LzGuimaraes.FocusLifeHub.Planejamento.Estrategia.dto.EstrategiaRequestDTO;
import dev.LzGuimaraes.FocusLifeHub.Planejamento.Estrategia.dto.EstrategiaResponseDTO;
import dev.LzGuimaraes.FocusLifeHub.Planejamento.comum.ContextoUsuario;
import dev.LzGuimaraes.FocusLifeHub.User.UserModel;
import dev.LzGuimaraes.FocusLifeHub.User.UserRepository;

/**
 * CRUD da Estratégia de Investimentos do usuário autenticado.
 * A estratégia é apenas um container de metodologia: o sistema não interpreta
 * o conteúdo dela (nome/descrição são livres).
 */
@Service
public class EstrategiaService {

    private final EstrategiaRepository estrategiaRepository;
    private final UserRepository userRepository;
    private final CarteiraInvestimentoRepository carteiraRepository;
    private final ContextoUsuario contextoUsuario;

    public EstrategiaService(EstrategiaRepository estrategiaRepository,
                             UserRepository userRepository,
                             CarteiraInvestimentoRepository carteiraRepository,
                             ContextoUsuario contextoUsuario) {
        this.estrategiaRepository = estrategiaRepository;
        this.userRepository = userRepository;
        this.carteiraRepository = carteiraRepository;
        this.contextoUsuario = contextoUsuario;
    }

    /** Estratégia do usuário autenticado ou 404 (usado no vínculo carteira→estratégia). */
    public EstrategiaModel exigirDoUsuario(Long id) {
        return estrategiaRepository.findByIdAndUserId(id, contextoUsuario.id())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Estratégia com ID " + id + " não encontrada"));
    }

    public Page<EstrategiaResponseDTO> getAll(Pageable pageable) {
        return estrategiaRepository.findByUserId(contextoUsuario.id(), pageable).map(this::toResponse);
    }

    public EstrategiaResponseDTO getById(Long id) {
        return toResponse(exigirDoUsuario(id));
    }

    public EstrategiaResponseDTO create(EstrategiaRequestDTO dto) {
        Long userId = contextoUsuario.id();
        UserModel user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário com ID " + userId + " não encontrado"));

        EstrategiaModel estrategia = new EstrategiaModel();
        estrategia.setNome(dto.nome());
        estrategia.setDescricao(dto.descricao());
        estrategia.setAtiva(dto.ativa() == null || dto.ativa());
        estrategia.setUser(user);
        estrategia.setCreatedAt(LocalDateTime.now());
        estrategia.setUpdatedAt(LocalDateTime.now());

        return toResponse(estrategiaRepository.save(estrategia));
    }

    public EstrategiaResponseDTO update(Long id, EstrategiaRequestDTO dto) {
        EstrategiaModel estrategia = exigirDoUsuario(id);

        if (dto.nome() != null && !dto.nome().isBlank()) {
            estrategia.setNome(dto.nome());
        }
        if (dto.descricao() != null) {
            estrategia.setDescricao(dto.descricao());
        }
        if (dto.ativa() != null) {
            estrategia.setAtiva(dto.ativa());
        }
        estrategia.setUpdatedAt(LocalDateTime.now());

        return toResponse(estrategiaRepository.save(estrategia));
    }

    /**
     * Exclui a estratégia desvinculando as carteiras que a usavam (a carteira
     * continua existindo — apenas deixa de ter estratégia).
     */
    @Transactional
    public void delete(Long id) {
        EstrategiaModel estrategia = exigirDoUsuario(id);
        for (CarteiraInvestimentoModel carteira : carteiraRepository.findByEstrategiaId(id)) {
            carteira.setEstrategia(null);
        }
        estrategiaRepository.delete(estrategia);
    }

    private EstrategiaResponseDTO toResponse(EstrategiaModel estrategia) {
        Long userId = (estrategia.getUser() != null) ? estrategia.getUser().getId() : null;
        return new EstrategiaResponseDTO(
                estrategia.getId(),
                estrategia.getNome(),
                estrategia.getDescricao(),
                estrategia.getAtiva(),
                userId);
    }
}
