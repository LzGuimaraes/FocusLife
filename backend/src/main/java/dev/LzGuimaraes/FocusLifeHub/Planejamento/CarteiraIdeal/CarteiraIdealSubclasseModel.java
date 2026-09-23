package dev.LzGuimaraes.FocusLifeHub.Planejamento.CarteiraIdeal;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonIgnore;

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
 * Subclasse da Carteira Ideal (ex.: Ações → Bancos 10%, Energia 8%).
 * A soma das subclasses de uma classe não deve exceder o percentual da classe
 * (gerado como AVISO, não bloqueia — o usuário pode estar em transição).
 */
@Entity
@Table(name = "carteira_ideal_subclasse")
@Getter
@Setter
public class CarteiraIdealSubclasseModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 80)
    private String nome;

    @Column(name = "percentual_ideal", nullable = false, precision = 9, scale = 4)
    private BigDecimal percentualIdeal = BigDecimal.ZERO;

    @Column(nullable = false)
    private Integer ordem = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "classe_id")
    @JsonIgnore
    private CarteiraIdealClasseModel classe;
}
