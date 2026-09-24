package dev.LzGuimaraes.FocusLifeHub.Planejamento.Aporte;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import dev.LzGuimaraes.FocusLifeHub.Ativo.CategoriaInvestimento;
import dev.LzGuimaraes.FocusLifeHub.Planejamento.CarteiraIdeal.dto.ComparativoResponseDTO;

/**
 * ALOCAÇÃO — "quanto dinheiro cabe em cada ativo?" (§21 a §25 da regra).
 *
 * Desce a hierarquia da Carteira Ideal (CLASSE → SUBCLASSE → ATIVO) sempre com
 * TETO na CAPACIDADE de cada um — e a capacidade sai do LIMITE OPERACIONAL
 * (meta × (1 + margem)), não do déficit. Quem enche sai do ciclo e o dinheiro
 * continua com os demais.
 *
 * O ORÇAMENTO de um nível é `min(limite explícito do nível, Σ capacidades
 * elegíveis dos ativos dele)`. O DÉFICIT do nível NÃO entra: reservar o déficit
 * de quem não consegue absorver era o que prendia dinheiro (§14).
 *
 * A divisão DENTRO do nível é pela NOTA DO CHECKLIST — é toda a inteligência que
 * sobrou, de propósito: o ativo que o usuário avaliou melhor recebe primeiro.
 * Quando nenhum ativo do nível tem nota, o peso cai para o próprio espaço
 * (rateio proporcional clássico).
 *
 * O que não couber em ninguém fica NÃO ALOCADO — o motor nunca distribui
 * dinheiro artificialmente só para zerar o aporte.
 */
@Service
public class AlocacaoService {

    /**
     * Rodadas de redistribuição entre níveis. Como cada rodada agora só oferece o
     * que é ABSORVÍVEL (capacidade elegível), a convergência acontece na primeira
     * ou segunda; o limite existe só para não girar à toa.
     */
    private static final int RODADAS = 6;

    /**
     * Meio centavo: o que importa para o TROCO. O piso de ruído (`tolerancia`) é
     * outra coisa — ele existe para não tratar centavos de arredondamento de
     * percentual como déficit de verdade, e usá-lo aqui descartaria troco real
     * (R$ 0,51 some dentro de um piso de R$ 2,85 num patrimônio de R$ 28.500).
     */
    private static final double CENTAVO = 0.005;

    /**
     * Piso de ruído do cálculo, em reais: 0,01% do patrimônio (nunca menos de
     * meio centavo). Percentual é guardado com 4 casas, então uma meta que
     * espelha a carteira difere do real em centavos — que com um piso fixo
     * contariam como déficit de verdade.
     */
    public static double tolerancia(double total) {
        return Math.max(0.005d, total * 0.0001d);
    }

