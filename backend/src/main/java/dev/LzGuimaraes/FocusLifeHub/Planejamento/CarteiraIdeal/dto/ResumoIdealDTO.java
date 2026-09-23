package dev.LzGuimaraes.FocusLifeHub.Planejamento.CarteiraIdeal.dto;

import java.math.BigDecimal;
import java.util.List;

import dev.LzGuimaraes.FocusLifeHub.Ativo.CategoriaInvestimento;

/**
 * Resumo enxuto para o Dashboard (Módulo 10): só o agregado por classe, sem
 * subclasses/ativos — evita trafegar o comparativo inteiro no widget.
 */
public record ResumoIdealDTO(
        Long carteira_id,
        String moeda,
        BigDecimal valor_total,
        BigDecimal soma_percentuais_ideal,
        int total_classes,
        int total_ativos_com_meta,
        List<ClasseResumoDTO> classes,
        List<String> avisos
) {

    public record ClasseResumoDTO(
            CategoriaInvestimento classe,
            BigDecimal percentual_ideal,
            BigDecimal percentual_atual,
            BigDecimal valor_ideal,
            BigDecimal valor_atual,
            BigDecimal deficit,
            BigDecimal excesso
    ) {}
}
