package dev.LzGuimaraes.FocusLifeHub.Despesa;

import java.time.LocalDate;
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
import dev.LzGuimaraes.FocusLifeHub.Exceptions.BusinessRuleException;
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
            despesa.getTipo() != null ? despesa.getTipo() : TipoLancamento.DESPESA,
            despesa.getCategoria(),
            carteiraId
        );
    }

    /**
     * Aplica as regras do tipo de lançamento: um SALDO é um crédito sem
     * vencimento nem status de pagamento; uma DESPESA exige data de vencimento.
     */
    private void aplicarTipo(DespesaModel despesa, TipoLancamento tipo, LocalDate dataVencimento, boolean pago) {
        despesa.setTipo(tipo);
        if (tipo == TipoLancamento.SALDO) {
            despesa.setDataVencimento(null);
            despesa.setPago(null);
            return;
        }
        if (dataVencimento == null) {
            throw new BusinessRuleException("Informe a data de vencimento da despesa.");
        }
        despesa.setDataVencimento(dataVencimento);
        despesa.setPago(pago);
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

        TipoLancamento tipo = dto.tipo() != null ? dto.tipo() : TipoLancamento.DESPESA;

        DespesaModel despesa = new DespesaModel();
        despesa.setNome(dto.nome());
        despesa.setSaldo(Math.abs(dto.saldo() != null ? dto.saldo() : 0f));
        despesa.setCarteiraDividas(carteira);
        aplicarTipo(despesa, tipo, dto.dataVencimento(), dto.pago() != null && dto.pago());
        // Categoria é opcional e só existe para DESPESA (gasto); um SALDO é crédito.
        despesa.setCategoria(tipo == TipoLancamento.DESPESA ? dto.categoria() : null);

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

        // O tipo vigente mantém-se quando o payload não o informa (ex.: toggle de pago).
        TipoLancamento tipo = dto.tipo() != null
                ? dto.tipo()
                : (despesa.getTipo() != null ? despesa.getTipo() : TipoLancamento.DESPESA);

        if (tipo == TipoLancamento.SALDO) {
            aplicarTipo(despesa, TipoLancamento.SALDO, null, false);
        } else {
            despesa.setTipo(TipoLancamento.DESPESA);
            // Categoria (opcional). `categoria` nulo significa "não mexa" (o
            // outro formulário de despesa não envia categoria); para LIMPAR é
            // preciso pedir explicitamente. Evita apagar a categoria em
            // updates parciais (ex.: só alternar "pago").
            if (Boolean.TRUE.equals(dto.removerCategoria())) {
                despesa.setCategoria(null);
            } else if (dto.categoria() != null) {
                despesa.setCategoria(dto.categoria());
            }
            // Alterações parciais (ex.: só marcar como pago) não invalidam a data já gravada.
            if (dto.dataVencimento() != null) {
                despesa.setDataVencimento(dto.dataVencimento());
            }
            if (dto.pago() != null) {
                despesa.setPago(dto.pago());
            }
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
