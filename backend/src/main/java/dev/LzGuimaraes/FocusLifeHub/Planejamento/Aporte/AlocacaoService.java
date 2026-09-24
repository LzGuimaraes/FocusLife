package dev.LzGuimaraes.FocusLifeHub.Planejamento.Aporte;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import dev.LzGuimaraes.FocusLifeHub.Ativo.CategoriaInvestimento;
import dev.LzGuimaraes.FocusLifeHub.Planejamento.CarteiraIdeal.dto.ComparativoResponseDTO;

/**
 * ALOCAÇÃO — "quanto dinheiro cabe em cada ativo sem violar as regras?"
 *
 * Entra aqui só o que já passou pela elegibilidade. O rateio desce a hierarquia
 * da Carteira Ideal (CLASSE → SUBCLASSE → SETOR → ATIVO) sempre com TETO em
 * cada nível, e ninguém ultrapassa a própria capacidade: quem enche sai do
 * ciclo e o dinheiro continua com os demais elegíveis (§15).
 *
 * O que não couber em ninguém fica NÃO ALOCADO — o motor nunca distribui
 * dinheiro artificialmente só para zerar o aporte (§14).
 *
 * A regra de divisão dentro do nível é a ESTRATÉGIA configurada pelo usuário e
 * usa o Priority Score / déficit / prioridade — nunca a elegibilidade, que já
 * foi decidida antes.
 */
@Service
public class AlocacaoService {

    /**
     * Piso de ruído do cálculo, em reais: 0,01% do patrimônio (nunca menos de
     * meio centavo).
     *
     * POR QUE RELATIVO: o percentual é guardado com 4 casas, então uma meta que
     * espelha a carteira atual difere do valor real em até 0,00005 p.p. — que num
     * patrimônio grande vira centavos. Com um piso fixo de meio centavo, esses
     * centavos contavam como déficit/excesso de verdade e o rateio escolhia o
     * destino do dinheiro por ruído de arredondamento.
     */
    public static double tolerancia(double total) {
        return Math.max(0.005d, total * 0.0001d);
    }

    /**
     * Rateia o valor do aporte POR CLASSE → SUBCLASSE → SETOR → CANDIDATO.
     *
     * Regras, todas com teto no déficit (nunca empurra dinheiro para quem já
     * está no alvo):
     *   1. só classes com déficit entram, e o valor é repartido entre elas na
     *      proporção do déficit de cada uma;
     *   2. dentro da classe, se ela tem subclasses com alvo, o orçamento é
     *      repartido do mesmo jeito entre as subclasses — e o que sobrar fica
     *      com os candidatos que não estão em nenhuma subclasse;
     *   3. dentro do "bucket", o peso vem da estratégia configurada (déficit por
     *      ticker, Priority Score ou prioridade);
     *   4. candidato inelegível não recebe NADA — o descarte aconteceu na fase de
     *      elegibilidade e a alocação apenas respeita o veredito.
     *
     * @return null quando não há orçamento ou nenhuma classe com déficit.
     */
    public OrcamentoAporte ratear(List<AporteCandidato> candidatos, ComparativoResponseDTO comparativo,
                                  BigDecimal valorAporte, ScoreConfigModel config) {
        return ratear(candidatos, comparativo, valorAporte, config, Map.of());
    }

