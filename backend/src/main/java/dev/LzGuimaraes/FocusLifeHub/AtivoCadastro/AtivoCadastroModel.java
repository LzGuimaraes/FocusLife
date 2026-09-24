package dev.LzGuimaraes.FocusLifeHub.AtivoCadastro;

import java.util.UUID;

import dev.LzGuimaraes.FocusLifeHub.SetorMercado.SetorMercadoModel;
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

/**
 * Catálogo de ativos cadastrados (tickers de renda variável) usados para
 * seleção no cadastro de investimentos. Cadastro feito manualmente no banco.
 *
 * `setorMercado` é o setor DO TICKER (V32): diferente do setor da Carteira
 * Ideal (que é um balde com % alvo), este sobrevive à carteira e vale para
 * qualquer usuário — é o que permite somar exposição por setor e o motor de
 * aporte decidir por setor. Alimentado pelo sync do catálogo (manual/script) e
 * complementado quando o usuário classifica o ativo numa carteira.
 */
@Entity
@Table(name = "ativo_cadastro")
@Getter
@Setter
public class AtivoCadastroModel {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String nome;

    @Enumerated(EnumType.STRING)
    private TipoAtivoCadastro tipo;

    private Float precoAtual;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "setor_mercado_id")
    private SetorMercadoModel setorMercado;
}
