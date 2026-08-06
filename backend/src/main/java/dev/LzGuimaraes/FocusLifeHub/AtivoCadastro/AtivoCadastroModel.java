package dev.LzGuimaraes.FocusLifeHub.AtivoCadastro;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * Catálogo de ativos cadastrados (tickers de renda variável) usados para
 * seleção no cadastro de investimentos. Cadastro feito manualmente no banco.
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
}
