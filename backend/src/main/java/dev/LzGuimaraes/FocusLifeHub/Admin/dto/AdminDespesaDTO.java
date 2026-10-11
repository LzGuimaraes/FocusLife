package dev.LzGuimaraes.FocusLifeHub.Admin.dto;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonFormat;

import dev.LzGuimaraes.FocusLifeHub.Despesa.CategoriaDespesa;
import dev.LzGuimaraes.FocusLifeHub.Despesa.TipoLancamento;

/**
 * Lançamento (conta a pagar ou saldo) de um usuário-alvo, na visão ADMIN
 * (somente leitura).
 *
 * Diferente do {@code DespesaResponseDTO} dos endpoints comuns, inclui o nome e
 * a moeda da carteira dona — sem isso o admin não consegue dizer "exatamente"
 * onde o lançamento vive. Nenhum campo sensível novo é exposto: são os mesmos
 * dados que o próprio usuário vê na carteira de dívidas dele.
 */
public record AdminDespesaDTO(
    Long id,
    String nome,
    Float saldo,
    @JsonFormat(pattern = "yyyy-MM-dd") LocalDate dataVencimento,
    Boolean pago,
    TipoLancamento tipo,
    CategoriaDespesa categoria,
    Long carteiraId,
    String carteiraNome,
    String carteiraMoeda
) {}
