package dev.LzGuimaraes.FocusLifeHub.Investimento.ComprasVendas;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.LzGuimaraes.FocusLifeHub.Ativo.AtivoModel;
import dev.LzGuimaraes.FocusLifeHub.Ativo.AtivoRepository;
import dev.LzGuimaraes.FocusLifeHub.Ativo.CategoriaInvestimento;
import dev.LzGuimaraes.FocusLifeHub.AtivoCadastro.AtivoCadastroModel;
import dev.LzGuimaraes.FocusLifeHub.AtivoCadastro.AtivoCadastroRepository;
import dev.LzGuimaraes.FocusLifeHub.AtivoCadastro.TipoAtivoCadastro;
import dev.LzGuimaraes.FocusLifeHub.Exceptions.BusinessRuleException;
import dev.LzGuimaraes.FocusLifeHub.Exceptions.ResourceNotFoundException;
import dev.LzGuimaraes.FocusLifeHub.Investimento.ComprasVendas.dto.OperacaoCompraVendaRequestDTO;
import dev.LzGuimaraes.FocusLifeHub.Investimento.ComprasVendas.dto.OperacaoCompraVendaResponseDTO;
import dev.LzGuimaraes.FocusLifeHub.Investimento.ComprasVendas.dto.ResumoComprasVendasDTO;
import dev.LzGuimaraes.FocusLifeHub.Planejamento.comum.CarteiraLookup;
import dev.LzGuimaraes.FocusLifeHub.Planejamento.comum.ContextoUsuario;

/**
 * Regras de Compras/Vendas.
 *
 * O usuário registra a COMPRA e, quando vender, a VENDA. Todo o cálculo de
 * dinheiro (lucro, rentabilidade, prazo, resumo e série do gráfico) fica AQUI e
 * é devolvido pronto no DTO — o front nunca recalcula valores.
 *
 * Isolamento por usuário em TODAS as consultas: a operação é sempre buscada por
 * {@code (id, userId)} e a carteira referenciada é validada com o dono logado.
 */
@Service
public class OperacaoCompraVendaService {

    private final OperacaoCompraVendaRepository repository;
    private final ContextoUsuario contextoUsuario;
    private final CarteiraLookup carteiraLookup;
    private final AtivoCadastroRepository ativoCadastroRepository;
    private final AtivoRepository ativoRepository;

    public OperacaoCompraVendaService(OperacaoCompraVendaRepository repository,
                                      ContextoUsuario contextoUsuario,
                                      CarteiraLookup carteiraLookup,
                                      AtivoCadastroRepository ativoCadastroRepository,
                                      AtivoRepository ativoRepository) {
        this.repository = repository;
        this.contextoUsuario = contextoUsuario;
        this.carteiraLookup = carteiraLookup;
        this.ativoCadastroRepository = ativoCadastroRepository;
        this.ativoRepository = ativoRepository;
    }

    /* ── Consultas ── */

    @Transactional(readOnly = true)
    public Page<OperacaoCompraVendaResponseDTO> getAll(Long carteiraId, LocalDate de, LocalDate ate,
                                                       Pageable pageable) {
        Long userId = contextoUsuario.id();
        List<OperacaoCompraVendaModel> filtradas = filtrarPorData(
                buscarDoUsuario(userId, carteiraId), de, ate);
        return paginar(filtradas, pageable, userId);
    }

    @Transactional(readOnly = true)
    public OperacaoCompraVendaResponseDTO getById(Long id) {
        Long userId = contextoUsuario.id();
        OperacaoCompraVendaModel op = exigirOperacao(id, userId);
        return toResponse(op, precosCatalogo(List.of(op)), precosPosicao(List.of(op), userId));
    }

