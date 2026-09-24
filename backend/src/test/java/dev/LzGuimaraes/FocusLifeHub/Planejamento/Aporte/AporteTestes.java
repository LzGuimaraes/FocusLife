package dev.LzGuimaraes.FocusLifeHub.Planejamento.Aporte;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import dev.LzGuimaraes.FocusLifeHub.Ativo.CategoriaInvestimento;
import dev.LzGuimaraes.FocusLifeHub.Planejamento.CarteiraIdeal.dto.ComparativoResponseDTO;

/**
 * Construtores de cenário para os testes do motor de aporte.
 *
 * Os objetos são montados "à mão" (sem banco) com os mesmos campos que o
 * `AporteService` produz. Depois da refatoração, a regra do candidato é:
 *
 *     alvo        = meta × R                      (o DESEJÁVEL)
 *     limite      = min(meta × (1+margem), cadastrado)   (o PERMITIDO)
 *     capacidade  = max(0, limiteEmReais − valorAtual)
 *
 * Ou seja: a capacidade NÃO é o déficit. Use {@link #comMeta} quando o teste
 * quiser essa matemática completa (é o construtor que prova o caso "ativo na
 * meta continua candidato") e {@link #candidato} quando só interessar o número
 * final da capacidade.
 */
final class AporteTestes {

    private AporteTestes() {}

    /* ── Candidatos ── */

    /**
     * Candidato elegível numa classe, sem subclasse.
     *
     * @param limiteEmReais teto em R$ do ativo (é dele que sai a CAPACIDADE, não
     *                      do alvo da meta)
     */
    static AporteCandidato candidato(CategoriaInvestimento classe, String nome, Integer nota,
                                     double valorAtual, double limiteEmReais, Double preco) {
        return candidato(classe, nome, nota, valorAtual, limiteEmReais, preco, null, null, true);
    }

    /** Candidato numa SUBCLASSE. */
    static AporteCandidato candidatoNaSubclasse(CategoriaInvestimento classe, String nome, Integer nota,
                                                double valorAtual, double limiteEmReais, Double preco,
                                                long subclasseId, String subclasseNome) {
        return candidato(classe, nome, nota, valorAtual, limiteEmReais, preco, subclasseId, subclasseNome, true);
    }

    /** Candidato DESCARTADO (ex.: já está no limite): capacidade 0 e não elegível. */
    static AporteCandidato candidatoSemCapacidade(CategoriaInvestimento classe, String nome, Integer nota,
                                                  double valorAtual) {
        return candidato(classe, nome, nota, valorAtual, valorAtual, 1.0, null, null, false);
    }

    /**
     * Candidato montado com a MESMA matemática do `AporteService`:
     * meta × R (alvo), meta × (1+margem) (limite operacional, reduzido pelo
     * limite cadastrado quando ele for menor) e capacidade até o limite.
     */
    static AporteCandidato comMeta(CategoriaInvestimento classe, String nome, Integer nota,
                                   double percentualIdeal, double valorAtual, double patrimonioProjetado,
                                   double margem, BigDecimal limiteCadastrado, Double preco) {
        ReferenciaAporte.Limite limite = ReferenciaAporte.limite(
                BigDecimal.valueOf(percentualIdeal), limiteCadastrado,
                BigDecimal.valueOf(valorAtual), BigDecimal.valueOf(patrimonioProjetado), margem);
        boolean elegivel = limite.capacidade().signum() > 0;
        return new AporteCandidato(
                null, 1L, nome, true, null, null, classe,
                (nota != null) ? BigDecimal.valueOf(nota) : null,
                nota != null, 5, 5, List.of(),
                elegivel,
                elegivel ? StatusElegibilidade.ELEGIVEL : StatusElegibilidade.SEM_CAPACIDADE,
                elegivel ? List.of() : List.of("Sem capacidade: não há espaço até o limite operacional."),
                limiteCadastrado, !elegivel,
                limite.limiteOperacionalPercentual(), limite.limitePercentual(), limite.limiteEmReais(),
                limite.capacidade(),
                ReferenciaAporte.percentualAtualProjetado(BigDecimal.valueOf(valorAtual),
                        BigDecimal.valueOf(patrimonioProjetado)),
                BigDecimal.valueOf(percentualIdeal),
                ReferenciaAporte.moeda(valorAtual), limite.alvoEmReais(),
                limite.deficitAteMeta(), ReferenciaAporte.moeda(Math.max(0d, valorAtual - limite.alvoEmReais().doubleValue())),
                BigDecimal.ZERO,
                (preco != null) ? BigDecimal.valueOf(preco) : null);
    }

    private static AporteCandidato candidato(CategoriaInvestimento classe, String nome, Integer nota,
                                             double valorAtual, double limiteEmReais, Double preco,
                                             Long subclasseId, String subclasseNome, boolean elegivel) {
        double capacidade = Math.max(0d, limiteEmReais - valorAtual);
        return new AporteCandidato(
                null, 1L, nome, true, subclasseId, subclasseNome, classe,
                (nota != null) ? BigDecimal.valueOf(nota) : null,
                nota != null, 5, 5, List.of(),
                elegivel,
                elegivel ? StatusElegibilidade.ELEGIVEL : StatusElegibilidade.SEM_CAPACIDADE,
                elegivel ? List.of() : List.of("Sem capacidade: não há espaço até o limite operacional."),
                null, !elegivel,
                BigDecimal.ZERO, BigDecimal.ZERO, ReferenciaAporte.moeda(limiteEmReais),
                ReferenciaAporte.moeda(capacidade),
                BigDecimal.ZERO, BigDecimal.ZERO,
                ReferenciaAporte.moeda(valorAtual), ReferenciaAporte.moeda(limiteEmReais),
                ReferenciaAporte.moeda(capacidade), BigDecimal.ZERO, BigDecimal.ZERO,
                (preco != null) ? BigDecimal.valueOf(preco) : null);
    }

