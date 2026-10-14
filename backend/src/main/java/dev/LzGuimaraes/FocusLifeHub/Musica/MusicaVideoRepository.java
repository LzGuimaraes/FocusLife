package dev.LzGuimaraes.FocusLifeHub.Musica;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface MusicaVideoRepository extends JpaRepository<MusicaVideoModel, Long> {
    List<MusicaVideoModel> findByEstiloIdOrderByIdDesc(Long estiloId);
    boolean existsByEstiloId(Long estiloId);
    boolean existsByEstiloIdAndYoutubeId(Long estiloId, String youtubeId);
    boolean existsByEstiloIdAndYoutubeIdAndIdNot(Long estiloId, String youtubeId, Long id);
    @Query(value = "select * from musica_video where estilo_id = :estiloId and id <> :anteriorId order by random() limit 1", nativeQuery = true)
    Optional<MusicaVideoModel> sortear(@Param("estiloId") Long estiloId, @Param("anteriorId") Long anteriorId);
}
