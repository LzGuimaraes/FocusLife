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

            EstrategiaAporte estrategia_aporte
    ) {}

    public record Response(
            Long id,
            BigDecimal peso_quality,
            BigDecimal peso_deficit,
            BigDecimal peso_excesso,
            BigDecimal peso_prioridade,
            BigDecimal peso_quality_efetivo,
            BigDecimal soma_pesos,
            EstrategiaAporte estrategia_aporte,
            /** false = o usuário nunca personalizou (os pesos são os padrões). */
            Boolean personalizada,
            List<Termo> termos
    ) {}
}
