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
 * `AporteService` produz: o candidato carrega a CAPACIDADE (déficit do ativo
 * limitado pelo teto) e a classe carrega o alvo JÁ PROJETADO (percentualIdeal × R).
 */
final class AporteTestes {

    private AporteTestes() {}

    /* ── Candidatos ── */

    /** Candidato elegível numa classe, sem subclasse. */
    static AporteCandidato candidato(CategoriaInvestimento classe, String nome, Integer nota,
                                     double valorAtual, double valorAlvo, Double preco) {
        return candidato(classe, nome, nota, valorAtual, valorAlvo, preco, null, null, true);
    }

    /** Candidato numa SUBCLASSE. */
    static AporteCandidato candidatoNaSubclasse(CategoriaInvestimento classe, String nome, Integer nota,
                                                double valorAtual, double valorAlvo, Double preco,
                                                long subclasseId, String subclasseNome) {
        return candidato(classe, nome, nota, valorAtual, valorAlvo, preco, subclasseId, subclasseNome, true);
    }

    /** Candidato DESCARTADO (ex.: já está no próprio teto): capacidade 0 e não elegível. */
    static AporteCandidato candidatoSemCapacidade(CategoriaInvestimento classe, String nome, Integer nota,
                                                  double valorAtual) {
        return candidato(classe, nome, nota, valorAtual, valorAtual, 1.0, null, null, false);
    }

    private static AporteCandidato candidato(CategoriaInvestimento classe, String nome, Integer nota,
                                             double valorAtual, double valorAlvo, Double preco,
                                             Long subclasseId, String subclasseNome, boolean elegivel) {
        double capacidade = Math.max(0d, valorAlvo - valorAtual);
        return new AporteCandidato(
                null, 1L, nome, true, subclasseId, subclasseNome, classe,
                (nota != null) ? BigDecimal.valueOf(nota) : null,
                nota != null, 5, 5, List.of(),
                elegivel,
                elegivel ? StatusElegibilidade.ELEGIVEL : StatusElegibilidade.SEM_CAPACIDADE,
                elegivel ? List.of() : List.of("Sem capacidade: O ativo já está no próprio alvo."),
                null, false, ReferenciaAporte.moeda(capacidade),
                BigDecimal.ZERO, BigDecimal.ZERO,
                ReferenciaAporte.moeda(valorAtual), ReferenciaAporte.moeda(valorAlvo),
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
