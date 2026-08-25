import { useCallback, useEffect, useMemo, useState } from "react";
import { toast } from "sonner";
import api from "../api/api";
import Layout from "../components/Layout";
import TreinoFormModal from "../components/TreinoFormModal";
import TreinoExecucaoModal from "../components/TreinoExecucaoModal";
import { PageHeader, CardGrid, EmptyState, Spinner, ProgressBar } from "../components/UI";
import { formatLocalDate, parseLocalDate } from "../utils/date";
import type { WeekHistory, WeekResponse, WeekSummary, Workout, WorkoutOccurrence } from "../types/treinos";

/* ── Constantes de semana ── */
const DAY_ORDER = ["MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY"];
const DAY_LABEL: Record<string, string> = {
  MONDAY: "Segunda", TUESDAY: "Terça", WEDNESDAY: "Quarta",
  THURSDAY: "Quinta", FRIDAY: "Sexta", SATURDAY: "Sábado", SUNDAY: "Domingo",
};
const MESES = ["Jan", "Fev", "Mar", "Abr", "Mai", "Jun", "Jul", "Ago", "Set", "Out", "Nov", "Dez"];
const EVAL: Record<string, { emoji: string; color: string }> = {
  EXCELLENT: { emoji: "🔥", color: "#047857" },
  GOOD: { emoji: "🟢", color: "#059669" },
  REGULAR: { emoji: "🟡", color: "#b45309" },
  LOW: { emoji: "🟠", color: "#c2410c" },
  NONE: { emoji: "🔴", color: "#dc2626" },
  NO_PLANNED: { emoji: "⚪", color: "#64748b" },
};

const getWeekRange = (offset: number) => {
  const now = new Date();
  const today = new Date(now.getFullYear(), now.getMonth(), now.getDate());
  const monday = new Date(today.getFullYear(), today.getMonth(), today.getDate() - ((today.getDay() + 6) % 7) + offset * 7);
  const sunday = new Date(monday.getFullYear(), monday.getMonth(), monday.getDate() + 6);
  return { start: monday, end: sunday };
};

const fmtRange = (start: Date, end: Date) => {
  const d = (x: Date) => `${String(x.getDate()).padStart(2, "0")} ${MESES[x.getMonth()]}`;
  return `${d(start)} — ${d(end)}`;
};

const fmtDate = (iso: string) => {
  const dt = parseLocalDate(iso);
  return `${String(dt.getDate()).padStart(2, "0")}/${String(dt.getMonth() + 1).padStart(2, "0")}`;
};

const statusOf = (occ: WorkoutOccurrence): { label: string; bg: string; color: string } => {
  const s = occ.session;
  if (!s) return { label: "Planejado", bg: "#f1f5f9", color: "#64748b" };
  if (s.status === "COMPLETED") return { label: "Concluído", bg: "#d1fae5", color: "#047857" };
  if (s.status === "PARTIAL") return { label: `Parcial ${s.completionPercentage}%`, bg: "#dbeafe", color: "#1d4ed8" };
  return { label: "Pendente", bg: "#fef3c7", color: "#b45309" };
};

