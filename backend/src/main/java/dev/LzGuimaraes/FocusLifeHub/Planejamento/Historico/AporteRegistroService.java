package dev.LzGuimaraes.FocusLifeHub.Planejamento.Historico;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.LzGuimaraes.FocusLifeHub.Carteira.CarteiraInvestimentoModel;
import dev.LzGuimaraes.FocusLifeHub.Exceptions.BusinessRuleException;
import dev.LzGuimaraes.FocusLifeHub.Exceptions.ResourceNotFoundException;
import dev.LzGuimaraes.FocusLifeHub.Planejamento.CarteiraIdeal.CarteiraIdealService;
import dev.LzGuimaraes.FocusLifeHub.Planejamento.CarteiraIdeal.dto.MeusAtivosResponseDTO;
import dev.LzGuimaraes.FocusLifeHub.Planejamento.Historico.dto.AporteRegistroDTO;
import dev.LzGuimaraes.FocusLifeHub.Planejamento.comum.CarteiraLookup;
import dev.LzGuimaraes.FocusLifeHub.Planejamento.comum.ContextoUsuario;

/**
 * Histórico de aportes executados (§24).
 *
 * O sistema NÃO inventa o aporte: ele registra o que o usuário confirma ter
 * feito. Além de servir de registro, o histórico alimenta o motor com um sinal
 * de CONCENTRAÇÃO RECENTE — "você já aportou R$ X neste ativo nos últimos 30
 * dias" — que aparece como alerta e nas explicações, sem penalidade silenciosa.
 */
@Service
public class AporteRegistroService {

    /** Janela considerada "recente" para o alerta de concentração. */
    public static final int DIAS_RECENTES = 30;

    private final AporteRegistroRepository repository;
    private final CarteiraLookup carteiraLookup;
    private final CarteiraIdealService carteiraIdealService;
    private final ContextoUsuario contextoUsuario;

    public AporteRegistroService(AporteRegistroRepository repository,
                                 CarteiraLookup carteiraLookup,
                                 CarteiraIdealService carteiraIdealService,
                                 ContextoUsuario contextoUsuario) {
        this.repository = repository;
        this.carteiraLookup = carteiraLookup;
        this.carteiraIdealService = carteiraIdealService;
        this.contextoUsuario = contextoUsuario;
    }

