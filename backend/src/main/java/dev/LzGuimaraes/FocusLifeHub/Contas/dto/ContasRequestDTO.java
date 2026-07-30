package dev.LzGuimaraes.FocusLifeHub.Contas.dto;

import dev.LzGuimaraes.FocusLifeHub.Contas.CategoriaAtivo;
import dev.LzGuimaraes.FocusLifeHub.Contas.CategoriaInvestimento;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record ContasRequestDTO(

    @NotBlank(message = "O nome é obrigatório")
    String nome,

    @NotNull(message = "A categoria é obrigatória (CONTA ou INVESTIMENTO)")
    CategoriaAtivo categoria,

    CategoriaInvestimento categoriaInvestimento,

    @PositiveOrZero(message = "A quantidade não pode ser negativa.")
    Float quantidade,

    @PositiveOrZero(message = "O valor unitário não pode ser negativo.")
    Float valorUnitario,

    @PositiveOrZero(message = "O preço atual não pode ser negativo.")
    Float precoAtual,

    @NotNull(message = "O ID da carteira (financas_id) é obrigatório")
    Long financas_id,

    @PositiveOrZero(message = "O saldo não pode ser negativo.")
    Float saldo,

    String instituicao,

    String dataAplicacao,

    String vencimento,

    @PositiveOrZero(message = "A rentabilidade não pode ser negativa.")
    Float rentabilidade,

    Boolean pago
) {}