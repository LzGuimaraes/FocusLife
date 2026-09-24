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
        // preço máximo de compra e prioridade manual saíram do modelo:
        // a decisão de aporte agora usa só déficit + nota do checklist.
        Integer ordem
) {}
