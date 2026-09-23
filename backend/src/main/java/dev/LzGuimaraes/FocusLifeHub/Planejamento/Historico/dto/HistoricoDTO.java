package dev.LzGuimaraes.FocusLifeHub.Planejamento.Historico.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.Size;

/**
 * DTOs do histórico de avaliações (Módulo 8).
 *
 * O histórico é por ATIVO por DIA: a série responde "como o score deste ativo
 * evoluiu" e a série da carteira responde "como o conjunto evoluiu".
 */
public final class HistoricoDTO {

    private HistoricoDTO() {}

    /** Um dia do histórico de um ativo. */
    public record Ponto(
            LocalDate data,
            BigDecimal quality_score,
            BigDecimal contribution_score,
            BigDecimal percentual_atual,
            BigDecimal percentual_ideal,
            BigDecimal deficit,
            BigDecimal excesso,
            Integer prioridade_manual
    ) {}

    public record SerieAtivo(
            UUID ativo_cadastro_id,
            Long ativo_id,
            String ticker,
            List<Ponto> pontos,
            BigDecimal primeiro_score,
            BigDecimal ultimo_score,
            /** ultimo − primeiro (em pontos percentuais). */
            BigDecimal variacao
    ) {}

    /** Um dia do histórico agregado da carteira. */
    public record PontoCarteira(
            LocalDate data,
            /** Média simples dos ativos avaliados naquele dia. */
            BigDecimal quality_medio,
            int ativos_avaliados,
            BigDecimal deficit_total,
            BigDecimal excesso_total,
            BigDecimal valor_carteira_total
    ) {}

    public record SerieCarteira(
            Long carteira_id,
            String moeda,
            int dias_registrados,
            List<PontoCarteira> pontos
    ) {}

    /** Ativo que possui histórico — usado na lista de seleção. */
    public record AtivoComHistorico(
            UUID ativo_cadastro_id,
            Long ativo_id,
            String ticker,
            int registros,
            LocalDate ultimo_registro,
            BigDecimal ultimo_score
    ) {}

    /** Corpo do registro (o snapshot do dia). */
    public record RegistrarRequest(
            /** Opcional: enriquece o histórico com o contexto da carteira. */
            Long carteira_investimento_id,

            @Size(max = 1000, message = "A observação deve ter no máximo 1000 caracteres")
            String observacao
    ) {}

    public record RegistrarResponse(
            LocalDate data_referencia,
            int registrados,
            int atualizados,
            List<String> tickers
    ) {}
}
