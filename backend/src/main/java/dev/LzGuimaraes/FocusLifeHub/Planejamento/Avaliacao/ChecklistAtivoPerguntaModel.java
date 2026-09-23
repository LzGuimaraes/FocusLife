package dev.LzGuimaraes.FocusLifeHub.Planejamento.Avaliacao;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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

/**
 * Pergunta de um checklist DO ATIVO: guarda o snapshot da definição vinda do
 * modelo (ou criada direto) + a RESPOSTA do usuário.
 *
 * As duas respostas possíveis ficam separadas de propósito:
 *   • `valorNumerico` / `respostaTexto` → o que o usuário respondeu (bruto)
 *   • `notaAtribuida`                   → a nota que vale no score
 * Para tipos com faixa, a nota é derivada do valor no momento da resposta e
 * fica gravada — mudar as faixas depois não reescreve o passado.
 */
@Entity
@Table(name = "checklist_ativo_pergunta")
@Getter
@Setter
public class ChecklistAtivoPerguntaModel extends PerguntaBaseModel {

    @Column(name = "nota_atribuida", precision = 9, scale = 4)
    private BigDecimal notaAtribuida;

    @Column(name = "valor_numerico", precision = 18, scale = 4)
    private BigDecimal valorNumerico;

    @Column(name = "resposta_texto", length = 1000)
    private String respostaTexto;

    @Column(length = 1000)
    private String observacao;

    @Column(name = "respondido_em")
    private LocalDateTime respondidoEm;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "checklist_id")
    @JsonIgnore
    private ChecklistAtivoModel checklist;

    @OneToMany(mappedBy = "pergunta", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("ordem ASC, id ASC")
    private List<ChecklistAtivoPerguntaRegraModel> regras = new ArrayList<>();
}
