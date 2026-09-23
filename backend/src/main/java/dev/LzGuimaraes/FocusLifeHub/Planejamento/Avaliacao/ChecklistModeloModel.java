package dev.LzGuimaraes.FocusLifeHub.Planejamento.Avaliacao;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import dev.LzGuimaraes.FocusLifeHub.AtivoCadastro.TipoAtivoCadastro;
import dev.LzGuimaraes.FocusLifeHub.User.UserModel;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
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
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * Modelo de checklist (Módulo 4): template reutilizável criado pelo usuário,
 * que pode ser aplicado a vários ativos. Ex.: "Checklist padrão de ações".
 *
 * Aplicar o modelo a um ativo COPIA as perguntas e regras para o checklist
 * daquele ativo; depois disso, modelo e checklist evoluem independentes.
 */
@Entity
@Table(name = "checklist_modelo")
@Getter
@Setter
public class ChecklistModeloModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String nome;

    @Column(length = 1000)
    private String descricao;

    /** Tipo de ativo sugerido para uso do modelo (opcional, apenas sugestão). */
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_alvo", length = 40)
    private TipoAtivoCadastro tipoAlvo;

    /**
     * QUALIDADE (o ativo é bom?) ou MOMENTO (é hora de aportar?). O checklist
     * do ativo copia este tipo no snapshot; as duas notas são independentes.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoChecklist tipo = TipoChecklist.QUALIDADE;

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

    @OneToMany(mappedBy = "modelo", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("ordem ASC, id ASC")
    private List<ChecklistModeloPerguntaModel> perguntas = new ArrayList<>();
}
