package dev.LzGuimaraes.FocusLifeHub.Planejamento.Aporte;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import dev.LzGuimaraes.FocusLifeHub.Ativo.CategoriaInvestimento;
import dev.LzGuimaraes.FocusLifeHub.Planejamento.CarteiraIdeal.dto.ComparativoResponseDTO;

/**
 * Primeiro preenche déficits até a meta; depois usa a folga até o limite.
 * Classe e subclasse recebem proporcionalmente à capacidade efetiva de seus
 * filhos. Dentro do grupo, a nota pondera o rateio. Todos os valores monetários
 * são calculados em centavos e todos os caminhos respeitam a mesma hierarquia.
 */
@Service
public class AlocacaoService {
    private static final BigDecimal CENTAVO = new BigDecimal("0.01");

    /** Tolerância para classificar percentuais; não é piso para aplicar dinheiro. */
    public static double tolerancia(double total) {
        return Math.max(0.005d, total * 0.0001d);
    }

    public OrcamentoAporte ratear(List<AporteCandidato> candidatos, ComparativoResponseDTO comparativo,
                                  BigDecimal valorAporte) {
        return complementar(candidatos, comparativo, valorAporte, null);
    }

    /** Completa um plano anterior usando capacidades ampliadas, na mesma ordem de candidatos. */
    public OrcamentoAporte complementar(List<AporteCandidato> candidatos, ComparativoResponseDTO comparativo,
                                         BigDecimal valorAporte, OrcamentoAporte inicial) {
        if (valorAporte == null || valorAporte.signum() <= 0) {
            return null;
        }
        Plano plano = new Plano(candidatos, comparativo);
        if (inicial != null) {
            for (int i = 0; i < candidatos.size(); i++) {
                plano.valores.set(i, inicial.deCandidato(i));
            }
        }
        BigDecimal aporte = centavos(valorAporte);
        distribuirHierarquia(plano, aporte.subtract(plano.alocado()), true);
        // Cotas são indivisíveis. Arredondar antes da segunda etapa devolve
        // também o dinheiro reservado a déficits que não fecham uma cota.
        arredondarUnidades(plano);
        completarUnidades(plano, aporte.subtract(plano.alocado()), true);
        distribuirHierarquia(plano, aporte.subtract(plano.alocado()), false);
        arredondarUnidades(plano);
        completarUnidades(plano, aporte.subtract(plano.alocado()), false);

        Map<CategoriaInvestimento, BigDecimal> porClasse = new LinkedHashMap<>();
        Map<Long, BigDecimal> porSubclasse = new LinkedHashMap<>();
        List<BigDecimal> quantidades = new ArrayList<>();
        for (int i = 0; i < candidatos.size(); i++) {
            AporteCandidato c = candidatos.get(i);
            BigDecimal valor = plano.valores.get(i);
            if (valor.signum() > 0) {
                porClasse.merge(c.classe(), valor, BigDecimal::add);
                if (c.subclasseId() != null) {
                    porSubclasse.merge(c.subclasseId(), valor, BigDecimal::add);
                }
            }
            BigDecimal preco = c.precoUnitario();
            quantidades.add(preco == null || preco.signum() <= 0 ? null
                    : valor.divide(preco, compraEmUnidadesInteiras(c.classe()) ? 0 : 8, RoundingMode.DOWN));
        }
        // Inclui toda a sobra, tanto por limites quanto por unidades indivisíveis.
        return new OrcamentoAporte(Map.copyOf(porClasse), Map.copyOf(porSubclasse),
                List.copyOf(plano.valores), Collections.unmodifiableList(quantidades),
                plano.alocado(), aporte.subtract(plano.alocado()));
    }

