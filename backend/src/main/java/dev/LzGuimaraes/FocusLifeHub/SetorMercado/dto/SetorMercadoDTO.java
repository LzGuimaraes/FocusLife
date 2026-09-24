package dev.LzGuimaraes.FocusLifeHub.SetorMercado.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Setor de mercado (catálogo global).
 *
 * `ativos` e `alvos_carteira` são CONTAGENS DE USO: servem para o usuário saber
 * o que quebra antes de desativar/excluir um setor — e para conferir se a
 * classificação manual do catálogo pegou.
 */
public final class SetorMercadoDTO {

    private SetorMercadoDTO() {}

    public record Response(
            Long id,
            String nome,
            String slug,
            String segmento,
            Boolean ativo,
            /** Quantos tickers do catálogo estão neste setor. */
            long ativos,
            /** Quantos alvos de Carteira Ideal usam este setor. */
            long alvos_carteira
    ) {}

    public record Request(
            @NotBlank(message = "O nome do setor é obrigatório")
            @Size(max = 120, message = "O nome do setor deve ter no máximo 120 caracteres")
            String nome,

            @Size(max = 120, message = "O segmento deve ter no máximo 120 caracteres")
            String segmento
    ) {}

    public record AlterRequest(
            @Size(max = 120, message = "O nome do setor deve ter no máximo 120 caracteres")
            String nome,

            @Size(max = 120, message = "O segmento deve ter no máximo 120 caracteres")
            String segmento,

            /** false = esconde o setor das listas sem apagar histórico. */
            Boolean ativo
    ) {}
}
