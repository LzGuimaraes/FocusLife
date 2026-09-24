package dev.LzGuimaraes.FocusLifeHub.Planejamento.Aporte.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import dev.LzGuimaraes.FocusLifeHub.Ativo.CategoriaInvestimento;
import dev.LzGuimaraes.FocusLifeHub.Planejamento.Aporte.StatusElegibilidade;

/**
 * Resposta do motor de aporte, com as perguntas separadas (e só elas):
 *
 *   DÉFICIT       → quanto falta para a carteira desejada?   (classe/subclasse)
 *   ELEGIBILIDADE → esse ativo pode receber dinheiro agora?  (elegivel + motivos)
 *   NOTA          → entre os que podem, qual vem primeiro?   (nota do checklist)
 *   ALOCAÇÃO      → quanto cabe em cada um?                  (capacidade + sugestão)
 *
 * A NOTA vem do checklist (por subclasse, nota por ativo) e é o ÚNICO critério
 * de ordem: não há mais pesos configuráveis, cenários nem rebalanceamento.
 */
public final class RankingAportesDTO {

    private RankingAportesDTO() {}

    /** Alerta do motor. O tipo permite filtrar/agrupar na tela. */
    public record Alerta(String tipo, String mensagem) {}

    /**
     * Ação recomendada. MANTER ≠ APORTAR: um ativo pode ser bom e continuar na
     * carteira sem receber dinheiro agora (já no alvo, sem nota, no limite).
     */
    public enum AcaoAtivo {
        APORTAR("Aportar", "Recebe parte deste aporte."),
        MANTER("Manter", "Continue com o ativo, mas não aporte agora."),
        NAO_APORTAR("Não aportar", "Descartado na elegibilidade: não recebe neste momento."),
        AVALIAR("Avaliar", "Sem nota no checklist: avalie para ele entrar na ordem.");

        private final String label;
        private final String descricao;

        AcaoAtivo(String label, String descricao) {
            this.label = label;
            this.descricao = descricao;
        }

        public String getLabel() {
            return label;
        }

        public String getDescricao() {
            return descricao;
        }
    }

    /** Status de equilíbrio de um nível (classe/subclasse) frente à tolerância. */
    public enum StatusNivel {
        ABAIXO, EQUILIBRADO, ACIMA, SEM_ALVO
    }

    public record Item(
            int posicao,
            UUID ativo_cadastro_id,
            Long meta_id,
            String ticker,
            CategoriaInvestimento classe,
            /** false = posição sem ticker de catálogo (renda fixa, caixinha). */
            boolean vinculado,
            Long subclasse_id,
            String subclasse_nome,

            /* ── NOTA do checklist (é o critério de ordem) ── */
            BigDecimal nota,
            boolean avaliada,
            int perguntas,
            int respondidas,
            /** Critérios eliminatórios reprovados no checklist (texto pronto). */
            List<String> bloqueios,

            /* ── Elegibilidade ── */
            boolean elegivel,
            StatusElegibilidade status,
            List<String> motivos_inelegibilidade,
            BigDecimal limite_maximo,
            boolean limite_atingido,
            /** Quanto o ativo ainda pode receber (déficit + tolerância, respeitando o limite). */
            BigDecimal capacidade_aporte,

            /* ── Situação na carteira ── */
            BigDecimal percentual_atual,
            BigDecimal percentual_ideal,
            BigDecimal valor_atual,
            BigDecimal valor_ideal,
            BigDecimal deficit,
            BigDecimal excesso,
            BigDecimal tolerancia,

            /** Quanto deste aporte o item recebeu (null quando nenhum valor foi informado). */
            BigDecimal sugestao_aporte,
            /** Explicação objetiva da decisão. */
            String motivo,
            AcaoAtivo acao
    ) {}

    /**
     * Onde o dinheiro entra: a CLASSE (e a subclasse) abaixo do alvo.
     *
     * A prioridade é decidida no nível da classe/subclasse — o ativo entra
     * depois, como destino dentro do orçamento já aprovado pela classe.
     */
    public record ClasseAporteDTO(
            CategoriaInvestimento classe,
            BigDecimal percentual_atual,
            BigDecimal percentual_ideal,
            BigDecimal valor_atual,
            BigDecimal valor_ideal,
            BigDecimal deficit,
            BigDecimal excesso,
            BigDecimal tolerancia,
            BigDecimal limite_maximo,
            StatusNivel status,
            /** Quanto deste aporte a classe recebe (0 = já está no alvo ou acima). */
            BigDecimal sugerido,
            String motivo,
            List<SubclasseAporteDTO> subclasses
    ) {}

    public record SubclasseAporteDTO(
            Long id,
            String nome,
            BigDecimal percentual_atual,
            BigDecimal percentual_ideal,
            BigDecimal valor_atual,
            BigDecimal valor_ideal,
            BigDecimal deficit,
            BigDecimal excesso,
            BigDecimal tolerancia,
            BigDecimal limite_maximo,
            StatusNivel status,
            BigDecimal sugerido,
            String motivo
    ) {}

    public record Response(
            Long carteira_id,
            String moeda,
            /** Patrimônio de HOJE (sem o aporte). */
            BigDecimal valor_total,
            BigDecimal valor_aporte,
            /**
             * Patrimônio DEPOIS do aporte (hoje + aporte). É a referência dos
             * alvos: o déficit de cada classe/ativo é medido contra ele, porque
             * depois de investir o total muda e o alvo (% do total) cresce junto.
             */
            BigDecimal valor_total_com_aporte,
            BigDecimal valor_alocado,
            BigDecimal valor_nao_alocado,
            /** Quantos ativos podem receber e quantos foram descartados. */
            Integer total_elegiveis,
            Integer total_descartados,
            /** Explicação legível do valor não alocado. */
            String nao_alocado_explicacao,
            List<String> avisos,
            List<Alerta> alertas,
            List<ClasseAporteDTO> classes,
            /** TODOS os ativos analisados: elegíveis e descartados (com o motivo). */
            List<Item> itens
    ) {}
}
