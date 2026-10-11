package dev.LzGuimaraes.FocusLifeHub.Investimento.ComprasVendas;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.LzGuimaraes.FocusLifeHub.Ativo.AtivoModel;
import dev.LzGuimaraes.FocusLifeHub.Ativo.AtivoRepository;
import dev.LzGuimaraes.FocusLifeHub.AtivoCadastro.AtivoCadastroModel;
import dev.LzGuimaraes.FocusLifeHub.AtivoCadastro.AtivoCadastroRepository;
import dev.LzGuimaraes.FocusLifeHub.Exceptions.BusinessRuleException;
import dev.LzGuimaraes.FocusLifeHub.Exceptions.ResourceNotFoundException;
import dev.LzGuimaraes.FocusLifeHub.Investimento.ComprasVendas.dto.AtivoPrecoAlvoRequestDTO;
import dev.LzGuimaraes.FocusLifeHub.Investimento.ComprasVendas.dto.AtivoPrecoAlvoResponseDTO;
import dev.LzGuimaraes.FocusLifeHub.Planejamento.comum.ContextoUsuario;

/**
 * Regras de PREÇO TETO / PREÇO MÍNIMO por usuário+ativo.
 *
 * A UI sinaliza quando o preço atual está acima do teto ou abaixo do mínimo;
 * quem decide isso é o backend (as flags vêm prontas no DTO). A regra é por
 * catálogo OU por posição (nunca os dois), e há um UPSERT que reaproveita a
 * regra existente para o mesmo ativo.
 */
@Service
public class AtivoPrecoAlvoService {

    private final AtivoPrecoAlvoRepository repository;
    private final ContextoUsuario contextoUsuario;
    private final AtivoCadastroRepository ativoCadastroRepository;
    private final AtivoRepository ativoRepository;

    public AtivoPrecoAlvoService(AtivoPrecoAlvoRepository repository,
                                 ContextoUsuario contextoUsuario,
                                 AtivoCadastroRepository ativoCadastroRepository,
                                 AtivoRepository ativoRepository) {
        this.repository = repository;
        this.contextoUsuario = contextoUsuario;
        this.ativoCadastroRepository = ativoCadastroRepository;
        this.ativoRepository = ativoRepository;
    }

    @Transactional(readOnly = true)
    public List<AtivoPrecoAlvoResponseDTO> listar() {
        Long userId = contextoUsuario.id();
        List<AtivoPrecoAlvoModel> regras = repository.findByUserIdOrderByIdDesc(userId);
        Map<UUID, BigDecimal> catalogo = precosCatalogo(regras);
        Map<Long, BigDecimal> posicao = precosPosicao(regras, userId);
        return regras.stream().map(r -> toResponse(r, catalogo, posicao)).toList();
    }

    @Transactional
    public AtivoPrecoAlvoResponseDTO create(AtivoPrecoAlvoRequestDTO dto) {
        Long userId = contextoUsuario.id();
        validar(dto);
        if (regraExistente(userId, dto).isPresent()) {
            throw new BusinessRuleException(
                    "Já existe uma regra de preço para este ativo. Edite a regra existente.");
        }
        AtivoPrecoAlvoModel regra = new AtivoPrecoAlvoModel();
        regra.setUserId(userId);
        return salvar(regra, dto, userId);
    }

    @Transactional
    public AtivoPrecoAlvoResponseDTO update(Long id, AtivoPrecoAlvoRequestDTO dto) {
        Long userId = contextoUsuario.id();
        validar(dto);
        AtivoPrecoAlvoModel regra = repository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Regra de preço com ID " + id + " não encontrada"));
        return salvar(regra, dto, userId);
    }

    @Transactional
    public void delete(Long id) {
        Long userId = contextoUsuario.id();
        AtivoPrecoAlvoModel regra = repository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Regra de preço com ID " + id + " não encontrada"));
        repository.delete(regra);
    }

    /** Cria a regra se não existir, ou atualiza a existente do mesmo ativo. */
    @Transactional
    public AtivoPrecoAlvoResponseDTO upsert(AtivoPrecoAlvoRequestDTO dto) {
        Long userId = contextoUsuario.id();
        validar(dto);
        AtivoPrecoAlvoModel regra = regraExistente(userId, dto)
                .orElseGet(() -> {
                    AtivoPrecoAlvoModel nova = new AtivoPrecoAlvoModel();
                    nova.setUserId(userId);
                    return nova;
                });
        return salvar(regra, dto, userId);
    }

    /* ── Apoio ── */

    private AtivoPrecoAlvoResponseDTO salvar(AtivoPrecoAlvoModel regra, AtivoPrecoAlvoRequestDTO dto,
                                             Long userId) {
        if (dto.ativo_cadastro_id() != null) {
            AtivoCadastroModel catalogo = ativoCadastroRepository.findById(dto.ativo_cadastro_id())
                    .orElseThrow(() -> new ResourceNotFoundException("Ativo do catálogo não encontrado"));
            regra.setAtivoCadastroId(catalogo.getId());
            regra.setAtivoId(null);
            regra.setTicker(catalogo.getNome());
        } else {
            AtivoModel posicao = ativoRepository.findById(dto.ativo_id())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Posição com ID " + dto.ativo_id() + " não encontrada"));
            if (!posicaoPertenceAoUsuario(posicao, userId)) {
                throw new ResourceNotFoundException("Posição com ID " + dto.ativo_id() + " não encontrada");
            }
            regra.setAtivoId(posicao.getId());
            regra.setAtivoCadastroId(null);
            regra.setTicker(textoOuNulo(dto.ticker()) != null ? dto.ticker().trim() : posicao.getNome());
        }
        regra.setPrecoTeto(dto.preco_teto());
        regra.setPrecoMinimo(dto.preco_minimo());
        if (regra.getCreatedAt() == null) {
            regra.setCreatedAt(LocalDateTime.now());
        }
        regra.setUpdatedAt(LocalDateTime.now());

        AtivoPrecoAlvoModel salva = repository.save(regra);
        return toResponse(salva, precosCatalogo(List.of(salva)), precosPosicao(List.of(salva), userId));
    }

