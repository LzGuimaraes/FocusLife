package dev.LzGuimaraes.FocusLifeHub.Ativo;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import dev.LzGuimaraes.FocusLifeHub.Ativo.dto.AtivoRequestDTO;
import dev.LzGuimaraes.FocusLifeHub.Ativo.dto.AtivoResponseDTO;
import dev.LzGuimaraes.FocusLifeHub.Carteira.CarteiraInvestimentoModel;
import dev.LzGuimaraes.FocusLifeHub.Carteira.CarteiraInvestimentoRepository;
import dev.LzGuimaraes.FocusLifeHub.Exceptions.ResourceNotFoundException;
import dev.LzGuimaraes.FocusLifeHub.config.JWTUserData;

@Service
public class AtivoService {

    private final AtivoRepository ativoRepository;
    private final CarteiraInvestimentoRepository carteiraInvestimentoRepository;

    public AtivoService(AtivoRepository ativoRepository,
                        CarteiraInvestimentoRepository carteiraInvestimentoRepository) {
        this.ativoRepository = ativoRepository;
        this.carteiraInvestimentoRepository = carteiraInvestimentoRepository;
    }

    private Long getAuthenticatedUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        JWTUserData jwtData = (JWTUserData) authentication.getPrincipal();
        return jwtData.userId();
    }

    private CarteiraInvestimentoModel resolveCarteira(Long carteiraId, Long userId) {
        CarteiraInvestimentoModel carteira = carteiraInvestimentoRepository.findById(carteiraId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Carteira de Investimento com ID " + carteiraId + " não encontrada"));
        if (carteira.getUser() == null || !carteira.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("Carteira de Investimento com ID " + carteiraId + " não encontrada");
        }
        return carteira;
    }

    private void checkOwnership(AtivoModel ativo, Long userId) {
        CarteiraInvestimentoModel carteira = ativo.getCarteiraInvestimento();
        if (carteira == null || carteira.getUser() == null
                || !carteira.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("Ativo com ID " + ativo.getId() + " não encontrado");
        }
    }

    private AtivoResponseDTO toResponse(AtivoModel ativo) {
        Long carteiraId = (ativo.getCarteiraInvestimento() != null) ? ativo.getCarteiraInvestimento().getId() : null;
        return new AtivoResponseDTO(
            ativo.getId(),
            ativo.getNome(),
            ativo.getCategoriaInvestimento(),
            ativo.getQuantidade(),
            ativo.getValorUnitario(),
            ativo.getPrecoAtual(),
            ativo.getSaldo(),
            ativo.getInstituicao(),
            ativo.getDataAplicacao(),
            ativo.getVencimento(),
            ativo.getDataVencimento(),
            ativo.getRentabilidade(),
            carteiraId
        );
    }

    public Page<AtivoResponseDTO> getAll(Pageable pageable) {
        Long userId = getAuthenticatedUserId();
        return ativoRepository.findByCarteiraInvestimento_UserId(userId, pageable)
                .map(this::toResponse);
    }

    public AtivoResponseDTO getById(Long id) {
        Long userId = getAuthenticatedUserId();
        AtivoModel ativo = ativoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ativo com ID " + id + " não encontrado"));
        checkOwnership(ativo, userId);
        return toResponse(ativo);
    }

    public List<AtivoResponseDTO> getByCarteira(Long carteiraId) {
        Long userId = getAuthenticatedUserId();
        resolveCarteira(carteiraId, userId);
        return ativoRepository.findByCarteiraInvestimentoId(carteiraId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public AtivoResponseDTO create(AtivoRequestDTO dto) {
        Long userId = getAuthenticatedUserId();
        CarteiraInvestimentoModel carteira = resolveCarteira(dto.carteira_investimento_id(), userId);

        AtivoModel ativo = new AtivoModel();
        ativo.setNome(dto.nome());
        ativo.setCategoriaInvestimento(dto.categoriaInvestimento());
        ativo.setQuantidade(dto.quantidade());
        ativo.setValorUnitario(dto.valorUnitario());
        ativo.setPrecoAtual(dto.precoAtual());
        ativo.setInstituicao(dto.instituicao());
        ativo.setDataAplicacao(dto.dataAplicacao());
        ativo.setVencimento(dto.vencimento());
        ativo.setDataVencimento(dto.dataVencimento());
        ativo.setRentabilidade(dto.rentabilidade());
        ativo.setCarteiraInvestimento(carteira);

        // Saldo atual = Preço Atual × Quantidade (se preço atual informado); senão Preço Médio × Quantidade
        if (dto.quantidade() != null && dto.valorUnitario() != null) {
            float qtd = dto.quantidade();
            float preco = dto.precoAtual() != null ? dto.precoAtual() : dto.valorUnitario();
            ativo.setSaldo(preco * qtd);
        } else {
            float saldoInicial = dto.saldo() != null ? dto.saldo() : 0f;
            ativo.setSaldo(Math.abs(saldoInicial));
        }

        return toResponse(ativoRepository.save(ativo));
    }

    public AtivoResponseDTO update(Long id, AtivoRequestDTO dto) {
        Long userId = getAuthenticatedUserId();
        AtivoModel ativo = ativoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ativo com ID " + id + " não encontrado para alteração"));
        checkOwnership(ativo, userId);

        if (dto.nome() != null && !dto.nome().isBlank()) {
            ativo.setNome(dto.nome());
        }
        if (dto.categoriaInvestimento() != null) {
            ativo.setCategoriaInvestimento(dto.categoriaInvestimento());
        }
        ativo.setQuantidade(dto.quantidade());
        ativo.setValorUnitario(dto.valorUnitario());
        ativo.setPrecoAtual(dto.precoAtual());
        ativo.setInstituicao(dto.instituicao());
        ativo.setDataAplicacao(dto.dataAplicacao());
        ativo.setVencimento(dto.vencimento());
        ativo.setDataVencimento(dto.dataVencimento());
        ativo.setRentabilidade(dto.rentabilidade());

        // Saldo atual = Preço Atual × Quantidade (se preço atual informado); senão Preço Médio × Quantidade
        if (ativo.getQuantidade() != null && ativo.getValorUnitario() != null) {
            float qtd = ativo.getQuantidade();
            float preco = ativo.getPrecoAtual() != null ? ativo.getPrecoAtual() : ativo.getValorUnitario();
            ativo.setSaldo(preco * qtd);
        } else if (dto.saldo() != null) {
            ativo.setSaldo(Math.abs(dto.saldo()));
        }

        // Reatribuição de carteira
        if (dto.carteira_investimento_id() != null
                && (ativo.getCarteiraInvestimento() == null
                    || !ativo.getCarteiraInvestimento().getId().equals(dto.carteira_investimento_id()))) {
            ativo.setCarteiraInvestimento(resolveCarteira(dto.carteira_investimento_id(), userId));
        }

        return toResponse(ativoRepository.save(ativo));
    }

    public void delete(Long id) {
        Long userId = getAuthenticatedUserId();
        AtivoModel ativo = ativoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ativo com ID " + id + " não encontrado para exclusão"));
        checkOwnership(ativo, userId);
        ativoRepository.deleteById(id);
    }
}
