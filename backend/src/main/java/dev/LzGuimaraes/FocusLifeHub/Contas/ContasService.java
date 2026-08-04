package dev.LzGuimaraes.FocusLifeHub.Contas;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder; 
import org.springframework.stereotype.Service;

import dev.LzGuimaraes.FocusLifeHub.Exceptions.ResourceNotFoundException;
import dev.LzGuimaraes.FocusLifeHub.Contas.dto.ContasRequestDTO;
import dev.LzGuimaraes.FocusLifeHub.Contas.dto.ContasResponseDTO;
import dev.LzGuimaraes.FocusLifeHub.Financas.FinancasModel;
import dev.LzGuimaraes.FocusLifeHub.Financas.FinancasRepository;
import dev.LzGuimaraes.FocusLifeHub.Financas.TipoCarteira;
import dev.LzGuimaraes.FocusLifeHub.config.JWTUserData;

@Service
public class ContasService {

    private final ContasRepository contasRepository;
    private final FinancasRepository financasRepository;
    private final ContasMapper contasMapper;

    public ContasService(
            ContasRepository contasRepository,
            FinancasRepository financasRepository,
            ContasMapper contasMapper) {
        this.contasRepository = contasRepository;
        this.financasRepository = financasRepository;
        this.contasMapper = contasMapper;
    }

    private Long getAuthenticatedUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        JWTUserData jwtData = (JWTUserData) authentication.getPrincipal();
        return jwtData.userId();
    }

    public Page<ContasResponseDTO> getAllContas(Pageable pageable) {
        Long userId = getAuthenticatedUserId();
        return contasRepository.findByFinancas_UserId(userId, pageable)
                .map(contasMapper::toResponse);
    }

    public ContasResponseDTO getContaById(Long id) {
        Long userId = getAuthenticatedUserId();
        ContasModel conta = contasRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Conta com ID " + id + " não encontrada"));

        if (!conta.getFinancas().getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("Conta com ID " + id + " não encontrada");
        }

        return contasMapper.toResponse(conta);
    }

    public List<ContasResponseDTO> getContasByFinancaId(Long financasId) {
        Long userId = getAuthenticatedUserId();
        FinancasModel financa = financasRepository.findById(financasId)
                .orElseThrow(() -> new ResourceNotFoundException("Carteira (Financa) com ID " + financasId + " não encontrada"));

        if (!financa.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("Carteira (Financa) com ID " + financasId + " não encontrada");
        }
        
        return contasRepository.findByFinancasId(financasId)
                .stream()
                .map(contasMapper::toResponse)
                .collect(Collectors.toList());
    }

    public List<ContasResponseDTO> getContasVencendo(LocalDate data) {
        Long userId = getAuthenticatedUserId();
        return contasRepository.findByFinancas_UserIdAndPagoFalseAndDataVencimento(userId, data)
                .stream()
                .map(contasMapper::toResponse)
                .collect(Collectors.toList());
    }

    public ContasResponseDTO createConta(ContasRequestDTO dto) {
        Long userId = getAuthenticatedUserId();

        FinancasModel financa = financasRepository.findById(dto.financas_id())
                .orElseThrow(() -> new ResourceNotFoundException("Carteira (Financa) com ID " + dto.financas_id() + " não encontrada"));

        if (!financa.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("Carteira (Financa) com ID " + dto.financas_id() + " não encontrada");
        }

        ContasModel ativo = contasMapper.toModel(dto, financa);

        // Validar compatibilidade: DESPESAS só aceita CONTA, INVESTIMENTO só aceita INVESTIMENTO
        if (financa.getTipoCarteira() == TipoCarteira.DESPESAS && dto.categoria() != CategoriaAtivo.CONTA) {
            throw new IllegalArgumentException("Carteiras de Despesas só aceitam ativos do tipo CONTA.");
        }
        if (financa.getTipoCarteira() == TipoCarteira.INVESTIMENTO && dto.categoria() != CategoriaAtivo.INVESTIMENTO) {
            throw new IllegalArgumentException("Carteiras de Investimento só aceitam ativos do tipo INVESTIMENTO.");
        }

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

        if (!ativo.getFinancas().getUser().getId().equals(userId)) {
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

        if (dto.financas_id() != null && !dto.financas_id().equals(ativo.getFinancas().getId())) {
            FinancasModel newFinanca = financasRepository.findById(dto.financas_id())
                    .orElseThrow(() -> new ResourceNotFoundException("Nova carteira (Financa) com ID " + dto.financas_id() + " não encontrada"));

            if (!newFinanca.getUser().getId().equals(userId)) {
                throw new ResourceNotFoundException("Nova carteira (Financa) com ID " + dto.financas_id() + " não encontrada");
            }
            ativo.setFinancas(newFinanca);
        }

        ContasModel updated = contasRepository.save(ativo);
        return contasMapper.toResponse(updated);
    }

    public void deleteConta(Long id) {
        Long userId = getAuthenticatedUserId();
        
        ContasModel conta = contasRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Conta com ID " + id + " não encontrada para exclusão"));
        
        if (!conta.getFinancas().getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("Conta com ID " + id + " não encontrada para exclusão");
        }

        contasRepository.deleteById(id);
    }
}