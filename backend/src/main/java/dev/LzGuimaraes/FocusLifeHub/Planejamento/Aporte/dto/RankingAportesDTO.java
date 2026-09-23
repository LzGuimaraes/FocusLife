package dev.LzGuimaraes.FocusLifeHub.Planejamento.Aporte.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import dev.LzGuimaraes.FocusLifeHub.Ativo.CategoriaInvestimento;
import dev.LzGuimaraes.FocusLifeHub.Planejamento.Aporte.EstrategiaAporte;

/**
 * Ranking de prioridade de aporte (Módulos 6 e 9).
 *
 * Une as duas pontuações, que são coisas diferentes:
 *   • QUALITY SCORE       → qualidade do ativo (notas dos checklists do usuário)
 *   • CONTRIBUTION SCORE  → prioridade de aporte (qualidade + distância da meta
 *                           + excesso + prioridade manual, com os pesos que o
 *                           usuário configurou)
 *
 * `peso_quality_aplicado` deixa explícito quando o termo de qualidade não
 * entrou na conta (ativo ainda sem avaliação): nesse caso o termo sai do
 * cálculo e o denominador é renormalizado, para não punir quem não avaliou.
 */
public final class RankingAportesDTO {

    private RankingAportesDTO() {}

    /**
     * Estado do ativo no motor de decisão (§9 do spec).
     *
     * SEM_AVALIACAO NÃO é "ruim": significa que falta avaliação configurada. O
     * termo de qualidade simplesmente sai da conta (o peso é renormalizado).
     */
    public enum EstadoAtivo {
        APROVADO("Aprovado", "Pode receber aporte normalmente."),
        RESTRITO("Restrito", "Aporte reduzido pelo momento/valuation."),
        NAO_APORTAR("Não aportar", "Bloqueado por critério eliminatório ou limite de concentração."),
        SEM_AVALIACAO("Sem avaliação", "Sem checklist de qualidade/momento respondido.");

        private final String label;
        private final String descricao;

        EstadoAtivo(String label, String descricao) {
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

    /** Status de equilíbrio de um nível (classe/subclasse) frente à tolerância (§18). */
    public enum StatusNivel {
        ABAIXO, EQUILIBRADO, ACIMA, SEM_ALVO
    }

    /** Alerta do motor (§31). O tipo permite filtrar na tela. */
    public record Alerta(String tipo, String mensagem) {}

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

            /* ── Qualidade (Módulo 5) ── */
            BigDecimal quality_score,
            boolean qualidade_avaliada,
            BigDecimal peso_quality_aplicado,

            /* ── Momento / valuation (§10, §11, §12) ── */
            BigDecimal momento_score,
            boolean momento_avaliado,
            /** Fator 0 a 1 aplicado à prioridade de aporte (1 = neutro). */
            BigDecimal fator_momento,

            /* ── Elegibilidade (§8, §9, §19) ── */
            EstadoAtivo estado,
            /** Por que está bloqueado/reduzido (vazio quando aprovado). */
            List<String> bloqueios,
            BigDecimal limite_maximo,
            /** true = atingiu o limite de concentração (não recebe mais). */
            boolean limite_atingido,

            /* ── Prioridade de aporte (Módulo 6) ── */
            BigDecimal contribution_score,

            /* ── Situação na carteira (Módulo 1) ── */
            BigDecimal percentual_atual,
            BigDecimal percentual_ideal,
            BigDecimal valor_atual,
            BigDecimal valor_ideal,
            BigDecimal deficit,
            BigDecimal excesso,
            BigDecimal tolerancia,
            /** Quanto o ativo PODE receber (déficit + tolerância, respeitando o limite). */
            BigDecimal teto,
            Integer prioridade_manual,

            /** Quanto deste aporte o ativo recebeu (null quando nenhum valor foi informado). */
            BigDecimal sugestao_aporte,

            /** Explicação objetiva da decisão (§29). */
            String motivo,

            /** Quanto este item já recebeu de aporte nos últimos 30 dias (§24). */
            BigDecimal aportes_recentes,
            Integer aportes_recentes_qtd
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
            /** Explicação legível do valor não alocado (§32). */
            String nao_alocado_explicacao,
            Boolean redistribuir,
            EstrategiaAporte estrategia_aporte,
            List<ScoreConfigDTO.Termo> termos,
            List<String> avisos,
            List<Alerta> alertas,
            List<ClasseAporteDTO> classes,
            List<Item> itens
    ) {}
}
