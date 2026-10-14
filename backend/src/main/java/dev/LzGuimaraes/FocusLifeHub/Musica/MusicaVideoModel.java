package dev.LzGuimaraes.FocusLifeHub.Musica;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "musica_video", uniqueConstraints = @UniqueConstraint(name = "uk_musica_video_estilo_youtube", columnNames = {"estilo_id", "youtube_id"}))
@Getter
@Setter
public class MusicaVideoModel {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "estilo_id", nullable = false)
    private EstiloMusicalModel estilo;
    @Column(nullable = false, length = 255)
    private String url;
    @Column(name = "youtube_id", nullable = false, length = 11)
    private String youtubeId;
}
