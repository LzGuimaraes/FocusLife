package dev.LzGuimaraes.FocusLifeHub.Despesa;

import com.fasterxml.jackson.annotation.JsonIgnore;

import dev.LzGuimaraes.FocusLifeHub.Carteira.CarteiraDividasModel;
import dev.LzGuimaraes.FocusLifeHub.Financeiro.ItemFinanceiroModel;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * Lançamento de uma carteira de dívidas: uma despesa (conta a pagar) ou um
 * saldo (crédito/dinheiro disponível). Antigas linhas de tb_ativos com
 * categoria = 'CONTA'. Pertence sempre a uma carteira_dividas.
 */
@Entity
@Table(name = "despesa")
@Getter
@Setter
public class DespesaModel extends ItemFinanceiroModel {

    /**
     * DESPESA = conta a pagar; SALDO = crédito que abate as despesas no saldo
     * final. O DEFAULT garante que registros antigos continuem sendo despesas.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 20)
    private TipoLancamento tipo = TipoLancamento.DESPESA;

    /**
     * Categoria OPCIONAL do gasto (ver {@link CategoriaDespesa}). Fica nula
     * quando o usuário não descreve a categoria — a despesa continua válida.
     * Só faz sentido para DESPESA; um SALDO (crédito) não tem categoria.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "categoria", length = 30)
    private CategoriaDespesa categoria;

    private Boolean pago;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "carteira_dividas_id")
    @JsonIgnore
    private CarteiraDividasModel carteiraDividas;
}