export default function Treinos() {
  const [week, setWeek] = useState<WeekResponse | null>(null);
  const [workouts, setWorkouts] = useState<Workout[]>([]);
  const [history, setHistory] = useState<WeekHistory[]>([]);
  const [loading, setLoading] = useState(true);
  const [weekOffset, setWeekOffset] = useState(0);
  const [formModal, setFormModal] = useState<{ open: boolean; editing: Workout | null }>({ open: false, editing: null });
  const [execModal, setExecModal] = useState<{ open: boolean; occurrence: WorkoutOccurrence | null }>({ open: false, occurrence: null });

  const fetchWeek = useCallback(async (offset: number) => {
    const { start, end } = getWeekRange(offset);
    try {
      const r = await api.get(`/workouts/week?start=${formatLocalDate(start)}&end=${formatLocalDate(end)}`);
      setWeek(r.data);
    } catch {
      toast.error("Erro ao carregar a semana");
    } finally {
      setLoading(false);
    }
  }, []);

  const fetchWorkouts = useCallback(async () => {
    try {
      const r = await api.get("/workouts/all");
      setWorkouts(r.data);
    } catch { /* não bloqueia a página */ }
  }, []);

  const fetchHistory = useCallback(async () => {
    try {
      const r = await api.get("/workouts/history?weeks=12");
      setHistory(r.data);
    } catch { /* não bloqueia a página */ }
  }, []);

  useEffect(() => { fetchWeek(weekOffset); }, [weekOffset, fetchWeek]);
  useEffect(() => { fetchWorkouts(); fetchHistory(); }, [fetchWorkouts, fetchHistory]);

  const refreshAll = useCallback(() => {
    fetchWeek(weekOffset);
    fetchWorkouts();
    fetchHistory();
  }, [weekOffset, fetchWeek, fetchWorkouts, fetchHistory]);

  const inactiveWorkouts = useMemo(() => workouts.filter(w => !w.isActive), [workouts]);

  const byDay = useMemo(() => {
    const map = new Map<string, WorkoutOccurrence[]>();
    for (const occ of week?.occurrences ?? []) {
      const arr = map.get(occ.dayOfWeek) ?? [];
      arr.push(occ);
      map.set(occ.dayOfWeek, arr);
    }
    return DAY_ORDER.map(d => ({ day: d, items: map.get(d) ?? [] })).filter(g => g.items.length > 0);
  }, [week]);

  const openCreate = () => setFormModal({ open: true, editing: null });

  const openEdit = async (workoutId: number) => {
    try {
      const r = await api.get(`/workouts/all/${workoutId}`);
      setFormModal({ open: true, editing: r.data });
    } catch {
      toast.error("Erro ao carregar o treino");
    }
  };

  const handleDelete = (workoutId: number) => {
    toast("Excluir este treino? O histórico já realizado será mantido.", {
      action: {
        label: "Sim, excluir",
        onClick: () => {
          toast.promise(api.delete(`/workouts/delete/${workoutId}`), {
            loading: "Excluindo...",
            success: () => { refreshAll(); return "Treino excluído!"; },
            error: "Erro ao excluir",
          });
        },
      },
      cancel: { label: "Cancelar", onClick: () => {} },
    });
  };

  const handleToggleActive = async (workout: Workout) => {
    const payload = {
      title: workout.title,
      description: workout.description,
      dayOfWeek: workout.dayOfWeek,
      isActive: !workout.isActive,
      exercises: workout.exercises.map(e => ({
        id: e.id, name: e.name, sets: e.sets, repetitions: e.repetitions,
        weight: e.weight, durationMinutes: e.durationMinutes, distanceKm: e.distanceKm,
        notes: e.notes, order: e.order,
      })),
    };
    try {
      await api.put(`/workouts/alter/${workout.id}`, payload);
      toast.success(workout.isActive ? "Treino desativado" : "Treino reativado!");
      refreshAll();
    } catch {
      toast.error("Erro ao atualizar o treino");
    }
  };

  const closeExec = () => {
    setExecModal({ open: false, occurrence: null });
    refreshAll(); // sincroniza toggles que não finalizaram
  };

  const goToWeek = (startStr: string) => {
    const target = parseLocalDate(startStr);
    const { start } = getWeekRange(0);
    const diffDays = Math.round((target.getTime() - start.getTime()) / 86400000);
    setWeekOffset(Math.round(diffDays / 7));
    window.scrollTo({ top: 0, behavior: "smooth" });
  };

  const range = getWeekRange(weekOffset);
  const hasWorkouts = workouts.length > 0;

  return (
    <Layout>
      <PageHeader icon="💪" title="Treinos" subtitle="Acompanhe sua rotina de exercícios e consistência semanal" actionLabel="Novo Treino" onAction={openCreate} />

      {loading && !week ? <Spinner text="Carregando treinos..." /> : !hasWorkouts ? (
        <EmptyState icon="🏋️" title="Você ainda não possui treinos" text="Crie seu primeiro treino e comece a acompanhar sua consistência." actionLabel="Criar Treino" onAction={openCreate} />
      ) : (
        <>
          {/* ── Navegação de semanas ── */}
          <div style={{ display: "flex", alignItems: "center", justifyContent: "center", gap: "10px", flexWrap: "wrap", marginBottom: "24px" }}>
            <button onClick={() => setWeekOffset(o => o - 1)} style={navBtn} title="Semana anterior">← Anterior</button>
            <span style={{ fontSize: "15px", fontWeight: 700, color: "var(--color-text)", minWidth: "170px", textAlign: "center" }}>{fmtRange(range.start, range.end)}</span>
            <button onClick={() => setWeekOffset(o => o + 1)} style={navBtn} title="Próxima semana">Próxima →</button>
            {weekOffset !== 0 && <button onClick={() => setWeekOffset(0)} style={{ ...navBtn, background: "#ede9fe", color: "#6d28d9" }}>Semana atual</button>}
          </div>

          {/* ── Resumo da semana ── */}
          {week && <SummarySection summary={week.summary} />}

          {/* ── Semana sem treinos planejados ── */}
          {week && week.summary.plannedCount === 0 && (
            <div style={{ background: "white", borderRadius: "var(--radius-lg)", padding: "18px", boxShadow: "var(--shadow-sm)", textAlign: "center", color: "var(--color-text-secondary)", fontSize: "14px", marginBottom: "24px" }}>
              Nenhum treino planejado para esta semana.
            </div>
          )}

          {/* ── Treinos da semana por dia ── */}
          {byDay.length > 0 && (
            <div style={{ marginBottom: "12px" }}>
              {byDay.map(group => (
                <div key={group.day}>
                  <div style={{ display: "flex", alignItems: "center", gap: "10px", margin: "16px 0 10px" }}>
                    <span style={{ fontSize: "13px", fontWeight: 700, letterSpacing: "0.08em", color: "#8b5cf6", textTransform: "uppercase" }}>{DAY_LABEL[group.day]}</span>
                    <div style={{ flex: 1, height: "1px", background: "#e2e8f0" }} />
                  </div>
                  <CardGrid>
                    {group.items.map(occ => (
                      <div key={occ.workoutId + occ.date} style={{ background: "white", borderRadius: "var(--radius-lg)", padding: "18px", boxShadow: "var(--shadow-sm)", cursor: "pointer", transition: "all var(--transition-base)", borderLeft: "4px solid #8b5cf6", display: "flex", flexDirection: "column", gap: "8px" }}
                        onClick={() => setExecModal({ open: true, occurrence: occ })}
                        onMouseEnter={e => { e.currentTarget.style.transform = "translateY(-3px)"; e.currentTarget.style.boxShadow = "var(--shadow-lg)"; }}
                        onMouseLeave={e => { e.currentTarget.style.transform = "translateY(0)"; e.currentTarget.style.boxShadow = "var(--shadow-sm)"; }}>
                        <div style={{ display: "flex", justifyContent: "space-between", alignItems: "start", gap: "8px" }}>
                          <h3 style={{ fontSize: "16px", fontWeight: 700, color: "var(--color-text)", margin: 0 }}>{occ.title}</h3>
                          <Badge status={statusOf(occ)} />
                        </div>
                        {occ.description && <p style={{ fontSize: "13px", color: "var(--color-text-secondary)", margin: 0 }}>{occ.description}</p>}
                        <p style={{ fontSize: "12px", color: "var(--color-text-muted)", margin: 0 }}>
                          {occ.exercises.length} {occ.exercises.length === 1 ? "exercício" : "exercícios"} · {fmtDate(occ.date)}
                        </p>
                        {occ.session && occ.session.status !== "PENDING" && <ProgressBar value={occ.session.completionPercentage} />}
                        <div style={{ display: "flex", gap: "8px", marginTop: "4px" }} onClick={e => e.stopPropagation()}>
                          <button onClick={() => openEdit(occ.workoutId)} style={btnSm}>✏️ Editar</button>
                          <button onClick={() => handleDelete(occ.workoutId)} style={{ ...btnSm, background: "var(--color-danger-light)", color: "var(--color-danger)" }}>🗑 Excluir</button>
                        </div>
                      </div>
                    ))}
                  </CardGrid>
                </div>
              ))}
            </div>
          )}

          {/* ── Treinos inativos ── */}
          {inactiveWorkouts.length > 0 && (
            <div style={{ marginTop: "20px" }}>
              <h2 style={{ fontSize: "17px", fontWeight: 700, color: "var(--color-text)", margin: "0 0 12px" }}>Treinos inativos</h2>
              <CardGrid>
                {inactiveWorkouts.map(w => (
                  <div key={w.id} style={{ background: "white", borderRadius: "var(--radius-lg)", padding: "18px", boxShadow: "var(--shadow-sm)", borderLeft: "4px solid #cbd5e1", opacity: 0.85, display: "flex", flexDirection: "column", gap: "8px" }}>
                    <h3 style={{ fontSize: "15px", fontWeight: 700, color: "var(--color-text)", margin: 0 }}>{w.title}</h3>
                    <p style={{ fontSize: "12px", color: "var(--color-text-muted)", margin: 0 }}>{DAY_LABEL[w.dayOfWeek]} · {w.exercises.length} {w.exercises.length === 1 ? "exercício" : "exercícios"}</p>
                    <div style={{ display: "flex", gap: "8px", marginTop: "4px" }}>
                      <button onClick={() => handleToggleActive(w)} style={{ ...btnSm, background: "#d1fae5", color: "#047857" }}>▶ Reativar</button>
                      <button onClick={() => openEdit(w.id)} style={btnSm}>✏️ Editar</button>
                      <button onClick={() => handleDelete(w.id)} style={{ ...btnSm, background: "var(--color-danger-light)", color: "var(--color-danger)" }}>🗑 Excluir</button>
                    </div>
                  </div>
                ))}
              </CardGrid>
            </div>
          )}
        </>
      )}

      {/* ── Histórico ── */}
      {history.length > 0 && <HistorySection history={history} onOpen={goToWeek} />}

      <TreinoFormModal open={formModal.open} editing={formModal.editing} onClose={() => setFormModal({ open: false, editing: null })} onSaved={refreshAll} />
      <TreinoExecucaoModal open={execModal.open} occurrence={execModal.occurrence} onClose={closeExec} onUpdated={refreshAll} />
    </Layout>
  );
}

