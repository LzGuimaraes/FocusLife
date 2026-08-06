package dev.LzGuimaraes.FocusLifeHub.Carteira;

import com.fasterxml.jackson.annotation.JsonIgnore;

import dev.LzGuimaraes.FocusLifeHub.User.UserModel;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;

/**
 * Base comum das carteiras. Após a divisão da antiga tabela tb_financas,
 * cada tipo de carteira vive em uma tabela própria (carteira_investimento /
 * carteira_dividas) e não há mais a coluna discriminadora tipo_carteira.
 */
@MappedSuperclass
@Getter
@Setter
public abstract class CarteiraModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nome;

    private String moeda;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    @JsonIgnore
    private UserModel user;
}
