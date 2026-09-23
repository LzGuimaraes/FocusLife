package dev.LzGuimaraes.FocusLifeHub.Planejamento.Historico.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Histórico de aportes executados (§24).
 *
 * O registro é feito em LOTE: normalmente o usuário aceita a sugestão do motor
 * e lança todos os aportes do dia de uma vez. Cada item vira uma linha, com o
 * percentual do ativo na carteira antes e depois.
 */
public final class AporteRegistroDTO {

    private AporteRegistroDTO() {}

    public record ItemRequest(
            UUID ativo_cadastro_id,

            Long ativo_id,

            @Size(max = 255, message = "O ticker deve ter no máximo 255 caracteres")
            String ticker,

            String classe,

            @NotNull(message = "O valor do aporte é obrigatório")
            BigDecimal valor,

            /** Opcionais: quando informados, ficam gravados junto do aporte. */
            BigDecimal preco,

            BigDecimal quantidade
    ) {}

    public record Request(
            @NotNull(message = "O ID da carteira de investimento é obrigatório")
            Long carteira_investimento_id,

            /** Data do aporte (default = hoje). */
            LocalDate data,

            @Size(max = 1000, message = "A observação deve ter no máximo 1000 caracteres")
            String observacao,

            @NotNull(message = "Informe os itens do aporte")
            @Size(min = 1, message = "Informe ao menos um item do aporte")
            @Valid
            List<ItemRequest> itens
    ) {}

    public record Response(
            Long id,
            LocalDate data,
            UUID ativo_cadastro_id,
            Long ativo_id,
            String ticker,
            String classe,
            BigDecimal valor,
            BigDecimal preco,
            BigDecimal quantidade,
            BigDecimal percentual_antes,
            BigDecimal percentual_depois,
            String observacao,
            LocalDateTime created_at
    ) {}

    /** Resumo do que foi lançado (retorno do POST). */
    public record Resultado(
            int registrados,
            BigDecimal valor_total,
            LocalDate data,
            List<Response> itens
    ) {}
}
