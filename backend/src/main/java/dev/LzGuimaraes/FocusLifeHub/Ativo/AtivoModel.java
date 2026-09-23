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

    /**
     * Subclasse da Carteira Ideal à qual esta POSIÇÃO pertence (V26).
     *
     * Existe para os ativos que não têm ticker de catálogo — renda fixa,
     * Tesouro, caixinhas ("Caixa PICPAY"). Nesses casos a meta é o percentual
     * da subclasse, e toda posição atribuída a ela conta para o alvo. Para
     * ativos com ticker, o caminho normal continua sendo a meta
     * (`meta_ativo.subclasse_id`); esta coluna é o fallback/override.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subclasse_id")
    @JsonIgnore
    private dev.LzGuimaraes.FocusLifeHub.Planejamento.CarteiraIdeal.CarteiraIdealSubclasseModel subclasse;

    /**
     * SETOR da Carteira Ideal ao qual esta POSIÇÃO pertence (V28) — nível
     * opcional DENTRO da subclasse. Vale para ativos sem ticker (renda fixa,
     * caixinhas), que não têm meta própria.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "setor_id")
    @JsonIgnore
    private dev.LzGuimaraes.FocusLifeHub.Planejamento.CarteiraIdeal.CarteiraIdealSetorModel setor;
}