    public OrcamentoAporte ratear(List<AporteCandidato> candidatos, ComparativoResponseDTO comparativo,
                                  BigDecimal valorAporte) {
        if (valorAporte == null || valorAporte.signum() <= 0) {
            return null;
        }

        double total = nz(comparativo.valor_total());
        double tol = tolerancia(total);

        Map<CategoriaInvestimento, List<ComparativoResponseDTO.SubclasseComparativoDTO>> subsPorClasse =
                new LinkedHashMap<>();
        for (ComparativoResponseDTO.ClasseComparativoDTO c : comparativo.classes()) {
            subsPorClasse.put(c.classe(), c.subclasses());
        }

        Map<CategoriaInvestimento, BigDecimal> porClasse = new LinkedHashMap<>();
        Map<Long, BigDecimal> porSubclasse = new LinkedHashMap<>();
        List<BigDecimal> porCandidato = new ArrayList<>(Collections.nCopies(candidatos.size(), moeda(0d)));

        // Quem pode receber: as classes que têm ATIVO na carteira. O DÉFICIT da
        // classe NÃO entra na conta (§14) — ele é informativo. Reservar o déficit de
        // quem não consegue absorver era o que prendia dinheiro.
        Map<CategoriaInvestimento, List<Integer>> indicesPorClasse = new LinkedHashMap<>();
        Map<CategoriaInvestimento, Double> espacoAteLimite = new LinkedHashMap<>();
        for (ComparativoResponseDTO.ClasseComparativoDTO c : comparativo.classes()) {
            List<Integer> daClasse = indicesDaClasse(candidatos, c.classe());
            if (daClasse.isEmpty()) {
                continue;   // classe com meta e sem nenhum ativo: não tem por onde receber
            }
            indicesPorClasse.put(c.classe(), daClasse);
            // O único teto que a CLASSE impõe é o limite EXPLÍCITO dela (§15):
            //
            //     capacidadeClasse = min(limite da classe, Σ capacidades elegíveis)
            //
            // Sem limite cadastrado, a classe não tem teto próprio: quem limita é a
            // capacidade dos ativos dela. Estar acima da meta não zera nada.
            if (c.limite_maximo() != null && c.limite_maximo().signum() > 0) {
                double limiteEmReais = c.limite_maximo().doubleValue() / 100d * total;
                espacoAteLimite.put(c.classe(), Math.max(0d, limiteEmReais - nz(c.valor_atual())));
            }
        }

        // Rodadas: o valor que não achou destino elegível numa classe procura outra
        // classe com espaço. Sem isso, um ativo sem nota bloquearia o dinheiro.
        double restanteTotal = valorAporte.doubleValue();
        for (int rodada = 0; rodada < RODADAS && restanteTotal > tol; rodada++) {
            Map<CategoriaInvestimento, Double> capacidade = new LinkedHashMap<>();
            for (Map.Entry<CategoriaInvestimento, List<Integer>> entrada : indicesPorClasse.entrySet()) {
                CategoriaInvestimento classe = entrada.getKey();
                double jaRecebeu = porClasse.getOrDefault(classe, moeda(0d)).doubleValue();

                // A fatia da classe no rateio é o que os ATIVOS ELEGÍVEIS dela ainda
                // absorvem — NUNCA o déficit do nível. Uma classe com déficit grande e
                // ativos já no limite (ou sem ativo elegível) não pode reservar dinheiro
                // que não tem onde entrar: era isso que diluía o fator em todas as
                // rodadas e deixava sobra no fim, mesmo com outras classes ainda tendo
                // capacidade.
                double capacidadeElegivel = capacidadeElegivel(candidatos, entrada.getValue(), porCandidato);
                Double limite = espacoAteLimite.get(classe);
                double fatia = (limite != null)
                        ? Math.min(capacidadeElegivel, Math.max(0d, limite - jaRecebeu))
                        : capacidadeElegivel;
                if (fatia > tol) {
                    capacidade.put(classe, fatia);
                }
            }
            double somaCapacidade = capacidade.values().stream().mapToDouble(Double::doubleValue).sum();
            if (somaCapacidade <= tol) {
                break;   // ninguém mais tem espaço: o que sobrar fica não alocado
            }
            double fator = Math.min(1d, restanteTotal / somaCapacidade);
            double distribuidoNaRodada = 0d;

            for (Map.Entry<CategoriaInvestimento, Double> entrada : capacidade.entrySet()) {
                CategoriaInvestimento classe = entrada.getKey();
                double orcamentoClasse = entrada.getValue() * fator;
                List<Integer> daClasse = indicesPorClasse.get(classe);

                List<ComparativoResponseDTO.SubclasseComparativoDTO> subs =
                        subsPorClasse.getOrDefault(classe, List.of()).stream()
                                .filter(s -> capacidadeElegivelDaSubclasse(candidatos, daClasse, s, porCandidato) > tol
                                        || espacoAteLimiteDaSubclasse(s, total) != null)
                                .toList();

                double distribuido;
                if (subs.isEmpty()) {
                    // Classe sem subclasse com alvo: ela mesma é o bucket.
                    distribuido = distribuir(candidatos, daClasse, orcamentoClasse, tol, porCandidato);
                } else {
                    distribuido = distribuirEntreSubclasses(candidatos, daClasse, subs,
                            total, orcamentoClasse, tol, porCandidato, porSubclasse);
                }

                // A classe registra o que os ATIVOS dela receberam (é o que o painel mostra).
                double noBucket = 0d;
                for (int i : daClasse) {
                    noBucket += porCandidato.get(i).doubleValue();
                }
                porClasse.put(classe, moeda(noBucket));
                distribuidoNaRodada += distribuido;
            }

            if (distribuidoNaRodada <= tol) {
                break;   // ninguém tem espaço: para de tentar
            }
            restanteTotal -= distribuidoNaRodada;
        }

        // Arredonda para UNIDADES INTEIRAS (a compra é de cotas, não de frações
        // de real) e devolve a sobra para quem tem a MELHOR NOTA — é a mesma
        // preferência que decide a ordem do ranking.
        List<BigDecimal> quantidades = new ArrayList<>(Collections.nCopies(candidatos.size(), (BigDecimal) null));
        double sobra = arredondarParaUnidades(candidatos, porCandidato, quantidades);
        sobra = redistribuirTroco(candidatos, porCandidato, quantidades, sobra, tol);
        sobra = absorverTrocoComFracao(candidatos, porCandidato, quantidades, sobra, tol);

        // Os totais por classe/subclasse saem do resultado FINAL (depois do
        // arredondamento e da redistribuição): antes eles eram somados rodada a
        // rodada, e o painel mostraria um valor que não é o que o ativo recebeu.
        Map<CategoriaInvestimento, BigDecimal> totalPorClasse = new LinkedHashMap<>();
        Map<Long, BigDecimal> totalPorSubclasse = new LinkedHashMap<>();
        for (int i = 0; i < candidatos.size(); i++) {
            BigDecimal valor = porCandidato.get(i);
            if (valor.signum() <= 0) {
                continue;
            }
            totalPorClasse.merge(candidatos.get(i).classe(), valor, BigDecimal::add);
            if (candidatos.get(i).subclasseId() != null) {
                totalPorSubclasse.merge(candidatos.get(i).subclasseId(), valor, BigDecimal::add);
            }
        }

        double alocado = porCandidato.stream().mapToDouble(BigDecimal::doubleValue).sum();
        // `quantidades` pode ter null (ativo sem preço conhecido) → List.copyOf
        // recusaria a lista inteira por causa de um elemento nulo.
        return new OrcamentoAporte(Map.copyOf(totalPorClasse), Map.copyOf(totalPorSubclasse),
                List.copyOf(porCandidato), Collections.unmodifiableList(quantidades),
                moeda(alocado), moeda(sobra));
    }

