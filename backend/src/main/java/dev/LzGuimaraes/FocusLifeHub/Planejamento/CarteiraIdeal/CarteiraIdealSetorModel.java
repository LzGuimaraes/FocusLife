package dev.LzGuimaraes.FocusLifeHub.Planejamento.CarteiraIdeal;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonIgnore;

import dev.LzGuimaraes.FocusLifeHub.SetorMercado.SetorMercadoModel;
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
 * SETOR da Carteira Ideal — nível opcional entre a SUBCLASSE e o ATIVO.
 *
 * A hierarquia completa é CARTEIRA → CLASSE → SUBCLASSE → SETOR → ATIVO, e o
 * setor herda a lógica da subclasse: o percentual dele é uma FATIA DA
 * SUBCLASSE (a soma dos setores de uma subclasse fecha em 100% dela), com
 * tolerância e limite máximo de concentração próprios.
 *
 * É OPCIONAL de propósito: subclasse sem setor com alvo continua sendo o nível
 * onde o aporte é decidido, então quem não quer setor não convive com ele.
 */
@Entity
@Table(name = "carteira_ideal_setor")
@Getter
@Setter
public class CarteiraIdealSetorModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 80)
    private String nome;

    /** % do setor DENTRO da subclasse (a soma dos setores fecha em 100% dela). */
    @Column(name = "percentual_ideal", nullable = false, precision = 9, scale = 4)
    private BigDecimal percentualIdeal = BigDecimal.ZERO;

    /** Tolerância (pontos percentuais da SUBCLASSE) de equilíbrio. */
    @Column(nullable = false, precision = 9, scale = 4)
    private BigDecimal tolerancia = BigDecimal.ZERO;

    /** Teto de concentração (% do patrimônio). Acima dele, sem novos aportes. */
    @Column(name = "limite_maximo", precision = 9, scale = 4)
    private BigDecimal limiteMaximo;

    @Column(nullable = false)
    private Integer ordem = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subclasse_id")
    @JsonIgnore
    private CarteiraIdealSubclasseModel subclasse;

    /**
     * Setor do CATÁLOGO global (V32) — a identidade deste balde.
     *
     * O `nome` continua sendo o rótulo exibido (compatibilidade com os setores
     * criados antes do catálogo), mas é este vínculo que permite somar exposição
     * por setor entre carteiras e manter a classificação no banco.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "setor_mercado_id")
    @JsonIgnore
    private SetorMercadoModel setorMercado;
}
