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
 *   • na SUBCLASSE, o percentual é uma FATIA DA CLASSE (as subclasses somam 100%
 *     da classe) e o "atual" é a soma das posições daquela subclasse — incluindo
 *     as que não têm ticker (renda fixa, caixinhas), atribuídas desde a V26;
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
            /** Faixa (p.p.) em que a classe conta como EQUILIBRADA. */
            BigDecimal tolerancia,
            /** Teto de concentração (%). Ao atingir, a classe não recebe aporte. */
            BigDecimal limite_maximo,
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
            BigDecimal excesso,
            BigDecimal tolerancia,
            BigDecimal limite_maximo,
            /** Setores desta subclasse (opcional). O % do setor é fatia da SUBCLASSE. */
            List<SetorComparativoDTO> setores
    ) {}

    public record SetorComparativoDTO(
            Long id,
            String nome,
            BigDecimal percentual_ideal,
            BigDecimal percentual_atual,
            BigDecimal valor_ideal,
            BigDecimal valor_atual,
            BigDecimal deficit,
            BigDecimal excesso,
            BigDecimal tolerancia,
            BigDecimal limite_maximo
    ) {}

    public record AtivoComparativoDTO(
            Long meta_id,
            UUID ativo_cadastro_id,
            String ticker,
            Long subclasse_id,
            Long setor_id,
            BigDecimal percentual_ideal,
            BigDecimal percentual_atual,
            BigDecimal valor_ideal,
            BigDecimal valor_atual,
            BigDecimal deficit,
            BigDecimal excesso,
            /** Tolerância (p.p. sobre o % ideal) — define o teto do aporte do ativo. */
            BigDecimal tolerancia,
            /** Teto de concentração (%) do ativo. */
            BigDecimal limite_maximo,
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