    /**
     * A compra é de UNIDADES: ação, FII e ETF são cotas inteiras (ninguém compra
     * 72,7 cotas); cripto, renda fixa e Tesouro aceitam fração, então o valor
     * sugerido fica como está e a quantidade vira uma fração informativa.
     */
    public static boolean compraEmUnidadesInteiras(CategoriaInvestimento classe) {
        return classe == CategoriaInvestimento.ACOES
                || classe == CategoriaInvestimento.FIIS
                || classe == CategoriaInvestimento.ETFS;
    }

    /**
     * CAPACIDADE ELEGÍVEL de um bucket: o quanto os ativos ELEGÍVEIS dele ainda
     * conseguem absorver (capacidade do ativo menos o que ele já recebeu).
     *
     * É este número — e não o déficit do nível — que define a fatia do nível no
     * rateio. Déficit da classe e capacidade elegível da classe são coisas
     * diferentes: uma classe pode ter R$ 1.000 de déficit e R$ 0 de capacidade
     * (todos os ativos no teto). Nesse caso ela não recebe, mas também NÃO pode
     * segurar dinheiro que outras classes conseguiriam usar.
     */
    private double capacidadeElegivel(List<AporteCandidato> candidatos, List<Integer> indices,
                                      List<BigDecimal> porCandidato) {
        double soma = 0d;
        for (int i : indices) {
            AporteCandidato c = candidatos.get(i);
            if (!c.elegivel()) {
                continue;
            }
            soma += Math.max(0d, c.capacidade().doubleValue() - porCandidato.get(i).doubleValue());
        }
        return soma;
    }

    /**
     * Capacidade elegível de uma SUBCLASSE: o menor valor entre o espaço que ainda
     * falta para o alvo dela e o que os ativos elegíveis dela absorvem.
     */
    private double capacidadeElegivelDaSubclasse(List<AporteCandidato> candidatos, List<Integer> daClasse,
                                                 ComparativoResponseDTO.SubclasseComparativoDTO sub,
                                                 List<BigDecimal> porCandidato) {
        List<Integer> daSub = daClasse.stream()
                .filter(i -> sub.id().equals(candidatos.get(i).subclasseId()))
                .toList();
        return capacidadeElegivel(candidatos, daSub, porCandidato);
    }

