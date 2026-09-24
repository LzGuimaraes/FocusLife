package dev.LzGuimaraes.FocusLifeHub.Planejamento.Historico;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.LzGuimaraes.FocusLifeHub.Exceptions.BusinessRuleException;
import dev.LzGuimaraes.FocusLifeHub.Exceptions.ResourceNotFoundException;
import dev.LzGuimaraes.FocusLifeHub.Planejamento.Aporte.AporteService;
import dev.LzGuimaraes.FocusLifeHub.Planejamento.Aporte.dto.RankingAportesDTO;
import dev.LzGuimaraes.FocusLifeHub.Planejamento.Avaliacao.ChecklistAtivoService;
import dev.LzGuimaraes.FocusLifeHub.Planejamento.Avaliacao.dto.ChecklistAtivoDTO;
import dev.LzGuimaraes.FocusLifeHub.Planejamento.Historico.dto.HistoricoDTO;
import dev.LzGuimaraes.FocusLifeHub.Carteira.CarteiraInvestimentoModel;
import dev.LzGuimaraes.FocusLifeHub.Planejamento.comum.CarteiraLookup;
import dev.LzGuimaraes.FocusLifeHub.Planejamento.comum.ContextoUsuario;
import dev.LzGuimaraes.FocusLifeHub.User.UserRepository;

/**
 * Histórico de avaliações (Módulo 8).
 *
 * O registro é um RETRATO DO DIA: para cada ativo que tem avaliação, grava o
 * Quality Score consolidado e — quando uma carteira é informada — o contexto
 * da carteira naquele momento (percentual atual/ideal, déficit, excesso e o
 * Contribution Score).
 *
 * Registrar duas vezes no mesmo dia ATUALIZA a linha do dia (índices únicos
 * parciais garantem isso no banco), então o usuário pode reavaliar sem medo de
 * poluir o histórico com duplicatas.
 */
@Service
public class HistoricoService {

    private final AtivoScoreHistoricoRepository historicoRepository;
    private final UserRepository userRepository;
    private final ContextoUsuario contextoUsuario;
    private final CarteiraLookup carteiraLookup;
    private final ChecklistAtivoService checklistAtivoService;
    private final AporteService aporteService;

    public HistoricoService(AtivoScoreHistoricoRepository historicoRepository,
                            UserRepository userRepository,
                            ContextoUsuario contextoUsuario,
                            CarteiraLookup carteiraLookup,
                            ChecklistAtivoService checklistAtivoService,
                            AporteService aporteService) {
        this.historicoRepository = historicoRepository;
        this.userRepository = userRepository;
        this.contextoUsuario = contextoUsuario;
        this.carteiraLookup = carteiraLookup;
        this.checklistAtivoService = checklistAtivoService;
        this.aporteService = aporteService;
    }

    /* ══════════════════════════════════════════════════════════════════
       Registro do dia
       ══════════════════════════════════════════════════════════════════ */

    @Transactional
    public HistoricoDTO.RegistrarResponse registrar(HistoricoDTO.RegistrarRequest request) {
        Long userId = contextoUsuario.id();
        LocalDate hoje = LocalDate.now();

        // Contexto de portfólio (opcional): vem do mesmo cálculo do ranking.
        Map<UUID, RankingAportesDTO.Item> contexto = new HashMap<>();
        BigDecimal valorTotal = null;
        Long carteiraId = (request != null) ? request.carteira_investimento_id() : null;
        if (carteiraId != null) {
            carteiraLookup.exigirCarteiraDoUsuario(carteiraId);
            RankingAportesDTO.Response ranking = aporteService.ranking(carteiraId, null);
            valorTotal = ranking.valor_total();
            for (RankingAportesDTO.Item item : ranking.itens()) {
                if (item.ativo_cadastro_id() != null) {
                    contexto.put(item.ativo_cadastro_id(), item);
                }
            }
        }

        List<ChecklistAtivoDTO.AtivoAvaliado> avaliados = checklistAtivoService.resumoPorAtivo();

        int registrados = 0;
        int atualizados = 0;
        List<String> tickers = new ArrayList<>();

        for (ChecklistAtivoDTO.AtivoAvaliado ativo : avaliados) {
            RankingAportesDTO.Item ctx = (ativo.ativo_cadastro_id() != null)
                    ? contexto.get(ativo.ativo_cadastro_id())
                    : null;

            // Nada a registrar: sem score e sem contexto de carteira.
            if (ativo.quality_score() == null && ctx == null) {
                continue;
            }

            Optional<AtivoScoreHistoricoModel> existente = buscarDoDia(userId, ativo, hoje);
            AtivoScoreHistoricoModel linha = existente.orElseGet(AtivoScoreHistoricoModel::new);
            if (existente.isEmpty()) {
                linha.setUser(userRepository.getReferenceById(userId));
                linha.setCreatedAt(LocalDateTime.now());
                registrados++;
            } else {
                atualizados++;
            }

            linha.setDataReferencia(hoje);
            linha.setAtivoCadastroId(ativo.ativo_cadastro_id());
            linha.setAtivoId(ativo.ativo_id());
            linha.setTicker(ativo.ticker());
            linha.setQualityScore(ativo.quality_score());
            linha.setTotalChecklists(ativo.total_checklists());
            linha.setTotalPerguntas(ativo.total_perguntas());
            linha.setTotalRespondidas(ativo.total_respondidas());
            if (request != null && request.observacao() != null && !request.observacao().isBlank()) {
                linha.setObservacao(request.observacao().trim());
            }

            if (ctx != null) {
                linha.setCarteiraInvestimentoId(carteiraId);
                linha.setContributionScore(ctx.nota());
                linha.setPercentualAtual(ctx.percentual_atual());
                linha.setPercentualIdeal(ctx.percentual_ideal());
                linha.setDeficit(ctx.deficit());
                linha.setExcesso(ctx.excesso());
                linha.setValorCarteiraTotal(valorTotal);
            }

            historicoRepository.save(linha);
            tickers.add(ativo.ticker() != null ? ativo.ticker() : "posição " + ativo.ativo_id());
        }

        if (tickers.isEmpty()) {
            throw new BusinessRuleException(
                    "Não há avaliação para registrar ainda: responda um checklist de ativo (e/ou defina a Carteira Ideal) antes.");
        }

        return new HistoricoDTO.RegistrarResponse(hoje, registrados, atualizados, tickers);
    }

