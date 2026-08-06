package dev.LzGuimaraes.FocusLifeHub.Despesa;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import dev.LzGuimaraes.FocusLifeHub.Carteira.CarteiraDividasModel;
import dev.LzGuimaraes.FocusLifeHub.Carteira.CarteiraDividasRepository;
import dev.LzGuimaraes.FocusLifeHub.Despesa.dto.DespesaLogResponseDTO;
import dev.LzGuimaraes.FocusLifeHub.Despesa.dto.DespesaRequestDTO;
import dev.LzGuimaraes.FocusLifeHub.Despesa.dto.DespesaResponseDTO;
import dev.LzGuimaraes.FocusLifeHub.Exceptions.ResourceNotFoundException;
import dev.LzGuimaraes.FocusLifeHub.config.JWTUserData;

@Service
public class DespesaService {

    private final DespesaRepository despesaRepository;
    private final DespesaLogRepository despesaLogRepository;
    private final CarteiraDividasRepository carteiraDividasRepository;

    public DespesaService(DespesaRepository despesaRepository,
                          DespesaLogRepository despesaLogRepository,
                          CarteiraDividasRepository carteiraDividasRepository) {
        this.despesaRepository = despesaRepository;
        this.despesaLogRepository = despesaLogRepository;
        this.carteiraDividasRepository = carteiraDividasRepository;
    }

    private Long getAuthenticatedUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        JWTUserData jwtData = (JWTUserData) authentication.getPrincipal();
        return jwtData.userId();
    }

    private CarteiraDividasModel resolveCarteira(Long carteiraId, Long userId) {
        CarteiraDividasModel carteira = carteiraDividasRepository.findById(carteiraId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Carteira de Dívidas com ID " + carteiraId + " não encontrada"));
        if (carteira.getUser() == null || !carteira.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("Carteira de Dívidas com ID " + carteiraId + " não encontrada");
        }
        return carteira;
    }

    private void checkOwnership(DespesaModel despesa, Long userId) {
        CarteiraDividasModel carteira = despesa.getCarteiraDividas();
        if (carteira == null || carteira.getUser() == null
                || !carteira.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("Despesa com ID " + despesa.getId() + " não encontrada");
        }
    }

    private DespesaResponseDTO toResponse(DespesaModel despesa) {
        Long carteiraId = (despesa.getCarteiraDividas() != null) ? despesa.getCarteiraDividas().getId() : null;
        return new DespesaResponseDTO(
            despesa.getId(),
            despesa.getNome(),
            despesa.getSaldo(),
            despesa.getDataVencimento(),
            despesa.getPago(),
            carteiraId
        );
    }

    public Page<DespesaResponseDTO> getAll(Pageable pageable) {
        Long userId = getAuthenticatedUserId();
        return despesaRepository.findByCarteiraDividas_UserId(userId, pageable)
                .map(this::toResponse);
    }

    public DespesaResponseDTO getById(Long id) {
        Long userId = getAuthenticatedUserId();
        DespesaModel despesa = despesaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Despesa com ID " + id + " não encontrada"));
        checkOwnership(despesa, userId);
        return toResponse(despesa);
    }

    public List<DespesaResponseDTO> getByCarteira(Long carteiraId) {
        Long userId = getAuthenticatedUserId();
        resolveCarteira(carteiraId, userId);
        return despesaRepository.findByCarteiraDividasId(carteiraId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public Page<DespesaLogResponseDTO> getLogs(Long id, int page, int size) {
        Long userId = getAuthenticatedUserId();
        DespesaModel despesa = despesaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Despesa com ID " + id + " não encontrada"));
        checkOwnership(despesa, userId);

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "criadoEm"));
        return despesaLogRepository.findByContaId(id, pageable)
                .map(log -> new DespesaLogResponseDTO(log.getId(), log.getAcao(), log.getCriadoEm()));
    }

    public DespesaResponseDTO create(DespesaRequestDTO dto) {
        Long userId = getAuthenticatedUserId();
        CarteiraDividasModel carteira = resolveCarteira(dto.carteira_dividas_id(), userId);

        DespesaModel despesa = new DespesaModel();
        despesa.setNome(dto.nome());
        despesa.setSaldo(Math.abs(dto.saldo() != null ? dto.saldo() : 0f));
        despesa.setDataVencimento(dto.dataVencimento());
        despesa.setPago(dto.pago() != null && dto.pago());
        despesa.setCarteiraDividas(carteira);

        return toResponse(despesaRepository.save(despesa));
    }

    public DespesaResponseDTO update(Long id, DespesaRequestDTO dto) {
        Long userId = getAuthenticatedUserId();
        DespesaModel despesa = despesaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Despesa com ID " + id + " não encontrada para alteração"));
        checkOwnership(despesa, userId);

        if (dto.nome() != null && !dto.nome().isBlank()) {
            despesa.setNome(dto.nome());
        }
        if (dto.saldo() != null) {
            despesa.setSaldo(Math.abs(dto.saldo()));
        }
        if (dto.dataVencimento() != null) {
            despesa.setDataVencimento(dto.dataVencimento());
        }
        if (dto.pago() != null) {
            despesa.setPago(dto.pago());
        }
        if (dto.carteira_dividas_id() != null
                && (despesa.getCarteiraDividas() == null
                    || !despesa.getCarteiraDividas().getId().equals(dto.carteira_dividas_id()))) {
            despesa.setCarteiraDividas(resolveCarteira(dto.carteira_dividas_id(), userId));
        }

        return toResponse(despesaRepository.save(despesa));
    }

    public void delete(Long id) {
        Long userId = getAuthenticatedUserId();
        DespesaModel despesa = despesaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Despesa com ID " + id + " não encontrada para exclusão"));
        checkOwnership(despesa, userId);
        despesaRepository.deleteById(id);
    }
}
