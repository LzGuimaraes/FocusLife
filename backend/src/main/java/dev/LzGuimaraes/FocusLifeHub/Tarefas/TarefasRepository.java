package dev.LzGuimaraes.FocusLifeHub.Tarefas;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;

@Repository
public interface TarefasRepository extends JpaRepository<TarefasModel, Long> {
    Page<TarefasModel> findByUserId(Long userId, Pageable pageable);
    List<TarefasModel> findByUserIdAndPrazo(Long userId, LocalDate prazo);
}
