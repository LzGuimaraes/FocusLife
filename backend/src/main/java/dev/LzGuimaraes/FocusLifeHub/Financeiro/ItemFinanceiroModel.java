package dev.LzGuimaraes.FocusLifeHub.Financeiro;

import java.time.LocalDate;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;

/**
 * Campos comuns entre Ativo (investimento) e Despesa (conta a pagar).
 * Cada tipo vive em sua própria tabela e não há mais a coluna
 * discriminadora categoria.
 */
@MappedSuperclass
@Getter
@Setter
public abstract class ItemFinanceiroModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nome;

    private Float saldo;

    private LocalDate dataVencimento;
}
