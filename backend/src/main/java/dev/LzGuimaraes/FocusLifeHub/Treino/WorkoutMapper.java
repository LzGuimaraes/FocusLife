package dev.LzGuimaraes.FocusLifeHub.Treino;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import dev.LzGuimaraes.FocusLifeHub.Treino.dto.WorkoutExerciseRequestDTO;
import dev.LzGuimaraes.FocusLifeHub.Treino.dto.WorkoutExerciseResponseDTO;
import dev.LzGuimaraes.FocusLifeHub.Treino.dto.WorkoutRequestDTO;
import dev.LzGuimaraes.FocusLifeHub.Treino.dto.WorkoutResponseDTO;
import dev.LzGuimaraes.FocusLifeHub.Treino.dto.WorkoutSessionDTO;
import dev.LzGuimaraes.FocusLifeHub.User.UserModel;

@Component
public class WorkoutMapper {

    public WorkoutModel toModel(WorkoutRequestDTO dto, UserModel user) {
        WorkoutModel workout = new WorkoutModel();
        workout.setTitle(dto.title());
        workout.setDescription(dto.description());
        workout.setDayOfWeek(dto.dayOfWeek());
        workout.setIsActive(dto.isActive() == null || dto.isActive());
        workout.setUser(user);
        return workout;
    }

    public WorkoutResponseDTO toResponse(WorkoutModel model) {
        List<WorkoutExerciseResponseDTO> exercises = model.getExercises().stream()
                .sorted((a, b) -> Integer.compare(
                        a.getSortOrder() == null ? 0 : a.getSortOrder(),
                        b.getSortOrder() == null ? 0 : b.getSortOrder()))
                .map(this::toExerciseResponse)
                .collect(Collectors.toList());

        return new WorkoutResponseDTO(
            model.getId(),
            model.getTitle(),
            model.getDescription(),
            model.getDayOfWeek(),
            model.getIsActive(),
            exercises,
            model.getCreatedAt(),
            model.getUpdatedAt()
        );
    }

    public WorkoutExerciseResponseDTO toExerciseResponse(WorkoutExerciseModel ex) {
        return new WorkoutExerciseResponseDTO(
            ex.getId(),
            ex.getName(),
            ex.getSets(),
            ex.getRepetitions(),
            ex.getWeight(),
            ex.getDurationMinutes(),
            ex.getDistanceKm(),
            ex.getNotes(),
            ex.getSortOrder()
        );
    }

    public WorkoutExerciseModel toExerciseModel(WorkoutExerciseRequestDTO dto, WorkoutModel workout, int order) {
        WorkoutExerciseModel ex = new WorkoutExerciseModel();
        ex.setWorkout(workout);
        applyExerciseFields(ex, dto, order);
        return ex;
    }

    public void applyExerciseFields(WorkoutExerciseModel ex, WorkoutExerciseRequestDTO dto, int order) {
        ex.setName(dto.name());
        ex.setSets(dto.sets());
        ex.setRepetitions(dto.repetitions());
        ex.setWeight(dto.weight());
        ex.setDurationMinutes(dto.durationMinutes());
        ex.setDistanceKm(dto.distanceKm());
        ex.setNotes(dto.notes());
        ex.setSortOrder(order);
    }

    public WorkoutSessionDTO toSessionDTO(WorkoutSessionModel session) {
        return new WorkoutSessionDTO(
            session.getId(),
            session.getStatus() != null ? session.getStatus().name() : SessionStatus.PENDING.name(),
            session.getCompletionPercentage() != null ? session.getCompletionPercentage() : 0,
            session.getCompletedAt(),
            session.getNotes()
        );
    }
}
