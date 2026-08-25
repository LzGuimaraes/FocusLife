/* ═══════════════════════════════════════════════════════════
   Tipos compartilhados da aba Treinos (espelham os DTOs do backend)
   ═══════════════════════════════════════════════════════════ */

export interface WorkoutExercise {
  id: number;
  name: string;
  sets: number | null;
  repetitions: number | null;
  weight: number | null;
  durationMinutes: number | null;
  distanceKm: number | null;
  notes: string | null;
  order: number;
}

/** Treino recorrente (definição). */
export interface Workout {
  id: number;
  title: string;
  description: string | null;
  dayOfWeek: string; // MONDAY..SUNDAY
  isActive: boolean;
  exercises: WorkoutExercise[];
  createdAt?: string | null;
  updatedAt?: string | null;
}

/** Ocorrência semanal (execução). */
export interface WorkoutSession {
  id: number;
  status: "PENDING" | "PARTIAL" | "COMPLETED";
  completionPercentage: number;
  completedAt: string | null;
  notes: string | null;
}

/** Item do checklist de uma ocorrência. */
export interface ExerciseChecklistItem {
  workoutExerciseId: number;
  completionId: number | null;
  name: string;
  sets: number | null;
  repetitions: number | null;
  weight: number | null;
  durationMinutes: number | null;
  distanceKm: number | null;
  notes: string | null;
  completed: boolean;
}

/** Treino da semana em um dia específico. */
export interface WorkoutOccurrence {
  workoutId: number;
  title: string;
  description: string | null;
  date: string; // yyyy-MM-dd
  dayOfWeek: string;
  session: WorkoutSession | null; // null = ocorrência futura (planejada)
  exercises: ExerciseChecklistItem[];
}

export interface WeekSummary {
  start: string;
  end: string;
  plannedCount: number;
  completedCount: number;
  executedCount: number;
  consistencyPercent: number | null;
  currentStreak: number;
  bestStreak: number;
  hasPlannedWorkouts: boolean;
  evaluationKey: string;
  evaluationLabel: string;
  evaluationMessage: string;
}

export interface WeekResponse {
  start: string;
  end: string;
  occurrences: WorkoutOccurrence[];
  summary: WeekSummary;
}

export interface WeekHistory {
  start: string;
  end: string;
  plannedCount: number;
  completedCount: number;
  executedCount: number;
  consistencyPercent: number | null;
  evaluationKey: string;
  evaluationLabel: string;
  evaluationMessage: string;
}
