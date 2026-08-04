package dev.LzGuimaraes.FocusLifeHub.Materia;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "sessao_estudo")
@Getter
@Setter
public class SessaoEstudo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long materiaId;

    private LocalDateTime inicio;

    private LocalDateTime fim;

    private long duracaoSegundos;
}
