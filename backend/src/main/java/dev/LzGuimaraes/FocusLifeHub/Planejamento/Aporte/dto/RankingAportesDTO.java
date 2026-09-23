package dev.LzGuimaraes.FocusLifeHub.Planejamento.Aporte.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import dev.LzGuimaraes.FocusLifeHub.Ativo.CategoriaInvestimento;
import dev.LzGuimaraes.FocusLifeHub.Planejamento.Aporte.EstrategiaAporte;

/**
 * Ranking de prioridade de aporte (Módulos 6 e 9).
 *
 * Une as duas pontuações, que são coisas diferentes:
 *   • QUALITY SCORE       → qualidade do ativo (notas dos checklists do usuário)
 *   • CONTRIBUTION SCORE  → prioridade de aporte (qualidade + distância da meta
 *                           + excesso + prioridade manual, com os pesos que o
 *                           usuário configurou)
 *
 * `peso_quality_aplicado` deixa explícito quando o termo de qualidade não
 * entrou na conta (ativo ainda sem avaliação): nesse caso o termo sai do
 * cálculo e o denominador é renormalizado, para não punir quem não avaliou.
 */
public final class RankingAportesDTO {

    private RankingAportesDTO() {}

    public record Item(
            int posicao,
            UUID ativo_cadastro_id,
            Long meta_id,
            String ticker,
            CategoriaInvestimento classe,

            /* ── Qualidade (Módulo 5) ── */
            BigDecimal quality_score,
            boolean qualidade_avaliada,
            BigDecimal peso_quality_aplicado,

            /* ── Prioridade de aporte (Módulo 6) ── */
            BigDecimal contribution_score,

            /* ── Situação na carteira (Módulo 1) ── */
            BigDecimal percentual_atual,
            BigDecimal percentual_ideal,
            BigDecimal valor_atual,
            BigDecimal valor_ideal,
            BigDecimal deficit,
            BigDecimal excesso,
            Integer prioridade_manual,

            /** Quanto deste aporte este ativo receberia (quando um valor é informado). */
            BigDecimal sugestao_aporte
    ) {}

    public record Response(
            Long carteira_id,
            String moeda,
            BigDecimal valor_total,
            BigDecimal valor_aporte,
            BigDecimal valor_alocado,
            BigDecimal valor_nao_alocado,
            EstrategiaAporte estrategia_aporte,
            List<ScoreConfigDTO.Termo> termos,
            List<String> avisos,
            List<Item> itens
    ) {}
}
