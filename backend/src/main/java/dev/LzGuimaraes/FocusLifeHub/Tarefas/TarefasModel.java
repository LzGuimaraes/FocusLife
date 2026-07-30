package dev.LzGuimaraes.FocusLifeHub.Tarefas;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonIgnore;

import dev.LzGuimaraes.FocusLifeHub.Tarefas.Enum.Prioridade;
import dev.LzGuimaraes.FocusLifeHub.Tarefas.Enum.TarefaStatus;
import dev.LzGuimaraes.FocusLifeHub.User.UserModel;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "tb_tarefas")
@Getter
@Setter
public class TarefasModel {
    @Id
    @GeneratedValue
    private Long id;
    private String titulo;
    @Enumerated(EnumType.STRING) 
    private TarefaStatus status;
    @Enumerated(EnumType.STRING)
    private Prioridade prioridade;
    private Date prazo;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    @JsonIgnore
    private UserModel user;

}
