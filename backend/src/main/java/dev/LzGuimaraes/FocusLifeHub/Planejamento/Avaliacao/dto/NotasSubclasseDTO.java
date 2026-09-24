package dev.LzGuimaraes.FocusLifeHub.Planejamento.Avaliacao.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * NOTAS POR SUBCLASSE — o jeito curto de avaliar.
 *
 * Em vez de abrir um checklist por empresa, o usuário define UM conjunto de
 * perguntas para a SUBCLASSE ("Financeiro", "Bens Industriais") e dá a nota de
 * cada empresa numa única página. As notas continuam sendo POR ATIVO (é o que
 * permite comparar duas empresas do mesmo setor), mas as perguntas são as
 * mesmas — e é essa a nota que o motor de aporte usa para ordenar.
 *
 * As listas são ALINHADAS POR POSIÇÃO: `notas[i]` é a nota da pergunta `i` de
 * `perguntas`. O front desenha as colunas na ordem de `perguntas` e manda de
 * volta na mesma ordem, então não há chave para procurar.
 */
public final class NotasSubclasseDTO {

    private NotasSubclasseDTO() {}

    /** Uma pergunta do checklist padrão da subclasse. */
    public record Pergunta(Long id, String titulo, BigDecimal nota_maxima, int ordem) {}

    /** Uma empresa avaliada: notas na ordem das perguntas (null = não respondida). */
    public record Item(
            UUID ativo_cadastro_id,
            Long ativo_id,
            String ticker,
            Long checklist_id,
            List<BigDecimal> notas,
            /** Nota final 0–100 (null enquanto nada foi respondido). */
            BigDecimal score
    ) {}

    /** Tudo o que a página precisa para desenhar a grade de uma subclasse. */
    public record Painel(
            String subclasse_slug,
            String subclasse_nome,
            Long modelo_id,
            String modelo_nome,
            List<Pergunta> perguntas,
            List<Item> itens
    ) {}

    /** Nota de UMA empresa (mesma ordem das perguntas). */
    public record NotaItem(
            UUID ativo_cadastro_id,
            Long ativo_id,
            /** null/vazio = limpar a resposta daquela pergunta. */
            List<BigDecimal> notas
    ) {}

    public record SalvarRequest(
            /** Nome exibido da subclasse (usado ao criar o checklist padrão). */
            String subclasse_nome,
            List<NotaItem> itens
    ) {}

    public record SalvarResponse(int avaliados, List<Item> itens) {}
}