    /**
     * Corta o valor sugerido no número INTEIRO de cotas que cabe nele.
     *
     * @return a sobra (dinheiro que não fecha uma cota) para redistribuir
     */
    private double arredondarParaUnidades(List<AporteCandidato> candidatos, List<BigDecimal> porCandidato,
                                          List<BigDecimal> quantidades) {
        double sobra = 0d;
        for (int i = 0; i < candidatos.size(); i++) {
            AporteCandidato c = candidatos.get(i);
            BigDecimal valor = porCandidato.get(i);
            BigDecimal preco = c.precoUnitario();
            if (valor.signum() <= 0 || preco == null || preco.signum() <= 0) {
                continue;
            }
            if (!compraEmUnidadesInteiras(c.classe())) {
                quantidades.set(i, valor.divide(preco, 8, RoundingMode.DOWN));
                continue;
            }
            long unidades = (long) Math.floor(valor.doubleValue() / preco.doubleValue());
            BigDecimal exato = moeda(preco.doubleValue() * unidades);
            sobra += valor.doubleValue() - exato.doubleValue();
            quantidades.set(i, BigDecimal.valueOf(unidades));
            porCandidato.set(i, exato);
        }
        return sobra;
    }

    /**
     * Sobrou troco que não fecha uma COTA INTEIRA? Quem aceita FRAÇÃO (cripto,
     * renda fixa, Tesouro) absorve — é dinheiro que pode ser investido e deixá-lo
     * parado com capacidade disponível seria mentir sobre o aporte.
     *
     * O troco SÓ permanece quando ninguém tem espaço: aí ele é legítimo.
     *
     * @return o que não pôde ser alocado
     */
    private double absorverTrocoComFracao(List<AporteCandidato> candidatos, List<BigDecimal> porCandidato,
                                          List<BigDecimal> quantidades, double sobra, double tol) {
        if (sobra <= CENTAVO) {
            return 0d;
        }
        for (int i = 0; i < candidatos.size() && sobra > CENTAVO; i++) {   // ordem = nota
            AporteCandidato c = candidatos.get(i);
            if (!c.elegivel() || compraEmUnidadesInteiras(c.classe())) {
                continue;   // quem compra cota inteira não recebe fração de real
            }
            BigDecimal preco = c.precoUnitario();
            double espaco = c.capacidade().doubleValue() - porCandidato.get(i).doubleValue();
            if (espaco <= CENTAVO) {
                continue;
            }
            double adicionar = Math.min(espaco, sobra);
            porCandidato.set(i, moeda(porCandidato.get(i).doubleValue() + adicionar));
            if (preco != null && preco.signum() > 0) {
                quantidades.set(i, (quantidades.get(i) != null ? quantidades.get(i) : BigDecimal.ZERO)
                        .add(BigDecimal.valueOf(adicionar / preco.doubleValue())));
            }
            sobra -= adicionar;
        }
        return sobra;
    }

    /**
     * Devolve a sobra do arredondamento POR ORDEM DE NOTA, em COTAS INTEIRAS: o
     * candidato melhor avaliado que ainda tem espaço recebe quantas cotas
     * couberem, e o que sobrar passa para o próximo.
     */
    private double redistribuirTroco(List<AporteCandidato> candidatos, List<BigDecimal> porCandidato,
                                     List<BigDecimal> quantidades, double sobra, double tol) {
        for (int rodada = 0; rodada < 4 && sobra > CENTAVO; rodada++) {
            boolean distribuiu = false;
            for (int i = 0; i < candidatos.size(); i++) {
                AporteCandidato c = candidatos.get(i);
                BigDecimal preco = c.precoUnitario();
                if (!c.elegivel() || preco == null || preco.signum() <= 0
                        || !compraEmUnidadesInteiras(c.classe())) {
                    continue;
                }
                double precoD = preco.doubleValue();
                double espaco = c.capacidade().doubleValue() - porCandidato.get(i).doubleValue();
                double cabem = Math.min(Math.floor(espaco / precoD), Math.floor(sobra / precoD));
                if (cabem < 1d) {
                    continue;
                }
                double adicionar = moeda(precoD * cabem).doubleValue();
                porCandidato.set(i, moeda(porCandidato.get(i).doubleValue() + adicionar));
                quantidades.set(i, quantidades.get(i).add(BigDecimal.valueOf((long) cabem)));
                sobra -= adicionar;
                distribuiu = true;
            }
            if (!distribuiu) {
                break;
            }
        }
        return sobra;
    }