    /**
     * Registra um lote de aportes (normalmente os sugeridos que o usuário
     * executou). O percentual ANTES/DEPOIS é calculado a partir da carteira no
     * momento do registro, para o histórico ser auditável depois.
     */
    @Transactional
    public AporteRegistroDTO.Resultado registrar(AporteRegistroDTO.Request req) {
        Long userId = contextoUsuario.id();
        CarteiraInvestimentoModel carteira = carteiraLookup.exigirCarteiraDoUsuario(
                req.carteira_investimento_id());
        LocalDate data = (req.data() != null) ? req.data() : LocalDate.now();

        MeusAtivosResponseDTO meus = carteiraIdealService.meusAtivos(carteira.getId());
        double totalAntes = nz(meus.valor_total());
        double totalAportado = req.itens().stream()
                .mapToDouble(i -> nz(i.valor()))
                .sum();
        if (totalAportado <= 0d) {
            throw new BusinessRuleException("O valor total do aporte precisa ser maior que zero.");
        }
        double totalDepois = totalAntes + totalAportado;

        // Valor atual por item, para calcular a participação antes/depois.
        Map<String, Double> valorAtualPorItem = new HashMap<>();
        Map<String, Double> valorAtualPorNome = new HashMap<>();
        for (MeusAtivosResponseDTO.MeuAtivoDTO a : meus.ativos()) {
            double valor = nz(a.valor_atual());
            valorAtualPorItem.put(chave(a.ativo_cadastro_id(), a.ativo_ids()), valor);
            if (a.ticker() != null) {
                // Fallback pelo nome/ticker: cobre a posição sem catálogo
                // (renda fixa, caixinha) mesmo quando o item não manda o id dela.
                valorAtualPorNome.put(a.ticker().trim().toLowerCase(), valor);
            }
        }

        List<AporteRegistroDTO.Response> respostas = new ArrayList<>();
        for (AporteRegistroDTO.ItemRequest item : req.itens()) {
            BigDecimal valor = (item.valor() != null) ? item.valor() : BigDecimal.ZERO;
            if (valor.signum() <= 0) {
                continue;   // item sem valor não vira aporte
            }
            AporteRegistroModel registro = new AporteRegistroModel();
            registro.setUserId(userId);
            registro.setCarteiraInvestimentoId(carteira.getId());
            registro.setData(data);
            registro.setAtivoCadastroId(item.ativo_cadastro_id());
            registro.setAtivoId(item.ativo_id());
            registro.setTicker(item.ticker());
            registro.setClasse(item.classe());
            registro.setValor(valor);
            registro.setPreco(item.preco());
            registro.setQuantidade(item.quantidade());
            registro.setObservacao(req.observacao());
            registro.setCreatedAt(LocalDateTime.now());

            double atual = valorAtualPorItem.getOrDefault(chave(item.ativo_cadastro_id(),
                    (item.ativo_id() != null) ? List.of(item.ativo_id()) : List.of()), 0d);
            if (atual <= 0d && item.ticker() != null) {
                atual = valorAtualPorNome.getOrDefault(item.ticker().trim().toLowerCase(), 0d);
            }
            registro.setPercentualAntes(percentual(atual, totalAntes));
            registro.setPercentualDepois(percentual(atual + valor.doubleValue(), totalDepois));

            respostas.add(toResponse(repository.save(registro)));
        }

        if (respostas.isEmpty()) {
            throw new BusinessRuleException("Nenhum item tinha valor maior que zero.");
        }
        return new AporteRegistroDTO.Resultado(respostas.size(), moeda(totalAportado), data, respostas);
    }

    @Transactional(readOnly = true)
    public List<AporteRegistroDTO.Response> listar(Long carteiraId) {
        carteiraLookup.exigirCarteiraDoUsuario(carteiraId);
        return repository.findByCarteiraInvestimentoIdOrderByDataDescIdDesc(carteiraId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public void excluir(Long id) {
        AporteRegistroModel registro = repository.findByIdAndUserId(id, contextoUsuario.id())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Registro de aporte com ID " + id + " não encontrado"));
        repository.delete(registro);
    }

    /** Aportes dos últimos {@value #DIAS_RECENTES} dias de uma carteira (para o motor). */
    @Transactional(readOnly = true)
    public List<AporteRegistroModel> recentes(Long carteiraId) {
        return repository.findByCarteiraInvestimentoIdAndDataGreaterThanEqual(
                carteiraId, LocalDate.now().minusDays(DIAS_RECENTES));
    }

    /* ── Helpers ── */

    private AporteRegistroDTO.Response toResponse(AporteRegistroModel r) {
        return new AporteRegistroDTO.Response(
                r.getId(), r.getData(), r.getAtivoCadastroId(), r.getAtivoId(), r.getTicker(),
                r.getClasse(), r.getValor(), r.getPreco(), r.getQuantidade(),
                r.getPercentualAntes(), r.getPercentualDepois(), r.getObservacao(), r.getCreatedAt());
    }

    /** Chave do item: ticker do catálogo quando houver; senão o id da posição. */
    private String chave(UUID ativoCadastroId, List<Long> ativoIds) {
        if (ativoCadastroId != null) {
            return "cat:" + ativoCadastroId;
        }
        return "pos:" + ((ativoIds != null && !ativoIds.isEmpty()) ? ativoIds.get(0) : "?");
    }

    private BigDecimal percentual(double valor, double total) {
        if (total <= 0d) {
            return BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
        }
        return BigDecimal.valueOf(valor / total * 100d).setScale(4, RoundingMode.HALF_UP);
    }

    private BigDecimal moeda(double valor) {
        return BigDecimal.valueOf(valor).setScale(2, RoundingMode.HALF_UP);
    }

    private double nz(BigDecimal valor) {
        return (valor != null) ? valor.doubleValue() : 0d;
    }
}
