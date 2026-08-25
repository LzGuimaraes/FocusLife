package dev.LzGuimaraes.FocusLifeHub.Treino;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WorkoutRepository extends JpaRepository<WorkoutModel, Long> {

    List<WorkoutModel> findByUserIdOrderByCreatedAtAsc(Long userId);
}
