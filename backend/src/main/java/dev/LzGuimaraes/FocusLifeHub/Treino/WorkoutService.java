package dev.LzGuimaraes.FocusLifeHub.Treino;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.LzGuimaraes.FocusLifeHub.Exceptions.ResourceNotFoundException;
import dev.LzGuimaraes.FocusLifeHub.Treino.dto.ExerciseChecklistItemDTO;
import dev.LzGuimaraes.FocusLifeHub.Treino.dto.WeekHistoryDTO;
import dev.LzGuimaraes.FocusLifeHub.Treino.dto.WeekResponseDTO;
import dev.LzGuimaraes.FocusLifeHub.Treino.dto.WeekSummaryDTO;
import dev.LzGuimaraes.FocusLifeHub.Treino.dto.WorkoutExerciseRequestDTO;
import dev.LzGuimaraes.FocusLifeHub.Treino.dto.WorkoutOccurrenceDTO;
import dev.LzGuimaraes.FocusLifeHub.Treino.dto.WorkoutRequestDTO;
import dev.LzGuimaraes.FocusLifeHub.Treino.dto.WorkoutResponseDTO;
import dev.LzGuimaraes.FocusLifeHub.Treino.dto.WorkoutSessionDTO;
import dev.LzGuimaraes.FocusLifeHub.User.UserModel;
import dev.LzGuimaraes.FocusLifeHub.User.UserRepository;
import dev.LzGuimaraes.FocusLifeHub.config.JWTUserData;

@Service
public class WorkoutService {

    /** Timezone configurado pela aplicação (application.properties). */
    private static final ZoneId ZONE = ZoneId.of("America/Cuiaba");

    /** Limite de semanas retroativas consideradas no histórico/sequência. */
    private static final int MAX_STREAK_WEEKS = 200;

    private final WorkoutRepository workoutRepository;
    private final WorkoutSessionRepository workoutSessionRepository;
    private final UserRepository userRepository;
    private final WorkoutMapper workoutMapper;

    public WorkoutService(WorkoutRepository workoutRepository,
                          WorkoutSessionRepository workoutSessionRepository,
                          UserRepository userRepository,
                          WorkoutMapper workoutMapper) {
        this.workoutRepository = workoutRepository;
        this.workoutSessionRepository = workoutSessionRepository;
        this.userRepository = userRepository;
        this.workoutMapper = workoutMapper;
    }

