package dev.LzGuimaraes.FocusLifeHub.Planejamento.CarteiraIdeal.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import dev.LzGuimaraes.FocusLifeHub.Ativo.CategoriaInvestimento;

/** Configuração da Carteira Ideal como está salva (ideal puro, sem comparativo). */
public record CarteiraIdealResponseDTO(
        Long carteira_id,
        String moeda,
        Long estrategia_id,
        String estrategia_nome,
        BigDecimal soma_percentuais_ideal,
        List<ClasseIdealResponseDTO> classes,
        List<MetaIdealResponseDTO> metas,
        List<String> avisos
) {

    public record ClasseIdealResponseDTO(
            Long id,
            CategoriaInvestimento classe,
            BigDecimal percentual_ideal,
            BigDecimal tolerancia,
            BigDecimal limite_maximo,
            Integer ordem,
            List<SubclasseIdealResponseDTO> subclasses
    ) {}

    public record SubclasseIdealResponseDTO(
            Long id,
            String nome,
            BigDecimal percentual_ideal,
            BigDecimal tolerancia,
            BigDecimal limite_maximo,
            Integer ordem,
            List<SetorIdealResponseDTO> setores
    ) {}

    public record SetorIdealResponseDTO(
            Long id,
            /** Setor do catálogo global (null só em dado antigo sem vínculo). */
            Long setor_mercado_id,
            String nome,
            BigDecimal percentual_ideal,
            BigDecimal tolerancia,
            BigDecimal limite_maximo,
            Integer ordem
    ) {}

    public record MetaIdealResponseDTO(
            Long id,
            UUID ativo_cadastro_id,
            String ticker,
            CategoriaInvestimento classe,
            Long subclasse_id,
            String subclasse_nome,
            Long setor_id,
            String setor_nome,
            BigDecimal percentual_ideal,
            BigDecimal tolerancia,
            BigDecimal limite_maximo,
            /** Regra de compra: acima deste preço o ativo é descartado do aporte. */
            BigDecimal preco_maximo_compra,
            Integer prioridade_manual,
            Integer ordem
    ) {}
}
