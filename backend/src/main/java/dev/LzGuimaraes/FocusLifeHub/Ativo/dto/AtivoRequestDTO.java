package dev.LzGuimaraes.FocusLifeHub.Ativo.dto;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonFormat;

import dev.LzGuimaraes.FocusLifeHub.Ativo.CategoriaInvestimento;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AtivoRequestDTO(

    @NotBlank(message = "O nome é obrigatório")
    String nome,

    @NotNull(message = "A categoria de investimento é obrigatória")
    CategoriaInvestimento categoriaInvestimento,

    Float quantidade,

    Float valorUnitario,

    Float precoAtual,

    Float saldo,

    String instituicao,

    String dataAplicacao,

    String vencimento,

    @NotNull(message = "A data de vencimento é obrigatória")
    @JsonFormat(pattern = "yyyy-MM-dd")
    LocalDate dataVencimento,

    Float rentabilidade,

    @NotNull(message = "O ID da carteira de investimento é obrigatório")
    Long carteira_investimento_id
) {}
