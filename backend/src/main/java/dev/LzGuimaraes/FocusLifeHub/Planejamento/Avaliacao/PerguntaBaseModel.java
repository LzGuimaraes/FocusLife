package dev.LzGuimaraes.FocusLifeHub.Planejamento.Avaliacao;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;

/**
 * Definição de uma pergunta de checklist — comum ao MODELO (template) e à
 * INSTÂNCIA (checklist do ativo). A instância copia estes campos no momento
 * da criação (snapshot), por isso a definição fica em uma base compartilhada
 * em vez de ser duplicada nas duas entidades.
 */
@MappedSuperclass
@Getter
@Setter
public abstract class PerguntaBaseModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String titulo;

    @Column(length = 1000)
    private String descricao;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TipoPergunta tipo;

    /** Peso da pergunta no score do checklist (nunca zero). */
    @Column(nullable = false, precision = 9, scale = 4)
    private BigDecimal peso = BigDecimal.ONE;

    /** Nota máxima possível desta pergunta. */
    @Column(name = "nota_maxima", nullable = false, precision = 9, scale = 4)
    private BigDecimal notaMaxima = BigDecimal.TEN;

    /** Se false, a pergunta é informativa e não entra no score. */
    @Column(name = "conta_no_score", nullable = false)
    private Boolean contaNoScore = true;

    /**
     * CRITÉRIO ELIMINATÓRIO: se esta pergunta for reprovada, o ativo entra em
     * "NÃO APORTAR" — independentemente do déficit. O déficit continua
     * existindo e sendo mostrado; o que muda é a elegibilidade para NOVOS
     * aportes. A reprovação é a nota abaixo de `notaMinima` (ou nota zero,
     * quando `notaMinima` não for definida).
     */
    @Column(nullable = false)
    private Boolean bloqueadora = false;

    /** Nota mínima para o critério bloqueador ser aprovado (null = qualquer nota > 0). */
    @Column(name = "nota_minima", precision = 9, scale = 4)
    private BigDecimal notaMinima;

    @Column(nullable = false)
    private Integer ordem = 0;
}