    private java.util.Optional<AtivoPrecoAlvoModel> regraExistente(Long userId, AtivoPrecoAlvoRequestDTO dto) {
        if (dto.ativo_cadastro_id() != null) {
            return repository.findByUserIdAndAtivoCadastroId(userId, dto.ativo_cadastro_id());
        }
        return repository.findByUserIdAndAtivoId(userId, dto.ativo_id());
    }

    private void validar(AtivoPrecoAlvoRequestDTO dto) {
        boolean porCatalogo = dto.ativo_cadastro_id() != null;
        boolean porPosicao = dto.ativo_id() != null;
        if (porCatalogo == porPosicao) {
            throw new BusinessRuleException(
                    "Informe o ativo do catálogo OU a posição (exatamente um dos dois).");
        }
        if (dto.preco_teto() == null && dto.preco_minimo() == null) {
            throw new BusinessRuleException("Informe o preço teto, o preço mínimo, ou ambos.");
        }
        if (dto.preco_teto() != null && dto.preco_teto().signum() <= 0) {
            throw new BusinessRuleException("O preço teto deve ser maior que zero.");
        }
        if (dto.preco_minimo() != null && dto.preco_minimo().signum() <= 0) {
            throw new BusinessRuleException("O preço mínimo deve ser maior que zero.");
        }
        if (dto.preco_teto() != null && dto.preco_minimo() != null
                && dto.preco_minimo().compareTo(dto.preco_teto()) > 0) {
            throw new BusinessRuleException("O preço mínimo não pode ser maior que o preço teto.");
        }
    }

    private AtivoPrecoAlvoResponseDTO toResponse(AtivoPrecoAlvoModel regra,
                                                 Map<UUID, BigDecimal> catalogo,
                                                 Map<Long, BigDecimal> posicao) {
        BigDecimal precoAtual = precoAtual(regra, catalogo, posicao);
        boolean acimaTeto = precoAtual != null && regra.getPrecoTeto() != null
                && precoAtual.compareTo(regra.getPrecoTeto()) >= 0;
        boolean abaixoMinimo = precoAtual != null && regra.getPrecoMinimo() != null
                && precoAtual.compareTo(regra.getPrecoMinimo()) <= 0;
        return new AtivoPrecoAlvoResponseDTO(
                regra.getId(), regra.getAtivoCadastroId(), regra.getAtivoId(), regra.getTicker(),
                regra.getPrecoTeto(), regra.getPrecoMinimo(), precoAtual,
                acimaTeto, abaixoMinimo, regra.getUpdatedAt());
    }

    private BigDecimal precoAtual(AtivoPrecoAlvoModel regra, Map<UUID, BigDecimal> catalogo,
                                  Map<Long, BigDecimal> posicao) {
        if (regra.getAtivoCadastroId() != null) {
            return catalogo.get(regra.getAtivoCadastroId());
        }
        return (regra.getAtivoId() != null) ? posicao.get(regra.getAtivoId()) : null;
    }

    private Map<UUID, BigDecimal> precosCatalogo(Collection<AtivoPrecoAlvoModel> regras) {
        Set<UUID> ids = regras.stream().map(AtivoPrecoAlvoModel::getAtivoCadastroId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        if (ids.isEmpty()) {
            return Map.of();
        }
        Map<UUID, BigDecimal> mapa = new HashMap<>();
        for (AtivoCadastroModel a : ativoCadastroRepository.findAllById(ids)) {
            if (a.getPrecoAtual() != null) {
                mapa.put(a.getId(), BigDecimal.valueOf(a.getPrecoAtual()));
            }
        }
        return mapa;
    }

    private Map<Long, BigDecimal> precosPosicao(Collection<AtivoPrecoAlvoModel> regras, Long userId) {
        Set<Long> ids = regras.stream().map(AtivoPrecoAlvoModel::getAtivoId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        if (ids.isEmpty()) {
            return Map.of();
        }
        Map<Long, BigDecimal> mapa = new HashMap<>();
        for (AtivoModel a : ativoRepository.findAllById(ids)) {
            if (!posicaoPertenceAoUsuario(a, userId)) {
                continue;
            }
            Float preco = a.getPrecoAtual();
            if (preco == null && a.getAtivoCadastro() != null) {
                preco = a.getAtivoCadastro().getPrecoAtual();
            }
            if (preco != null) {
                mapa.put(a.getId(), BigDecimal.valueOf(preco));
            }
        }
        return mapa;
    }

    private static boolean posicaoPertenceAoUsuario(AtivoModel a, Long userId) {
        return a.getCarteiraInvestimento() != null
                && a.getCarteiraInvestimento().getUser() != null
                && userId.equals(a.getCarteiraInvestimento().getUser().getId());
    }

    private static String textoOuNulo(String v) {
        return (v == null || v.isBlank()) ? null : v.trim();
    }
}
