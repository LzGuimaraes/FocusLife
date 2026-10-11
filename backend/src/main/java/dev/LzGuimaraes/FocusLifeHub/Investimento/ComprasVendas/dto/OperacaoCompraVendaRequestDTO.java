package dev.LzGuimaraes.FocusLifeHub.Investimento.ComprasVendas.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonFormat;

import dev.LzGuimaraes.FocusLifeHub.Ativo.CategoriaInvestimento;
import jakarta.validation.constraints.NotNull;

/**
 * Payload de criação/edição de uma operação de compra e venda.
 * Os nomes dos campos são snake_case, iguais aos do JSON da API.
 */
public record OperacaoCompraVendaRequestDTO(

    /** Carteira de investimento (opcional — a lista é global do usuário). */
    Long carteira_investimento_id,

    /** Ticker do catálogo (ativo_cadastro) — preenche o snapshot. */
    UUID ativo_cadastro_id,

    /** Posição já existente do usuário (opcional). */
    Long ativo_id,

    /** Snapshot do ticker (obrigatório quando não há catálogo/posição). */
    String ticker,

    String nome,

    CategoriaInvestimento classe,

    @NotNull(message = "A data da compra é obrigatória")
    @JsonFormat(pattern = "yyyy-MM-dd")
    LocalDate data_compra,

    @JsonFormat(pattern = "yyyy-MM-dd")
    LocalDate data_venda,

    BigDecimal quantidade,

    /** Quantidade vendida; nulo = venda integral da compra. */
    BigDecimal quantidade_vendida,

    @NotNull(message = "O valor inicial é obrigatório")
    BigDecimal valor_inicial,

    BigDecimal valor_venda
) {
}
