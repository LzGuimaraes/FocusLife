package dev.LzGuimaraes.FocusLifeHub.Jornadas;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
@Entity @Table(name="jornada_atividade") @Getter @Setter
public class Atividade {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(nullable=false) private Long userId;
    @Version private long version;
    private long revisao;
    @Column(nullable=false) private String titulo;
    @Column(length=10000) private String descricao;
    private LocalDate inicio;
    private LocalDate prazo;
    private String unidade;
    private boolean fracionavel;
    @Column(precision=19,scale=4) private BigDecimal total;
    @Column(nullable=false,precision=19,scale=4) private BigDecimal saldoInicial=BigDecimal.ZERO;
    private LocalDate saldoData;
    @Enumerated(EnumType.STRING) private StatusAtividade status=StatusAtividade.NAO_INICIADA;
    private String prioridade;
    private LocalTime horario;
    private String agrupamento;
    @ElementCollection @CollectionTable(name="jornada_dia",joinColumns=@JoinColumn(name="atividade_id"))
    @Column(name="dia") @Enumerated(EnumType.STRING) private Set<DayOfWeek> diasSemana=new HashSet<>();
    private String origem;
    private Long origemId;
    private Double percentualLegado;
}