    /* ══════════════════════════════════════════════════════════════════
       Consultas
       ══════════════════════════════════════════════════════════════════ */

    /** Ativos que já possuem histórico (para a tela de seleção). */
    @Transactional(readOnly = true)
    public List<HistoricoDTO.AtivoComHistorico> ativos() {
        Long userId = contextoUsuario.id();
        List<AtivoScoreHistoricoModel> todas = historicoRepository
                .findByUserIdOrderByDataReferenciaDescIdDesc(userId);

        Map<String, List<AtivoScoreHistoricoModel>> porAtivo = new LinkedHashMap<>();
        for (AtivoScoreHistoricoModel linha : todas) {
            porAtivo.computeIfAbsent(chaveDoAtivo(linha), k -> new ArrayList<>()).add(linha);
        }

        List<HistoricoDTO.AtivoComHistorico> lista = new ArrayList<>();
        for (List<AtivoScoreHistoricoModel> grupo : porAtivo.values()) {
            AtivoScoreHistoricoModel ultimo = grupo.get(0); // já ordenado desc
            lista.add(new HistoricoDTO.AtivoComHistorico(
                    ultimo.getAtivoCadastroId(),
                    ultimo.getAtivoId(),
                    ultimo.getTicker(),
                    grupo.size(),
                    ultimo.getDataReferencia(),
                    ultimo.getQualityScore()));
        }
        lista.sort(Comparator.comparing(HistoricoDTO.AtivoComHistorico::ticker,
                Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));
        return lista;
    }

    /** Evolução do score de um ativo (Módulo 8). */
    @Transactional(readOnly = true)
    public HistoricoDTO.SerieAtivo serieAtivo(UUID ativoCadastroId, Long ativoId, LocalDate de, LocalDate ate) {
        Long userId = contextoUsuario.id();
        if ((ativoCadastroId == null) == (ativoId == null)) {
            throw new BusinessRuleException("Informe o ativo do catálogo (ativo_cadastro_id) OU a posição (ativo_id).");
        }

        List<AtivoScoreHistoricoModel> linhas = (ativoCadastroId != null)
                ? historicoRepository.findByUserIdAndAtivoCadastroIdOrderByDataReferenciaAsc(userId, ativoCadastroId)
                : historicoRepository.findByUserIdAndAtivoIdOrderByDataReferenciaAsc(userId, ativoId);

        List<HistoricoDTO.Ponto> pontos = linhas.stream()
                .filter(l -> dentroDoPeriodo(l.getDataReferencia(), de, ate))
                .map(this::toPonto)
                .toList();

        BigDecimal primeiro = primeiroScore(pontos);
        BigDecimal ultimo = ultimoScore(pontos);
        BigDecimal variacao = (primeiro != null && ultimo != null)
                ? ultimo.subtract(primeiro).setScale(4, RoundingMode.HALF_UP)
                : null;

        String ticker = linhas.isEmpty() ? null : linhas.get(linhas.size() - 1).getTicker();
        return new HistoricoDTO.SerieAtivo(ativoCadastroId, ativoId, ticker, pontos, primeiro, ultimo, variacao);
    }