    @Transactional(readOnly = true)
    public ResumoComprasVendasDTO resumo(Long carteiraId, LocalDate de, LocalDate ate) {
        Long userId = contextoUsuario.id();
        List<OperacaoCompraVendaModel> filtradas = filtrarPorData(
                buscarDoUsuario(userId, carteiraId), de, ate);

        Map<UUID, BigDecimal> precosCatalogo = precosCatalogo(filtradas);
        Map<Long, BigDecimal> precosPosicao = precosPosicao(filtradas, userId);

        List<ResumoComprasVendas.Linha> linhas = new ArrayList<>();
        for (OperacaoCompraVendaModel op : filtradas) {
            CalculadoraOperacao.Resultado r = CalculadoraOperacao.calcular(
                    op, precoAtual(op, precosCatalogo, precosPosicao));
            linhas.add(new ResumoComprasVendas.Linha(referencia(op), op.getValorInicial(),
                    r.valorBruto(), r.lucro(), r.custoVendido(), r.aberta()));
        }

        ResumoComprasVendas.Resultado res = ResumoComprasVendas.montar(
                linhas, ResumoComprasVendas.decidirGranularidade(de, ate));

        List<ResumoComprasVendasDTO.Ponto> serie = res.serie().stream()
                .map(p -> new ResumoComprasVendasDTO.Ponto(
                        p.periodo(), p.valorBruto(), p.valorPercentualAcumulado()))
                .toList();

        return new ResumoComprasVendasDTO(res.totalInvestido(), res.totalVendido(),
                res.lucroTotal(), res.rentabilidadePercentual(),
                res.qtdOperacoes(), res.qtdAbertas(), res.qtdFechadas(), serie);
    }

    /* ── Escrita ── */

    @Transactional
    public OperacaoCompraVendaResponseDTO create(OperacaoCompraVendaRequestDTO dto) {
        Long userId = contextoUsuario.id();
        validar(dto);

        OperacaoCompraVendaModel op = new OperacaoCompraVendaModel();
        op.setUserId(userId);
        aplicarReferencias(op, dto, userId);
        aplicarValores(op, dto);
        LocalDateTime agora = LocalDateTime.now();
        op.setCreatedAt(agora);
        op.setUpdatedAt(agora);

        OperacaoCompraVendaModel salvo = repository.save(op);
        return toResponse(salvo, precosCatalogo(List.of(salvo)), precosPosicao(List.of(salvo), userId));
    }

    @Transactional
    public OperacaoCompraVendaResponseDTO update(Long id, OperacaoCompraVendaRequestDTO dto) {
        Long userId = contextoUsuario.id();
        validar(dto);

        OperacaoCompraVendaModel op = exigirOperacao(id, userId);
        aplicarReferencias(op, dto, userId);
        aplicarValores(op, dto);
        op.setUpdatedAt(LocalDateTime.now());

        OperacaoCompraVendaModel salvo = repository.save(op);
        return toResponse(salvo, precosCatalogo(List.of(salvo)), precosPosicao(List.of(salvo), userId));
    }

    @Transactional
    public void delete(Long id) {
        Long userId = contextoUsuario.id();
        repository.delete(exigirOperacao(id, userId));
    }

    /* ── Regras de validação (HTTP 400 via BusinessRuleException) ── */

    private void validar(OperacaoCompraVendaRequestDTO dto) {
        if (dto.valor_inicial() != null && dto.valor_inicial().signum() < 0) {
            throw new BusinessRuleException("O valor inicial não pode ser negativo.");
        }
        if (dto.valor_venda() != null && dto.valor_venda().signum() < 0) {
            throw new BusinessRuleException("O valor da venda não pode ser negativo.");
        }
        if (dto.quantidade() != null && dto.quantidade().signum() < 0) {
            throw new BusinessRuleException("A quantidade não pode ser negativa.");
        }
        if (dto.quantidade_vendida() != null && dto.quantidade_vendida().signum() < 0) {
            throw new BusinessRuleException("A quantidade vendida não pode ser negativa.");
        }
        if (dto.valor_venda() != null && dto.data_venda() == null) {
            throw new BusinessRuleException("Informe a data da venda.");
        }
        if (dto.valor_venda() == null && dto.data_venda() != null) {
            throw new BusinessRuleException("Informe o valor da venda.");
        }
        if (dto.quantidade_vendida() != null && dto.valor_venda() == null) {
            throw new BusinessRuleException("Quantidade vendida só faz sentido com uma venda registrada.");
        }
        if (dto.data_venda() != null && dto.data_venda().isBefore(dto.data_compra())) {
            throw new BusinessRuleException("A data da venda não pode ser anterior à data da compra.");
        }
        if (dto.quantidade() != null && dto.quantidade_vendida() != null
                && dto.quantidade_vendida().compareTo(dto.quantidade()) > 0) {
            throw new BusinessRuleException("A quantidade vendida não pode ser maior que a comprada.");
        }
    }