    private Long getAuthenticatedUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        JWTUserData jwtData = (JWTUserData) authentication.getPrincipal();
        return jwtData.userId();
    }

    /* ═══════════════════════════════════════════════════════════
       CRUD do treino (definição recorrente)
       ═══════════════════════════════════════════════════════════ */

    public List<WorkoutResponseDTO> getAllWorkouts() {
        Long userId = getAuthenticatedUserId();
        return workoutRepository.findByUserIdOrderByCreatedAtAsc(userId)
                .stream()
                .map(workoutMapper::toResponse)
                .collect(Collectors.toList());
    }

    public WorkoutResponseDTO getWorkout(Long id) {
        Long userId = getAuthenticatedUserId();
        return workoutMapper.toResponse(findOwnedWorkout(id, userId));
    }

    @Transactional
    public WorkoutResponseDTO createWorkout(WorkoutRequestDTO dto) {
        Long userId = getAuthenticatedUserId();
        UserModel user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário com ID " + userId + " não encontrado"));

        WorkoutModel workout = workoutMapper.toModel(dto, user);
        LocalDateTime now = LocalDateTime.now(ZONE);
        workout.setCreatedAt(now);
        workout.setUpdatedAt(now);
        syncExercises(workout, dto.exercises());

        WorkoutModel saved = workoutRepository.save(workout);
        return workoutMapper.toResponse(saved);
    }

    @Transactional
    public WorkoutResponseDTO updateWorkout(Long id, WorkoutRequestDTO dto) {
        Long userId = getAuthenticatedUserId();
        WorkoutModel workout = findOwnedWorkout(id, userId);

        workout.setTitle(dto.title());
        workout.setDescription(dto.description());
        workout.setDayOfWeek(dto.dayOfWeek());
        if (dto.isActive() != null) {
            workout.setIsActive(dto.isActive());
        }
        workout.setUpdatedAt(LocalDateTime.now(ZONE));
        syncExercises(workout, dto.exercises());

        WorkoutModel saved = workoutRepository.save(workout);
        return workoutMapper.toResponse(saved);
    }

    @Transactional
    public void deleteWorkout(Long id) {
        Long userId = getAuthenticatedUserId();
        WorkoutModel workout = findOwnedWorkout(id, userId);
        // Apenas a definição é removida (cascade nos exercícios). As ocorrências
        // (workout_session) permanecem com snapshots, preservando o histórico.
        workoutRepository.delete(workout);
    }

    /**
     * Sincroniza a lista de exercícios do treino. Exercícios existentes são
     * atualizados, novos são criados e os removidos são deletados (orphanRemoval).
     * NÃO afeta ocorrências já materializadas (histórico permanece intacto).
     */
    private void syncExercises(WorkoutModel workout, List<WorkoutExerciseRequestDTO> dtos) {
        if (dtos == null) {
            workout.getExercises().clear();
            return;
        }
        Map<Long, WorkoutExerciseModel> existing = workout.getExercises().stream()
                .collect(Collectors.toMap(WorkoutExerciseModel::getId, e -> e, (a, b) -> a));

        List<WorkoutExerciseModel> updated = new ArrayList<>();
        int order = 0;
        for (WorkoutExerciseRequestDTO dto : dtos) {
            WorkoutExerciseModel exercise = existing.get(dto.id());
            if (exercise == null) {
                exercise = new WorkoutExerciseModel();
                exercise.setWorkout(workout);
            }
            workoutMapper.applyExerciseFields(exercise, dto, order++);
            updated.add(exercise);
        }
        workout.getExercises().clear();
        workout.getExercises().addAll(updated);
    }

    /* ═══════════════════════════════════════════════════════════
       Semana (ocorrências planejadas + execução)
       ═══════════════════════════════════════════════════════════ */

    @Transactional
    public WeekResponseDTO getWeek(LocalDate start, LocalDate end) {
        Long userId = getAuthenticatedUserId();
        List<WorkoutModel> workouts = workoutRepository.findByUserIdOrderByCreatedAtAsc(userId);
        LocalDate today = LocalDate.now(ZONE);

        List<WorkoutOccurrenceDTO> occurrences = new ArrayList<>();
        for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
            for (WorkoutModel workout : workouts) {
                boolean hasSession = workoutSessionRepository.existsByWorkoutIdAndScheduledDate(workout.getId(), date);

                // Dia diferente só é considerado se já existir ocorrência executada
                // (preserva o histórico mesmo após o dia do treino ser alterado).
                if (workout.getDayOfWeek() != date.getDayOfWeek() && !hasSession) continue;
                LocalDate created = toLocalDate(workout.getCreatedAt());
                if (!hasSession && created != null && date.isBefore(created)) continue;

                boolean applicable = Boolean.TRUE.equals(workout.getIsActive()) || hasSession;
                if (!applicable) continue;

                // Ocorrências passadas/atuais são materializadas sob demanda;
                // futuras ficam virtuais (planejadas, ainda não realizadas).
                WorkoutSessionModel session = null;
                if (!date.isAfter(today)) {
                    session = ensureSession(workout, date);
                }
                occurrences.add(buildOccurrence(workout, date, session));
            }
        }

        // Ocorrências cujo treino foi excluído: o histórico permanece visível
        // via snapshots (workout_session.title / workout_exercise_completion).
        List<WorkoutSessionModel> weekSessions = workoutSessionRepository.findByUserIdAndScheduledDateBetween(userId, start, end);
        Set<Long> existingWorkoutIds = workouts.stream().map(WorkoutModel::getId).collect(Collectors.toSet());
        for (WorkoutSessionModel s : weekSessions) {
            if (!existingWorkoutIds.contains(s.getWorkoutId())) {
                occurrences.add(buildOrphanOccurrence(s));
            }
        }

        List<WorkoutSessionModel> allSessions = workoutSessionRepository.findByUserId(userId);
        WeekSummaryDTO summary = computeWeekSummary(workouts, allSessions, start, end);
        return new WeekResponseDTO(start, end, occurrences, summary);
    }

    public List<WeekHistoryDTO> getHistory(int weeks) {
        Long userId = getAuthenticatedUserId();
        List<WorkoutModel> workouts = workoutRepository.findByUserIdOrderByCreatedAtAsc(userId);
        List<WorkoutSessionModel> allSessions = workoutSessionRepository.findByUserId(userId);
        Map<String, WorkoutSessionModel> sessionIndex = indexSessions(allSessions);

        LocalDate today = LocalDate.now(ZONE);
        LocalDate thisWeekStart = startOfWeek(today);

        List<WeekHistoryDTO> history = new ArrayList<>();
        LocalDate weekEnd = thisWeekStart.minusDays(1);
        int n = Math.max(1, weeks);
        for (int i = 0; i < n; i++) {
            LocalDate weekStart = weekEnd.minusDays(6);
            WeekSummaryDTO summary = computeSummaryForRange(workouts, sessionIndex, weekStart, weekEnd);
            if (summary.plannedCount() > 0 || summary.executedCount() > 0) {
                history.add(new WeekHistoryDTO(
                    summary.start(), summary.end(),
                    summary.plannedCount(), summary.completedCount(), summary.executedCount(),
                    summary.consistencyPercent(),
                    summary.evaluationKey(), summary.evaluationLabel(), summary.evaluationMessage()));
            }
            weekEnd = weekStart.minusDays(1);
        }
        return history;
    }

    /* ═══════════════════════════════════════════════════════════
       Execução (checklist e finalização)
       ═══════════════════════════════════════════════════════════ */

    @Transactional
    public WorkoutSessionDTO toggleExerciseCompletion(Long sessionId, Long exerciseId, Boolean completed) {
        Long userId = getAuthenticatedUserId();
        WorkoutSessionModel session = findOwnedSession(sessionId, userId);

        WorkoutExerciseCompletionModel completion = session.getCompletions().stream()
                .filter(c -> exerciseId != null && exerciseId.equals(c.getWorkoutExerciseId()))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Exercício não encontrado nesta ocorrência"));

        completion.setCompleted(completed != null && completed);
        recalculateSession(session);
        workoutSessionRepository.save(session);
        return workoutMapper.toSessionDTO(session);
    }

    @Transactional
    public WorkoutSessionDTO completeSession(Long sessionId, String notes) {
        Long userId = getAuthenticatedUserId();
        WorkoutSessionModel session = findOwnedSession(sessionId, userId);

        if (notes != null) {
            session.setNotes(notes);
        }
        if (session.getCompletions().isEmpty()) {
            session.setCompletionPercentage(100);
            session.setStatus(SessionStatus.COMPLETED);
        } else {
            recalculateSession(session);
        }

        LocalDateTime now = LocalDateTime.now(ZONE);
        session.setCompletedAt(now);
        session.setUpdatedAt(now);
        workoutSessionRepository.save(session);
        return workoutMapper.toSessionDTO(session);
    }

    /** Recalcula percentual e status a partir dos exercícios marcados. */
    private void recalculateSession(WorkoutSessionModel session) {
        List<WorkoutExerciseCompletionModel> completions = session.getCompletions();
        if (completions.isEmpty()) {
            session.setCompletionPercentage(session.getStatus() == SessionStatus.COMPLETED ? 100 : 0);
            return;
        }
        long done = completions.stream().filter(c -> Boolean.TRUE.equals(c.getCompleted())).count();
        int pct = Math.round(done * 100f / completions.size());
        session.setCompletionPercentage(pct);
        session.setStatus(pct == 100 ? SessionStatus.COMPLETED : (pct > 0 ? SessionStatus.PARTIAL : SessionStatus.PENDING));
        session.setUpdatedAt(LocalDateTime.now(ZONE));
    }

    /* ═══════════════════════════════════════════════════════════
       Helpers
       ═══════════════════════════════════════════════════════════ */

    private WorkoutModel findOwnedWorkout(Long id, Long userId) {
        WorkoutModel workout = workoutRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Treino com ID " + id + " não encontrado"));
        if (workout.getUser() == null || !workout.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("Treino com ID " + id + " não encontrado");
        }
        return workout;
    }

    private WorkoutSessionModel findOwnedSession(Long sessionId, Long userId) {
        WorkoutSessionModel session = workoutSessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Ocorrência com ID " + sessionId + " não encontrada"));
        if (!session.getUserId().equals(userId)) {
            throw new ResourceNotFoundException("Ocorrência com ID " + sessionId + " não encontrada");
        }
        return session;
    }

    /** Cria (sob demanda) a ocorrência da semana para o treino, com checklist de exercícios. */
    private WorkoutSessionModel ensureSession(WorkoutModel workout, LocalDate date) {
        return workoutSessionRepository.findByWorkoutIdAndScheduledDate(workout.getId(), date)
                .orElseGet(() -> {
                    WorkoutSessionModel session = new WorkoutSessionModel();
                    session.setWorkoutId(workout.getId());
                    session.setUserId(workout.getUser().getId());
                    session.setTitle(workout.getTitle());
                    session.setScheduledDate(date);
                    session.setStatus(SessionStatus.PENDING);
                    session.setCompletionPercentage(0);
                    LocalDateTime now = LocalDateTime.now(ZONE);
                    session.setCreatedAt(now);
                    session.setUpdatedAt(now);

                    List<WorkoutExerciseCompletionModel> completions = workout.getExercises().stream()
                            .sorted(Comparator.comparingInt(e -> e.getSortOrder() == null ? 0 : e.getSortOrder()))
                            .map(e -> {
                                WorkoutExerciseCompletionModel c = new WorkoutExerciseCompletionModel();
                                c.setSession(session);
                                c.setWorkoutExerciseId(e.getId());
                                c.setExerciseName(e.getName());
                                c.setCompleted(false);
                                return c;
                            }).collect(Collectors.toList());
                    session.setCompletions(completions);
                    return workoutSessionRepository.save(session);
                });
    }

    private WorkoutOccurrenceDTO buildOccurrence(WorkoutModel workout, LocalDate date, WorkoutSessionModel session) {
        List<ExerciseChecklistItemDTO> exercises;
        if (session != null) {
            // Checklist vem das ocorrências (snapshots). Quando o treino ainda
            // existe, enriquecemos com as métricas atuais da definição.
            Map<Long, WorkoutExerciseModel> defs = workout.getExercises().stream()
                    .collect(Collectors.toMap(WorkoutExerciseModel::getId, e -> e, (a, b) -> a));
            exercises = session.getCompletions().stream()
                    .sorted(Comparator.comparing(WorkoutExerciseCompletionModel::getId))
                    .map(c -> {
                        WorkoutExerciseModel def = defs.get(c.getWorkoutExerciseId());
                        return new ExerciseChecklistItemDTO(
                            c.getWorkoutExerciseId(), c.getId(), c.getExerciseName(),
                            def != null ? def.getSets() : null,
                            def != null ? def.getRepetitions() : null,
                            def != null ? def.getWeight() : null,
                            def != null ? def.getDurationMinutes() : null,
                            def != null ? def.getDistanceKm() : null,
                            c.getNotes(), c.getCompleted());
                    }).collect(Collectors.toList());
        } else {
            // Ocorrência futura: usa a definição atual (tudo pendente).
            exercises = workout.getExercises().stream()
                    .sorted(Comparator.comparingInt(e -> e.getSortOrder() == null ? 0 : e.getSortOrder()))
                    .map(e -> new ExerciseChecklistItemDTO(
                        e.getId(), null, e.getName(),
                        e.getSets(), e.getRepetitions(), e.getWeight(),
                        e.getDurationMinutes(), e.getDistanceKm(), e.getNotes(), false))
                    .collect(Collectors.toList());
        }

        WorkoutSessionDTO sessionDTO = session != null ? workoutMapper.toSessionDTO(session) : null;
        return new WorkoutOccurrenceDTO(
            workout.getId(), workout.getTitle(), workout.getDescription(),
            date, workout.getDayOfWeek(), sessionDTO, exercises);
    }

    /** Ocorrência de um treino que foi excluído (renderizada apenas com snapshots). */
    private WorkoutOccurrenceDTO buildOrphanOccurrence(WorkoutSessionModel session) {
        List<ExerciseChecklistItemDTO> exercises = session.getCompletions().stream()
                .sorted(Comparator.comparing(WorkoutExerciseCompletionModel::getId))
                .map(c -> new ExerciseChecklistItemDTO(
                    c.getWorkoutExerciseId(), c.getId(), c.getExerciseName(),
                    null, null, null, null, null, c.getNotes(), c.getCompleted()))
                .collect(Collectors.toList());

        return new WorkoutOccurrenceDTO(
            session.getWorkoutId(), session.getTitle(), null,
            session.getScheduledDate(), session.getScheduledDate().getDayOfWeek(),
            workoutMapper.toSessionDTO(session), exercises);
    }

    /* ═══════════════════════════════════════════════════════════
       Resumo da semana + consistência + sequência
       ═══════════════════════════════════════════════════════════ */

    private WeekSummaryDTO computeWeekSummary(List<WorkoutModel> workouts,
                                              List<WorkoutSessionModel> allSessions,
                                              LocalDate start, LocalDate end) {
        Map<String, WorkoutSessionModel> sessionIndex = indexSessions(allSessions);
        WeekSummaryDTO summary = computeSummaryForRange(workouts, sessionIndex, start, end);

        LocalDate today = LocalDate.now(ZONE);
        int[] streaks = computeStreaks(workouts, allSessions, today);
        return new WeekSummaryDTO(
            summary.start(), summary.end(),
            summary.plannedCount(), summary.completedCount(), summary.executedCount(),
            summary.consistencyPercent(), streaks[0], streaks[1],
            summary.hasPlannedWorkouts(),
            summary.evaluationKey(), summary.evaluationLabel(), summary.evaluationMessage());
    }

    private WeekSummaryDTO computeSummaryForRange(List<WorkoutModel> workouts,
                                                  Map<String, WorkoutSessionModel> sessionIndex,
                                                  LocalDate start, LocalDate end) {
        int planned = countPlanned(workouts, sessionIndex, start, end);

        List<WorkoutSessionModel> sessions = sessionIndex.values().stream()
                .filter(s -> !s.getScheduledDate().isBefore(start) && !s.getScheduledDate().isAfter(end))
                .collect(Collectors.toList());

        int completed = (int) sessions.stream().filter(s -> s.getStatus() == SessionStatus.COMPLETED).count();
        int executed = (int) sessions.stream()
                .filter(s -> s.getCompletionPercentage() != null && s.getCompletionPercentage() > 0).count();
        int sumPct = sessions.stream()
                .mapToInt(s -> s.getCompletionPercentage() == null ? 0 : s.getCompletionPercentage()).sum();

        Integer consistency = planned > 0 ? Math.round(sumPct / (float) planned) : null;

        String[] evaluation = evaluate(consistency, planned);
        return new WeekSummaryDTO(
            start, end, planned, completed, executed, consistency, null, null,
            planned > 0, evaluation[0], evaluation[1], evaluation[2]);
    }

    /**
     * Quantos treinos foram "planejados" para a semana: treinos cujo dia da
     * semana cai dentro do intervalo, que já existiam na data (createdAt) e que
     * estão ativos OU possuem ocorrência naquela semana (cobre desativados no
     * histórico). Semana sem treinos não é penalizada.
     */
    private int countPlanned(List<WorkoutModel> workouts,
                             Map<String, WorkoutSessionModel> sessionIndex,
                             LocalDate start, LocalDate end) {
        int planned = 0;
        for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
            for (WorkoutModel workout : workouts) {
                boolean hasSession = sessionIndex.containsKey(key(workout.getId(), date));

                // Dia diferente só conta se já existir ocorrência executada
                // (preserva o histórico mesmo após o dia do treino ser alterado).
                if (workout.getDayOfWeek() != date.getDayOfWeek() && !hasSession) continue;
                LocalDate created = toLocalDate(workout.getCreatedAt());
                if (!hasSession && created != null && date.isBefore(created)) continue;
                if (Boolean.TRUE.equals(workout.getIsActive()) || hasSession) {
                    planned++;
                }
            }
        }

        // Ocorrências cujo treino foi excluído continuam contando como planejadas
        // (histórico preservado via snapshots).
        Set<Long> existingWorkoutIds = workouts.stream().map(WorkoutModel::getId).collect(Collectors.toSet());
        for (WorkoutSessionModel s : sessionIndex.values()) {
            if (!existingWorkoutIds.contains(s.getWorkoutId())
                    && !s.getScheduledDate().isBefore(start)
                    && !s.getScheduledDate().isAfter(end)) {
                planned++;
            }
        }
        return planned;
    }

    /**
     * Sequência atual: semanas completas consecutivas (a partir da última semana
     * passada) com consistência >= 75%. Semanas sem treino planejado não contam
     * nem quebram a sequência. A maior sequência histórica é calculada no mesmo
     * passe.
     */
    private int[] computeStreaks(List<WorkoutModel> workouts,
                                 List<WorkoutSessionModel> allSessions,
                                 LocalDate today) {
        LocalDate thisWeekStart = startOfWeek(today);

        LocalDate earliest = thisWeekStart;
        for (WorkoutModel w : workouts) {
            LocalDate created = toLocalDate(w.getCreatedAt());
            if (created != null && created.isBefore(earliest)) earliest = created;
        }
        for (WorkoutSessionModel s : allSessions) {
            if (s.getScheduledDate().isBefore(earliest)) earliest = s.getScheduledDate();
        }
        LocalDate earliestWeekStart = startOfWeek(earliest);

        Map<String, WorkoutSessionModel> sessionIndex = indexSessions(allSessions);

        int current = 0;
        int best = 0;
        int run = 0;
        boolean broken = false;

        LocalDate weekEnd = thisWeekStart.minusDays(1);
        LocalDate weekStart = weekEnd.minusDays(6);
        int guard = 0;
        while (!weekEnd.isBefore(earliestWeekStart) && guard++ < MAX_STREAK_WEEKS) {
            WeekSummaryDTO summary = computeSummaryForRange(workouts, sessionIndex, weekStart, weekEnd);
            boolean qualifies = summary.plannedCount() > 0
                    && summary.consistencyPercent() != null
                    && summary.consistencyPercent() >= 75;
            boolean neutral = summary.plannedCount() == 0;

            if (qualifies) {
                if (!broken) current++;
                run++;
                if (run > best) best = run;
            } else if (!neutral) {
                broken = true;
                run = 0;
            }

            weekEnd = weekStart.minusDays(1);
            weekStart = weekEnd.minusDays(6);
        }
        return new int[]{current, best};
    }

    /** Avaliação objetiva do desempenho semanal (chave, rótulo, mensagem). */
    private String[] evaluate(Integer consistency, int planned) {
        if (planned == 0) {
            return new String[]{"NO_PLANNED", "Sem treinos", "Nenhum treino planejado para esta semana."};
        }
        if (consistency == null || consistency == 0) {
            return new String[]{"NONE", "Nenhum realizado", "Nenhum treino realizado nesta semana."};
        }
        if (consistency >= 90) {
            return new String[]{"EXCELLENT", "Excelente", "Excelente semana! Consistência alta, continue assim."};
        }
        if (consistency >= 75) {
            return new String[]{"GOOD", "Muito bom", "Muito bom! Você manteve uma boa consistência."};
        }
        if (consistency >= 50) {
            return new String[]{"REGULAR", "Regular", "Semana regular. Busque manter pelo menos 75% de consistência."};
        }
        return new String[]{"LOW", "Baixa", "Semana com baixa consistência. Revise sua rotina para recuperar o ritmo."};
    }

    private Map<String, WorkoutSessionModel> indexSessions(List<WorkoutSessionModel> sessions) {
        Map<String, WorkoutSessionModel> index = new HashMap<>();
        for (WorkoutSessionModel s : sessions) {
            index.putIfAbsent(key(s.getWorkoutId(), s.getScheduledDate()), s);
        }
        return index;
    }

    private String key(Long workoutId, LocalDate date) {
        return workoutId + ":" + date;
    }

    private LocalDate startOfWeek(LocalDate date) {
        int diff = (date.getDayOfWeek().getValue() - DayOfWeek.MONDAY.getValue() + 7) % 7;
        return date.minusDays(diff);
    }

    private LocalDate toLocalDate(LocalDateTime dateTime) {
        return dateTime != null ? dateTime.toLocalDate() : null;
    }
}
