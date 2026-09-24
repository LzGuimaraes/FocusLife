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

    /** Peso do termo MOMENTO na fórmula (0 desliga o termo). */
    @Column(name = "peso_momento", nullable = false, precision = 9, scale = 4)
    private BigDecimal pesoMomento = TermoScore.MOMENTO.getPesoPadrao();

    /**
     * Peso do termo OPORTUNIDADE DE PREÇO (0 desliga o termo).
     *
     * Só entra para ativos ELEGÍVEIS que tenham preço máximo de compra
     * configurado: quanto mais o preço atual está abaixo do seu limite, mais
     * oportunidade existe. Nunca compensa uma regra de compra violada — quem
     * viola o preço máximo é descartado antes do ranking, não pontuado.
     */
    @Column(name = "peso_preco", nullable = false, precision = 9, scale = 4)
    private BigDecimal pesoPreco = TermoScore.PRECO.getPesoPadrao();

    /* ── Faixas da NOTA DE MOMENTO → FATOR de aporte (0 a 1) ──
       até faixa1 → 0 | até faixa2 → 0,25 | até faixa3 → 0,50 | até faixa4 → 0,75
       acima da faixa4 → 1,00. Valores configuráveis pelo usuário. */

    @Column(name = "momento_faixa_1", nullable = false, precision = 9, scale = 4)
    private BigDecimal momentoFaixa1 = new BigDecimal("25");

    @Column(name = "momento_faixa_2", nullable = false, precision = 9, scale = 4)
    private BigDecimal momentoFaixa2 = new BigDecimal("50");

    @Column(name = "momento_faixa_3", nullable = false, precision = 9, scale = 4)
    private BigDecimal momentoFaixa3 = new BigDecimal("75");

    @Column(name = "momento_faixa_4", nullable = false, precision = 9, scale = 4)
    private BigDecimal momentoFaixa4 = new BigDecimal("90");

    /**
     * true = o valor que não encontrou destino elegível procura outra classe com
     * déficit; false = fica não alocado (sempre com o motivo explicado na tela).
     */
    @Column(nullable = false)
    private Boolean redistribuir = true;

    /**
     * true = o motor também sugere REDUZIR o que passou do alvo (dentro da faixa
     * de tolerância) e soma esse valor ao orçamento do aporte. Sugestão — nada
     * é vendido automaticamente.
     */
    @Column(nullable = false)
    private Boolean rebalancear = false;

    /**
     * Como o TETO do ativo é calculado:
     *   TETO_ESTRITO      → déficit do ativo + tolerância (padrão)
     *   TETO_ATE_A_CLASSE → o teto do ativo passa a ser o déficit da
     *                       classe/subclasse a que ele pertence
     * O limite máximo de concentração continua valendo nos dois modos.
     */
    @Column(name = "teto_ativo_modo", nullable = false, length = 30)
    private String tetoAtivoModo = "TETO_ESTRITO";

    /**
     * Ordem das travas (CSV): BLOQUEIO,LIMITE,PRECO,CLASSE,SUBCLASSE,SETOR,
     * TETO_ATIVO,MOMENTO,SCORE. Define QUAL descarte aparece como status do
     * ativo (a primeira trava violada nesta ordem) e como o motivo é explicado.
     */
    @Column(nullable = false, length = 300)
    private String precedencia = "BLOQUEIO,LIMITE,PRECO,CLASSE,SUBCLASSE,SETOR,TETO_ATIVO,MOMENTO,SCORE";

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
            case QUALITY -> nz(pesoQuality, TermoScore.QUALITY);
            case DEFICIT -> nz(pesoDeficit, TermoScore.DEFICIT);
            case EXCESSO -> nz(pesoExcesso, TermoScore.EXCESSO);
            case PRIORIDADE -> nz(pesoPrioridade, TermoScore.PRIORIDADE);
            case MOMENTO -> nz(pesoMomento, TermoScore.MOMENTO);
            case PRECO -> nz(pesoPreco, TermoScore.PRECO);
        };
    }

    /**
     * Pesos nulos vêm de configurações gravadas antes do termo existir
     * (coluna nova): cai para o peso padrão em vez de 0, senão o termo novo
     * entraria desligado para quem já usava o sistema.
     */
    private BigDecimal nz(BigDecimal valor, TermoScore termo) {
        return (valor != null) ? valor : termo.getPesoPadrao();
    }

    public void definirPeso(TermoScore termo, BigDecimal valor) {
        switch (termo) {
            case QUALITY -> setPesoQuality(valor);
            case DEFICIT -> setPesoDeficit(valor);
            case EXCESSO -> setPesoExcesso(valor);
            case PRIORIDADE -> setPesoPrioridade(valor);
            case MOMENTO -> setPesoMomento(valor);
            case PRECO -> setPesoPreco(valor);
        };
    }

    /** Soma dos pesos — denominador da fórmula. */
    public BigDecimal somaPesos() {
        return pesoDe(TermoScore.QUALITY)
                .add(pesoDe(TermoScore.DEFICIT))
                .add(pesoDe(TermoScore.EXCESSO))
                .add(pesoDe(TermoScore.PRIORIDADE))
                .add(pesoDe(TermoScore.MOMENTO))
                .add(pesoDe(TermoScore.PRECO));
    }
}
