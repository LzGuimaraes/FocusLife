package dev.LzGuimaraes.FocusLifeHub.Planejamento.Avaliacao.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * DTOs do CHECKLIST DO ATIVO (Módulos 2, 3 e 5).
 *
 * A âncora é o ativo do catálogo (`ativo_cadastro_id`) ou, para ativos que não
 * existem no catálogo (ex.: renda fixa), a posição (`ativo_id`).
 */
public final class ChecklistAtivoDTO {

    private ChecklistAtivoDTO() {}

    /** Criação: em branco (`perguntas` vazio) ou a partir de um modelo (`modelo_id`). */
    public record Request(
            UUID ativo_cadastro_id,

            Long ativo_id,

            @Size(max = 120, message = "O nome deve ter no máximo 120 caracteres")
            String nome,

            /** Quando informado, o checklist nasce com as perguntas do modelo (snapshot). */
            Long modelo_id,

            @DecimalMin(value = "0.0001", message = "O peso deve ser maior que zero")
            BigDecimal peso,

            Integer ordem,

            @Valid
            List<PerguntaDTO.PerguntaRequest> perguntas
    ) {}

    /** Alteração dos metadados do checklist (as perguntas têm endpoints próprios). */
    public record UpdateRequest(
            @Size(max = 120, message = "O nome deve ter no máximo 120 caracteres")
            String nome,

            @DecimalMin(value = "0.0001", message = "O peso deve ser maior que zero")
            BigDecimal peso,

            Integer ordem,

            Boolean ativa
    ) {}

    public record Response(
            Long id,
            UUID ativo_cadastro_id,
            Long ativo_id,
            String ticker,
            String nome,
            Long modelo_origem_id,
            BigDecimal peso,
            Integer ordem,
            Boolean ativa,

            /** Quality Score deste checklist (null quando nada pontuado foi respondido). */
            BigDecimal score,
            int total_perguntas,
            int total_respondidas,
            int total_pontuadas_respondidas,

            List<PerguntaDTO.PerguntaResponse> perguntas
    ) {}

    /** Uma resposta enviada pelo usuário para uma pergunta. */
    public record RespostaItem(
            @NotNull(message = "O ID da pergunta é obrigatório")
            Long pergunta_id,

            /** Nota digitada (NOTA/SIM_NAO) ou escolhida (MULTIPLA_ESCOLHA). */
            BigDecimal nota,

            /** Valor bruto para tipos com faixa (NUMERO/PERCENTUAL). */
            BigDecimal valor,

            /** Resposta bruta de texto/lista. */
            String texto,

            @Size(max = 1000, message = "A observação deve ter no máximo 1000 caracteres")
            String observacao
    ) {}

    public record RespostasRequest(
            @NotNull(message = "Informe as respostas")
            @Valid
            List<RespostaItem> respostas
    ) {}

    /** Resumo de um ativo avaliado (tela de Avaliação de Ativos). */
    public record AtivoAvaliado(
            UUID ativo_cadastro_id,
            Long ativo_id,
            String ticker,
            int total_checklists,
            int total_perguntas,
            int total_respondidas,

            /** Quality Score do ativo: média dos checklists ponderada por `peso`. */
            BigDecimal quality_score
    ) {}
}
