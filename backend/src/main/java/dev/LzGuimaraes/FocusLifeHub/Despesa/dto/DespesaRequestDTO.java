package dev.LzGuimaraes.FocusLifeHub.Despesa.dto;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonFormat;

import dev.LzGuimaraes.FocusLifeHub.Despesa.CategoriaDespesa;
import dev.LzGuimaraes.FocusLifeHub.Despesa.TipoLancamento;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record DespesaRequestDTO(

    @NotBlank(message = "O nome é obrigatório")
    String nome,

    @NotNull(message = "O valor (saldo) é obrigatório")
    Float saldo,

    // A obrigatoriedade da data de vencimento depende do tipo: é exigida para
    // DESPESA e ignorada para SALDO (validado no DespesaService).
    @JsonFormat(pattern = "yyyy-MM-dd")
    LocalDate dataVencimento,

    Boolean pago,

    // DESPESA (padrão quando ausente) ou SALDO.
    TipoLancamento tipo,

    // Categoria OPCIONAL do gasto (lista fixa). Sem @NotNull de propósito: a
    // despesa pode ficar sem categoria. Ignorada/limpa quando tipo = SALDO.
    CategoriaDespesa categoria,

    // Sinaliza remoção explícita da categoria. Necessário porque `categoria`
    // nulo tanto pode significar "não mexa" (update parcial vindo de outro
    // formulário) quanto "apague" — separar em um flag evita apagar a
    // categoria sem querer (ex.: alternar "pago" na página global).
    Boolean removerCategoria,

    @NotNull(message = "O ID da carteira de dívidas é obrigatório")
    Long carteira_dividas_id
) {}
