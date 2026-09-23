package dev.LzGuimaraes.FocusLifeHub.Planejamento.Historico;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnore;

import dev.LzGuimaraes.FocusLifeHub.User.UserModel;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * Retrato diário da avaliação de um ativo (Módulo 8).
 *
 * Cada linha guarda o Quality Score consolidado do ativo naquele dia + o
 * contexto da carteira (percentual atual/ideal, déficit, excesso e o
 * Contribution Score). O histórico é append-only: uma vez gravado, o dia passa
 * a ser o registro do que o usuário avaliou naquele momento.
 *
 * `carteiraInvestimentoId` e `ativoId` são referências informativas (sem FK)
 * para o histórico sobreviver à exclusão da carteira/posição; `ticker` é
 * copiado como snapshot.
 */
@Entity
@Table(name = "ativo_score_historico")
@Getter
@Setter
public class AtivoScoreHistoricoModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "carteira_investimento_id")
    private Long carteiraInvestimentoId;

    @Column(name = "ativo_cadastro_id")
    private UUID ativoCadastroId;

    @Column(name = "ativo_id")
    private Long ativoId;

    @Column(length = 120)
    private String ticker;

    @Column(name = "data_referencia", nullable = false)
    private LocalDate dataReferencia;

    @Column(name = "quality_score", precision = 9, scale = 4)
    private BigDecimal qualityScore;

    @Column(name = "total_checklists", nullable = false)
    private Integer totalChecklists = 0;

    @Column(name = "total_perguntas", nullable = false)
    private Integer totalPerguntas = 0;

    @Column(name = "total_respondidas", nullable = false)
    private Integer totalRespondidas = 0;

    @Column(name = "contribution_score", precision = 9, scale = 4)
    private BigDecimal contributionScore;

    @Column(name = "percentual_atual", precision = 9, scale = 4)
    private BigDecimal percentualAtual;

    @Column(name = "percentual_ideal", precision = 9, scale = 4)
    private BigDecimal percentualIdeal;

    @Column(precision = 18, scale = 2)
    private BigDecimal deficit;

    @Column(precision = 18, scale = 2)
    private BigDecimal excesso;

    @Column(name = "valor_carteira_total", precision = 18, scale = 2)
    private BigDecimal valorCarteiraTotal;

    @Column(name = "prioridade_manual")
    private Integer prioridadeManual;

    @Column(length = 1000)
    private String observacao;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    @JsonIgnore
    private UserModel user;
}
