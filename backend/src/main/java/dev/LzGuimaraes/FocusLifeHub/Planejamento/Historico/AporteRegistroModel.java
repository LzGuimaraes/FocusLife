package dev.LzGuimaraes.FocusLifeHub.Planejamento.Historico;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * APORTE EXECUTADO (§24): registro do que entrou de dinheiro de verdade, com
 * data, valor, preço/quantidade quando informados e a participação do ativo na
 * carteira ANTES e DEPOIS.
 *
 * O motor usa esse histórico como informação de risco de concentração recente
 * (não como penalidade automática): o objetivo é o usuário ver que já colocou
 * dinheiro naquele ativo nas últimas semanas antes de repetir.
 */
@Entity
@Table(name = "aporte_registro")
@Getter
@Setter
public class AporteRegistroModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "carteira_investimento_id")
    private Long carteiraInvestimentoId;

    @Column(nullable = false)
    private LocalDate data;

    @Column(name = "ativo_cadastro_id")
    private UUID ativoCadastroId;

    @Column(name = "ativo_id")
    private Long ativoId;

    private String ticker;

    @Column(length = 40)
    private String classe;

    @Column(nullable = false, precision = 18, scale = 4)
    private BigDecimal valor = BigDecimal.ZERO;

    @Column(precision = 18, scale = 4)
    private BigDecimal preco;

    @Column(precision = 18, scale = 8)
    private BigDecimal quantidade;

    /** % do ativo no patrimônio ANTES deste aporte. */
    @Column(name = "percentual_antes", precision = 9, scale = 4)
    private BigDecimal percentualAntes;

    /** % do ativo no patrimônio DEPOIS deste aporte. */
    @Column(name = "percentual_depois", precision = 9, scale = 4)
    private BigDecimal percentualDepois;

    @Column(length = 1000)
    private String observacao;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
