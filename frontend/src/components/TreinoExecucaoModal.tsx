import { useEffect, useState } from "react";
import { toast } from "sonner";
import api from "../api/api";
import Modal from "./Modal";
import { ProgressBar } from "./UI";
import { parseLocalDate } from "../utils/date";
import { formatExerciseMetrics } from "../utils/workout";
import type { ExerciseChecklistItem, WorkoutOccurrence, WorkoutSession } from "../types/treinos";

interface Props {
  open: boolean;
  occurrence: WorkoutOccurrence | null;
  onClose: () => void;
  onUpdated: () => void;
}

const statusStyle = (s: string) =>
  s === "COMPLETED" ? { label: "Concluído", bg: "#d1fae5", color: "#047857" }
  : s === "PARTIAL" ? { label: "Realizado parcialmente", bg: "#dbeafe", color: "#1d4ed8" }
  : { label: "Não realizado", bg: "#fef3c7", color: "#b45309" };

const fmtDateTime = (iso: string | null) => {
  if (!iso) return "";
  const d = new Date(iso);
  const p = (n: number) => String(n).padStart(2, "0");
  return `${p(d.getDate())}/${p(d.getMonth() + 1)}/${d.getFullYear()} ${p(d.getHours())}:${p(d.getMinutes())}`;
};

export default function TreinoExecucaoModal({ open, occurrence, onClose, onUpdated }: Props) {
  const [occ, setOcc] = useState<WorkoutOccurrence | null>(null);
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    if (open) setOcc(occurrence);
  }, [open, occurrence]);

  if (!occ) return null;

  const session = occ.session;
  const interactive = !!session;
  const done = occ.exercises.filter(e => e.completed).length;
  const pct = session?.completionPercentage ?? 0;
  const st = statusStyle(session?.status ?? "PENDING");
  const dateLabel = parseLocalDate(occ.date).toLocaleDateString("pt-BR", { weekday: "long", day: "2-digit", month: "2-digit" });

  const toggle = async (ex: ExerciseChecklistItem) => {
    if (!session || ex.completionId == null) return;
    const next = !ex.completed;
    setOcc(o => o ? ({ ...o, exercises: o.exercises.map(x => x.workoutExerciseId === ex.workoutExerciseId ? { ...x, completed: next } : x) }) : o);
    try {
      const r = await api.put(`/workouts/sessions/${session.id}/exercises/${ex.workoutExerciseId}`, { completed: next });
      setOcc(o => o ? ({ ...o, session: r.data as WorkoutSession }) : o);
    } catch {
      setOcc(o => o ? ({ ...o, exercises: o.exercises.map(x => x.workoutExerciseId === ex.workoutExerciseId ? { ...x, completed: !next } : x) }) : o);
      toast.error("Erro ao atualizar o exercício");
    }
  };

  const finalize = async () => {
    if (!session || busy) return;
    setBusy(true);
    try {
      const r = await api.post(`/workouts/sessions/${session.id}/complete`, { notes: null });
      const s = r.data as WorkoutSession;
      setOcc(o => o ? ({ ...o, session: s }) : o);
      const p = s.completionPercentage;
      toast.success(p >= 100 ? "Treino concluído! 💪" : p > 0 ? `Treino parcialmente concluído (${p}%)` : "Nenhum exercício marcado.");
      onUpdated();
      onClose();
    } catch {
      toast.error("Erro ao finalizar o treino");
    } finally {
      setBusy(false);
    }
  };

  return (
    <Modal open={open} onClose={onClose} title={occ.title} width="600px">
      <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", gap: "10px", flexWrap: "wrap" }}>
        <span style={{ fontSize: "13px", color: "var(--color-text-secondary)" }}>📅 {dateLabel}</span>
        <span style={{ background: st.bg, color: st.color, padding: "3px 10px", borderRadius: "var(--radius-full)", fontSize: "12px", fontWeight: 600 }}>{st.label}</span>
      </div>

      {interactive ? (
        <>
          <div style={{ margin: "14px 0" }}>
            <div style={{ display: "flex", justifyContent: "space-between", marginBottom: "6px" }}>
              <span style={{ fontSize: "13px", fontWeight: 600, color: "#374151" }}>{done} de {occ.exercises.length} exercícios concluídos</span>
              <span style={{ fontSize: "13px", fontWeight: 700, color: "var(--color-text)" }}>{pct}%</span>
            </div>
            <ProgressBar value={pct} />
          </div>

          <div style={{ display: "flex", flexDirection: "column", gap: "8px", marginBottom: "16px" }}>
            {occ.exercises.map(ex => (
              <button key={ex.completionId ?? ex.workoutExerciseId} type="button" onClick={() => toggle(ex)}
                style={{
                  display: "flex", alignItems: "flex-start", gap: "12px", textAlign: "left",
                  background: ex.completed ? "#f0fdf4" : "#f8fafc",
                  border: `1.5px solid ${ex.completed ? "#86efac" : "#e2e8f0"}`,
                  borderRadius: "10px", padding: "12px 14px", cursor: "pointer", transition: "all 0.15s ease",
                }}>
                <span style={{ flexShrink: 0, width: "22px", height: "22px", borderRadius: "7px", border: `2px solid ${ex.completed ? "#10b981" : "#cbd5e1"}`, background: ex.completed ? "#10b981" : "transparent", display: "flex", alignItems: "center", justifyContent: "center", color: "white", fontSize: "13px", fontWeight: 700, marginTop: "1px" }}>
                  {ex.completed ? "✓" : ""}
                </span>
                <span style={{ minWidth: 0 }}>
                  <span style={{ display: "block", fontSize: "14px", fontWeight: 600, color: ex.completed ? "#047857" : "#0f172a" }}>{ex.name}</span>
                  {formatExerciseMetrics(ex) && <span style={{ display: "block", fontSize: "12px", color: "var(--color-text-muted)", marginTop: "2px" }}>{formatExerciseMetrics(ex)}</span>}
                  {ex.notes && <span style={{ display: "block", fontSize: "12px", color: "var(--color-text-secondary)", marginTop: "2px", fontStyle: "italic" }}>{ex.notes}</span>}
                </span>
              </button>
            ))}
          </div>

          {session?.status !== "COMPLETED" && (
            <div style={{ display: "flex", justifyContent: "flex-end" }}>
              <button type="button" onClick={finalize} disabled={busy}
                style={{ padding: "11px 24px", background: "#10b981", color: "white", border: "none", borderRadius: "10px", cursor: "pointer", fontSize: "14px", fontWeight: 700, transition: "all 0.15s ease" }}
                onMouseEnter={e => { e.currentTarget.style.background = "#059669"; }}
                onMouseLeave={e => { e.currentTarget.style.background = "#10b981"; }}>
                {busy ? "Salvando..." : "Finalizar treino"}
              </button>
            </div>
          )}

          {session?.completedAt && (
            <p style={{ fontSize: "12px", color: "var(--color-text-muted)", margin: "12px 0 0", textAlign: "center" }}>
              ✓ Finalizado em {fmtDateTime(session.completedAt)}
            </p>
          )}
        </>
      ) : (
        <p style={{ fontSize: "14px", color: "var(--color-text-secondary)", textAlign: "center", padding: "20px 0" }}>
          🗓️ Ocorrência futura — este treino está planejado, mas a semana de execução ainda não chegou.
        </p>
      )}
    </Modal>
  );
}
