package dev.LzGuimaraes.FocusLifeHub.Estudos;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;

import dev.LzGuimaraes.FocusLifeHub.Materia.MateriaModel;
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

@Entity
@Table(name="tb-estudos")
@Getter
@Setter
public class EstudosModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long atividadeId;
    private String nome;
    private int duracao_min;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate data;
    @jakarta.persistence.Column(length=10000)
    private String notas;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "materia_id")
    @JsonIgnore
    private MateriaModel materia;

}