    /**
     * Mesmo rateio, com o PESO de cada SETOR da Carteira Ideal (id do setor →
     * Priority Score agregado do setor).
     *
     * Antes o setor recebia sempre o PRÓPRIO déficit, na ordem de cadastro: o
     * primeiro setor da subclasse consumia o orçamento inteiro e os demais ficavam
     * com a sobra — ou seja, "quem estava cadastrado primeiro" decidia o aporte,
     * como se preço e checklist não existissem. Agora os setores da subclasse
     * concorrem pelo orçamento com peso (déficit + preço + checklist, os mesmos
     * pesos que o usuário configura em Pontuação), cada um com TETO no próprio
     * déficit, e o ticker continua entrando pelo Priority Score dele.
     *
     * Setor sem score (nenhum ativo avaliado, por exemplo) não fica de fora: quando
     * NENHUM setor da subclasse tem score, todos voltam a concorrer pelo déficit,
     * que era o comportamento anterior.
     */
    public OrcamentoAporte ratear(List<AporteCandidato> candidatos, ComparativoResponseDTO comparativo,
                                  BigDecimal valorAporte, ScoreConfigModel config,
                                  Map<Long, BigDecimal> pesoPorSetor) {
        if (valorAporte == null || valorAporte.signum() <= 0) {
            return null;
        }

        double total = nz(comparativo.valor_total());
        double tol = tolerancia(total);

        // Déficit de cada classe JÁ com a tolerância aplicada: dentro da faixa a
        // classe conta como equilibrada e não puxa aporte.
        Map<CategoriaInvestimento, Double> deficitClasse = new LinkedHashMap<>();
        for (ComparativoResponseDTO.ClasseComparativoDTO c : comparativo.classes()) {
            double deficit = deficitComTolerancia(c.percentual_ideal(), c.tolerancia(), c.percentual_atual(), total)
                    .doubleValue();
            if (deficit > tol) {
                deficitClasse.put(c.classe(), deficit);
            }
        }
        if (deficitClasse.isEmpty()) {
            return null;   // nenhuma classe abaixo do alvo (fora da tolerância)
        }

        Map<CategoriaInvestimento, List<ComparativoResponseDTO.SubclasseComparativoDTO>> subsPorClasse =
                new HashMap<>();
        Map<CategoriaInvestimento, Double> valorIdealClasse = new HashMap<>();
        for (ComparativoResponseDTO.ClasseComparativoDTO c : comparativo.classes()) {
            subsPorClasse.put(c.classe(), c.subclasses());
            valorIdealClasse.put(c.classe(), nz(c.valor_ideal()));
        }

        Map<CategoriaInvestimento, BigDecimal> porClasse = new LinkedHashMap<>();
        Map<Long, BigDecimal> porSubclasse = new LinkedHashMap<>();
        Map<Long, BigDecimal> porSetor = new LinkedHashMap<>();
        List<BigDecimal> porCandidato = new ArrayList<>(
                Collections.nCopies(candidatos.size(), moeda(0d)));

        // REDISTRIBUIÇÃO (§21): o valor que não achou destino elegível numa classe
        // pode procurar outra classe com déficit — quantas rodadas forem precisas
        // (limitado, para não girar em falso). Desligado, uma única rodada e o que
        // sobrar fica não alocado, com o motivo explicado.
        boolean redistribuir = config.getRedistribuir() == null || config.getRedistribuir();
        int rodadas = redistribuir ? 6 : 1;
        double restanteTotal = valorAporte.doubleValue();

        for (int rodada = 0; rodada < rodadas && restanteTotal > tol; rodada++) {
            Map<CategoriaInvestimento, Double> capacidade = new LinkedHashMap<>();
            for (Map.Entry<CategoriaInvestimento, Double> entrada : deficitClasse.entrySet()) {
                double jaRecebeu = porClasse.getOrDefault(entrada.getKey(), moeda(0d)).doubleValue();
                double sobra = entrada.getValue() - jaRecebeu;
                if (sobra > tol) {
                    capacidade.put(entrada.getKey(), sobra);
                }
            }
            double somaCapacidade = capacidade.values().stream().mapToDouble(Double::doubleValue).sum();
            if (somaCapacidade <= tol) {
                break;
            }
            double fator = Math.min(1d, restanteTotal / somaCapacidade);
            double distribuidoNaRodada = 0d;

            for (Map.Entry<CategoriaInvestimento, Double> entrada : capacidade.entrySet()) {
                CategoriaInvestimento classe = entrada.getKey();
                double orcamentoClasse = Math.min(entrada.getValue(), entrada.getValue() * fator);

                List<Integer> daClasse = indicesDaClasse(candidatos, classe);
                if (daClasse.isEmpty()) {
                    continue;   // classe com alvo e sem nenhum ativo: valor fica sem destino
                }

                List<ComparativoResponseDTO.SubclasseComparativoDTO> subs = subsPorClasse
                        .getOrDefault(classe, List.of()).stream()
                        .filter(s -> nz(s.percentual_ideal()) > 0d)
                        .toList();

                // Classe sem subclasse com alvo: ela mesma é o bucket.
                if (subs.isEmpty()) {
                    distribuidoNaRodada += distribuir(candidatos, daClasse, orcamentoClasse, config, tol, porCandidato);
                    // A classe PRECISA registrar o que recebeu: sem isso o painel
                    // mostrava R$ 0,00 numa classe que recebeu dinheiro, e o motivo
                    // caía no "nenhum ativo elegível" (falso).
                    double noBucket = 0d;
                    for (int i : daClasse) {
                        noBucket += porCandidato.get(i).doubleValue();
                    }
                    porClasse.put(classe, moeda(noBucket));
                    continue;
                }

                double restanteClasse = orcamentoClasse;
                double valorIdealDaClasse = valorIdealClasse.getOrDefault(classe, 0d);
                for (ComparativoResponseDTO.SubclasseComparativoDTO sub : subs) {
                    // O percentual da subclasse é uma FATIA DA CLASSE: o teto dela
                    // só pode ser calculado em R$ (valor_ideal/valor_atual da
                    // subclasse), nunca tratando o percentual como % da carteira.
                    double alvoSub = nz(sub.valor_ideal())
                            + nz(sub.tolerancia()) / 100d * valorIdealDaClasse;
                    double tetoSub = Math.max(0d, alvoSub - nz(sub.valor_atual()));
                    double jaNaSub = porSubclasse.getOrDefault(sub.id(), moeda(0d)).doubleValue();
                    double orcamentoSub = Math.min(Math.max(0d, tetoSub - jaNaSub), restanteClasse);
                    List<Integer> daSub = (orcamentoSub > tol)
                            ? indicesDaSubclasse(candidatos, daClasse, sub.id())
                            : List.of();
                    if (daSub.isEmpty()) {
                        continue;
                    }

                    // SETOR (nível opcional): se a subclasse tem setores com alvo, o
                    // orçamento dela desce um nível antes de chegar ao ativo. O
                    // percentual do setor é fatia da SUBCLASSE (teto em R$).
                    List<ComparativoResponseDTO.SetorComparativoDTO> setores = sub.setores().stream()
                            .filter(s -> nz(s.percentual_ideal()) > 0d)
                            .toList();
                    double distribuido;
                    if (setores.isEmpty()) {
                        distribuido = distribuir(candidatos, daSub, orcamentoSub, config, tol, porCandidato);
                    } else {
                        double restanteSub = orcamentoSub;
                        double valorIdealDaSub = nz(sub.valor_ideal());

                        // Quem CONCORRE: setor com espaço (déficit − já recebido) e com
                        // pelo menos um ativo ELEGÍVEL dentro. Setor sem candidato
                        // elegível não reserva orçamento para quem não pode receber.
                        List<ComparativoResponseDTO.SetorComparativoDTO> concorrentes = new ArrayList<>();
                        List<Double> espacos = new ArrayList<>();
                        List<Double> pesos = new ArrayList<>();
                        for (ComparativoResponseDTO.SetorComparativoDTO st : setores) {
                            double alvoSetor = nz(st.valor_ideal())
                                    + nz(st.tolerancia()) / 100d * valorIdealDaSub;
                            double tetoSetor = Math.max(0d, alvoSetor - nz(st.valor_atual()));
                            double jaNoSetor = porSetor.getOrDefault(st.id(), moeda(0d)).doubleValue();
                            double espaco = Math.max(0d, tetoSetor - jaNoSetor);
                            if (espaco <= tol) {
                                continue;
                            }
                            List<Integer> doSetor = indicesDaSetor(candidatos, daSub, st.id());
                            if (doSetor.stream().noneMatch(i -> candidatos.get(i).elegivel())) {
                                continue;
                            }
                            concorrentes.add(st);
                            espacos.add(espaco);
                            pesos.add(pesoPorSetor.getOrDefault(st.id(), BigDecimal.ZERO).doubleValue());
                        }

                        // Sem NENHUM score de setor, o peso vira o próprio espaço —
                        // rateio proporcional ao déficit, igual ao que já existia.
                        boolean temScore = !pesos.isEmpty() && pesos.stream().allMatch(p -> p > 0d);
                        for (int k = 0; k < pesos.size(); k++) {
                            if (!temScore) {
                                pesos.set(k, espacos.get(k));
                            }
                        }

                        List<Double> cotas = repartir(pesos, espacos, orcamentoSub, tol);
                        for (int k = 0; k < concorrentes.size(); k++) {
                            if (cotas.get(k) <= tol) {
                                continue;
                            }
                            List<Integer> doSetor = indicesDaSetor(candidatos, daSub, concorrentes.get(k).id());
                            double doSetorDistribuido = distribuir(candidatos, doSetor, cotas.get(k),
                                    config, tol, porCandidato);
                            if (doSetorDistribuido > 0d) {
                                porSetor.merge(concorrentes.get(k).id(), moeda(doSetorDistribuido), BigDecimal::add);
                                restanteSub -= doSetorDistribuido;
                            }
                        }
                        // Sobra da subclasse → candidatos fora de qualquer setor com alvo.
                        List<Long> idsComAlvo = setores.stream()
                                .map(ComparativoResponseDTO.SetorComparativoDTO::id).toList();
                        List<Integer> foraDeSetor = daSub.stream()
                                .filter(i -> candidatos.get(i).setorId() == null
                                        || !idsComAlvo.contains(candidatos.get(i).setorId()))
                                .toList();
                        distribuido = orcamentoSub - restanteSub;
                        if (restanteSub > tol && !foraDeSetor.isEmpty()) {
                            distribuido += distribuir(candidatos, foraDeSetor, restanteSub, config, tol, porCandidato);
                        }
                    }

                    if (distribuido > 0d) {
                        porSubclasse.merge(sub.id(), moeda(distribuido), BigDecimal::add);
                        restanteClasse -= distribuido;
                        distribuidoNaRodada += distribuido;
                    }
                }

                // Sobra da classe → candidatos fora de qualquer subclasse com alvo.
                List<Long> idsComAlvo = subs.stream().map(ComparativoResponseDTO.SubclasseComparativoDTO::id).toList();
                List<Integer> semSubclasse = daClasse.stream()
                        .filter(i -> candidatos.get(i).subclasseId() == null
                                || !idsComAlvo.contains(candidatos.get(i).subclasseId()))
                        .toList();
                if (restanteClasse > tol && !semSubclasse.isEmpty()) {
                    distribuidoNaRodada += distribuir(candidatos, semSubclasse, restanteClasse, config, tol, porCandidato);
                }

                double depois = 0d;
                for (int i : daClasse) {
                    depois += porCandidato.get(i).doubleValue();
                }
                // A classe só "gastou" o que efetivamente saiu para os ativos dela.
                porClasse.put(classe, moeda(depois));
            }

            if (distribuidoNaRodada <= tol) {
                break;   // ninguém tem espaço: para de tentar (evita laço infinito)
            }
            restanteTotal -= distribuidoNaRodada;
        }

        double alocado = porCandidato.stream().mapToDouble(BigDecimal::doubleValue).sum();
        return new OrcamentoAporte(Map.copyOf(porClasse), Map.copyOf(porSubclasse), Map.copyOf(porSetor),
                List.copyOf(porCandidato), moeda(alocado));
    }

