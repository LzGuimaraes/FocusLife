package dev.LzGuimaraes.FocusLifeHub.Planejamento.Aporte.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import dev.LzGuimaraes.FocusLifeHub.Ativo.CategoriaInvestimento;
import dev.LzGuimaraes.FocusLifeHub.Planejamento.Aporte.EstrategiaAporte;
import dev.LzGuimaraes.FocusLifeHub.Planejamento.Aporte.StatusElegibilidade;

/**
 * Resposta do motor de aporte, com as QUATRO perguntas separadas (§26).
 *
 *   DÉFICIT       → quanto falta para a carteira desejada?      (classe/subclasse/setor)
 *   ELEGIBILIDADE → esse ativo pode receber dinheiro agora?     (elegivel + motivos)
 *   RANKING       → entre os que podem, qual vem primeiro?      (priority_score)
 *   ALOCAÇÃO      → quanto cabe em cada um?                     (capacidade_aporte + sugestao_aporte)
 *
 * Um ativo com déficit enorme e Quality Score 98 continua no `itens[]` (para a
 * tela explicar), mas chega com `elegivel = false`, `motivos_inelegibilidade`
 * preenchidos e `sugestao_aporte = 0`: o déficit dele NÃO o torna comprável.
 */
public final class RankingAportesDTO {

    private RankingAportesDTO() {}

    /** Alerta do motor. O tipo permite filtrar/agrupar na tela. */
    public record Alerta(String tipo, String mensagem) {}

    /**
     * Ação recomendada para o ativo. MANTER ≠ APORTAR: um ativo pode ser bom e
     * continuar na carteira sem receber dinheiro agora (já no alvo, ou
     * descartado por preço/limite).
     */
    public enum AcaoAtivo {
        APORTAR("Aportar", "Recebe parte deste aporte."),
        MANTER("Manter", "Continue com o ativo, mas não aporte agora."),
        NAO_APORTAR("Não aportar", "Descartado na elegibilidade: não recebe neste momento."),
        AVALIAR("Avaliar", "Sem avaliação cadastrada: informe para ele entrar no ranking.");

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

    /** Status de equilíbrio de um nível (classe/subclasse/setor) frente à tolerância. */
    public enum StatusNivel {
        ABAIXO, EQUILIBRADO, ACIMA, SEM_ALVO
    }

    /**
     * Sugestão de REDUÇÃO (§19, §21, §37): quanto está acima do alvo + tolerância
     * e poderia financiar os déficits. O nível diz de quem é o corte.
     */
    public record RebalanceamentoDTO(
            String nivel,
            String nome,
            CategoriaInvestimento classe,
            BigDecimal percentual_atual,
            BigDecimal percentual_ideal,
            BigDecimal excesso,
            BigDecimal sugerido_vender,
            String motivo
    ) {}

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
            /** Setor dentro da subclasse (opcional). */
            Long setor_id,
            String setor_nome,

            /* ── Qualidade (checklists do usuário) ── */
            BigDecimal quality_score,
            boolean qualidade_avaliada,
            BigDecimal peso_quality_aplicado,

            /* ── Momento / valuation ── */
            BigDecimal momento_score,
            boolean momento_avaliado,
            /** Fator 0 a 1 aplicado ao Priority Score (1 = neutro). */
            BigDecimal fator_momento,

            /* ── ELEGIBILIDADE (§2, §3, §17) ── */
            /** false = descartado: NÃO participa do ranking nem do rateio. */
            boolean elegivel,
            StatusElegibilidade status,
            /** Explicação de cada motivo de descarte (vazio quando elegível). */
            List<String> motivos_inelegibilidade,
            /** Critérios eliminatórios reprovados no checklist (texto pronto). */
            List<String> bloqueios,
            BigDecimal limite_maximo,
            /** true = atingiu o limite de concentração (não recebe mais). */
            boolean limite_atingido,
            /** Quanto o ativo ainda pode receber (déficit + tolerância, respeitando o limite). */
            BigDecimal capacidade_aporte,

