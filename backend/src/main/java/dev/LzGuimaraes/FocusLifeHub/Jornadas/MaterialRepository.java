package dev.LzGuimaraes.FocusLifeHub.Jornadas;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;
public interface MaterialRepository extends JpaRepository<Material,Long> {
    List<Material> findByUserIdOrderByIdDesc(Long userId);
    Optional<Material> findByIdAndUserId(Long id,Long userId);
    List<Material> findByAtividadeId(Long id);
}
