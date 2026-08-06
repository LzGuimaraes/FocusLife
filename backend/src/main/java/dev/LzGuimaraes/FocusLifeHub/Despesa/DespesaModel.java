package dev.LzGuimaraes.FocusLifeHub.Despesa;

import com.fasterxml.jackson.annotation.JsonIgnore;

import dev.LzGuimaraes.FocusLifeHub.Carteira.CarteiraDividasModel;
import dev.LzGuimaraes.FocusLifeHub.Financeiro.ItemFinanceiroModel;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * Despesa / conta a pagar (antigas linhas de tb_ativos com categoria = 'CONTA').
 * Pertence sempre a uma carteira_dividas.
 */
@Entity
@Table(name = "despesa")
@Getter
@Setter
public class DespesaModel extends ItemFinanceiroModel {

    private Boolean pago;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "carteira_dividas_id")
    @JsonIgnore
    private CarteiraDividasModel carteiraDividas;
}
