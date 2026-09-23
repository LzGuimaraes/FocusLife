package dev.LzGuimaraes.FocusLifeHub.Planejamento.CarteiraIdeal.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import dev.LzGuimaraes.FocusLifeHub.Ativo.CategoriaInvestimento;

/**
 * Ativos que o usuário JÁ TEM na carteira (vindos das posições), para montar as
 * metas a partir do que existe — sem precisar recadastrar os ativos.
 *
 * Inclui também as posições que NÃO estão vinculadas ao catálogo (renda fixa, ou
 * ativo que ficou sem vínculo): elas aparecem agrupadas pelo nome, com
 * `vinculado = false`, para o usuário entender por que não têm meta por ticker e
 * — quando for o caso — vincular com um clique (`sugestao_catalogo_id`).
 *
 * Posições sem vínculo continuam participando do cálculo por CLASSE.
 */
public record MeusAtivosResponseDTO(
        Long carteira_id,
        String moeda,
        BigDecimal valor_total,
        /** Quantidade de POSIÇÕES (não de linhas) sem vínculo com o catálogo. */
        int posicoes_sem_catalogo,
        List<MeuAtivoDTO> ativos
) {

    public record MeuAtivoDTO(
            /** null quando a posição (ou o grupo) não está vinculada ao catálogo. */
            UUID ativo_cadastro_id,
            boolean vinculado,
            /** Nome do ativo no catálogo quando vinculado; senão, o nome da posição. */
            String ticker,
            /** Posições agrupadas nesta linha (permite vincular todas de uma vez). */
            List<Long> ativo_ids,

            /* ── Ajuda para vincular um ativo que ficou "solto" ── */
            UUID sugestao_catalogo_id,
            String sugestao_catalogo_nome,

            CategoriaInvestimento classe,

            /* ── Situação atual na carteira ── */
            BigDecimal quantidade,
            BigDecimal preco_atual,
            BigDecimal valor_atual,
            BigDecimal percentual_atual,

            /* ── Meta já definida (null quando ainda não há meta) ── */
            Long meta_id,
            BigDecimal percentual_ideal,
            /** Tolerância (p.p. sobre o % ideal) — amplia o alvo e define o teto do aporte. */
            BigDecimal tolerancia,
            /** Teto de concentração (%) do ativo (null = sem teto). */
            BigDecimal limite_maximo,
            Integer prioridade_manual,
            Long subclasse_id,
            String subclasse_nome,
            /** Setor dentro da subclasse (nível opcional). */
            Long setor_id,
            String setor_nome
    ) {

        public boolean possuiMeta() {
            return meta_id != null;
        }
    }
}
