package dev.LzGuimaraes.FocusLifeHub.Planejamento.Aporte;

/**
 * Candidato a receber aporte, já com TODAS as fases do motor resolvidas:
 *
 *   situação na carteira (déficit/excesso/tolerância)
 *   → preço (atual, médio e oportunidade)
 *   → ELEGIBILIDADE (pode ou não receber + motivo)
 *   → PRIORITY SCORE (ordem entre os elegíveis)
 *   → CAPACIDADE (quanto ainda cabe)
 *
 * Serve tanto para o rateio quanto para a resposta da API — é o mesmo objeto,
 * então a tela nunca mostra um número diferente do que o motor usou.
 *
 * `capacidade` (= teto) é quanto o ativo PODE receber agora. Nunca é
 * ultrapassada pelo rateio: quem enche sai do ciclo.
 */
public record AporteCandidato(
        /* ── Identificação / hierarquia ── */
        java.util.UUID ativoCadastroId,
        Long metaId,
        String nome,
        boolean vinculado,
        Long subclasseId,
        String subclasseNome,
        Long setorId,
        String setorNome,
        dev.LzGuimaraes.FocusLifeHub.Ativo.CategoriaInvestimento classe,

        /* ── Avaliação (checklists do usuário) ── */
        java.math.BigDecimal quality,
        java.math.BigDecimal momento,
        java.math.BigDecimal fator,
        /** Critérios eliminatórios reprovados (texto pronto para a tela). */
        java.util.List<String> bloqueios,

        /* ── Elegibilidade ── */
        boolean elegivel,
        StatusElegibilidade status,
        /** Todos os motivos do descarte (vazio quando elegível). */
        java.util.List<String> motivos,
        java.math.BigDecimal limiteMaximo,
        boolean limiteAtingido,
        /** Quanto o ativo ainda pode receber (déficit + tolerância, respeitando o limite). */
        java.math.BigDecimal capacidade,

        /* ── Preço ── */
        java.math.BigDecimal precoAtual,
        java.math.BigDecimal precoMedio,
        java.math.BigDecimal precoMaximoCompra,
        /** 0..1 = (preço máximo − preço atual) / preço máximo. Null sem regra de preço. */
        java.math.BigDecimal oportunidadePreco,

        /* ── Ranking ── */
        java.math.BigDecimal priorityScore,

        /* ── Situação na carteira ── */
        java.math.BigDecimal percentualAtual,
        java.math.BigDecimal percentualIdeal,
        java.math.BigDecimal valorAtual,
        java.math.BigDecimal valorIdeal,
        java.math.BigDecimal deficit,
        java.math.BigDecimal excesso,
        java.math.BigDecimal tolerancia,
        Integer prioridade,

        /* ── Concentração recente (§24) ── */
        java.math.BigDecimal aportesRecentes,
        int aportesRecentesQtd,

        /* ── Termos normalizados do Priority Score (para a fórmula aberta) ── */
        Double qualityNorm,
        double deficitNorm,
        double excessoNorm,
        double prioridadeNorm,
        Double momentoNorm,
        Double oportunidadeNorm
) {}