/* ── Componentes internos ── */

function Badge({ status }: { status: { label: string; bg: string; color: string } }) {
  return <span style={{ background: status.bg, color: status.color, padding: "3px 10px", borderRadius: "var(--radius-full)", fontSize: "12px", fontWeight: 600, whiteSpace: "nowrap" }}>{status.label}</span>;
}

function SummarySection({ summary }: { summary: WeekSummary }) {
  const evalInfo = EVAL[summary.evaluationKey] ?? EVAL.NO_PLANNED;
  return (
    <div style={{ marginBottom: "12px" }}>
      <h2 style={{ fontSize: "15px", fontWeight: 700, color: "var(--color-text)", margin: "0 0 10px" }}>Esta semana</h2>
      <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(150px, 1fr))", gap: "14px" }}>
        <StatCard icon="📋" label="Treinos planejados" value={summary.plannedCount} />
        <StatCard icon="✅" label="Treinos realizados" value={summary.executedCount} />
        <StatCard icon="📈" label="Consistência" value={summary.consistencyPercent != null ? `${summary.consistencyPercent}%` : "—"} />
        <StatCard icon="🔥" label="Sequência atual" value={summary.currentStreak > 0 ? `${summary.currentStreak} semana${summary.currentStreak > 1 ? "s" : ""}` : "0"} sub={summary.bestStreak > 0 ? `Maior: ${summary.bestStreak} semana${summary.bestStreak > 1 ? "s" : ""}` : undefined} />
      </div>

      {summary.hasPlannedWorkouts ? (
        <div style={{ marginTop: "14px", background: "white", borderRadius: "var(--radius-lg)", padding: "18px", boxShadow: "var(--shadow-sm)" }}>
          <div style={{ display: "flex", justifyContent: "space-between", marginBottom: "8px" }}>
            <span style={{ fontSize: "13px", fontWeight: 600, color: "#374151" }}>Consistência semanal</span>
            <span style={{ fontSize: "13px", fontWeight: 700, color: "var(--color-text)" }}>{summary.consistencyPercent ?? 0}%</span>
          </div>
          <ProgressBar value={summary.consistencyPercent ?? 0} />
          <div style={{ display: "flex", alignItems: "center", gap: "10px", marginTop: "12px" }}>
            <span style={{ fontSize: "24px" }}>{evalInfo.emoji}</span>
            <div>
              <p style={{ fontSize: "14px", fontWeight: 700, color: evalInfo.color, margin: 0 }}>{summary.evaluationLabel}</p>
              <p style={{ fontSize: "12px", color: "var(--color-text-secondary)", margin: "2px 0 0" }}>{summary.evaluationMessage}</p>
            </div>
          </div>
        </div>
      ) : (
        <div style={{ marginTop: "14px", background: "white", borderRadius: "var(--radius-lg)", padding: "16px", boxShadow: "var(--shadow-sm)", textAlign: "center", color: "var(--color-text-secondary)", fontSize: "14px" }}>
          {summary.evaluationMessage}
        </div>
      )}
    </div>
  );
}

