package dev.LzGuimaraes.FocusLifeHub.Despesa;

/**
 * Tipo de lançamento de uma carteira de dívidas.
 *
 * <p>DESPESA = débito (conta a pagar). SALDO = crédito (dinheiro disponível,
 * reserva, rendimento) que abate as despesas no cálculo do saldo final.<br>
 * Saldo final da carteira = Σ(SALDO) − Σ(DESPESA).
 */
public enum TipoLancamento {
    DESPESA,
    SALDO
}
