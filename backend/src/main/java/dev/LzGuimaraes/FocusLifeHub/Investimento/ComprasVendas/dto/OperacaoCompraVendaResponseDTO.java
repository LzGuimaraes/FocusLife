package dev.LzGuimaraes.FocusLifeHub.Investimento.ComprasVendas.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonFormat;

import dev.LzGuimaraes.FocusLifeHub.Ativo.CategoriaInvestimento;

/**
 * Resposta de uma operação. Os campos calculados ({@code lucro},
 * {@code rentabilidade_percentual}, {@code prazo_dias}, {@code aberta}) vêm do
 * backend — o front NÃO recalcula dinheiro.
 */
public record OperacaoCompraVendaResponseDTO(
    Long id,
    Long carteira_investimento_id,
    UUID ativo_cadastro_id,
    Long ativo_id,
    String ticker,
    String nome,
    CategoriaInvestimento classe,

    @JsonFormat(pattern = "yyyy-MM-dd")
    LocalDate data_compra,

    @JsonFormat(pattern = "yyyy-MM-dd")
    LocalDate data_venda,

    BigDecimal quantidade,
    BigDecimal quantidade_vendida,
    BigDecimal valor_inicial,
    BigDecimal valor_venda,

    /** Custo da parte vendida (rateado na venda parcial; null se aberta). */
    BigDecimal custo_vendido,

    /** Lucro/prejuízo em R$ (null na operação aberta). */
    BigDecimal lucro,

    /** Rentabilidade % sobre o custo (null se não houver base). */
    BigDecimal rentabilidade_percentual,

    /** Dias entre compra e venda (null na operação aberta). */
    Long prazo_dias,

    /** true = só compra registrada (resultado não realizado). */
    boolean aberta,

    /** Preço atual do catálogo (usado no cálculo da operação aberta). */
    BigDecimal preco_atual,

    LocalDateTime created_at,
    LocalDateTime updated_at
) {
}
