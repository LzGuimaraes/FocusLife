package dev.LzGuimaraes.FocusLifeHub.Email;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(
    name = "email_log",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_email_log_usuario_tipo_ref_data",
        columnNames = {"usuario_id", "tipo", "referencia_id", "data_referencia"}
    )
)
@Getter
@Setter
public class EmailLogModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long usuario_id;

    @Enumerated(EnumType.STRING)
    private TipoEmailLog tipo;

    private Long referencia_id;

    private LocalDate data_referencia;

    private LocalDateTime enviado_em;
}
