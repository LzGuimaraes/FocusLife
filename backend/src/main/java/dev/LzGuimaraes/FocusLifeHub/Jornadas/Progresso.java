package dev.LzGuimaraes.FocusLifeHub.Jornadas;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
@Entity @Table(name="jornada_progresso",uniqueConstraints=@UniqueConstraint(columnNames={"atividade_id","request_id"})) @Getter @Setter
public class Progresso {
    public enum Modo { INCREMENTO, ACUMULADO }
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(nullable=false) private Long atividadeId;
    @Column(nullable=false) private UUID requestId;
    @Column(nullable=false) private LocalDate data;
    @Enumerated(EnumType.STRING) private Modo modo;
    @Column(nullable=false,precision=19,scale=4) private BigDecimal quantidade;
    @Column(length=4000) private String observacao;
    private Integer motivacao;
}
