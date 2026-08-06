package dev.LzGuimaraes.FocusLifeHub.Carteira;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

import dev.LzGuimaraes.FocusLifeHub.Carteira.dto.CarteiraRequestDTO;
import dev.LzGuimaraes.FocusLifeHub.Carteira.dto.CarteiraResponseDTO;
import dev.LzGuimaraes.FocusLifeHub.Contas.ContasRepository;
import dev.LzGuimaraes.FocusLifeHub.Exceptions.ResourceNotFoundException;
import dev.LzGuimaraes.FocusLifeHub.User.UserModel;
import dev.LzGuimaraes.FocusLifeHub.User.UserRepository;
import dev.LzGuimaraes.FocusLifeHub.config.JWTUserData;

/**
 * Implementação compartilhada do CRUD das carteiras (investimento e dívidas).
 * O tipo concreto (entidade/repositório) é definido pelas subclasses.
 */
public abstract class AbstractCarteiraService<T extends CarteiraModel> {

    private final CarteiraRepository<T> repository;
    private final UserRepository userRepository;
    private final ContasRepository contasRepository;

    protected AbstractCarteiraService(
            CarteiraRepository<T> repository,
            UserRepository userRepository,
            ContasRepository contasRepository) {
        this.repository = repository;
        this.userRepository = userRepository;
        this.contasRepository = contasRepository;
    }

    protected abstract T createEmpty();

    private Long getAuthenticatedUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        JWTUserData jwtData = (JWTUserData) authentication.getPrincipal();
        return jwtData.userId();
    }

    private void checkOwnership(T carteira, Long userId) {
        if (carteira.getUser() == null || !carteira.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("Carteira com ID " + carteira.getId() + " não encontrada");
        }
    }

    private CarteiraResponseDTO toResponse(T carteira) {
        Long userId = (carteira.getUser() != null) ? carteira.getUser().getId() : null;
        return new CarteiraResponseDTO(carteira.getId(), carteira.getNome(), carteira.getMoeda(), userId);
    }

    public Page<CarteiraResponseDTO> getAll(Pageable pageable) {
        Long userId = getAuthenticatedUserId();
        return repository.findByUserId(userId, pageable)
                .map(this::toResponse);
    }

    public CarteiraResponseDTO getById(Long id) {
        Long userId = getAuthenticatedUserId();
        T carteira = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Carteira com ID " + id + " não encontrada"));
        checkOwnership(carteira, userId);
        return toResponse(carteira);
    }

    public CarteiraResponseDTO create(CarteiraRequestDTO dto) {
        Long userId = getAuthenticatedUserId();
        UserModel user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário com ID " + userId + " não encontrado"));

        T carteira = createEmpty();
        carteira.setNome(dto.nome());
        carteira.setMoeda(dto.moeda().toUpperCase());
        carteira.setUser(user);

        T saved = repository.save(carteira);
        return toResponse(saved);
    }

    public CarteiraResponseDTO update(Long id, CarteiraRequestDTO dto) {
        Long userId = getAuthenticatedUserId();
        T carteira = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Carteira com ID " + id + " não encontrada para alteração"));
        checkOwnership(carteira, userId);

        if (dto.nome() != null && !dto.nome().isBlank()) {
            carteira.setNome(dto.nome());
        }
        if (dto.moeda() != null && !dto.moeda().isBlank()) {
            carteira.setMoeda(dto.moeda().toUpperCase());
        }

        T updated = repository.save(carteira);
        return toResponse(updated);
    }

    @Transactional
    public void delete(Long id) {
        Long userId = getAuthenticatedUserId();
        T carteira = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Carteira com ID " + id + " não encontrada para exclusão"));
        checkOwnership(carteira, userId);

        // Exclui as contas associadas antes para evitar violação de FK
        if (carteira instanceof CarteiraInvestimentoModel) {
            contasRepository.deleteAll(contasRepository.findByCarteiraInvestimentoId(id));
        } else {
            contasRepository.deleteAll(contasRepository.findByCarteiraDividasId(id));
        }

        repository.delete(carteira);
    }
}
