package dev.LzGuimaraes.FocusLifeHub.Materia;


import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;

import org.hibernate.annotations.Formula;

import dev.LzGuimaraes.FocusLifeHub.Estudos.EstudosModel;
import dev.LzGuimaraes.FocusLifeHub.User.UserModel;
import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "tb_materia")
public class MateriaModel {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String nome;
    private String descricao;

    @ElementCollection
    @CollectionTable(name = "materia_dias_semana", joinColumns = @JoinColumn(name = "materia_id"))
    @Column(name = "dia_semana")
    @Enumerated(EnumType.STRING)
    private Set<DayOfWeek> diasSemana = new HashSet<>();

    @Formula("(SELECT COALESCE(SUM(s.duracao_segundos), 0) FROM sessao_estudo s WHERE s.materia_id = id)")
    private Long totalSegundos;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    @JsonIgnore
    private UserModel user;
    @OneToMany(
        mappedBy = "materia",cascade = CascadeType.ALL,fetch = FetchType.LAZY
    )
    private List<EstudosModel> estudos = new ArrayList<>();

}