    /** Capacidade residual em dinheiro, considerando os três níveis do plano. */
    public List<BigDecimal> capacidadesRestantes(List<AporteCandidato> candidatos,
                                               ComparativoResponseDTO comparativo, OrcamentoAporte orcamento) {
        Plano plano = new Plano(candidatos, comparativo);
        for (int i = 0; i < candidatos.size(); i++) {
            plano.valores.set(i, orcamento.deCandidato(i));
        }
        List<BigDecimal> capacidades = new ArrayList<>();
        for (int i = 0; i < candidatos.size(); i++) {
            capacidades.add(plano.capacidadeIndividual(i, false)
                    .min(plano.espacoClasse(candidatos.get(i).classe()))
                    .min(plano.espacoSubclasse(candidatos.get(i).subclasseId())));
        }
        return List.copyOf(capacidades);
    }

    private void distribuirHierarquia(Plano plano, BigDecimal orcamento, boolean ateMeta) {
        List<CategoriaInvestimento> classes = new ArrayList<>(plano.grupos.keySet());
        List<BigDecimal> capacidades = classes.stream().map(c -> plano.capacidadeClasse(c, ateMeta)).toList();
        List<BigDecimal> cotas = repartir(capacidades, capacidades, orcamento);
        for (int k = 0; k < classes.size(); k++) {
            List<Grupo> grupos = plano.grupos.get(classes.get(k));
            List<BigDecimal> espacos = grupos.stream().map(g -> plano.capacidadeGrupo(g, ateMeta)).toList();
            List<BigDecimal> subCotas = repartir(espacos, espacos, cotas.get(k));
            for (int s = 0; s < grupos.size(); s++) {
                List<Integer> indices = grupos.get(s).indices();
                List<BigDecimal> individuais = indices.stream().map(i -> plano.capacidadeIndividual(i, ateMeta)).toList();
                List<BigDecimal> notas = indices.stream().map(i -> positivo(plano.candidatos.get(i).nota())).toList();
                List<BigDecimal> valores = repartir(notas, individuais, subCotas.get(s));
                for (int a = 0; a < indices.size(); a++) {
                    int i = indices.get(a);
                    plano.valores.set(i, plano.valores.get(i).add(valores.get(a)));
                }
            }
        }
    }

    /** Rateio com teto e maiores restos: preserva cada centavo do orçamento. */
    private List<BigDecimal> repartir(List<BigDecimal> pesos, List<BigDecimal> capacidades, BigDecimal orcamento) {
        int n = pesos.size();
        List<BigDecimal> valores = new ArrayList<>(Collections.nCopies(n, centavos(BigDecimal.ZERO)));
        BigDecimal restante = centavos(orcamento);
        while (restante.signum() > 0) {
            List<Integer> ativos = new ArrayList<>();
            BigDecimal somaPesos = BigDecimal.ZERO;
            for (int i = 0; i < n; i++) {
                if (capacidades.get(i).subtract(valores.get(i)).signum() > 0) {
                    ativos.add(i);
                    somaPesos = somaPesos.add(pesos.get(i));
                }
            }
            if (ativos.isEmpty()) {
                break;
            }
            // Sem nota positiva restante, o espaço vira peso. Ausência de nota
            // e nota zero não deixam dinheiro preso após os demais atingirem o teto.
            boolean peloEspaco = somaPesos.signum() == 0;
            if (peloEspaco) {
                somaPesos = ativos.stream().map(i -> capacidades.get(i).subtract(valores.get(i)))
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
            }
            BigDecimal antes = restante;
            List<BigDecimal> restos = new ArrayList<>(Collections.nCopies(n, BigDecimal.ZERO));
            for (int i : ativos) {
                BigDecimal espaco = capacidades.get(i).subtract(valores.get(i));
                BigDecimal peso = peloEspaco ? espaco : pesos.get(i);
                BigDecimal ideal = antes.multiply(peso).divide(somaPesos, 12, RoundingMode.DOWN).min(espaco);
                BigDecimal cota = centavos(ideal);
                restos.set(i, ideal.subtract(cota));
                valores.set(i, valores.get(i).add(cota));
                restante = restante.subtract(cota);
            }
            ativos.sort(Comparator.comparing((Integer i) -> restos.get(i)).reversed());
            for (int i : ativos) {
                if (restante.signum() > 0 && restos.get(i).signum() > 0
                        && capacidades.get(i).subtract(valores.get(i)).compareTo(CENTAVO) >= 0) {
                    valores.set(i, valores.get(i).add(CENTAVO));
                    restante = restante.subtract(CENTAVO);
                }
            }
            if (antes.compareTo(restante) == 0) {
                break;
            }
        }
        return valores;
    }

