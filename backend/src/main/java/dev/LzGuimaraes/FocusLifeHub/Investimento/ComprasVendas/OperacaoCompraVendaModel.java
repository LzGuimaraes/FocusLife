package dev.LzGuimaraes.FocusLifeHub.Investimento.ComprasVendas;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import dev.LzGuimaraes.FocusLifeHub.Ativo.CategoriaInvestimento;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * Uma OPERAÇÃO de compra e (opcional) venda de um ativo.
 *
 * A linha guarda o custo total da COMPRA ({@code valorInicial}) e, quando a
 * operação foi encerrada, o valor recebido na VENDA ({@code valorVenda}). O
 * lucro/prejuízo e a rentabilidade NÃO ficam gravados: são CALCULADOS no
 * backend ({@link CalculadoraOperacao}) para o front nunca recalcular dinheiro.
 *
 * Venda parcial: quando {@code quantidadeVendida} é menor que {@code quantidade},
 * o custo da parte vendida é rateado por quantidade — o que sobra continua
 * valendo o mesmo preço unitário da compra.
 *
 * {@code ativoCadastroId} aponta para o ticker do catálogo (que guarda SÓ o
 * ticker) e {@code ativoId} para a posição do usuário na carteira. Os dois são
 * INFORMATIVOS: {@code ticker}/{@code nome} ficam em snapshot para a operação
 * continuar legível mesmo se o catálogo ou a posição forem excluídos. Segue o
 * mesmo padrão de {@code aporte_registro} (V29): colunas escalares, sem FK de
 * banco — a integridade referencial é validada na aplicação.
 */
@Entity
@Table(name = "operacao_compra_venda")
@Getter
@Setter
public class OperacaoCompraVendaModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** Carteira de investimento da operação (informativa, opcional). */
    @Column(name = "carteira_investimento_id")
    private Long carteiraInvestimentoId;

    /** Ticker do catálogo global (informativo, opcional). */
    @Column(name = "ativo_cadastro_id")
    private UUID ativoCadastroId;

    /** Posição do usuário (informativo, opcional). */
    @Column(name = "ativo_id")
    private Long ativoId;

    /** Snapshot do ticker — sobrevive à exclusão do catálogo/posição. */
    private String ticker;

    /** Snapshot do nome do ativo. */
    private String nome;

    @Enumerated(EnumType.STRING)
    @Column(length = 40)
    private CategoriaInvestimento classe;

    @Column(name = "data_compra", nullable = false)
    private LocalDate dataCompra;

    @Column(name = "data_venda")
    private LocalDate dataVenda;

    /** Quantidade comprada (aceita fração para cripto/renda fixa). */
    @Column(precision = 18, scale = 8)
    private BigDecimal quantidade;

    /** Quantidade efetivamente vendida; nulo = venda integral. */
    @Column(name = "quantidade_vendida", precision = 18, scale = 8)
    private BigDecimal quantidadeVendida;

    /** Custo TOTAL pago na compra. */
    @Column(name = "valor_inicial", nullable = false, precision = 14, scale = 2)
    private BigDecimal valorInicial = BigDecimal.ZERO;

    /** Valor recebido na venda; nulo = operação ainda aberta. */
    @Column(name = "valor_venda", precision = 14, scale = 2)
    private BigDecimal valorVenda;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
