package dev.LzGuimaraes.FocusLifeHub.Planejamento.Avaliacao;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnore;

import dev.LzGuimaraes.FocusLifeHub.Ativo.AtivoModel;
import dev.LzGuimaraes.FocusLifeHub.AtivoCadastro.AtivoCadastroModel;
import dev.LzGuimaraes.FocusLifeHub.User.UserModel;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
 * Checklist de um ATIVO (Módulo 2): instância independente, criada em branco ou
 * a partir de um modelo. Um ativo pode ter N checklists (ex.: BBAS3 →
 * Fundamentos, Dividendos, Gestão, Riscos, Valuation).
 *
 * Âncora: `ativoCadastro` (ativo do catálogo/ticker) OU `ativo` (posição, para
 * ativos fora do catálogo como renda fixa). Exatamente um dos dois é
 * preenchido — garantido por CHECK no banco.
 *
 * `modeloOrigemId` é apenas informativo (sem FK): o checklist guarda o
 * SNAPSHOT das perguntas, então excluir o modelo não afeta nada.
 */
@Entity
@Table(name = "checklist_ativo")
@Getter
@Setter
public class ChecklistAtivoModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "modelo_origem_id")
    private Long modeloOrigemId;

    /** Nome do checklist (ex.: "Fundamentos"). Snapshot — não muda com o modelo. */
    @Column(nullable = false, length = 120)
    private String nome;

    /** Peso deste checklist no Quality Score do ativo (Módulo 5). */
    @Column(nullable = false, precision = 9, scale = 4)
    private BigDecimal peso = BigDecimal.ONE;

    @Column(nullable = false)
    private Integer ordem = 0;

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

    /** Ativo do catálogo (ticker) — âncora principal. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ativo_cadastro_id")
    @JsonIgnore
    private AtivoCadastroModel ativoCadastro;

    /** Posição do usuário — âncora alternativa (renda fixa, ativo sem ticker). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ativo_id")
    @JsonIgnore
    private AtivoModel ativo;

    @OneToMany(mappedBy = "checklist", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("ordem ASC, id ASC")
    private List<ChecklistAtivoPerguntaModel> perguntas = new ArrayList<>();
}
