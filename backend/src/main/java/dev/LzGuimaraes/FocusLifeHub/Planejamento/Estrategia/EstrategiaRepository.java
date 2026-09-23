package dev.LzGuimaraes.FocusLifeHub.Planejamento.Estrategia;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EstrategiaRepository extends JpaRepository<EstrategiaModel, Long> {

    Page<EstrategiaModel> findByUserId(Long userId, Pageable pageable);

    List<EstrategiaModel> findByUserIdOrderByNomeAsc(Long userId);

    Optional<EstrategiaModel> findByIdAndUserId(Long id, Long userId);
}
