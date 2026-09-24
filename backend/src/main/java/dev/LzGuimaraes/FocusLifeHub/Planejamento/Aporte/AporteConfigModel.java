package dev.LzGuimaraes.FocusLifeHub.Planejamento.Aporte;

import java.math.BigDecimal;
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
 * Configuração do MOTOR DE APORTE, uma por usuário.
 *
 * Hoje guarda só a MARGEM OPERACIONAL: o quanto o usuário aceita passar da meta
 * antes de o ativo contar como cheio. A margem é RELATIVA à meta
 * ({@code meta × (1 + margem)}), não uma soma de pontos percentuais.
 *
 * A metodologia é do usuário, não da carteira — por isso `user_id` único e sem
 * FK para carteira. Sem registro gravado o motor usa a margem padrão e NADA é
 * criado: só o PUT grava.
 */
@Entity
@Table(name = "aporte_config")
@Getter
@Setter
public class AporteConfigModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private UserModel user;

    /** Margem operacional em % (relativa à meta). Padrão 5%. */
    @Column(name = "margem_percentual", precision = 5, scale = 2, nullable = false)
    private BigDecimal margemPercentual;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
