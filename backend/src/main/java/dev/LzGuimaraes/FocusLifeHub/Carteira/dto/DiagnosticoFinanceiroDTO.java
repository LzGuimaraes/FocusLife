package dev.LzGuimaraes.FocusLifeHub.Carteira.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Diagnóstico dos dados financeiros (posições/despesas órfãs de migrações antigas).
 *
 * Nas migrations V8/V9/V10/V11 as carteiras antigas (tb_financas) e as contas
 * (tb_ativos) foram divididas em tabelas novas. Uma conta legada cuja carteira
 * tinha `tipo_carteira` diferente de INVESTIMENTO/DESPESAS ficou SEM vínculo — e
 * como todo acesso passa pela carteira, ela desapareceu das telas sem erro
 * nenhum. Este diagnóstico mostra exatamente isso.
 */
public record DiagnosticoFinanceiroDTO(
        List<CarteiraResumoDTO> carteiras,
        List<CarteiraResumoDTO> carteiras_dividas,
        long total_posicoes,
        long posicoes_sem_carteira,
        long despesas_sem_carteira,
        boolean pode_reparar,
        String limitacao,
        List<String> alertas
) {

    /** Resumo de uma carteira (de investimento ou de dívidas). */
    public record CarteiraResumoDTO(
            Long id,
            String nome,
            String moeda,
            long itens,
            BigDecimal valor
    ) {}

    /** Resultado de um reparo: quantas linhas foram religadas e para onde. */
    public record ReparoResultadoDTO(
            int posicoes_religadas,
            int despesas_religadas,
            Long carteira_investimento_id,
            Long carteira_dividas_id,
            List<String> mensagens
    ) {}

    /** Payload do reparo. Campos nulos = "escolha/crie automaticamente". */
    public record ReparoRequest(
            Long carteira_investimento_id,
            Long carteira_dividas_id
    ) {}
}
