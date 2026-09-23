package dev.LzGuimaraes.FocusLifeHub.Carteira;

import com.fasterxml.jackson.annotation.JsonIgnore;

import dev.LzGuimaraes.FocusLifeHub.Planejamento.Estrategia.EstrategiaModel;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "carteira_investimento")
@Getter
@Setter
public class CarteiraInvestimentoModel extends CarteiraModel {

    /**
     * Estratégia de investimentos seguida por esta carteira (opcional).
     * Adicionado pelo módulo de Planejamento — carteiras antigas ficam com NULL
     * e continuam funcionando exatamente como antes.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "estrategia_id")
    @JsonIgnore
    private EstrategiaModel estrategia;
}
