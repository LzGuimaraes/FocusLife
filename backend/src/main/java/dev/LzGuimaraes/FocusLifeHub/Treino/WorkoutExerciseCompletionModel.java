package dev.LzGuimaraes.FocusLifeHub.Treino;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "workout_exercise_completion")
public class WorkoutExerciseCompletionModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workout_session_id")
    @JsonIgnore
    private WorkoutSessionModel session;

    @Column(name = "workout_exercise_id")
    private Long workoutExerciseId;

    /** Snapshot do nome do exercício na ocorrência (protege o histórico). */
    @Column(name = "exercise_name")
    private String exerciseName;

    private Boolean completed = false;

    private String notes;
}
