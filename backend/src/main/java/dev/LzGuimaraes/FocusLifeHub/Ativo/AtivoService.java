package dev.LzGuimaraes.FocusLifeHub.Ativo;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import dev.LzGuimaraes.FocusLifeHub.Ativo.dto.AtivoRequestDTO;
import dev.LzGuimaraes.FocusLifeHub.Ativo.dto.AtivoResponseDTO;
import dev.LzGuimaraes.FocusLifeHub.AtivoCadastro.AtivoCadastroModel;
import dev.LzGuimaraes.FocusLifeHub.AtivoCadastro.AtivoCadastroRepository;
import dev.LzGuimaraes.FocusLifeHub.AtivoCadastro.TipoAtivoCadastro;
import dev.LzGuimaraes.FocusLifeHub.AtivoCadastro.dto.AtivoCadastroSyncDTO;
import dev.LzGuimaraes.FocusLifeHub.Carteira.CarteiraInvestimentoModel;
import dev.LzGuimaraes.FocusLifeHub.Carteira.CarteiraInvestimentoRepository;
import dev.LzGuimaraes.FocusLifeHub.Exceptions.ResourceNotFoundException;
import dev.LzGuimaraes.FocusLifeHub.config.JWTUserData;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AtivoService {
    private static final Logger log = LoggerFactory.getLogger(AtivoService.class);

    private final AtivoRepository ativoRepository;
    private final CarteiraInvestimentoRepository carteiraInvestimentoRepository;
    private final AtivoCadastroRepository ativoCadastroRepository;

    public AtivoService(AtivoRepository ativoRepository,
                        CarteiraInvestimentoRepository carteiraInvestimentoRepository,
                        AtivoCadastroRepository ativoCadastroRepository) {
        this.ativoRepository = ativoRepository;
        this.carteiraInvestimentoRepository = carteiraInvestimentoRepository;
        this.ativoCadastroRepository = ativoCadastroRepository;
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

    private AtivoCadastroModel resolveAtivoCadastro(UUID ativoCadastroId) {
        return ativoCadastroRepository.findById(ativoCadastroId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Ativo do catálogo com ID " + ativoCadastroId + " não encontrado"));
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
        UUID ativoCadastroId = (ativo.getAtivoCadastro() != null) ? ativo.getAtivoCadastro().getId() : null;

        // Preço atual "ao vivo": usa o preço do catálogo (fonte da verdade)
        // quando o card está vinculado; senão, mantém o preço guardado no card.
        Float precoAtual = ativo.getPrecoAtual();
        if (ativo.getAtivoCadastro() != null && ativo.getAtivoCadastro().getPrecoAtual() != null) {
            precoAtual = ativo.getAtivoCadastro().getPrecoAtual();
        }

        return new AtivoResponseDTO(
            ativo.getId(),
            ativo.getNome(),
            ativo.getCategoriaInvestimento(),
            ativo.getQuantidade(),
            ativo.getValorUnitario(),
            precoAtual,
            ativo.getSaldo(),
            ativo.getInstituicao(),
            ativo.getDataAplicacao(),
            ativo.getVencimento(),
            ativo.getDataVencimento(),
            ativo.getRentabilidade(),
            ativoCadastroId,
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
        if (dto.ativo_cadastro_id() != null) {
            ativo.setAtivoCadastro(resolveAtivoCadastro(dto.ativo_cadastro_id()));
        }

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

        // Reatribuição do ativo do catálogo
        if (dto.ativo_cadastro_id() != null
                && (ativo.getAtivoCadastro() == null
                    || !ativo.getAtivoCadastro().getId().equals(dto.ativo_cadastro_id()))) {
            ativo.setAtivoCadastro(resolveAtivoCadastro(dto.ativo_cadastro_id()));
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

    @Transactional
    public void deleteAllAtivos() {
        ativoRepository.deleteAllInBatch();
    }

    @Transactional
    public void bulkUpdatePrices(List<AtivoPriceUpdate> updates) {
        if (updates == null || updates.isEmpty()) return;
        Map<Long, Float> map = updates.stream()
                .filter(u -> u.ativoId != null && u.precoAtual != null)
                .collect(Collectors.toMap(u -> u.ativoId, u -> u.precoAtual));
        if (map.isEmpty()) return;
        Set<Long> ids = map.keySet();
        List<AtivoModel> ativos = ativoRepository.findAllById(ids);
        for (AtivoModel ativo : ativos) {
            Float novo = map.get(ativo.getId());
            if (novo != null) {
                ativo.setPrecoAtual(novo);
                if (ativo.getQuantidade() != null) {
                    ativo.setSaldo(novo * ativo.getQuantidade());
                }
            }
        }
        ativoRepository.saveAll(ativos);
    }

    /**
     * Sincroniza o catálogo de ativos (ativo_cadastro) e propaga o novo preço
     * para as posições (ativo) que referenciam cada ativo do catálogo,
     * recalculando o saldo (precoAtual × quantidade).
     */
    @Transactional
    public Map<String, Integer> syncCatalogo(List<AtivoCadastroSyncDTO> payload) {
        int received = (payload == null) ? 0 : payload.size();
        int created = 0;
        int updated = 0;
        int invalid = 0;

        if (payload == null || payload.isEmpty()) {
            return Map.of("received", received, "created", created, "updated", updated, "invalid", invalid);
        }

        for (AtivoCadastroSyncDTO dto : payload) {
            if (dto == null || dto.getNome() == null || dto.getNome().isBlank() || dto.getTipo() == null) {
                invalid++;
                continue;
            }

            String nome = dto.getNome().trim();
            TipoAtivoCadastro tipo;
            try {
                tipo = TipoAtivoCadastro.valueOf(dto.getTipo().trim().toUpperCase());
            } catch (Exception ex) {
                invalid++;
                log.warn("Tipo inválido para ativo '{}' : {}", nome, dto.getTipo());
                continue;
            }

            AtivoCadastroModel cadastro;
            Optional<AtivoCadastroModel> opt = ativoCadastroRepository.findByNomeIgnoreCase(nome);
            if (opt.isPresent()) {
                cadastro = opt.get();
                cadastro.setTipo(tipo);
                if (dto.getPrecoAtual() != null) {
                    cadastro.setPrecoAtual(dto.getPrecoAtual());
                }
                cadastro = ativoCadastroRepository.save(cadastro);
                updated++;
            } else {
                AtivoCadastroModel novo = new AtivoCadastroModel();
                novo.setNome(nome);
                novo.setTipo(tipo);
                novo.setPrecoAtual(dto.getPrecoAtual());
                cadastro = ativoCadastroRepository.save(novo);
                created++;
            }

            // Propaga o novo preço para as posições (cards) que usam este ativo do catálogo
            if (dto.getPrecoAtual() != null) {
                List<AtivoModel> posicoes = ativoRepository.findByAtivoCadastroId(cadastro.getId());
                for (AtivoModel posicao : posicoes) {
                    posicao.setPrecoAtual(dto.getPrecoAtual());
                    if (posicao.getQuantidade() != null) {
                        posicao.setSaldo(dto.getPrecoAtual() * posicao.getQuantidade());
                    }
                }
                if (!posicoes.isEmpty()) {
                    ativoRepository.saveAll(posicoes);
                }
            }
        }

        log.info("/ativos/admin/sync: received={}, created={}, updated={}, invalid={}", received, created, updated, invalid);
        return Map.of("received", received, "created", created, "updated", updated, "invalid", invalid);
    }
}
