package dev.LzGuimaraes.FocusLifeHub.Planejamento.Estrategia;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnore;

import dev.LzGuimaraes.FocusLifeHub.User.UserModel;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
 * Estratégia de investimentos do usuário (Módulo Planejamento).
 *
 * O sistema NÃO define a metodologia: a estratégia é apenas um container
 * nomeado (nome + descrição) ao qual uma carteira pode ser vinculada.
 * OPCIONAL: uma carteira pode existir sem estratégia.
 */
@Entity
@Table(name = "estrategia_investimento")
@Getter
@Setter
public class EstrategiaModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String nome;

    @Column(length = 1000)
    private String descricao;

    @Column(nullable = false)
    private Boolean ativa = true;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    @JsonIgnore
    private UserModel user;
}
