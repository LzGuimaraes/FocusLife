package dev.LzGuimaraes.FocusLifeHub.Despesa.dto;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonFormat;

public record DespesaResponseDTO(
    Long id,
    String nome,
    Float saldo,
    @JsonFormat(pattern = "yyyy-MM-dd")
    LocalDate dataVencimento,
    Boolean pago,
    Long carteira_dividas_id
) {}