function StatCard({ icon, label, value, sub }: { icon: string; label: string; value: string | number; sub?: string }) {
  return (
    <div style={{ background: "white", borderRadius: "var(--radius-lg)", padding: "16px", boxShadow: "var(--shadow-sm)", display: "flex", flexDirection: "column", gap: "4px" }}>
      <span style={{ fontSize: "12px", color: "var(--color-text-muted)", fontWeight: 500 }}>{icon} {label}</span>
      <span style={{ fontSize: "26px", fontWeight: 800, color: "var(--color-text)", lineHeight: 1.1 }}>{value}</span>
      {sub && <span style={{ fontSize: "12px", color: "var(--color-text-muted)" }}>{sub}</span>}
    </div>
  );
}

function HistorySection({ history, onOpen }: { history: WeekHistory[]; onOpen: (start: string) => void }) {
  return (
    <div style={{ marginTop: "28px" }}>
      <h2 style={{ fontSize: "17px", fontWeight: 700, color: "var(--color-text)", margin: "0 0 12px" }}>Histórico</h2>
      <div style={{ display: "flex", flexDirection: "column", gap: "10px" }}>
        {history.map(h => {
          const evalInfo = EVAL[h.evaluationKey] ?? EVAL.NO_PLANNED;
          return (
            <button key={h.start} onClick={() => onOpen(h.start)}
              style={{ textAlign: "left", background: "white", borderRadius: "var(--radius-lg)", padding: "16px 18px", boxShadow: "var(--shadow-sm)", border: "none", cursor: "pointer", transition: "all var(--transition-fast)", display: "flex", flexDirection: "column", gap: "8px", width: "100%" }}
              onMouseEnter={e => { e.currentTarget.style.boxShadow = "var(--shadow-md)"; e.currentTarget.style.transform = "translateY(-1px)"; }}
              onMouseLeave={e => { e.currentTarget.style.boxShadow = "var(--shadow-sm)"; e.currentTarget.style.transform = "translateY(0)"; }}>
              <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", gap: "8px", flexWrap: "wrap" }}>
                <span style={{ fontSize: "13px", fontWeight: 700, color: "var(--color-text)" }}>{fmtRange(parseLocalDate(h.start), parseLocalDate(h.end))}</span>
                <span style={{ fontSize: "12px", color: "var(--color-text-muted)" }}>
                  {h.plannedCount} {h.plannedCount === 1 ? "treino" : "treinos"} · {h.completedCount} concluído{h.completedCount === 1 ? "" : "s"}
                </span>
              </div>
              <div style={{ display: "flex", alignItems: "center", gap: "12px", flexWrap: "wrap" }}>
                <div style={{ flex: 1, minWidth: "140px" }}><ProgressBar value={h.consistencyPercent ?? 0} /></div>
                <span style={{ fontSize: "13px", fontWeight: 700, color: "var(--color-text)", width: "42px", textAlign: "right" }}>{h.consistencyPercent != null ? `${h.consistencyPercent}%` : "—"}</span>
                <span style={{ fontSize: "12px", fontWeight: 600, color: evalInfo.color, whiteSpace: "nowrap" }}>{evalInfo.emoji} {h.evaluationLabel}</span>
              </div>
            </button>
          );
        })}
      </div>
    </div>
  );
}

const navBtn: React.CSSProperties = { padding: "8px 16px", borderRadius: "var(--radius-md)", border: "none", cursor: "pointer", fontSize: "13px", fontWeight: 600, background: "white", color: "var(--color-text-secondary)", boxShadow: "var(--shadow-sm)", transition: "all var(--transition-fast)" };
const btnSm: React.CSSProperties = { padding: "7px 14px", borderRadius: "var(--radius-md)", border: "none", cursor: "pointer", fontSize: "12px", fontWeight: 600, background: "var(--color-bg)", color: "var(--color-text-secondary)", transition: "all var(--transition-fast)" };