    /** Evolução da carteira: qualidade média, déficits/excessos e valor total por dia. */
    @Transactional(readOnly = true)
    public HistoricoDTO.SerieCarteira serieCarteira(Long carteiraId, LocalDate de, LocalDate ate) {
        CarteiraInvestimentoModel carteira = carteiraLookup.exigirCarteiraDoUsuario(carteiraId);
        Long userId = contextoUsuario.id();

        List<AtivoScoreHistoricoModel> linhas = historicoRepository
                .findByUserIdAndCarteiraInvestimentoIdOrderByDataReferenciaAsc(userId, carteiraId)
                .stream()
                .filter(l -> dentroDoPeriodo(l.getDataReferencia(), de, ate))
                .toList();

        Map<LocalDate, List<AtivoScoreHistoricoModel>> porDia = new LinkedHashMap<>();
        for (AtivoScoreHistoricoModel linha : linhas) {
            porDia.computeIfAbsent(linha.getDataReferencia(), k -> new ArrayList<>()).add(linha);
        }

        List<HistoricoDTO.PontoCarteira> pontos = new ArrayList<>();
        for (Map.Entry<LocalDate, List<AtivoScoreHistoricoModel>> dia : porDia.entrySet()) {
            List<AtivoScoreHistoricoModel> doDia = dia.getValue();

            List<BigDecimal> scores = doDia.stream()
                    .map(AtivoScoreHistoricoModel::getQualityScore)
                    .filter(java.util.Objects::nonNull)
                    .toList();
            BigDecimal media = scores.isEmpty() ? null
                    : scores.stream().reduce(BigDecimal.ZERO, BigDecimal::add)
                            .divide(BigDecimal.valueOf(scores.size()), 4, RoundingMode.HALF_UP);

            BigDecimal deficit = doDia.stream().map(AtivoScoreHistoricoModel::getDeficit)
                    .filter(java.util.Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal excesso = doDia.stream().map(AtivoScoreHistoricoModel::getExcesso)
                    .filter(java.util.Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal valorTotal = doDia.stream().map(AtivoScoreHistoricoModel::getValorCarteiraTotal)
                    .filter(java.util.Objects::nonNull).findFirst().orElse(null);

            pontos.add(new HistoricoDTO.PontoCarteira(
                    dia.getKey(), media, doDia.size(),
                    deficit.setScale(2, RoundingMode.HALF_UP),
                    excesso.setScale(2, RoundingMode.HALF_UP),
                    valorTotal));
        }

        return new HistoricoDTO.SerieCarteira(carteiraId, carteira.getMoeda(), pontos.size(), pontos);
    }

    @Transactional
    public void deletar(Long id) {
        AtivoScoreHistoricoModel linha = historicoRepository
                .findByIdAndUserId(id, contextoUsuario.id())
                .orElseThrow(() -> new ResourceNotFoundException("Registro de histórico com ID " + id + " não encontrado"));
        historicoRepository.delete(linha);
    }

    /* ── Helpers ── */

    private Optional<AtivoScoreHistoricoModel> buscarDoDia(Long userId,
                                                          ChecklistAtivoDTO.AtivoAvaliado ativo,
                                                          LocalDate data) {
        if (ativo.ativo_cadastro_id() != null) {
            return historicoRepository.findByUserIdAndAtivoCadastroIdAndDataReferencia(
                    userId, ativo.ativo_cadastro_id(), data);
        }
        if (ativo.ativo_id() != null) {
            return historicoRepository.findByUserIdAndAtivoIdAndDataReferencia(userId, ativo.ativo_id(), data);
        }
        return Optional.empty();
    }

    private HistoricoDTO.Ponto toPonto(AtivoScoreHistoricoModel linha) {
        return new HistoricoDTO.Ponto(
                linha.getDataReferencia(),
                linha.getQualityScore(),
                linha.getContributionScore(),                linha.getPercentualAtual(),
                linha.getPercentualIdeal(),
                linha.getDeficit(),
                linha.getExcesso(),
                linha.getPrioridadeManual());
    }

    private String chaveDoAtivo(AtivoScoreHistoricoModel linha) {
        if (linha.getAtivoCadastroId() != null) {
            return "cat:" + linha.getAtivoCadastroId();
        }
        return "pos:" + linha.getAtivoId();
    }

    private boolean dentroDoPeriodo(LocalDate data, LocalDate de, LocalDate ate) {
        if (de != null && data.isBefore(de)) {
            return false;
        }
        return ate == null || !data.isAfter(ate);
    }

    private BigDecimal primeiroScore(List<HistoricoDTO.Ponto> pontos) {
        return pontos.stream().map(HistoricoDTO.Ponto::quality_score)
                .filter(java.util.Objects::nonNull).findFirst().orElse(null);
    }

    private BigDecimal ultimoScore(List<HistoricoDTO.Ponto> pontos) {
        return pontos.stream().map(HistoricoDTO.Ponto::quality_score)
                .filter(java.util.Objects::nonNull).reduce((a, b) -> b).orElse(null);
    }
}
