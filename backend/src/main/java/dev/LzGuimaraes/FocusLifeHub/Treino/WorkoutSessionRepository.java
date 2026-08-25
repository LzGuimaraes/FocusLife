package dev.LzGuimaraes.FocusLifeHub.Treino;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WorkoutSessionRepository extends JpaRepository<WorkoutSessionModel, Long> {

    List<WorkoutSessionModel> findByUserId(Long userId);

    List<WorkoutSessionModel> findByUserIdAndScheduledDateBetween(Long userId, LocalDate start, LocalDate end);

    Optional<WorkoutSessionModel> findByWorkoutIdAndScheduledDate(Long workoutId, LocalDate date);

    boolean existsByWorkoutIdAndScheduledDate(Long workoutId, LocalDate date);
}
