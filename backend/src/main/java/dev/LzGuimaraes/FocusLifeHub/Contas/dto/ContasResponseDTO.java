package dev.LzGuimaraes.FocusLifeHub.Contas.dto;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonFormat;

import dev.LzGuimaraes.FocusLifeHub.Contas.CategoriaAtivo;
import dev.LzGuimaraes.FocusLifeHub.Contas.CategoriaInvestimento;

public record ContasResponseDTO(
    Long id,
    String nome,
    CategoriaAtivo categoria,
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
    Boolean pago,
    Long carteira_investimento_id,
    Long carteira_dividas_id
) {}