package dev.LzGuimaraes.FocusLifeHub.Contas;

import java.time.LocalDate;
import java.util.ArrayList;
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
import dev.LzGuimaraes.FocusLifeHub.Carteira.CarteiraInvestimentoModel;
import dev.LzGuimaraes.FocusLifeHub.Carteira.CarteiraInvestimentoRepository;
import dev.LzGuimaraes.FocusLifeHub.Carteira.CarteiraModel;
import dev.LzGuimaraes.FocusLifeHub.Contas.dto.ContaLogResponseDTO;
import dev.LzGuimaraes.FocusLifeHub.Contas.dto.ContasRequestDTO;
import dev.LzGuimaraes.FocusLifeHub.Contas.dto.ContasResponseDTO;
import dev.LzGuimaraes.FocusLifeHub.Exceptions.ResourceNotFoundException;
import dev.LzGuimaraes.FocusLifeHub.config.JWTUserData;

@Service
public class ContasService {

    private final ContasRepository contasRepository;
    private final CarteiraInvestimentoRepository carteiraInvestimentoRepository;
    private final CarteiraDividasRepository carteiraDividasRepository;
    private final ContaLogRepository contaLogRepository;
    private final ContasMapper contasMapper;

    public ContasService(
            ContasRepository contasRepository,
            CarteiraInvestimentoRepository carteiraInvestimentoRepository,
            CarteiraDividasRepository carteiraDividasRepository,
            ContaLogRepository contaLogRepository,
            ContasMapper contasMapper) {
        this.contasRepository = contasRepository;
        this.carteiraInvestimentoRepository = carteiraInvestimentoRepository;
        this.carteiraDividasRepository = carteiraDividasRepository;
        this.contaLogRepository = contaLogRepository;
        this.contasMapper = contasMapper;
    }

