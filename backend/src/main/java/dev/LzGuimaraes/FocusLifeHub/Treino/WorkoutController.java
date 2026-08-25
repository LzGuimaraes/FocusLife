package dev.LzGuimaraes.FocusLifeHub.Treino;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import dev.LzGuimaraes.FocusLifeHub.Treino.dto.CompleteSessionRequestDTO;
import dev.LzGuimaraes.FocusLifeHub.Treino.dto.ExerciseCompletionRequestDTO;
import dev.LzGuimaraes.FocusLifeHub.Treino.dto.WeekHistoryDTO;
import dev.LzGuimaraes.FocusLifeHub.Treino.dto.WeekResponseDTO;
import dev.LzGuimaraes.FocusLifeHub.Treino.dto.WorkoutRequestDTO;
import dev.LzGuimaraes.FocusLifeHub.Treino.dto.WorkoutResponseDTO;
import dev.LzGuimaraes.FocusLifeHub.Treino.dto.WorkoutSessionDTO;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/workouts")
public class WorkoutController {

    private final WorkoutService workoutService;

    public WorkoutController(WorkoutService workoutService) {
        this.workoutService = workoutService;
    }

    @GetMapping("/all")
    public List<WorkoutResponseDTO> getAll() {
        return workoutService.getAllWorkouts();
    }

    @GetMapping("/all/{id}")
    public WorkoutResponseDTO getById(@PathVariable Long id) {
        return workoutService.getWorkout(id);
    }

    @GetMapping("/week")
    public WeekResponseDTO getWeek(
            @RequestParam("start") @DateTimeFormat(iso = ISO.DATE) LocalDate start,
            @RequestParam("end") @DateTimeFormat(iso = ISO.DATE) LocalDate end) {
        return workoutService.getWeek(start, end);
    }

    @GetMapping("/history")
    public List<WeekHistoryDTO> getHistory(@RequestParam(defaultValue = "12") int weeks) {
        return workoutService.getHistory(weeks);
    }

    @PostMapping("/create")
    public ResponseEntity<WorkoutResponseDTO> create(@Valid @RequestBody WorkoutRequestDTO dto) {
        return new ResponseEntity<>(workoutService.createWorkout(dto), HttpStatus.CREATED);
    }

    @PutMapping("/alter/{id}")
    public WorkoutResponseDTO update(@PathVariable Long id, @Valid @RequestBody WorkoutRequestDTO dto) {
        return workoutService.updateWorkout(id, dto);
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        workoutService.deleteWorkout(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/sessions/{sessionId}/exercises/{exerciseId}")
    public WorkoutSessionDTO toggleExercise(@PathVariable Long sessionId,
                                            @PathVariable Long exerciseId,
                                            @RequestBody ExerciseCompletionRequestDTO body) {
        return workoutService.toggleExerciseCompletion(sessionId, exerciseId, body.completed());
    }

    @PostMapping("/sessions/{sessionId}/complete")
    public WorkoutSessionDTO complete(@PathVariable Long sessionId,
                                      @RequestBody(required = false) CompleteSessionRequestDTO body) {
        return workoutService.completeSession(sessionId, body != null ? body.notes() : null);
    }
}
