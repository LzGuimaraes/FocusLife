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
            /**
             * PESO no rateio: a própria NOTA. Sem nota, o motor rateia pelo
             * ESPAÇO (capacidade) do ativo — nunca pela ausência de avaliação como
             * se fosse exclusão.
             */
            BigDecimal peso,
            /** Critérios eliminatórios reprovados no checklist (texto pronto). */
            List<String> bloqueios,

            /* ── Elegibilidade ── */
            boolean elegivel,
            StatusElegibilidade status,
            List<String> motivos_inelegibilidade,
            /** Limite de concentração CADASTRADO pelo usuário (null = não há). */
            BigDecimal limite_maximo,
            boolean limite_atingido,
            /** Limite OPERACIONAL em % = meta × (1 + margem). Ex.: meta 5% → 5,25%. */
            BigDecimal limite_operacional_percentual,
            /** Limite que realmente vale = min(operacional, cadastrado). */
            BigDecimal limite_percentual,
            /** Limite final em reais = `limite_percentual × R`. */
            BigDecimal limite_em_reais,
            /**
             * Quanto ainda cabe NELE até o limite — o teto do aporte do ativo.
             * NÃO é o déficit: um ativo exatamente na meta continua com capacidade
             * porque o alvo dele cresce com o patrimônio projetado.
             */
            BigDecimal capacidade_aporte,

            /* ── Situação na carteira ── */
            BigDecimal percentual_atual,
            BigDecimal percentual_ideal,
            BigDecimal valor_atual,
            BigDecimal valor_ideal,
            BigDecimal deficit,
            BigDecimal excesso,
            /** Tolerância (p.p.) da meta: ela só ROTULA o nível, não dá orçamento. */
            BigDecimal tolerancia,

            /** Quanto deste aporte o item recebeu (null quando nenhum valor foi informado). */
            BigDecimal sugestao_aporte,
            /** Preço atual da cota (null = desconhecido). */
            BigDecimal preco_unitario,
            /**
             * QUANTAS UNIDADES comprar: cotas inteiras para ação/FII/ETF e fração
             * (8 casas) para cripto, renda fixa e Tesouro. null quando não há preço
             * para contar.
             */
            BigDecimal quantidade,
            /** Explicação objetiva da decisão. */
            String motivo,
            AcaoAtivo acao
    ) {}

    /**
     * Onde o dinheiro entra: a CLASSE (e a subclasse) abaixo do alvo.
     *
     * O ORÇAMENTO de cada nível é limitado pela CAPACIDADE ELEGÍVEL (o que os
     * ativos dele absorvem) e, quando houver, pelo limite máximo cadastrado. O
     * DÉFICIT do nível é informativo: ele não reserva dinheiro.
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
            /** Limite operacional da classe = meta × (1 + margem). */
            BigDecimal limite_operacional_percentual,
            /** Soma das capacidades dos ativos ELEGÍVEIS da classe. */
            BigDecimal capacidade_elegivel,
            StatusNivel status,
            /** Quanto deste aporte a classe recebe (0 = nada entrou aqui). */
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
            /** Limite operacional em % DO PATRIMÔNIO (o % da subclasse é fatia da classe). */
            BigDecimal limite_operacional_percentual,
            BigDecimal capacidade_elegivel,
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
            /** Margem operacional usada no cálculo (%, relativa à meta). */
            BigDecimal margem_percentual,
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