    private void arredondarUnidades(Plano plano) {
        for (int i = 0; i < plano.candidatos.size(); i++) {
            AporteCandidato c = plano.candidatos.get(i);
            if (compraEmUnidadesInteiras(c.classe()) && c.precoUnitario() != null && c.precoUnitario().signum() > 0) {
                BigDecimal quantidade = plano.valores.get(i).divide(c.precoUnitario(), 0, RoundingMode.DOWN);
                plano.valores.set(i, centavos(quantidade.multiply(c.precoUnitario())));
            }
        }
    }

    /** Redistribui o saldo executável, sem ultrapassar os limites dos pais. */
    private void completarUnidades(Plano plano, BigDecimal saldo, boolean ateMeta) {
        List<Integer> ordem = new ArrayList<>();
        for (int i = 0; i < plano.candidatos.size(); i++) {
            ordem.add(i);
        }
        ordem.sort(Comparator
                .comparing((Integer i) -> plano.deficitRestante(i).signum() > 0, Comparator.reverseOrder())
                .thenComparing(i -> plano.candidatos.get(i).nota(), Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(i -> plano.candidatos.get(i).nome(), Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));
        // Cotas inteiras têm preferência no troco; a etapa seguinte permite que
        // quem aceita fração absorva o saldo que não fecha mais nenhuma cota.
        for (boolean inteiras : List.of(true, false)) {
            for (int i : ordem) {
                AporteCandidato c = plano.candidatos.get(i);
                if (compraEmUnidadesInteiras(c.classe()) != inteiras || saldo.signum() <= 0) {
                    continue;
                }
                BigDecimal espaco = plano.capacidadeIndividual(i, ateMeta)
                        .min(plano.espacoClasse(c.classe())).min(plano.espacoSubclasse(c.subclasseId())).min(saldo);
                if (inteiras && c.precoUnitario() != null && c.precoUnitario().signum() > 0) {
                    espaco = centavos(espaco.divide(c.precoUnitario(), 0, RoundingMode.DOWN).multiply(c.precoUnitario()));
                }
                plano.valores.set(i, plano.valores.get(i).add(espaco));
                saldo = saldo.subtract(espaco);
            }
        }
    }

    private record Grupo(Long subclasseId, List<Integer> indices) {}

    private static final class Plano {
        final List<AporteCandidato> candidatos;
        final List<BigDecimal> valores;
        final Map<CategoriaInvestimento, List<Grupo>> grupos = new LinkedHashMap<>();
        final Map<CategoriaInvestimento, BigDecimal> limitesClasses = new LinkedHashMap<>();
        final Map<Long, BigDecimal> limitesSubclasses = new LinkedHashMap<>();

        Plano(List<AporteCandidato> candidatos, ComparativoResponseDTO comparativo) {
            this.candidatos = candidatos;
            valores = new ArrayList<>(Collections.nCopies(candidatos.size(), centavos(BigDecimal.ZERO)));
            for (var classe : comparativo.classes()) {
                if (classe.limite_maximo() != null && classe.limite_maximo().signum() > 0) {
                    limitesClasses.put(classe.classe(), limiteRestante(classe.limite_maximo(), comparativo.valor_total(), classe.valor_atual()));
                }
                for (var sub : classe.subclasses()) {
                    if (sub.limite_maximo() != null && sub.limite_maximo().signum() > 0) {
                        limitesSubclasses.put(sub.id(), limiteRestante(sub.limite_maximo(), comparativo.valor_total(), sub.valor_atual()));
                    }
                }
                Map<Long, List<Integer>> porSub = new LinkedHashMap<>();
                for (int i = 0; i < candidatos.size(); i++) {
                    if (candidatos.get(i).classe() == classe.classe()) {
                        porSub.computeIfAbsent(candidatos.get(i).subclasseId(), id -> new ArrayList<>()).add(i);
                    }
                }
                grupos.put(classe.classe(), porSub.entrySet().stream()
                        .map(e -> new Grupo(e.getKey(), List.copyOf(e.getValue()))).toList());
            }
        }

        BigDecimal alocado() {
            return valores.stream().reduce(centavos(BigDecimal.ZERO), BigDecimal::add);
        }

        BigDecimal deficitRestante(int i) {
            return centavos(positivo(candidatos.get(i).deficit()).subtract(valores.get(i)));
        }

        BigDecimal capacidadeIndividual(int i, boolean ateMeta) {
            AporteCandidato c = candidatos.get(i);
            if (!c.elegivel() || !grupos.containsKey(c.classe())) {
                return centavos(BigDecimal.ZERO);
            }
            BigDecimal capacidade = centavos(positivo(c.capacidade()).subtract(valores.get(i)));
            return ateMeta ? capacidade.min(deficitRestante(i)) : capacidade;
        }

        BigDecimal capacidadeGrupo(Grupo grupo, boolean ateMeta) {
            return grupo.indices().stream().map(i -> capacidadeIndividual(i, ateMeta))
                    .reduce(BigDecimal.ZERO, BigDecimal::add).min(espacoSubclasse(grupo.subclasseId()));
        }

        BigDecimal capacidadeClasse(CategoriaInvestimento classe, boolean ateMeta) {
            return grupos.get(classe).stream().map(g -> capacidadeGrupo(g, ateMeta))
                    .reduce(BigDecimal.ZERO, BigDecimal::add).min(espacoClasse(classe));
        }

        BigDecimal espacoClasse(CategoriaInvestimento classe) {
            BigDecimal limite = limitesClasses.get(classe);
            BigDecimal usado = BigDecimal.ZERO;
            for (int i = 0; i < candidatos.size(); i++) {
                if (candidatos.get(i).classe() == classe) {
                    usado = usado.add(valores.get(i));
                }
            }
            return limite == null ? semTeto() : centavos(limite.subtract(usado));
        }

        BigDecimal espacoSubclasse(Long id) {
            BigDecimal limite = limitesSubclasses.get(id);
            if (limite == null) {
                return semTeto();
            }
            BigDecimal usado = BigDecimal.ZERO;
            for (int i = 0; i < candidatos.size(); i++) {
                if (id.equals(candidatos.get(i).subclasseId())) {
                    usado = usado.add(valores.get(i));
                }
            }
            return centavos(limite.subtract(usado));
        }

        private BigDecimal semTeto() {
            return candidatos.stream().map(c -> positivo(c.capacidade())).reduce(BigDecimal.ZERO, BigDecimal::add);
        }
    }

    private static BigDecimal limiteRestante(BigDecimal percentual, BigDecimal total, BigDecimal atual) {
        return centavos(percentual.multiply(total).movePointLeft(2).subtract(positivo(atual)));
    }

    private static BigDecimal positivo(BigDecimal valor) {
        return valor == null ? BigDecimal.ZERO : valor.max(BigDecimal.ZERO);
    }

    private static BigDecimal centavos(BigDecimal valor) {
        return positivo(valor).setScale(2, RoundingMode.DOWN);
    }

    public static boolean compraEmUnidadesInteiras(CategoriaInvestimento classe) {
        return classe == CategoriaInvestimento.ACOES || classe == CategoriaInvestimento.FIIS || classe == CategoriaInvestimento.ETFS;
    }

    public static double nz(BigDecimal valor) {
        return valor == null ? 0d : valor.doubleValue();
    }

    public BigDecimal moeda(double valor) {
        return BigDecimal.valueOf(Math.max(0d, valor)).setScale(2, RoundingMode.HALF_UP);
    }
}