    /**
     * Reparte o orçamento da classe entre as SUBCLASSES com espaço, proporcional à
     * capacidade ELEGÍVEL de cada uma, e desce para os ativos de cada uma. A sobra
     * da subclasse volta para os candidatos sem subclasse.
     *
     * O espaço da SUBCLASSE é:
     *
     *     min(limite explícito da subclasse, Σ capacidades elegíveis dela)
     *
     * e NUNCA o déficit da subclasse (§16): uma subclasse sem déficit não bloqueia
     * os ativos dela, e uma subclasse sem ativo elegível não segura dinheiro.
     *
     * Dois cuidados que já custaram caro:
     *   • o espaço da subclasse é limitado pelo que os ATIVOS ELEGÍVEIS dela
     *     absorvem (senão uma subclasse sem ativo elegível segura o dinheiro);
     *   • o id da subclasse vai junto do grupo — antes o código usava
     *     `subs.get(k)` enquanto pulava entradas, e creditava a subclasse errada.
     */
    private double distribuirEntreSubclasses(List<AporteCandidato> candidatos, List<Integer> daClasse,
                                             List<ComparativoResponseDTO.SubclasseComparativoDTO> subs,
                                             double total, double orcamentoClasse, double tol,
                                             List<BigDecimal> porCandidato, Map<Long, BigDecimal> porSubclasse) {
        List<Double> espacos = new ArrayList<>();
        List<Double> pesos = new ArrayList<>();
        List<List<Integer>> grupos = new ArrayList<>();
        List<Long> ids = new ArrayList<>();
        for (ComparativoResponseDTO.SubclasseComparativoDTO sub : subs) {
            List<Integer> indices = daClasse.stream()
                    .filter(i -> sub.id().equals(candidatos.get(i).subclasseId()))
                    .toList();
            double jaNaSub = porSubclasse.getOrDefault(sub.id(), moeda(0d)).doubleValue();
            double espaco = capacidadeElegivel(candidatos, indices, porCandidato);
            Double limite = espacoAteLimiteDaSubclasse(sub, total);
            if (limite != null) {
                espaco = Math.min(espaco, Math.max(0d, limite - jaNaSub));
            }
            if (espaco <= tol) {
                continue;   // sem ativo elegível com espaço (ou limite explícito já atingido)
            }
            espacos.add(espaco);
            pesos.add(espaco);
            grupos.add(indices);
            ids.add(sub.id());
        }
        if (grupos.isEmpty()) {
            return 0d;
        }

        List<Double> cotas = repartir(pesos, espacos, orcamentoClasse, tol);
        double distribuido = 0d;
        for (int k = 0; k < grupos.size(); k++) {
            double subDistribuido = distribuir(candidatos, grupos.get(k), cotas.get(k), tol, porCandidato);
            if (subDistribuido > 0d) {
                porSubclasse.merge(ids.get(k), moeda(subDistribuido), BigDecimal::add);
                distribuido += subDistribuido;
            }
        }

        // Sobra da classe → candidatos fora de qualquer subclasse com alvo.
        double restante = orcamentoClasse - distribuido;
        List<Long> idsComAlvo = subs.stream().map(ComparativoResponseDTO.SubclasseComparativoDTO::id).toList();
        List<Integer> semSubclasse = daClasse.stream()
                .filter(i -> candidatos.get(i).subclasseId() == null
                        || !idsComAlvo.contains(candidatos.get(i).subclasseId()))
                .toList();
        if (restante > tol && !semSubclasse.isEmpty()) {
            distribuido += distribuir(candidatos, semSubclasse, restante, tol, porCandidato);
        }
        return distribuido;
    }

