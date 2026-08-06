package dev.LzGuimaraes.FocusLifeHub.Ativo.dto;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonFormat;

import dev.LzGuimaraes.FocusLifeHub.Ativo.CategoriaInvestimento;

public record AtivoResponseDTO(
    Long id,
    String nome,
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
    Long carteira_investimento_id
) {}
