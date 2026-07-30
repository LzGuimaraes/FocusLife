package dev.LzGuimaraes.FocusLifeHub.Contas.dto;

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
    Float rentabilidade,
    Boolean pago,
    Long financas_id
) {}