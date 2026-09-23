package dev.LzGuimaraes.FocusLifeHub.Planejamento.Avaliacao;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;

/**
 * Regra de pontuação de uma pergunta — comum ao MODELO e à INSTÂNCIA.
 *
 * Cobre os dois jeitos de pontuar sem duplicar tabela:
 *   • FAIXA  (tipos NUMERO/PERCENTUAL): usa valorMin/valorMax → nota
 *   • OPÇÃO  (tipo MULTIPLA_ESCOLHA):   usa texto → nota
 *   • LISTA  (tipo LISTA):              usa texto (informativo)
 */
@MappedSuperclass
@Getter
@Setter
public abstract class RegraBaseModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Rótulo da opção (múltipla escolha/lista) ou da faixa (ex.: "15% a 25%"). */
    @Column(length = 150)
    private String texto;

    @Column(name = "valor_min", precision = 18, scale = 4)
    private BigDecimal valorMin;

    /** Nulo significa "sem limite superior" (ex.: acima de 15%). */
    @Column(name = "valor_max", precision = 18, scale = 4)
    private BigDecimal valorMax;

    @Column(nullable = false, precision = 9, scale = 4)
    private BigDecimal nota = BigDecimal.ZERO;

    @Column(nullable = false)
    private Integer ordem = 0;
}
