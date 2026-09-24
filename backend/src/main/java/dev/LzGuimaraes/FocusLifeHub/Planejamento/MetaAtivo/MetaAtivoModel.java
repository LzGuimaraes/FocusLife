package dev.LzGuimaraes.FocusLifeHub.Planejamento.MetaAtivo;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonIgnore;

import dev.LzGuimaraes.FocusLifeHub.Ativo.CategoriaInvestimento;
import dev.LzGuimaraes.FocusLifeHub.AtivoCadastro.AtivoCadastroModel;
import dev.LzGuimaraes.FocusLifeHub.Carteira.CarteiraInvestimentoModel;
import dev.LzGuimaraes.FocusLifeHub.Planejamento.CarteiraIdeal.CarteiraIdealSubclasseModel;
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
 * Meta individual de um ativo do catálogo dentro de uma carteira (Módulo 1) e
 * sua prioridade manual de aporte 0–10 (Módulo 7).
 *
 * A meta é do ATIVO (ativo_cadastro / ticker), não da linha de posição: se o
 * usuário tiver duas posições do mesmo ticker, a meta é uma só.
 */
@Entity
@Table(name = "meta_ativo")
@Getter
@Setter
public class MetaAtivoModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** % ideal que este ativo deve representar no valor total da carteira. */
    @Column(name = "percentual_ideal", nullable = false, precision = 9, scale = 4)
    private BigDecimal percentualIdeal = BigDecimal.ZERO;

    /**
     * Tolerância (pontos percentuais SOBRE o % ideal) que ainda conta como "no
     * alvo" e que define o TETO do aporte deste ativo.
     */
    @Column(nullable = false, precision = 9, scale = 4)
    private BigDecimal tolerancia = BigDecimal.ZERO;

    /** Teto de concentração (% do patrimônio). Acima dele, sem novos aportes. */
    @Column(name = "limite_maximo", precision = 9, scale = 4)
    private BigDecimal limiteMaximo;

    /**
     * PREÇO MÁXIMO DE COMPRA — regra de ELEGIBILIDADE definida pelo investidor.
     *
     * Acima deste preço o ativo é DESCARTADO do ranking de aporte (não recebe
     * dinheiro novo), por melhor que seja o Quality Score dele. NULL = o
     * investidor não definiu regra de preço para este ativo: ele concorre
     * normalmente. É um número absoluto (preço por cota/ação).
     */
    @Column(name = "preco_maximo_compra", precision = 14, scale = 2)
    private BigDecimal precoMaximoCompra;

    /** Prioridade manual de aporte definida pelo usuário (0 a 10). */
    @Column(name = "prioridade_manual", nullable = false)
    private Integer prioridadeManual = 0;

    @Column(nullable = false)
    private Integer ordem = 0;

    /** Classe da carteira ideal à qual a meta pertence (denormalizado p/ validação). */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private CategoriaInvestimento classe;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "carteira_investimento_id")
    @JsonIgnore
    private CarteiraInvestimentoModel carteiraInvestimento;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ativo_cadastro_id")
    @JsonIgnore
    private AtivoCadastroModel ativoCadastro;

    /** Subclasse opcional (ex.: Ações → Bancos). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subclasse_id")
    @JsonIgnore
    private CarteiraIdealSubclasseModel subclasse;

    /**
     * SETOR opcional (ex.: Ações → Bancos → "Bancos grandes"), dentro da
     * subclasse. Quando a subclasse tem setores com alvo, o aporte é decidido
     * nesse nível antes de chegar ao ticker.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "setor_id")
    @JsonIgnore
    private dev.LzGuimaraes.FocusLifeHub.Planejamento.CarteiraIdeal.CarteiraIdealSetorModel setor;
}
