package dev.LzGuimaraes.FocusLifeHub.Planejamento.MetaAtivo.dto;

import java.math.BigDecimal;
import java.util.UUID;

import dev.LzGuimaraes.FocusLifeHub.Ativo.CategoriaInvestimento;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public record MetaAtivoRequestDTO(

        @NotNull(message = "O ID da carteira de investimento é obrigatório")
        Long carteira_investimento_id,

        @NotNull(message = "O ID do ativo do catálogo é obrigatório")
        UUID ativo_cadastro_id,

        @NotNull(message = "A classe é obrigatória")
        CategoriaInvestimento classe,

        Long subclasse_id,

        @NotNull(message = "O percentual ideal é obrigatório")
        @DecimalMin(value = "0.0", message = "O percentual ideal não pode ser negativo")
        @DecimalMax(value = "100.0", message = "O percentual ideal não pode passar de 100%")
        BigDecimal percentual_ideal,

        Integer ordem
) {}
