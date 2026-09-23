package dev.LzGuimaraes.FocusLifeHub.Planejamento.Aporte.dto;

import java.math.BigDecimal;
import java.util.List;

import dev.LzGuimaraes.FocusLifeHub.Planejamento.Aporte.EstrategiaAporte;
import dev.LzGuimaraes.FocusLifeHub.Planejamento.Aporte.TermoScore;
import jakarta.validation.constraints.DecimalMin;

/**
 * DTOs da configuração de pontuação (Módulo 6).
 *
 * `termos` devolve o catálogo do que cada termo significa, junto com o peso
 * vigente — é o que torna a fórmula transparente para o usuário.
 */
public final class ScoreConfigDTO {

    private ScoreConfigDTO() {}

    public record Termo(
            TermoScore termo,
            String label,
            String descricao,
            BigDecimal peso,
            BigDecimal peso_padrao
    ) {}

    public record Request(
            @DecimalMin(value = "0.0", message = "O peso da qualidade não pode ser negativo")
            BigDecimal peso_quality,

            @DecimalMin(value = "0.0", message = "O peso do déficit não pode ser negativo")
            BigDecimal peso_deficit,

            @DecimalMin(value = "0.0", message = "O peso do excesso não pode ser negativo")
            BigDecimal peso_excesso,

            @DecimalMin(value = "0.0", message = "O peso da prioridade não pode ser negativo")
            BigDecimal peso_prioridade,

            @DecimalMin(value = "0.0", message = "O peso do momento não pode ser negativo")
            BigDecimal peso_momento,

            /** Faixas da nota de momento → fator (0 / 0,25 / 0,50 / 0,75 / 1,00). */
            BigDecimal momento_faixa_1,
            BigDecimal momento_faixa_2,
            BigDecimal momento_faixa_3,
            BigDecimal momento_faixa_4,

            Boolean redistribuir,

            /** true = sugerir redução do que está acima do alvo (com faixa de tolerância). */
            Boolean rebalancear,

            /** TETO_ESTRITO | TETO_ATE_A_CLASSE. */
            String teto_ativo_modo,

            /** Ordem das travas: BLOQUEIO,LIMITE,CLASSE,SUBCLASSE,SETOR,TETO_ATIVO,MOMENTO,SCORE. */
            String precedencia,

            EstrategiaAporte estrategia_aporte
    ) {}

    public record Response(
            Long id,
            BigDecimal peso_quality,
            BigDecimal peso_deficit,
            BigDecimal peso_excesso,
            BigDecimal peso_prioridade,
            BigDecimal peso_momento,
            BigDecimal peso_quality_efetivo,
            BigDecimal soma_pesos,
            BigDecimal momento_faixa_1,
            BigDecimal momento_faixa_2,
            BigDecimal momento_faixa_3,
            BigDecimal momento_faixa_4,
            Boolean redistribuir,
            Boolean rebalancear,
            String teto_ativo_modo,
            String precedencia,
            EstrategiaAporte estrategia_aporte,
            /** false = o usuário nunca personalizou (os pesos são os padrões). */
            Boolean personalizada,
            List<Termo> termos
    ) {}
}
