package dev.LzGuimaraes.FocusLifeHub.Planejamento.Avaliacao.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import dev.LzGuimaraes.FocusLifeHub.Ativo.CategoriaInvestimento;

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
 *
 * NEM TODO ATIVO TEM SUBCLASSE (e nem deve ter): cripto, renda fixa, Tesouro e
 * caixinhas não se dividem em setores. Nesses casos o balde da avaliação é a
 * PRÓPRIA CLASSE (`classe_inteira = true`) e o checklist padrão é o do TIPO do
 * ativo — renda fixa pergunta emissor/liquidez, cripto pergunta tese/custódia.
 * É o que o `Bucket` descreve: a lista de ONDE dá para dar notas.
 */
public final class NotasSubclasseDTO {

    private NotasSubclasseDTO() {}

    /** Uma pergunta do checklist padrão do balde. */
    public record Pergunta(Long id, String titulo, BigDecimal nota_maxima, int ordem) {}

    /**
     * Uma linha avaliável: um ativo do catálogo (ticker) OU uma posição sem
     * ticker (renda fixa, Tesouro, caixinha).
     */
    public record Item(
            UUID ativo_cadastro_id,
            Long ativo_id,
            String ticker,
            /** Nome cadastrado na posição — a tela mostra abaixo do ticker. */
            String nome,
            Long checklist_id,
            List<BigDecimal> notas,
            /** Nota final 0–100 (null enquanto nada foi respondido). */
            BigDecimal score
    ) {}

    /**
     * Onde dá para dar notas nesta carteira: cada SUBCLASSE e, para as classes
     * que não usam subclasse (cripto, renda fixa...), a CLASSE inteira.
     */
    public record Bucket(
            /** Identidade do checklist padrão (subclasse ou classe). */
            String slug,
            String nome,
            CategoriaInvestimento classe,
            /** Rótulo em português da classe ("Criptomoedas"). */
            String classe_label,
            /** true = o balde é a classe inteira (o ativo não tem subclasse). */
            boolean classe_inteira,
            int qtd_ativos
    ) {}

    /** Tudo o que a página precisa para desenhar a grade de um balde. */
    public record Painel(
            String subclasse_slug,
            String subclasse_nome,
            CategoriaInvestimento classe,
            String classe_label,
            boolean classe_inteira,
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
