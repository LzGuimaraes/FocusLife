package dev.LzGuimaraes.FocusLifeHub.Admin.dto;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonFormat;

import dev.LzGuimaraes.FocusLifeHub.Ativo.CategoriaInvestimento;

/**
 * Posição de investimento de um usuário-alvo, na visão ADMIN (somente leitura).
 *
 * Diferente do {@code AtivoResponseDTO} dos endpoints comuns, inclui o nome e a
 * moeda da carteira dona — sem isso o admin não consegue dizer "exatamente"
 * onde a posição vive. O preço atual segue a MESMA regra do endpoint comum:
 * quando a posição está vinculada a um ativo do catálogo, o preço do catálogo
 * prevalece sobre o preço guardado na posição.
 *
 * Nenhum campo sensível novo é exposto: são os mesmos dados que o próprio
 * usuário vê na carteira dele.
 */
public record AdminAtivoDTO(
    Long id,
    String nome,
    CategoriaInvestimento categoriaInvestimento,
    Float quantidade,
    Float valorUnitario,
    Float precoAtual,
    Float saldo,
    String instituicao,
    String dataAplicacao,
    String vencimento,
    @JsonFormat(pattern = "yyyy-MM-dd") LocalDate dataVencimento,
    Float rentabilidade,
    Long carteiraId,
    String carteiraNome,
    String carteiraMoeda
) {}
