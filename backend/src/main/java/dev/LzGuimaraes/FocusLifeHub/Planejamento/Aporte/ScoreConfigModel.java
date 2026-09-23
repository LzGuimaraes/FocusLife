package dev.LzGuimaraes.FocusLifeHub.Planejamento.Aporte;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnore;

import dev.LzGuimaraes.FocusLifeHub.User.UserModel;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
 * Pesos do Contribution Score e estratégia de rateio do aporte (Módulos 6 e 9).
 *
 * O sistema NÃO impõe metodologia: ele guarda os pesos definidos pelo usuário
 * e aplica a fórmula fixa α·Qualidade + β·Déficit − γ·Excesso + δ·Prioridade.
 * Um registro por usuário; sem registro, valem os pesos padrão de
 * {@link TermoScore} (nada é gravado até o usuário salvar).
 */
@Entity
@Table(name = "score_config")
@Getter
@Setter
public class ScoreConfigModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "peso_quality", nullable = false, precision = 9, scale = 4)
    private BigDecimal pesoQuality = TermoScore.QUALITY.getPesoPadrao();

    @Column(name = "peso_deficit", nullable = false, precision = 9, scale = 4)
    private BigDecimal pesoDeficit = TermoScore.DEFICIT.getPesoPadrao();

    @Column(name = "peso_excesso", nullable = false, precision = 9, scale = 4)
    private BigDecimal pesoExcesso = TermoScore.EXCESSO.getPesoPadrao();

    @Column(name = "peso_prioridade", nullable = false, precision = 9, scale = 4)
    private BigDecimal pesoPrioridade = TermoScore.PRIORIDADE.getPesoPadrao();

    @Enumerated(EnumType.STRING)
    @Column(name = "estrategia_aporte", nullable = false, length = 30)
    private EstrategiaAporte estrategiaAporte = EstrategiaAporte.padrao();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    @JsonIgnore
    private UserModel user;

    /** Configuração padrão (usada quando o usuário ainda não personalizou). */
    public static ScoreConfigModel comPadroes() {
        return new ScoreConfigModel();
    }

    public BigDecimal pesoDe(TermoScore termo) {
        return switch (termo) {
            case QUALITY -> pesoQuality;
            case DEFICIT -> pesoDeficit;
            case EXCESSO -> pesoExcesso;
            case PRIORIDADE -> pesoPrioridade;
        };
    }

    public void definirPeso(TermoScore termo, BigDecimal valor) {
        switch (termo) {
            case QUALITY -> setPesoQuality(valor);
            case DEFICIT -> setPesoDeficit(valor);
            case EXCESSO -> setPesoExcesso(valor);
            case PRIORIDADE -> setPesoPrioridade(valor);
        }
    }

    /** Soma dos pesos — denominador da fórmula. */
    public BigDecimal somaPesos() {
        return pesoQuality.add(pesoDeficit).add(pesoExcesso).add(pesoPrioridade);
    }
}