    /**
     * Distribui um orçamento dentro de um bucket respeitando a CAPACIDADE de cada
     * candidato.
     *
     * Capacidade = déficit do ativo (ideal + tolerância − atual), limitado pelo
     * limite de concentração quando houver; posições sem meta própria herdam a
     * capacidade do bucket (subclasse/classe). Ninguém recebe acima dela — o que
     * não couber vira valor não alocado com o motivo explicado.
     *
     * Peso = FATOR DE MOMENTO × estratégia configurada, usando a capacidade como
     * medida de necessidade estrutural. Fator 0 já foi descartado na
     * elegibilidade; aqui o peso só ordena quem já pode receber.
     */
    private double distribuir(List<AporteCandidato> candidatos, List<Integer> indices, double orcamento,
                              ScoreConfigModel config, double tol, List<BigDecimal> porCandidato) {
        if (orcamento <= tol || indices.isEmpty()) {
            return 0d;
        }

        List<Integer> elegiveis = new ArrayList<>();
        List<Double> pesos = new ArrayList<>();
        List<Double> espacos = new ArrayList<>();
        for (int i : indices) {
            AporteCandidato c = candidatos.get(i);
            if (!c.elegivel()) {
                continue;   // descartado na fase de elegibilidade: não entra no rateio
            }
            double espaco = c.capacidade().doubleValue() - porCandidato.get(i).doubleValue();
            if (espaco <= tol) {
                continue;   // já na capacidade máxima
            }
            double peso = c.fator().doubleValue() * config.getEstrategiaAporte().pesoDoAtivo(
                    c.priorityScore(), c.capacidade().doubleValue(),
                    (c.prioridade() != null) ? c.prioridade() : 0);
            if (peso <= 0d) {
                continue;
            }
            elegiveis.add(i);
            pesos.add(peso);
            espacos.add(espaco);
        }
        if (elegiveis.isEmpty()) {
            return 0d;
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
     * Rateio PONDERADO com TETO ("water-filling"), a mesma conta usada para
     * dividir entre setores e entre ativos.
     *
     * Divide o orçamento proporcional ao peso e REPETE com o que sobrou entre quem
     * ainda tem espaço — sem isso, um item que bate no teto travaria o rateio dos
     * demais. Devolve quanto cada item recebeu, na ordem recebida.
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

    private List<Integer> indicesDaSubclasse(List<AporteCandidato> candidatos, List<Integer> daClasse,
                                             Long subclasseId) {
        return daClasse.stream()
                .filter(i -> subclasseId.equals(candidatos.get(i).subclasseId()))
                .toList();
    }

    private List<Integer> indicesDaSetor(List<AporteCandidato> candidatos, List<Integer> daSubclasse,
                                         Long setorId) {
        return daSubclasse.stream()
                .filter(i -> setorId.equals(candidatos.get(i).setorId()))
                .toList();
    }

    /**
     * Déficit de um nível CONSIDERANDO a tolerância: dentro da faixa o item conta
     * como EQUILIBRADO e não puxa aporte. Diferente do déficit "cru" do
     * comparativo (que é sempre ideal − atual, sem faixa).
     */
    public BigDecimal deficitComTolerancia(BigDecimal percentualIdeal, BigDecimal tolerancia,
                                           BigDecimal percentualAtual, double total) {
        if (percentualIdeal == null || percentualIdeal.signum() <= 0) {
            return moeda(0d);
        }
        double alvo = (percentualIdeal.doubleValue() + nz(tolerancia)) / 100d * total;
        double atual = ((percentualAtual != null) ? percentualAtual.doubleValue() : 0d) / 100d * total;
        return moeda(Math.max(0d, alvo - atual));
    }

    public static double nz(BigDecimal valor) {
        return (valor != null) ? valor.doubleValue() : 0d;
    }

    /** Valor em reais, 2 casas, nunca negativo. */
    public BigDecimal moeda(double valor) {
        return BigDecimal.valueOf(Math.max(0d, valor)).setScale(2, RoundingMode.HALF_UP);
    }
}
