package dev.LzGuimaraes.FocusLifeHub.Planejamento.Avaliacao.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import dev.LzGuimaraes.FocusLifeHub.Planejamento.Avaliacao.TipoPergunta;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * DTOs de pergunta e de regra de pontuação, compartilhados entre MODELO de
 * checklist e CHECKLIST do ativo (a forma é a mesma; o que muda é o pai).
 *
 * `regras` cobre os dois jeitos de pontuar:
 *   • faixa numérica (NUMERO/PERCENTUAL): valor_min / valor_max / nota
 *   • opção (MULTIPLA_ESCOLHA):           texto / nota
 *   • lista (LISTA):                      texto
 */
public final class PerguntaDTO {

    private PerguntaDTO() {}

    public record RegraRequest(
            @Size(max = 150, message = "O rótulo da regra deve ter no máximo 150 caracteres")
            String texto,

            BigDecimal valor_min,

            BigDecimal valor_max,

            @NotNull(message = "A nota da regra é obrigatória")
            BigDecimal nota,

            Integer ordem
    ) {}

    public record RegraResponse(
            Long id,
            String texto,
            BigDecimal valor_min,
            BigDecimal valor_max,
            BigDecimal nota,
            Integer ordem
    ) {}

    public record PerguntaRequest(
            @NotBlank(message = "O título da pergunta é obrigatório")
            @Size(max = 255, message = "O título deve ter no máximo 255 caracteres")
            String titulo,

            @Size(max = 1000, message = "A descrição deve ter no máximo 1000 caracteres")
            String descricao,

            @NotNull(message = "O tipo da pergunta é obrigatório")
            TipoPergunta tipo,

            @DecimalMin(value = "0.0001", message = "O peso da pergunta deve ser maior que zero")
            BigDecimal peso,

            @NotNull(message = "A nota máxima é obrigatória")
            @DecimalMin(value = "0.0001", message = "A nota máxima deve ser maior que zero")
            BigDecimal nota_maxima,

            /** Se ausente, o backend usa o padrão do tipo (texto/lista não pontuam). */
            Boolean conta_no_score,

            Boolean obrigatoria,

            /**
             * Critério eliminatório: reprovada, o ativo fica em NÃO APORTAR.
             * A reprovação é nota < `nota_minima` (ou nota zero quando não definida).
             */
            Boolean bloqueadora,

            BigDecimal nota_minima,

            Integer ordem,

            @Valid
            List<RegraRequest> regras
    ) {}

    public record PerguntaResponse(
            Long id,
            String titulo,
            String descricao,
            TipoPergunta tipo,
            BigDecimal peso,
            BigDecimal nota_maxima,
            Boolean conta_no_score,
            Boolean obrigatoria,
            Boolean bloqueadora,
            BigDecimal nota_minima,
            Integer ordem,
            List<RegraResponse> regras,

            /* ── Resposta (preenchidos apenas no checklist do ativo) ── */
            BigDecimal nota_atribuida,
            BigDecimal valor_numerico,
            String resposta_texto,
            String observacao,
            LocalDateTime respondido_em
    ) {}
}
