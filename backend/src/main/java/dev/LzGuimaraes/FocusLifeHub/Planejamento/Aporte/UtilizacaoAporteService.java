package dev.LzGuimaraes.FocusLifeHub.Planejamento.Aporte;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.function.Function;

import org.springframework.stereotype.Service;

import dev.LzGuimaraes.FocusLifeHub.Planejamento.CarteiraIdeal.dto.ComparativoResponseDTO;

/** Ajusta somente a margem do plano para procurar uma sobra de até 5% do aporte. */
@Service
public class UtilizacaoAporteService {
    private static final BigDecimal PASSO = new BigDecimal("0.01");
    private final AlocacaoService alocacao;

    public UtilizacaoAporteService(AlocacaoService alocacao) {
        this.alocacao = alocacao;
    }

    public record Resultado(BigDecimal margemConfigurada, BigDecimal margemAplicada,
                            List<AporteCandidato> candidatos, OrcamentoAporte orcamento) {
        public boolean ajustada() {
            return margemAplicada.compareTo(margemConfigurada) > 0;
        }
    }

    /** A função deve manter a ordem e a identidade dos candidatos em todas as margens. */
    public Resultado calcular(ComparativoResponseDTO referencia, BigDecimal aporte, double margemConfigurada,
                              Function<Double, List<AporteCandidato>> candidatosNaMargem) {
        BigDecimal base = BigDecimal.valueOf(margemConfigurada).setScale(2, RoundingMode.HALF_UP);
        Resultado inicial = avaliar(referencia, aporte, base, base, candidatosNaMargem, null);
        if (aporte == null || aporte.signum() <= 0 || MetaUtilizacaoAporte.atingida(aporte, inicial.orcamento())) {
            return inicial;
        }

        BigDecimal maxima = margemSemRestricaoOperacional(referencia, base, inicial.candidatos());
        if (maxima.compareTo(base) <= 0) {
            return inicial;
        }
        Resultado viavel = avaliar(referencia, aporte, base, maxima, candidatosNaMargem, inicial.orcamento());
        BigDecimal objetivo = MetaUtilizacaoAporte.minimoAlocar(aporte).min(viavel.orcamento().alocado());
        if (objetivo.compareTo(inicial.orcamento().alocado()) <= 0) {
            // Sem melhoria executável, não eleva a margem inutilmente.
            return inicial;
        }

        // Cada teste completa o MESMO plano inicial: nenhuma compra já sugerida
        // é retirada ou trocada para reduzir a sobra.
        BigDecimal inferior = base;
        BigDecimal superior = maxima;
        while (superior.subtract(inferior).compareTo(PASSO) > 0) {
            BigDecimal meio = inferior.add(superior).divide(BigDecimal.valueOf(2), 2, RoundingMode.DOWN);
            Resultado teste = avaliar(referencia, aporte, base, meio, candidatosNaMargem, inicial.orcamento());
            if (teste.orcamento().alocado().compareTo(objetivo) >= 0) {
                superior = meio;
                viavel = teste;
            } else {
                inferior = meio;
            }
        }
        return viavel;
    }

    private Resultado avaliar(ComparativoResponseDTO referencia, BigDecimal aporte, BigDecimal base,
                               BigDecimal margem, Function<Double, List<AporteCandidato>> candidatosNaMargem,
                               OrcamentoAporte inicial) {
        List<AporteCandidato> candidatos = candidatosNaMargem.apply(margem.doubleValue());
        return new Resultado(base, margem, List.copyOf(candidatos),
                alocacao.complementar(candidatos, referencia, aporte, inicial));
    }

    private BigDecimal margemSemRestricaoOperacional(ComparativoResponseDTO referencia, BigDecimal base,
                                                     List<AporteCandidato> candidatos) {
        BigDecimal menorPercentual = null;
        for (AporteCandidato c : candidatos) {
            if (c.percentualIdeal() != null && c.percentualIdeal().signum() > 0) {
                menorPercentual = menorPercentual == null ? c.percentualIdeal() : menorPercentual.min(c.percentualIdeal());
            }
        }
        for (var classe : referencia.classes()) {
            if (classe.percentual_ideal().signum() > 0) {
                menorPercentual = menorPercentual == null ? classe.percentual_ideal() : menorPercentual.min(classe.percentual_ideal());
            }
            for (var sub : classe.subclasses()) {
                BigDecimal percentual = classe.percentual_ideal().multiply(sub.percentual_ideal()).movePointLeft(2);
                if (percentual.signum() > 0) {
                    menorPercentual = menorPercentual == null ? percentual : menorPercentual.min(percentual);
                }
            }
        }
        if (menorPercentual == null || referencia.valor_total().signum() <= 0) {
            return base;
        }
        return new BigDecimal("100").divide(menorPercentual, 12, RoundingMode.UP)
                .subtract(BigDecimal.ONE).multiply(new BigDecimal("100"))
                .setScale(2, RoundingMode.UP).max(base);
    }
}
