package dev.LzGuimaraes.FocusLifeHub.Musica;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "musica_estilo")
@Getter
@Setter
public class EstiloMusicalModel {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 80)
    private String nome;
    @Column(name = "nome_chave", nullable = false, unique = true, length = 80)
    private String nomeChave;
}
