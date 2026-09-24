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
import dev.LzGuimaraes.FocusLifeHub.Exceptions.BusinessRuleException;
import dev.LzGuimaraes.FocusLifeHub.Exceptions.ResourceNotFoundException;
import dev.LzGuimaraes.FocusLifeHub.config.JWTUserData;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AtivoService {
    private static final Logger log = LoggerFactory.getLogger(AtivoService.class);

    private final AtivoRepository ativoRepository;
    private final CarteiraInvestimentoRepository carteiraInvestimentoRepository;
    private final AtivoCadastroRepository ativoCadastroRepository;
    private final dev.LzGuimaraes.FocusLifeHub.Planejamento.CarteiraIdeal.CarteiraIdealSubclasseRepository subclasseRepository;
    private final dev.LzGuimaraes.FocusLifeHub.Planejamento.CarteiraIdeal.CarteiraIdealSetorRepository setorRepository;
    /** Catálogo global de setores: classifica o TICKER (V32). */
    private final dev.LzGuimaraes.FocusLifeHub.SetorMercado.SetorMercadoService setorMercadoService;

    public AtivoService(AtivoRepository ativoRepository,
                        CarteiraInvestimentoRepository carteiraInvestimentoRepository,
                        AtivoCadastroRepository ativoCadastroRepository,
                        dev.LzGuimaraes.FocusLifeHub.Planejamento.CarteiraIdeal.CarteiraIdealSubclasseRepository subclasseRepository,
                        dev.LzGuimaraes.FocusLifeHub.Planejamento.CarteiraIdeal.CarteiraIdealSetorRepository setorRepository,
                        dev.LzGuimaraes.FocusLifeHub.SetorMercado.SetorMercadoService setorMercadoService) {
        this.ativoRepository = ativoRepository;
        this.carteiraInvestimentoRepository = carteiraInvestimentoRepository;
        this.ativoCadastroRepository = ativoCadastroRepository;
        this.subclasseRepository = subclasseRepository;
        this.setorRepository = setorRepository;
        this.setorMercadoService = setorMercadoService;
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

    /**
     * Vincula posições já existentes a um ativo do catálogo.
     *
     * Necessário para posições antigas (ou cadastradas sem o autocomplete) que
     * ficaram sem `ativo_cadastro_id`: sem o vínculo elas aparecem na carteira,
     * mas não podem ter meta individual na Carteira Ideal.
     */
    @Transactional
    public int vincularCatalogo(List<Long> ativoIds, UUID ativoCadastroId) {
        Long userId = getAuthenticatedUserId();

        if (ativoIds == null || ativoIds.isEmpty()) {
            throw new BusinessRuleException("Informe ao menos uma posição para vincular.");
        }
        if (ativoCadastroId == null) {
            throw new BusinessRuleException("Informe o ativo do catálogo (ativo_cadastro_id).");
        }

        AtivoCadastroModel catalogo = resolveAtivoCadastro(ativoCadastroId);
        List<AtivoModel> posicoes = ativoRepository
                .findByIdInAndCarteiraInvestimento_UserId(ativoIds, userId);

        if (posicoes.size() != ativoIds.size()) {
            throw new ResourceNotFoundException(
                    "Uma ou mais posições informadas não foram encontradas na sua carteira.");
        }

        for (AtivoModel posicao : posicoes) {
            posicao.setAtivoCadastro(catalogo);
            // Mesma convenção do autocomplete: o nome da posição acompanha o ticker.
            posicao.setNome(catalogo.getNome());
            if (posicao.getPrecoAtual() == null) {
                posicao.setPrecoAtual(catalogo.getPrecoAtual());
            }
        }
        ativoRepository.saveAll(posicoes);
        return posicoes.size();
    }

    /**
     * Atribui posições a uma SUBCLASSE da Carteira Ideal (V26).
     *
     * É o caminho da renda fixa, do Tesouro e das caixinhas: ativos sem ticker
     * de catálogo ("Caixa PICPAY") não podem ter meta individual, mas precisam
     * contar para o alvo da subclasse. A meta continua sendo o percentual da
     * subclasse; aqui só dizemos a que subclasse cada posição pertence.
     *
     * Validações: a subclasse tem de ser de uma carteira do usuário; a posição
     * também; e as duas têm de ser da MESMA carteira. Se a posição tiver
     * categoria de investimento, ela tem de coincidir com a classe da subclasse
     * (senão o total da classe e o da subclasse contariam coisas diferentes).
     */
    @Transactional
    public int atribuirSubclasse(List<Long> ativoIds, Long subclasseId) {
        Long userId = getAuthenticatedUserId();

        if (ativoIds == null || ativoIds.isEmpty()) {
            throw new BusinessRuleException("Informe ao menos uma posição para classificar.");
        }

        List<AtivoModel> posicoes = ativoRepository
                .findByIdInAndCarteiraInvestimento_UserId(ativoIds, userId);
        if (posicoes.size() != ativoIds.size()) {
            throw new ResourceNotFoundException(
                    "Uma ou mais posições informadas não foram encontradas na sua carteira.");
        }

        // subclasse_id nulo = REMOVER a classificação (volta a contar só no total da classe).
        if (subclasseId == null) {
            for (AtivoModel posicao : posicoes) {
                posicao.setSubclasse(null);
            }
            ativoRepository.saveAll(posicoes);
            return posicoes.size();
        }

        dev.LzGuimaraes.FocusLifeHub.Planejamento.CarteiraIdeal.CarteiraIdealSubclasseModel subclasse =
                subclasseRepository.findById(subclasseId)
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "Subclasse com ID " + subclasseId + " não encontrada"));

        CarteiraInvestimentoModel carteira = subclasse.getClasse().getCarteiraInvestimento();
        if (carteira == null || carteira.getUser() == null || !carteira.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("Subclasse com ID " + subclasseId + " não encontrada");
        }

        for (AtivoModel posicao : posicoes) {
            if (posicao.getCarteiraInvestimento() == null
                    || !carteira.getId().equals(posicao.getCarteiraInvestimento().getId())) {
                throw new BusinessRuleException("A posição \"" + posicao.getNome() + "\" pertence a outra carteira.");
            }
            CategoriaInvestimento categoria = posicao.getCategoriaInvestimento();
            if (categoria != null && categoria != subclasse.getClasse().getClasse()) {
                throw new BusinessRuleException("A posição \"" + posicao.getNome() + "\" é "
                        + categoria + ", mas a subclasse \"" + subclasse.getNome() + "\" é de "
                        + subclasse.getClasse().getClasse() + ". Ajuste a categoria da posição ou escolha "
                        + "uma subclasse da mesma classe.");
            }
        }

        for (AtivoModel posicao : posicoes) {
            posicao.setSubclasse(subclasse);
        }
        ativoRepository.saveAll(posicoes);
        return posicoes.size();
    }

    /**
     * Atribui posições a um SETOR da Carteira Ideal (V28) — nível opcional
     * DENTRO da subclasse (ex.: Renda Fixa → Reserva → "Caixa").
     *
     * setor_id nulo REMOVE a classificação. Ao definir o setor, a subclasse da
     * posição passa a ser a do setor (coerência da hierarquia), a menos que a
     * posição já esteja numa subclasse diferente — nesse caso 400 explicando.
     */
    @Transactional
    public int atribuirSetor(List<Long> ativoIds, Long setorId) {
        Long userId = getAuthenticatedUserId();

        if (ativoIds == null || ativoIds.isEmpty()) {
            throw new BusinessRuleException("Informe ao menos uma posição para classificar.");
        }

        List<AtivoModel> posicoes = ativoRepository
                .findByIdInAndCarteiraInvestimento_UserId(ativoIds, userId);
        if (posicoes.size() != ativoIds.size()) {
            throw new ResourceNotFoundException(
                    "Uma ou mais posições informadas não foram encontradas na sua carteira.");
        }

        if (setorId == null) {
            for (AtivoModel posicao : posicoes) {
                posicao.setSetor(null);
            }
            ativoRepository.saveAll(posicoes);
            return posicoes.size();
        }

        dev.LzGuimaraes.FocusLifeHub.Planejamento.CarteiraIdeal.CarteiraIdealSetorModel setor =
                setorRepository.findById(setorId)
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "Setor com ID " + setorId + " não encontrado"));

        dev.LzGuimaraes.FocusLifeHub.Planejamento.CarteiraIdeal.CarteiraIdealSubclasseModel subclasse =
                setor.getSubclasse();
        CarteiraInvestimentoModel carteira = (subclasse != null && subclasse.getClasse() != null)
                ? subclasse.getClasse().getCarteiraInvestimento()
                : null;
        if (carteira == null || carteira.getUser() == null || !carteira.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("Setor com ID " + setorId + " não encontrado");
        }

        for (AtivoModel posicao : posicoes) {
            if (posicao.getCarteiraInvestimento() == null
                    || !carteira.getId().equals(posicao.getCarteiraInvestimento().getId())) {
                throw new BusinessRuleException("A posição \"" + posicao.getNome() + "\" pertence a outra carteira.");
            }
            if (posicao.getSubclasse() != null
                    && !posicao.getSubclasse().getId().equals(subclasse.getId())) {
                throw new BusinessRuleException("A posição \"" + posicao.getNome() + "\" está na subclasse \""
                        + posicao.getSubclasse().getNome() + "\", e o setor escolhido é da subclasse \""
                        + subclasse.getNome() + "\". Escolha um setor da mesma subclasse.");
            }
            CategoriaInvestimento categoria = posicao.getCategoriaInvestimento();
            if (categoria != null && categoria != subclasse.getClasse().getClasse()) {
                throw new BusinessRuleException("A posição \"" + posicao.getNome() + "\" é "
                        + categoria + ", mas o setor \"" + setor.getNome() + "\" é de "
                        + subclasse.getClasse().getClasse() + ".");
            }
        }

        for (AtivoModel posicao : posicoes) {
            posicao.setSubclasse(subclasse);   // o setor implica a subclasse dele
            posicao.setSetor(setor);
        }
        ativoRepository.saveAll(posicoes);

        // Classificar a POSIÇÃO classifica o TICKER (V32): o setor do catálogo é
        // o que sobrevive à carteira e vale para as próximas. Só preenche o que
        // está vazio — o catálogo mantido no banco tem precedência.
        if (setor.getSetorMercado() != null) {
            Set<AtivoCadastroModel> tickers = new java.util.HashSet<>();
            for (AtivoModel posicao : posicoes) {
                AtivoCadastroModel ticker = posicao.getAtivoCadastro();
                if (ticker != null && ticker.getSetorMercado() == null) {
                    ticker.setSetorMercado(setor.getSetorMercado());
                    tickers.add(ticker);
                }
            }
            if (!tickers.isEmpty()) {
                ativoCadastroRepository.saveAll(tickers);
            }
        }
        return posicoes.size();
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

            // SETOR do ticker: é a porta do cadastro MANUAL do catálogo (script/
            // banco). O setor é resolvido (ou criado) pelo nome normalizado, então
            // "Bancos", "bancos" e "BANCOS" caem na mesma linha do catálogo.
            if (dto.getSetor() != null && !dto.getSetor().isBlank()) {
                cadastro.setSetorMercado(setorMercadoService.criarOuObter(dto.getSetor(), null));
                cadastro = ativoCadastroRepository.save(cadastro);
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