    /* ── Apoio ── */

    private List<OperacaoCompraVendaModel> buscarDoUsuario(Long userId, Long carteiraId) {
        if (carteiraId != null) {
            carteiraLookup.exigirCarteiraDoUsuario(carteiraId);
            return repository.findByUserIdAndCarteiraInvestimentoIdOrderByDataCompraDescIdDesc(userId, carteiraId);
        }
        return repository.findByUserIdOrderByDataCompraDescIdDesc(userId);
    }

    private OperacaoCompraVendaModel exigirOperacao(Long id, Long userId) {
        return repository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Operação com ID " + id + " não encontrada"));
    }

    /** Data que coloca a operação na sua janela: a venda (fechada) ou a compra (aberta). */
    private static LocalDate referencia(OperacaoCompraVendaModel op) {
        return op.getDataVenda() != null ? op.getDataVenda() : op.getDataCompra();
    }

    private List<OperacaoCompraVendaModel> filtrarPorData(List<OperacaoCompraVendaModel> ops,
                                                          LocalDate de, LocalDate ate) {
        if (de == null && ate == null) {
            return ops;
        }
        return ops.stream().filter(o -> {
            LocalDate r = referencia(o);
            if (r == null) {
                return false;
            }
            if (de != null && r.isBefore(de)) {
                return false;
            }
            return ate == null || !r.isAfter(ate);
        }).toList();
    }

    private Page<OperacaoCompraVendaResponseDTO> paginar(List<OperacaoCompraVendaModel> ops,
                                                         Pageable pageable, Long userId) {
        Map<UUID, BigDecimal> catalogo = precosCatalogo(ops);
        Map<Long, BigDecimal> posicao = precosPosicao(ops, userId);
        List<OperacaoCompraVendaResponseDTO> todos = ops.stream()
                .map(o -> toResponse(o, catalogo, posicao))
                .toList();
        if (pageable.isUnpaged()) {
            return new PageImpl<>(todos);
        }
        int inicio = (int) Math.min(pageable.getOffset(), todos.size());
        int fim = Math.min(inicio + pageable.getPageSize(), todos.size());
        return new PageImpl<>(todos.subList(inicio, fim), pageable, todos.size());
    }

    private void aplicarReferencias(OperacaoCompraVendaModel op, OperacaoCompraVendaRequestDTO dto,
                                    Long userId) {
        if (dto.carteira_investimento_id() != null) {
            carteiraLookup.exigirCarteiraDoUsuario(dto.carteira_investimento_id());
            op.setCarteiraInvestimentoId(dto.carteira_investimento_id());
        } else {
            op.setCarteiraInvestimentoId(null);
        }

        AtivoCadastroModel catalogo = null;
        if (dto.ativo_cadastro_id() != null) {
            catalogo = ativoCadastroRepository.findById(dto.ativo_cadastro_id())
                    .orElseThrow(() -> new ResourceNotFoundException("Ativo do catálogo não encontrado"));
            op.setAtivoCadastroId(catalogo.getId());
        } else {
            op.setAtivoCadastroId(null);
        }

        AtivoModel posicao = null;
        if (dto.ativo_id() != null) {
            posicao = ativoRepository.findById(dto.ativo_id())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Posição com ID " + dto.ativo_id() + " não encontrada"));
            exigirPosicaoDoUsuario(posicao, userId);
            op.setAtivoId(posicao.getId());
        } else {
            op.setAtivoId(null);
        }

        CategoriaInvestimento classe = dto.classe();
        if (classe == null && catalogo != null) {
            classe = classeDoCatalogo(catalogo.getTipo());
        }
        op.setClasse(classe);

        String ticker = textoOuNulo(dto.ticker());
        String nome = textoOuNulo(dto.nome());
        if (ticker == null) {
            ticker = (catalogo != null) ? catalogo.getNome()
                    : (posicao != null) ? posicao.getNome() : null;
        }
        if (nome == null) {
            nome = (catalogo != null) ? catalogo.getNome()
                    : (posicao != null) ? posicao.getNome() : null;
        }
        if (ticker == null && nome == null) {
            throw new BusinessRuleException("Selecione um ativo ou informe o ticker da operação.");
        }
        op.setTicker(ticker != null ? ticker : nome);
        op.setNome(nome != null ? nome : ticker);
    }

