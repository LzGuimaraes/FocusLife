package dev.LzGuimaraes.FocusLifeHub.Tarefas;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;

@Repository
public interface TarefasRepository extends JpaRepository<TarefasModel, Long> {
    Page<TarefasModel> findByUserId(Long userId, Pageable pageable);
}
