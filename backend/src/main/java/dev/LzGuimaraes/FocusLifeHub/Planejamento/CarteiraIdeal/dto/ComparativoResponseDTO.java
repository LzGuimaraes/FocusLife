package dev.LzGuimaraes.FocusLifeHub.Planejamento.CarteiraIdeal.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import dev.LzGuimaraes.FocusLifeHub.Ativo.CategoriaInvestimento;

/**
 * Comparativo Carteira Atual × Carteira Ideal (Módulos 1 e 10).
 *
 * Convenções:
 *   • percentual_atual é sempre sobre o VALOR TOTAL da carteira;
 *   • deficit = max(0, valor_ideal − valor_atual);
 *   • excesso = max(0, valor_atual − valor_ideal);
 *   • a lista de ativos de cada classe traz TAMBÉM os ativos que o usuário tem
 *     mas ainda não têm meta (possui_meta = false): o comparativo é da carteira
 *     real, não de um espaço paralelo;
 *   • na subclasse, o "atual" é a soma das posições dos ativos que possuem
 *     meta naquela subclasse (posições não são classificadas por subclasse);
 *   • classes presentes apenas nas posições aparecem com ideal = 0 (excesso);
 *     classes só no ideal aparecem com atual = 0 (déficit).
 */
public record ComparativoResponseDTO(
        Long carteira_id,
        String moeda,
        BigDecimal valor_total,
        BigDecimal soma_percentuais_ideal,
        List<ClasseComparativoDTO> classes,
        List<String> avisos
) {

    public record ClasseComparativoDTO(
            CategoriaInvestimento classe,
            BigDecimal percentual_ideal,
            BigDecimal percentual_atual,
            BigDecimal valor_ideal,
            BigDecimal valor_atual,
            BigDecimal deficit,
            BigDecimal excesso,
            List<SubclasseComparativoDTO> subclasses,
            List<AtivoComparativoDTO> ativos
    ) {}

    public record SubclasseComparativoDTO(
            Long id,
            String nome,
            BigDecimal percentual_ideal,
            BigDecimal percentual_atual,
            BigDecimal valor_ideal,
            BigDecimal valor_atual,
            BigDecimal deficit,
            BigDecimal excesso
    ) {}

    public record AtivoComparativoDTO(
            Long meta_id,
            UUID ativo_cadastro_id,
            String ticker,
            Long subclasse_id,
            BigDecimal percentual_ideal,
            BigDecimal percentual_atual,
            BigDecimal valor_ideal,
            BigDecimal valor_atual,
            BigDecimal deficit,
            BigDecimal excesso,
            Integer prioridade_manual,
            /**
             * false = o ativo está na carteira do usuário mas ainda NÃO tem meta
             * individual. Nesse caso percentual_ideal é 0 e o valor entra como
             * excesso da classe — o comparativo mostra a carteira inteira, e não
             * apenas os ativos que já foram planejados.
             */
            boolean possui_meta
    ) {}
}
