package dev.LzGuimaraes.FocusLifeHub.Ativo;

import com.fasterxml.jackson.annotation.JsonIgnore;

import dev.LzGuimaraes.FocusLifeHub.AtivoCadastro.AtivoCadastroModel;
import dev.LzGuimaraes.FocusLifeHub.Carteira.CarteiraInvestimentoModel;
import dev.LzGuimaraes.FocusLifeHub.Financeiro.ItemFinanceiroModel;
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
 * Ativo de investimento (antigas linhas de tb_ativos com categoria = 'INVESTIMENTO').
 * Pertence sempre a uma carteira_investimento.
 */
@Entity
@Table(name = "ativo")
@Getter
@Setter
public class AtivoModel extends ItemFinanceiroModel {

    @Enumerated(EnumType.STRING)
    private CategoriaInvestimento categoriaInvestimento;

    private Float quantidade;

    private Float valorUnitario;

    private Float precoAtual;

    private String instituicao;

    private String dataAplicacao;

    private String vencimento;

    private Float rentabilidade;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ativo_cadastro_id")
    @JsonIgnore
    private AtivoCadastroModel ativoCadastro;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "carteira_investimento_id")
    @JsonIgnore
    private CarteiraInvestimentoModel carteiraInvestimento;
}
