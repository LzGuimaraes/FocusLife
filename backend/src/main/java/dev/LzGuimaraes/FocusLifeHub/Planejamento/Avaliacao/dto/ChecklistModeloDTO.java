package dev.LzGuimaraes.FocusLifeHub.Planejamento.Avaliacao.dto;

import java.util.List;

import dev.LzGuimaraes.FocusLifeHub.AtivoCadastro.TipoAtivoCadastro;
import dev.LzGuimaraes.FocusLifeHub.Planejamento.Avaliacao.TipoChecklist;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTOs do MODELO de checklist (Módulo 4).
 *
 * O PUT de alteração é REPLACE-ALL das perguntas (o modelo não guarda
 * respostas, então substituir o conjunto é seguro e mantém a validação em um
 * único lugar). Os checklists já criados a partir do modelo não são afetados.
 */
public final class ChecklistModeloDTO {

    private ChecklistModeloDTO() {}

    public record Request(
            @NotBlank(message = "O nome do modelo é obrigatório")
            @Size(max = 120, message = "O nome deve ter no máximo 120 caracteres")
            String nome,

            @Size(max = 1000, message = "A descrição deve ter no máximo 1000 caracteres")
            String descricao,

            TipoAtivoCadastro tipo_alvo,

            /** QUALIDADE (o ativo é bom?) ou MOMENTO (é hora de aportar?). */
            TipoChecklist tipo,

            Boolean ativa,

            @Valid
            List<PerguntaDTO.PerguntaRequest> perguntas
    ) {}

    public record Response(
            Long id,
            String nome,
            String descricao,
            TipoAtivoCadastro tipo_alvo,
            TipoChecklist tipo,
            Boolean ativa,
            Long user_id,
            List<PerguntaDTO.PerguntaResponse> perguntas
    ) {}

    /** Item de listagem: sem as perguntas, mas com a contagem. */
    public record Resumo(
            Long id,
            String nome,
            String descricao,
            TipoAtivoCadastro tipo_alvo,
            TipoChecklist tipo,
            Boolean ativa,
            long total_perguntas
    ) {}
}
