package dev.LzGuimaraes.FocusLifeHub.Planejamento.Avaliacao;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/** Pergunta de um MODELO de checklist. */
@Entity
@Table(name = "checklist_modelo_pergunta")
@Getter
@Setter
public class ChecklistModeloPerguntaModel extends PerguntaBaseModel {

    @Column(nullable = false)
    private Boolean obrigatoria = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "modelo_id")
    @JsonIgnore
    private ChecklistModeloModel modelo;

    @OneToMany(mappedBy = "pergunta", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("ordem ASC, id ASC")
    private List<ChecklistModeloPerguntaRegraModel> regras = new ArrayList<>();
}
