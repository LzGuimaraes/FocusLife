package dev.LzGuimaraes.FocusLifeHub.Jornadas;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;
public interface ProgressoRepository extends JpaRepository<Progresso,Long> {
    List<Progresso> findByAtividadeIdOrderByDataAscIdAsc(Long id);
    Optional<Progresso> findByAtividadeIdAndRequestId(Long id,UUID requestId);
    void deleteByAtividadeId(Long id);
}