    /**
     * Distribui um orçamento dentro de um bucket respeitando a CAPACIDADE de cada
     * candidato, com PESO = nota do checklist (fallback: o próprio espaço).
     *
     * @return quanto saiu de fato para os ativos do bucket
     */
    private double distribuir(List<AporteCandidato> candidatos, List<Integer> indices, double orcamento,
                              double tol, List<BigDecimal> porCandidato) {
        if (orcamento <= tol || indices.isEmpty()) {
            return 0d;
        }

        List<Integer> elegiveis = new ArrayList<>();
        List<Double> espacos = new ArrayList<>();
        for (int i : indices) {
            AporteCandidato c = candidatos.get(i);
            if (!c.elegivel()) {
                continue;   // descartado na elegibilidade: não entra no rateio
            }
            double espaco = c.capacidade().doubleValue() - porCandidato.get(i).doubleValue();
            if (espaco <= tol) {
                continue;   // já na capacidade máxima
            }
            elegiveis.add(i);
            espacos.add(espaco);
        }
        if (elegiveis.isEmpty()) {
            return 0d;
        }

        // Peso = NOTA do checklist; se NENHUM dos candidatos do bucket tem nota,
        // todos caem para o espaço (rateio proporcional à capacidade).
        boolean temNota = elegiveis.stream()
                .anyMatch(i -> candidatos.get(i).nota() != null && candidatos.get(i).nota().signum() > 0);
        List<Double> pesos = new ArrayList<>();
        for (int k = 0; k < elegiveis.size(); k++) {
            Double nota = candidatos.get(elegiveis.get(k)).nota() != null
                    ? candidatos.get(elegiveis.get(k)).nota().doubleValue() : null;
            pesos.add(temNota ? ((nota != null && nota > 0d) ? nota : 0d) : espacos.get(k));
        }

        List<Double> cotas = repartir(pesos, espacos, orcamento, tol);
        double distribuido = 0d;
        for (int k = 0; k < elegiveis.size(); k++) {
            if (cotas.get(k) > 0d) {
                int i = elegiveis.get(k);
                porCandidato.set(i, moeda(porCandidato.get(i).doubleValue() + cotas.get(k)));
                distribuido += cotas.get(k);
            }
        }
        return distribuido;
    }

    /**
     * Rateio PONDERADO com TETO ("water-filling"): divide proporcional ao peso e
     * REPETE com o que sobrou entre quem ainda tem espaço — sem isso, um item que
     * bate no teto travaria o rateio dos demais.
     */
    private List<Double> repartir(List<Double> pesos, List<Double> espacos, double orcamento, double tol) {
        int n = pesos.size();
        List<Double> ja = new ArrayList<>(Collections.nCopies(n, 0d));
        if (n == 0 || orcamento <= tol) {
            return ja;
        }

        double restante = orcamento;
        for (int rodada = 0; rodada < 12 && restante > tol; rodada++) {
            double somaPesos = 0d;
            for (int k = 0; k < n; k++) {
                if (espacos.get(k) - ja.get(k) > tol) {
                    somaPesos += pesos.get(k);
                }
            }
            if (somaPesos <= 0d) {
                break;
            }
            double nestaRodada = 0d;
            for (int k = 0; k < n; k++) {
                double espacoLivre = espacos.get(k) - ja.get(k);
                if (espacoLivre <= tol) {
                    continue;
                }
                double cota = restante * (pesos.get(k) / somaPesos);
                double adicionar = Math.min(cota, espacoLivre);
                ja.set(k, ja.get(k) + adicionar);
                nestaRodada += adicionar;
            }
            if (nestaRodada <= tol) {
                break;
            }
            restante -= nestaRodada;
        }
        return ja;
    }

    private List<Integer> indicesDaClasse(List<AporteCandidato> candidatos, CategoriaInvestimento classe) {
        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < candidatos.size(); i++) {
            if (candidatos.get(i).classe() == classe) {
                indices.add(i);
            }
        }
        return indices;
    }

    /**
     * Espaço de uma subclasse até o limite EXPLÍCITO dela (null = não há limite).
     * O `valor_ideal` da subclasse é o alvo; o teto, quando existe, vem do
     * `limite_maximo` cadastrado pelo usuário — e não do déficit.
     */
    private static Double espacoAteLimiteDaSubclasse(ComparativoResponseDTO.SubclasseComparativoDTO s,
                                                     double total) {
        if (s.limite_maximo() == null || s.limite_maximo().signum() <= 0) {
            return null;
        }
        double limiteEmReais = s.limite_maximo().doubleValue() / 100d * total;
        return Math.max(0d, limiteEmReais - nz(s.valor_atual()));
    }

    public static double nz(BigDecimal valor) {
        return (valor != null) ? valor.doubleValue() : 0d;
    }

    /** Valor em reais, 2 casas, nunca negativo. */
    public BigDecimal moeda(double valor) {
        return BigDecimal.valueOf(Math.max(0d, valor)).setScale(2, RoundingMode.HALF_UP);
    }
}