    /* ── Comparativo (alvos já projetados sobre R) ── */

    /**
     * Classe com alvos JÁ PROJETADOS. Os percentuais são DERIVADOS dos valores
     * (percentual = valor ÷ R), como no comparativo real: o motor calcula o déficit
     * da classe a partir do percentual, então percentual e valor têm de contar a
     * mesma história — é isso que o cenário tem de reproduzir.
     */
    static ComparativoResponseDTO.ClasseComparativoDTO classe(CategoriaInvestimento classe,
                                                             double patrimonioProjetado,
                                                             double valorAtual, double valorAlvo) {
        return classe(classe, patrimonioProjetado, valorAtual, valorAlvo, List.of());
    }

    /**
     * Classe com LIMITE MÁXIMO cadastrado — o único teto que a classe impõe
     * (§15: déficit de classe não é reserva).
     */
    static ComparativoResponseDTO.ClasseComparativoDTO classeComLimite(CategoriaInvestimento classe,
                                                                       double patrimonioProjetado,
                                                                       double valorAtual, double valorAlvo,
                                                                       double limiteMaximoPercentual) {
        ComparativoResponseDTO.ClasseComparativoDTO base =
                classe(classe, patrimonioProjetado, valorAtual, valorAlvo, List.of());
        return new ComparativoResponseDTO.ClasseComparativoDTO(
                base.classe(), base.percentual_ideal(), base.percentual_atual(),
                base.valor_ideal(), base.valor_atual(), base.deficit(), base.excesso(),
                base.tolerancia(), BigDecimal.valueOf(limiteMaximoPercentual), base.subclasses(), base.ativos());
    }

    static ComparativoResponseDTO.ClasseComparativoDTO classe(CategoriaInvestimento classe,
                                                             double patrimonioProjetado,
                                                             double valorAtual, double valorAlvo,
                                                             List<ComparativoResponseDTO.SubclasseComparativoDTO> subclasses) {
        return new ComparativoResponseDTO.ClasseComparativoDTO(
                classe,
                percentual(valorAlvo, patrimonioProjetado),
                percentual(valorAtual, patrimonioProjetado),
                ReferenciaAporte.moeda(valorAlvo), ReferenciaAporte.moeda(valorAtual),
                ReferenciaAporte.deficit(ReferenciaAporte.moeda(valorAlvo), ReferenciaAporte.moeda(valorAtual)),
                ReferenciaAporte.moeda(Math.max(0d, valorAtual - valorAlvo)),
                BigDecimal.ZERO, null, subclasses, List.of());
    }

    /** Subclasse: o `percentual_ideal` é FATIA DA CLASSE; o alvo em R$ vem de fora. */
    static ComparativoResponseDTO.SubclasseComparativoDTO subclasse(long id, String nome,
                                                                    double percentualIdealDaClasse,
                                                                    double valorAtual, double valorAlvo) {
        return new ComparativoResponseDTO.SubclasseComparativoDTO(
                id, nome, BigDecimal.valueOf(percentualIdealDaClasse), BigDecimal.ZERO,
                ReferenciaAporte.moeda(valorAlvo), ReferenciaAporte.moeda(valorAtual),
                ReferenciaAporte.deficit(ReferenciaAporte.moeda(valorAlvo), ReferenciaAporte.moeda(valorAtual)),
                ReferenciaAporte.moeda(Math.max(0d, valorAtual - valorAlvo)),
                BigDecimal.ZERO, null);
    }

    /** Comparativo cujo `valor_total` JÁ é o patrimônio projetado (R = T + A). */
    static ComparativoResponseDTO comparativo(double patrimonioProjetado,
                                              List<ComparativoResponseDTO.ClasseComparativoDTO> classes) {
        return new ComparativoResponseDTO(1L, "BRL", BigDecimal.valueOf(patrimonioProjetado),
                BigDecimal.valueOf(100), classes, List.of());
    }

    private static BigDecimal percentual(double valor, double patrimonio) {
        return BigDecimal.valueOf(valor / patrimonio * 100d).setScale(4, java.math.RoundingMode.HALF_UP);
    }

    /** Ordena como o `AporteService` ordena antes de ratear: elegível, nota, nome. */
    static List<AporteCandidato> ordenar(List<AporteCandidato> candidatos) {
        List<AporteCandidato> lista = new ArrayList<>(candidatos);
        lista.sort(java.util.Comparator
                .comparing(AporteCandidato::elegivel, java.util.Comparator.reverseOrder())
                .thenComparing(AporteCandidato::nota, java.util.Comparator.nullsLast(java.util.Comparator.reverseOrder()))
                .thenComparing(AporteCandidato::nome, java.util.Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));
        return lista;
    }
}
