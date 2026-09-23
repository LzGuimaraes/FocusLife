package dev.LzGuimaraes.FocusLifeHub.Planejamento.Avaliacao;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/** Regra de pontuação (faixa ou opção) de uma pergunta de MODELO. */
@Entity
@Table(name = "checklist_modelo_pergunta_regra")
@Getter
@Setter
public class ChecklistModeloPerguntaRegraModel extends RegraBaseModel {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pergunta_id")
    @JsonIgnore
    private ChecklistModeloPerguntaModel pergunta;
}