    private Long getAuthenticatedUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        JWTUserData jwtData = (JWTUserData) authentication.getPrincipal();
        return jwtData.userId();
    }

    private boolean pertenceAoUsuario(ContasModel conta, Long userId) {
        CarteiraModel carteira = conta.getCarteiraAtiva();
        return carteira != null && carteira.getUser() != null
                && carteira.getUser().getId().equals(userId);
    }

    /**
     * Resolve a carteira informada no DTO (exatamente uma das duas colunas
     * dedicadas) e valida que pertence ao usuário autenticado.
     */
    private CarteiraModel resolveCarteira(ContasRequestDTO dto, Long userId) {
        boolean temInvestimento = dto.carteira_investimento_id() != null;
        boolean temDividas = dto.carteira_dividas_id() != null;

        if (temInvestimento == temDividas) {
            throw new IllegalArgumentException(
                    "Informe exatamente uma carteira (carteira_investimento_id OU carteira_dividas_id).");
        }

        if (temInvestimento) {
            Long id = dto.carteira_investimento_id();
            CarteiraInvestimentoModel carteira = carteiraInvestimentoRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Carteira de Investimento com ID " + id + " não encontrada"));
            if (carteira.getUser() == null || !carteira.getUser().getId().equals(userId)) {
                throw new ResourceNotFoundException("Carteira de Investimento com ID " + id + " não encontrada");
            }
            return carteira;
        }

        Long id = dto.carteira_dividas_id();
        CarteiraDividasModel carteira = carteiraDividasRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Carteira de Dívidas com ID " + id + " não encontrada"));
        if (carteira.getUser() == null || !carteira.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("Carteira de Dívidas com ID " + id + " não encontrada");
        }
        return carteira;
    }

    public Page<ContasResponseDTO> getAllContas(Pageable pageable) {
        Long userId = getAuthenticatedUserId();
        return contasRepository.findByCarteiraInvestimento_UserIdOrCarteiraDividas_UserId(userId, userId, pageable)
                .map(contasMapper::toResponse);
    }

    public ContasResponseDTO getContaById(Long id) {
        Long userId = getAuthenticatedUserId();
        ContasModel conta = contasRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Conta com ID " + id + " não encontrada"));

        if (!pertenceAoUsuario(conta, userId)) {
            throw new ResourceNotFoundException("Conta com ID " + id + " não encontrada");
        }

        return contasMapper.toResponse(conta);
    }

    public List<ContasResponseDTO> getContasByCarteiraInvestimentoId(Long carteiraId) {
        Long userId = getAuthenticatedUserId();
        CarteiraInvestimentoModel carteira = carteiraInvestimentoRepository.findById(carteiraId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Carteira de Investimento com ID " + carteiraId + " não encontrada"));
        if (carteira.getUser() == null || !carteira.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("Carteira de Investimento com ID " + carteiraId + " não encontrada");
        }

        return contasRepository.findByCarteiraInvestimentoId(carteiraId)
                .stream()
                .map(contasMapper::toResponse)
                .collect(Collectors.toList());
    }

    public List<ContasResponseDTO> getContasByCarteiraDividasId(Long carteiraId) {
        Long userId = getAuthenticatedUserId();
        CarteiraDividasModel carteira = carteiraDividasRepository.findById(carteiraId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Carteira de Dívidas com ID " + carteiraId + " não encontrada"));
        if (carteira.getUser() == null || !carteira.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("Carteira de Dívidas com ID " + carteiraId + " não encontrada");
        }

        return contasRepository.findByCarteiraDividasId(carteiraId)
                .stream()
                .map(contasMapper::toResponse)
                .collect(Collectors.toList());
    }

    public List<ContasResponseDTO> getContasVencendo(LocalDate data) {
        Long userId = getAuthenticatedUserId();
        List<ContasModel> contas = new ArrayList<>();
        contas.addAll(contasRepository.findByCarteiraInvestimento_UserIdAndPagoFalseAndDataVencimento(userId, data));
        contas.addAll(contasRepository.findByCarteiraDividas_UserIdAndPagoFalseAndDataVencimento(userId, data));

        return contas.stream()
                .map(contasMapper::toResponse)
                .collect(Collectors.toList());
    }

    public Page<ContaLogResponseDTO> getContaLogs(Long id, int page, int size) {
        Long userId = getAuthenticatedUserId();
        ContasModel conta = contasRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Conta com ID " + id + " não encontrada"));

        if (!pertenceAoUsuario(conta, userId)) {
            throw new ResourceNotFoundException("Conta com ID " + id + " não encontrada");
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "criadoEm"));
        return contaLogRepository.findByContaId(id, pageable)
                .map(log -> new ContaLogResponseDTO(log.getId(), log.getAcao(), log.getCriadoEm()));
    }

    public ContasResponseDTO createConta(ContasRequestDTO dto) {
        Long userId = getAuthenticatedUserId();
        CarteiraModel carteira = resolveCarteira(dto, userId);

        // Validar compatibilidade: Dívidas só aceita CONTA, Investimento só aceita INVESTIMENTO
        if (carteira instanceof CarteiraDividasModel && dto.categoria() != CategoriaAtivo.CONTA) {
            throw new IllegalArgumentException("Carteiras de Dívidas só aceitam ativos do tipo CONTA.");
        }
        if (carteira instanceof CarteiraInvestimentoModel && dto.categoria() != CategoriaAtivo.INVESTIMENTO) {
            throw new IllegalArgumentException("Carteiras de Investimento só aceitam ativos do tipo INVESTIMENTO.");
        }

        ContasModel ativo = contasMapper.toModel(dto, carteira);

        // Saldo atual = Preço Atual × Quantidade (se preço atual informado); senão Preço Médio × Quantidade
        if (dto.categoria() == CategoriaAtivo.INVESTIMENTO
                && dto.categoriaInvestimento() != null
                && dto.quantidade() != null && dto.valorUnitario() != null) {
            float qtd = dto.quantidade();
            float preco = dto.precoAtual() != null ? dto.precoAtual() : dto.valorUnitario();
            ativo.setSaldo(preco * qtd);
        } else {
            float saldoInicial = dto.saldo() != null ? dto.saldo() : 0f;
            ativo.setSaldo(Math.abs(saldoInicial));
        }

        // Só seta pago se for CONTA
        if (dto.categoria() == CategoriaAtivo.CONTA) {
            ativo.setPago(dto.pago() != null && dto.pago());
        } else {
            ativo.setPago(null);
        }

        ContasModel saved = contasRepository.save(ativo);
        return contasMapper.toResponse(saved);
    }

    public ContasResponseDTO updateConta(Long id, ContasRequestDTO dto) {
        Long userId = getAuthenticatedUserId();
        ContasModel ativo = contasRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ativo com ID " + id + " não encontrado para alteração"));

        if (!pertenceAoUsuario(ativo, userId)) {
            throw new ResourceNotFoundException("Ativo com ID " + id + " não encontrado para alteração");
        }

        if (dto.nome() != null && !dto.nome().isBlank()) {
            ativo.setNome(dto.nome());
        }

        if (dto.categoria() != null) {
            ativo.setCategoria(dto.categoria());
        }

        ativo.setCategoriaInvestimento(dto.categoriaInvestimento());
        ativo.setQuantidade(dto.quantidade());
        ativo.setValorUnitario(dto.valorUnitario());
        ativo.setPrecoAtual(dto.precoAtual());
        ativo.setInstituicao(dto.instituicao());
        ativo.setDataAplicacao(dto.dataAplicacao());
        ativo.setVencimento(dto.vencimento());
        ativo.setDataVencimento(dto.dataVencimento());
        ativo.setRentabilidade(dto.rentabilidade());

        // Saldo atual = Preço Atual × Quantidade (se preço atual informado); senão Preço Médio × Quantidade
        if (dto.categoria() == CategoriaAtivo.INVESTIMENTO
                && dto.categoriaInvestimento() != null
                && dto.quantidade() != null && dto.valorUnitario() != null) {
            float qtd = dto.quantidade();
            float preco = dto.precoAtual() != null ? dto.precoAtual() : dto.valorUnitario();
            ativo.setSaldo(preco * qtd);
        } else if (dto.saldo() != null) {
            ativo.setSaldo(Math.abs(dto.saldo()));
        }

        if (dto.pago() != null && dto.categoria() == CategoriaAtivo.CONTA) {
            ativo.setPago(dto.pago());
        } else if (dto.categoria() == CategoriaAtivo.INVESTIMENTO) {
            ativo.setPago(null);
        }

        // Reatribuição de carteira (apenas se exatamente uma foi informada)
        boolean temInvestimento = dto.carteira_investimento_id() != null;
        boolean temDividas = dto.carteira_dividas_id() != null;
        if (temInvestimento != temDividas) {
            CarteiraModel nova = resolveCarteira(dto, userId);
            if (nova instanceof CarteiraInvestimentoModel) {
                if (ativo.getCarteiraInvestimento() == null
                        || !ativo.getCarteiraInvestimento().getId().equals(nova.getId())) {
                    ativo.setCarteiraInvestimento((CarteiraInvestimentoModel) nova);
                    ativo.setCarteiraDividas(null);
                }
            } else {
                if (ativo.getCarteiraDividas() == null
                        || !ativo.getCarteiraDividas().getId().equals(nova.getId())) {
                    ativo.setCarteiraDividas((CarteiraDividasModel) nova);
                    ativo.setCarteiraInvestimento(null);
                }
            }
        }

        ContasModel updated = contasRepository.save(ativo);
        return contasMapper.toResponse(updated);
    }

    public void deleteConta(Long id) {
        Long userId = getAuthenticatedUserId();

        ContasModel conta = contasRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Conta com ID " + id + " não encontrada para exclusão"));

        if (!pertenceAoUsuario(conta, userId)) {
            throw new ResourceNotFoundException("Conta com ID " + id + " não encontrada para exclusão");
        }

        contasRepository.deleteById(id);
    }
}
