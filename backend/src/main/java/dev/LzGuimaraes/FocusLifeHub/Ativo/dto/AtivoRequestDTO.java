package dev.LzGuimaraes.FocusLifeHub.Ativo.dto;

import java.time.LocalDate;
import java.util.UUID;

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

    @JsonFormat(pattern = "yyyy-MM-dd")
    LocalDate dataVencimento,

    Float rentabilidade,

    // ID do ativo do catálogo (ativo_cadastro) — renda variável envia
    UUID ativo_cadastro_id,

    @NotNull(message = "O ID da carteira de investimento é obrigatório")
    Long carteira_investimento_id
) {}
