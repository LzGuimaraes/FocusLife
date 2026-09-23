package dev.LzGuimaraes.FocusLifeHub.Planejamento.CarteiraIdeal;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import dev.LzGuimaraes.FocusLifeHub.Ativo.CategoriaInvestimento;
import dev.LzGuimaraes.FocusLifeHub.Carteira.CarteiraInvestimentoModel;
import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * Classe da Carteira Ideal (Módulo 1): quanto a carteira DEVERIA ter em cada
 * classe de investimento. A soma dos percentuais de todas as classes de uma
 * carteira precisa ser 100% (validado no serviço).
 *
 * A classe usa o mesmo enum das posições ({@link CategoriaInvestimento}), o que
 * permite comparar ideal × atual sem tabela de mapeamento.
 */
@Entity
@Table(name = "carteira_ideal_classe")
@Getter
@Setter
public class CarteiraIdealClasseModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private CategoriaInvestimento classe;

    @Column(name = "percentual_ideal", nullable = false, precision = 9, scale = 4)
    private BigDecimal percentualIdeal = BigDecimal.ZERO;

    /** Tolerância (pontos percentuais) em que a classe é considerada EQUILIBRADA. */
    @Column(nullable = false, precision = 9, scale = 4)
    private BigDecimal tolerancia = BigDecimal.ZERO;

    /** Teto de concentração (% do patrimônio). Acima dele, sem novos aportes. */
    @Column(name = "limite_maximo", precision = 9, scale = 4)
    private BigDecimal limiteMaximo;

    @Column(nullable = false)
    private Integer ordem = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "carteira_investimento_id")
    @JsonIgnore
    private CarteiraInvestimentoModel carteiraInvestimento;

    @OneToMany(mappedBy = "classe", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("ordem ASC, id ASC")
    @JsonIgnore
    private List<CarteiraIdealSubclasseModel> subclasses = new ArrayList<>();
}
