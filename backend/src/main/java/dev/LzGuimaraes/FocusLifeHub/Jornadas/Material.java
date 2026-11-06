package dev.LzGuimaraes.FocusLifeHub.Jornadas;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
@Entity @Table(name="jornada_material") @Getter @Setter
public class Material {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(nullable=false) private Long userId;
    private Long atividadeId;
    @Version private long version;
    @Column(nullable=false) private String titulo;
    @Column(nullable=false,length=2048) private String url;
    @Column(length=4000) private String descricao;
}
