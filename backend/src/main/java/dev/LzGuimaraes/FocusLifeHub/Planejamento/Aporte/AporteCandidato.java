package dev.LzGuimaraes.FocusLifeHub.Planejamento.Aporte;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Candidato a receber aporte, com o que o motor realmente usa.
 *
 * Depois da simplificação o objeto ficou do tamanho da decisão:
 *
 *   situação na carteira (déficit/excesso/tolerância)
 *   → ELEGIBILIDADE (pode ou não receber + motivo)
 *   → NOTA do checklist (ordem entre os elegíveis)
 *   → CAPACIDADE (quanto ainda cabe)
 *
 * Saíram: preço (atual/médio/máximo/oportunidade), momento, prioridade manual,
 * Priority Score com pesos e os termos normalizados da fórmula aberta.
 */
public record AporteCandidato(
        /* ── Identificação / hierarquia ── */
        UUID ativoCadastroId,
        Long metaId,
        String nome,
        boolean vinculado,
        Long subclasseId,
        String subclasseNome,
        dev.LzGuimaraes.FocusLifeHub.Ativo.CategoriaInvestimento classe,

        /* ── NOTA do checklist (0–100): é ela que ordena o aporte ── */
        BigDecimal nota,
        boolean avaliada,
        int perguntas,
        int respondidas,
        /** Critérios eliminatórios reprovados (texto pronto para a tela). */
        List<String> bloqueios,

        /* ── Elegibilidade ── */
        boolean elegivel,
        StatusElegibilidade status,
        /** Motivos do descarte (vazio quando elegível). */
        List<String> motivos,
        BigDecimal limiteMaximo,
        boolean limiteAtingido,
        /** Quanto o ativo ainda pode receber (déficit + tolerância, respeitando o limite). */
        BigDecimal capacidade,

        /* ── Situação na carteira ── */
        BigDecimal percentualAtual,
        BigDecimal percentualIdeal,
        BigDecimal valorAtual,
        BigDecimal valorIdeal,
        BigDecimal deficit,
        BigDecimal excesso,
        BigDecimal tolerancia
) {}
