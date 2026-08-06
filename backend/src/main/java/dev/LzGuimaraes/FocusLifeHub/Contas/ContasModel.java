package dev.LzGuimaraes.FocusLifeHub.Contas;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;

import dev.LzGuimaraes.FocusLifeHub.Carteira.CarteiraDividasModel;
import dev.LzGuimaraes.FocusLifeHub.Carteira.CarteiraInvestimentoModel;
import dev.LzGuimaraes.FocusLifeHub.Carteira.CarteiraModel;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "tb_ativos")
@Getter
@Setter
public class ContasModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nome;

    @Enumerated(EnumType.STRING)
    private CategoriaAtivo categoria;

    @Enumerated(EnumType.STRING)
    private CategoriaInvestimento categoriaInvestimento;

    private Float quantidade;

    private Float valorUnitario;

    private Float precoAtual;

    private Float saldo;

    private String instituicao;

    private String dataAplicacao;

    private String vencimento;

    @Column(nullable = false)
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate dataVencimento;

    private Float rentabilidade;

    private Boolean pago;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "carteira_investimento_id")
    @JsonIgnore
    private CarteiraInvestimentoModel carteiraInvestimento;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "carteira_dividas_id")
    @JsonIgnore
    private CarteiraDividasModel carteiraDividas;

    /**
     * Retorna a carteira dona deste ativo, independente do tipo
     * (investimento ou dívidas). Usado para checagem de dono e e-mails.
     */
    public CarteiraModel getCarteiraAtiva() {
        return carteiraInvestimento != null ? carteiraInvestimento : carteiraDividas;
    }
}