            /* ── PREÇO (§6, §7, §8) ── */
            BigDecimal preco_atual,
            /** Preço médio pago nas compras registradas — INFORMATIVO, não é regra. */
            BigDecimal preco_medio,
            /** Regra de compra: acima deste preço o ativo é descartado. */
            BigDecimal preco_maximo_compra,
            /** 0..1 = (máximo − atual) / máximo. Null = sem regra de preço para o ativo. */
            BigDecimal oportunidade_preco,

            /* ── RANKING (§10) ── */
            /** Priority Score (0–100): prioridade ENTRE os elegíveis. */
            BigDecimal priority_score,

            /* ── Situação na carteira ── */
            BigDecimal percentual_atual,
            BigDecimal percentual_ideal,
            BigDecimal valor_atual,
            BigDecimal valor_ideal,
            BigDecimal deficit,
            BigDecimal excesso,
            BigDecimal tolerancia,
            Integer prioridade_manual,

            /** Quanto deste aporte o ativo recebeu (null quando nenhum valor foi informado). */
            BigDecimal sugestao_aporte,

            /** Explicação objetiva da decisão (§18) — por que recebeu, ou por que não. */
            String motivo,

            /** Quanto este item já recebeu de aporte nos últimos 30 dias (§24). */
            BigDecimal aportes_recentes,
            Integer aportes_recentes_qtd,

            /** Cálculo aberto do Priority Score: a conta exata, com os pesos aplicados. */
            String formula,

            /** Ação recomendada: aportar, manter, não aportar ou avaliar. */
            AcaoAtivo acao
    ) {}

    /** Uma comparação de cenário: mesmos dados, pesos de decisão diferentes. */
    public record CenarioDTO(
            String nome,
            String descricao,
            String pesos,
            BigDecimal valor_alocado,
            BigDecimal valor_nao_alocado,
            List<ItemCenarioDTO> itens
    ) {}

    public record ItemCenarioDTO(
            String ticker,
            BigDecimal valor
    ) {}

    /**
     * Onde o dinheiro deve entrar: a CLASSE (e a subclasse) abaixo do alvo.
     *
     * A prioridade de aporte é decidida no nível da classe/subclasse — é ali
     * que o usuário define a estratégia. O ticker entra depois, como destino
     * dentro do orçamento já aprovado pela classe.
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
            /** Por que a classe não recebeu (quando for o caso). */
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
            String motivo,
            /** Setores da subclasse (nível opcional). O % do setor é fatia da SUBCLASSE. */
            List<SetorAporteDTO> setores
    ) {}

    public record SetorAporteDTO(
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
            BigDecimal sugerido
    ) {}

    public record Response(
            Long carteira_id,
            String moeda,
            BigDecimal valor_total,
            BigDecimal valor_aporte,
            BigDecimal valor_alocado,
            BigDecimal valor_nao_alocado,
            /** Valor sugerido de VENDA (rebalanceamento) — 0 quando desligado. */
            BigDecimal valor_vendas,
            /** Orçamento usado no plano = aporte + (vendas, quando ligado). */
            BigDecimal valor_orcamento,
            Boolean rebalancear,
            /** TETO_ESTRITO | TETO_ATE_A_CLASSE (configuração vigente). */
            String teto_ativo_modo,
            /** Quantos ativos disputaram o aporte (elegíveis) e quantos foram descartados. */
            Integer total_elegiveis,
            Integer total_descartados,
            /** Explicação legível do valor não alocado (§14). */
            String nao_alocado_explicacao,
            Boolean redistribuir,
            EstrategiaAporte estrategia_aporte,
            List<ScoreConfigDTO.Termo> termos,
            List<String> avisos,
            List<Alerta> alertas,
            /**
             * Ordem em que o motor aplica as travas. O usuário pode reordenar:
             * ela decide COMO o descarte é explicado (o status do ativo é a
             * primeira trava violada nesta ordem). Nenhuma trava é desligada.
             */
            List<String> precedencia,
            /** Cenários comparativos — só quando um valor de aporte foi informado. */
            List<CenarioDTO> cenarios,
            /** Sugestões de redução — vazio quando o rebalanceamento está desligado. */
            List<RebalanceamentoDTO> rebalanceamento,
            List<ClasseAporteDTO> classes,
            /** TODOS os ativos analisados: elegíveis e descartados (com o motivo). */
            List<Item> itens
    ) {}
}
