package dev.LzGuimaraes.FocusLifeHub.Despesa.dto;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonFormat;

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

    @NotNull(message = "O ID da carteira de dívidas é obrigatório")
    Long carteira_dividas_id
) {}
