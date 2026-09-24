package dev.LzGuimaraes.FocusLifeHub.Planejamento.Aporte;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Candidato a receber aporte, com o que o motor realmente usa.
 *
 * As TRÊS perguntas separadas, na ordem em que são respondidas:
 *
 *   1. ELEGIBILIDADE  posso investir neste ativo?
 *      (critério eliminatório do checklist · limite atingido · capacidade > 0)
 *   2. CAPACIDADE     quanto posso investir?  → `capacidade`, até o LIMITE
 *   3. NOTA           quem deve receber mais?  → peso no rateio
 *
 * `valorIdeal` e `deficit` são a META (o desejável); `limiteEmReais` e
 * `capacidade` são o LIMITE (o permitido). São pares diferentes de propósito, e
 * é essa separação que faz um ativo exatamente na meta continuar candidato.
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
        /** Limite de concentração CADASTRADO pelo usuário (null = não há). */
        BigDecimal limiteMaximo,
        boolean limiteAtingido,
        /** Limite OPERACIONAL em % = meta × (1 + margem). Ex.: meta 5% → 5,25%. */
        BigDecimal limiteOperacionalPercentual,
        /** Limite final em % = min(limite operacional, limite cadastrado). */
        BigDecimal limitePercentual,
        /** Limite final em reais = `limitePercentual × R`. */
        BigDecimal limiteEmReais,
        /**
         * Quanto o ativo ainda pode receber até o LIMITE (não até a meta):
         * {@code max(0, limiteEmReais − valorAtual)}. É este o número que
         * limita o aporte — um ativo exatamente na meta continua com capacidade.
         */
        BigDecimal capacidade,

        /* ── Situação na carteira ── */
        BigDecimal percentualAtual,
        BigDecimal percentualIdeal,
        BigDecimal valorAtual,
        /** Alvo projetado em reais: {@code meta% × R} (a META, não o limite). */
        BigDecimal valorIdeal,
        /** {@code max(0, alvo − valorAtual)} — o espaço DESEJÁVEL até a meta. */
        BigDecimal deficit,
        BigDecimal excesso,
        BigDecimal tolerancia,

        /**
         * Preço ATUAL da cota/unidade (catálogo quando vinculado). null = não se
         * sabe o preço: nesse caso o valor sugerido não é arredondado para
         * unidades inteiras, porque não há unidade para contar.
         */
        BigDecimal precoUnitario
) {}
