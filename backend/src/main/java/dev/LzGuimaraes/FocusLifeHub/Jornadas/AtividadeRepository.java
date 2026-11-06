package dev.LzGuimaraes.FocusLifeHub.Jornadas;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
public interface AtividadeRepository extends JpaRepository<Atividade,Long> {
    List<Atividade> findByUserIdOrderByIdDesc(Long userId);
    long countByUserId(Long userId);
    Optional<Atividade> findByIdAndUserId(Long id,Long userId);
    @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select a from Atividade a where a.id=:id and a.userId=:userId")
    Optional<Atividade> bloquear(@Param("id") Long id,@Param("userId") Long userId);
}
