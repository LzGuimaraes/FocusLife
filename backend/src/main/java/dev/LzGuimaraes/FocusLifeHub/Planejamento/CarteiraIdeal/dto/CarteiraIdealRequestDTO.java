package dev.LzGuimaraes.FocusLifeHub.Planejamento.CarteiraIdeal.dto;

import java.math.BigDecimal;
import java.util.List;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Payload completo da Carteira Ideal (classes + subclasses + metas).
 *
 * O PUT é um REPLACE-ALL transacional da configuração da carteira: o que não
 * vier no payload deixa de existir. Como é uma tela de configuração, isso
 * mantém a validação das somas em um único lugar e evita estados parciais.
 */
public record CarteiraIdealRequestDTO(

        /** Estratégia vinculada (opcional — null/ausente desvincula). */
        Long estrategia_id,

        List<ClasseIdealRequestDTO> classes,

        List<MetaIdealRequestDTO> metas
) {

    public record ClasseIdealRequestDTO(
            @NotNull(message = "A classe é obrigatória")
            dev.LzGuimaraes.FocusLifeHub.Ativo.CategoriaInvestimento classe,

            @NotNull(message = "O percentual ideal da classe é obrigatório")
            @DecimalMin(value = "0.0", message = "O percentual ideal não pode ser negativo")
            @DecimalMax(value = "100.0", message = "O percentual ideal não pode passar de 100%")
            BigDecimal percentual_ideal,

            Integer ordem,

            @Size(max = 50, message = "Uma classe pode ter no máximo 50 subclasses")
            List<SubclasseIdealRequestDTO> subclasses
    ) {}

    public record SubclasseIdealRequestDTO(
            @NotNull(message = "O nome da subclasse é obrigatório")
            @Size(max = 80, message = "O nome da subclasse deve ter no máximo 80 caracteres")
            String nome,

            @NotNull(message = "O percentual ideal da subclasse é obrigatório")
            @DecimalMin(value = "0.0", message = "O percentual ideal não pode ser negativo")
            @DecimalMax(value = "100.0", message = "O percentual ideal não pode passar de 100%")
            BigDecimal percentual_ideal,

            Integer ordem
    ) {}

    public record MetaIdealRequestDTO(
            @NotNull(message = "O ID do ativo do catálogo é obrigatório")
            java.util.UUID ativo_cadastro_id,

            @NotNull(message = "A classe da meta é obrigatória")
            dev.LzGuimaraes.FocusLifeHub.Ativo.CategoriaInvestimento classe,

            /** Nome da subclasse já declarada nesta mesma classe (opcional). */
            String subclasse_nome,

            @NotNull(message = "O percentual ideal da meta é obrigatório")
            @DecimalMin(value = "0.0", message = "O percentual ideal não pode ser negativo")
            @DecimalMax(value = "100.0", message = "O percentual ideal não pode passar de 100%")
            BigDecimal percentual_ideal,

            @DecimalMin(value = "0", message = "A prioridade manual vai de 0 a 10")
            @DecimalMax(value = "10", message = "A prioridade manual vai de 0 a 10")
            Integer prioridade_manual,

            Integer ordem
    ) {}
}