    private void aplicarValores(OperacaoCompraVendaModel op, OperacaoCompraVendaRequestDTO dto) {
        op.setDataCompra(dto.data_compra());
        op.setDataVenda(dto.data_venda());
        op.setQuantidade(dto.quantidade());
        op.setQuantidadeVendida(dto.quantidade_vendida());
        op.setValorInicial(dto.valor_inicial());
        op.setValorVenda(dto.valor_venda());
    }

    private OperacaoCompraVendaResponseDTO toResponse(OperacaoCompraVendaModel op,
                                                      Map<UUID, BigDecimal> catalogo,
                                                      Map<Long, BigDecimal> posicao) {
        BigDecimal preco = precoAtual(op, catalogo, posicao);
        CalculadoraOperacao.Resultado r = CalculadoraOperacao.calcular(op, preco);
        return new OperacaoCompraVendaResponseDTO(
                op.getId(), op.getCarteiraInvestimentoId(), op.getAtivoCadastroId(), op.getAtivoId(),
                op.getTicker(), op.getNome(), op.getClasse(),
                op.getDataCompra(), op.getDataVenda(),
                op.getQuantidade(), op.getQuantidadeVendida(), op.getValorInicial(), op.getValorVenda(),
                unitario(op.getValorInicial(), op.getQuantidade()),
                unitario(op.getValorVenda(), op.getQuantidade()),
                r.custoVendido(), r.lucro(), r.rentabilidadePercentual(), r.prazoDias(), r.aberta(),
                preco, op.getCreatedAt(), op.getUpdatedAt());
    }

    /** Preço unitário = valor total ÷ quantidade. Sem quantidade não há preço por unidade. */
    private static BigDecimal unitario(BigDecimal valor, BigDecimal quantidade) {
        if (valor == null || quantidade == null || quantidade.signum() == 0) {
            return null;
        }
        return valor.divide(quantidade, 2, java.math.RoundingMode.HALF_UP);
    }

    private BigDecimal precoAtual(OperacaoCompraVendaModel op, Map<UUID, BigDecimal> catalogo,
                                  Map<Long, BigDecimal> posicao) {
        if (op.getAtivoCadastroId() != null) {
            BigDecimal p = catalogo.get(op.getAtivoCadastroId());
            if (p != null) {
                return p;
            }
        }
        if (op.getAtivoId() != null) {
            return posicao.get(op.getAtivoId());
        }
        return null;
    }

    private Map<UUID, BigDecimal> precosCatalogo(Collection<OperacaoCompraVendaModel> ops) {
        Set<UUID> ids = ops.stream().map(OperacaoCompraVendaModel::getAtivoCadastroId)
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

    private Map<Long, BigDecimal> precosPosicao(Collection<OperacaoCompraVendaModel> ops, Long userId) {
        Set<Long> ids = ops.stream().map(OperacaoCompraVendaModel::getAtivoId)
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

    private void exigirPosicaoDoUsuario(AtivoModel posicao, Long userId) {
        if (!posicaoPertenceAoUsuario(posicao, userId)) {
            throw new ResourceNotFoundException("Posição com ID " + posicao.getId() + " não encontrada");
        }
    }

    private static boolean posicaoPertenceAoUsuario(AtivoModel a, Long userId) {
        return a.getCarteiraInvestimento() != null
                && a.getCarteiraInvestimento().getUser() != null
                && userId.equals(a.getCarteiraInvestimento().getUser().getId());
    }

    private static CategoriaInvestimento classeDoCatalogo(TipoAtivoCadastro tipo) {
        if (tipo == null) {
            return null;
        }
        return switch (tipo) {
            case ACAO, BDR -> CategoriaInvestimento.ACOES;
            case FII -> CategoriaInvestimento.FIIS;
            case ETF -> CategoriaInvestimento.ETFS;
            case CRIPTOMOEDA -> CategoriaInvestimento.CRIPTOMOEDAS;
        };
    }

    private static String textoOuNulo(String v) {
        return (v == null || v.isBlank()) ? null : v.trim();
    }
}
