package dev.LzGuimaraes.FocusLifeHub.Planejamento.CarteiraIdeal.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import dev.LzGuimaraes.FocusLifeHub.Ativo.CategoriaInvestimento;

/**
 * Ativos que o usuário JÁ TEM na carteira (vindos das posições), para montar as
 * metas a partir do que existe — sem precisar recadastrar os ativos.
 *
 * Só entram posições vinculadas ao catálogo (`ativo_cadastro_id`), porque a
 * meta individual é por ticker. Posições sem vínculo (ex.: renda fixa) são
 * contadas em `posicoes_sem_catalogo` para a interface avisar o usuário — elas
 * continuam participando do cálculo por CLASSE normalmente.
 */
public record MeusAtivosResponseDTO(
        Long carteira_id,
        String moeda,
        BigDecimal valor_total,
        int posicoes_sem_catalogo,
        List<MeuAtivoDTO> ativos
) {

    public record MeuAtivoDTO(
            UUID ativo_cadastro_id,
            String ticker,
            CategoriaInvestimento classe,

            /* ── Situação atual na carteira ── */
            BigDecimal quantidade,
            BigDecimal preco_atual,
            BigDecimal valor_atual,
            BigDecimal percentual_atual,

            /* ── Meta já definida (null quando ainda não há meta) ── */
            Long meta_id,
            BigDecimal percentual_ideal,
            Integer prioridade_manual,
            Long subclasse_id,
            String subclasse_nome
    ) {

        public boolean possuiMeta() {
            return meta_id != null;
        }
    }
}
