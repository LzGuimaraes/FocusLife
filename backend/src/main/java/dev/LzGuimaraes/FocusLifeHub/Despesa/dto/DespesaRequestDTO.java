package dev.LzGuimaraes.FocusLifeHub.Despesa.dto;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonFormat;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record DespesaRequestDTO(

    @NotBlank(message = "O nome é obrigatório")
    String nome,

    @NotNull(message = "O valor (saldo) é obrigatório")
    Float saldo,

    @NotNull(message = "A data de vencimento é obrigatória")
    @JsonFormat(pattern = "yyyy-MM-dd")
    LocalDate dataVencimento,

    Boolean pago,

    @NotNull(message = "O ID da carteira de dívidas é obrigatório")
    Long carteira_dividas_id
) {}
