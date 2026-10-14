package dev.LzGuimaraes.FocusLifeHub.Musica;

import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface EstiloMusicalRepository extends JpaRepository<EstiloMusicalModel, Long> {
    boolean existsByNomeChaveAndIdNot(String nomeChave, Long id);
    @Query("select new dev.LzGuimaraes.FocusLifeHub.Musica.MusicaDTO$Estilo(e.id, e.nome, count(v.id)) "
            + "from EstiloMusicalModel e left join MusicaVideoModel v on v.estilo = e "
            + "group by e.id, e.nome order by lower(e.nome), e.id")
    List<MusicaDTO.Estilo> listarComTotais();
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from EstiloMusicalModel e where e.id = :id")
    Optional<EstiloMusicalModel> buscarParaAtualizar(@Param("id") Long id);
}
