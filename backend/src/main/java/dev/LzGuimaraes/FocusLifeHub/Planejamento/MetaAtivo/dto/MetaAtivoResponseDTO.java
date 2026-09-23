package dev.LzGuimaraes.FocusLifeHub.Planejamento.MetaAtivo.dto;

import java.math.BigDecimal;
import java.util.UUID;

import dev.LzGuimaraes.FocusLifeHub.Ativo.CategoriaInvestimento;

public record MetaAtivoResponseDTO(
        Long id,
        Long carteira_investimento_id,
        UUID ativo_cadastro_id,
        String ticker,
        CategoriaInvestimento classe,
        Long subclasse_id,
        String subclasse_nome,
        BigDecimal percentual_ideal,
        Integer prioridade_manual,
        Integer ordem
) {}
