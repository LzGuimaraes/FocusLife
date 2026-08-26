import { useEffect, useState } from "react";
import { toast } from "sonner";
import api from "../api/api";
import Modal from "./Modal";
import { Input, Select, TextArea, NumberInput, Switch } from "./Form";
import type { Workout } from "../types/treinos";

interface ExerciseForm {
  id?: number;
  name: string;
  sets: string;
  repetitions: string;
  weight: string;
  durationMinutes: string;
  distanceKm: string;
  notes: string;
}

interface Props {
  open: boolean;
  editing: Workout | null;
  onClose: () => void;
  onSaved: () => void;
}

const DAYS = [
  { value: "MONDAY", label: "Segunda-feira" },
  { value: "TUESDAY", label: "Terça-feira" },
  { value: "WEDNESDAY", label: "Quarta-feira" },
  { value: "THURSDAY", label: "Quinta-feira" },
  { value: "FRIDAY", label: "Sexta-feira" },
  { value: "SATURDAY", label: "Sábado" },
  { value: "SUNDAY", label: "Domingo" },
];

const emptyExercise = (): ExerciseForm => ({ name: "", sets: "", repetitions: "", weight: "", durationMinutes: "", distanceKm: "", notes: "" });

const toExerciseForm = (e: { id: number; name: string; sets: number | null; repetitions: number | null; weight: number | null; durationMinutes: number | null; distanceKm: number | null; notes: string | null }): ExerciseForm => ({
  id: e.id,
  name: e.name,
  sets: e.sets != null ? String(e.sets) : "",
  repetitions: e.repetitions != null ? String(e.repetitions) : "",
  weight: e.weight != null ? String(e.weight) : "",
  durationMinutes: e.durationMinutes != null ? String(e.durationMinutes) : "",
  distanceKm: e.distanceKm != null ? String(e.distanceKm) : "",
  notes: e.notes || "",
});

export default function TreinoFormModal({ open, editing, onClose, onSaved }: Props) {
  const [title, setTitle] = useState("");
  const [description, setDescription] = useState("");
  const [dayOfWeek, setDayOfWeek] = useState("MONDAY");
  const [isActive, setIsActive] = useState(true);
  const [exercises, setExercises] = useState<ExerciseForm[]>([]);
  const [errors, setErrors] = useState<{ title?: string; exercises?: string }>({});

  useEffect(() => {
    if (!open) return;
    setErrors({});
    if (editing) {
      setTitle(editing.title);
      setDescription(editing.description || "");
      setDayOfWeek(editing.dayOfWeek);
      setIsActive(editing.isActive);
      setExercises(editing.exercises.map(toExerciseForm));
    } else {
      setTitle("");
      setDescription("");
      setDayOfWeek("MONDAY");
      setIsActive(true);
      setExercises([]);
    }
  }, [open, editing]);

  const num = (v: string): number | null => {
    const t = v.trim().replace(",", ".");
    if (t === "") return null;
    const n = Number(t);
    return Number.isFinite(n) ? n : null;
  };

  const updateExercise = (i: number, patch: Partial<ExerciseForm>) =>
    setExercises(list => list.map((e, idx) => (idx === i ? { ...e, ...patch } : e)));

  const moveExercise = (i: number, dir: -1 | 1) => {
    setExercises(list => {
      const j = i + dir;
      if (j < 0 || j >= list.length) return list;
      const next = [...list];
      [next[i], next[j]] = [next[j], next[i]];
      return next;
    });
  };

  const handleSubmit = async () => {
    const e: typeof errors = {};
    if (!title.trim()) e.title = "O título do treino é obrigatório.";
    if (exercises.some(ex => !ex.name.trim())) e.exercises = "Preencha o nome de todos os exercícios (ou remova os vazios).";
    setErrors(e);
    if (Object.keys(e).length > 0) return;

    const payload = {
      title: title.trim(),
      description: description.trim() || null,
      dayOfWeek,
      isActive,
      exercises: exercises.map((ex, i) => ({
        id: ex.id,
        name: ex.name.trim(),
        sets: num(ex.sets),
        repetitions: num(ex.repetitions),
        weight: num(ex.weight),
        durationMinutes: num(ex.durationMinutes),
        distanceKm: num(ex.distanceKm),
        notes: ex.notes.trim() || null,
        order: i,
      })),
    };

    const promise = editing
      ? api.put(`/workouts/alter/${editing.id}`, payload)
      : api.post("/workouts/create", payload);
    toast.promise(promise, {
      loading: "Salvando...",
      success: () => { onClose(); onSaved(); return editing ? "Treino atualizado!" : "Treino criado!"; },
      error: "Erro ao salvar o treino",
    });
  };

  return (
    <Modal open={open} onClose={onClose} title={editing ? "Editar Treino" : "Novo Treino"} onSubmit={handleSubmit} submitLabel="Salvar" width="660px">
      <Input label="Título do treino" value={title} onChange={e => setTitle(e.target.value)} placeholder="Ex: Calistenia, Musculação, Corrida" error={errors.title} required />
      <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "14px", alignItems: "end" }}>
        <Select label="Dia da semana" value={dayOfWeek} onChange={e => setDayOfWeek(e.target.value)} required>
          {DAYS.map(d => <option key={d.value} value={d.value}>{d.label}</option>)}
        </Select>
        <div style={{ paddingBottom: "10px" }}>
          <Switch label="Treino ativo" checked={isActive} onChange={setIsActive} />
        </div>
      </div>
      <TextArea label="Descrição (opcional)" value={description} onChange={e => setDescription(e.target.value)} placeholder="Ex: Treino focado em peito, tríceps e core" />

      <div>
        <span style={{ fontSize: "13px", fontWeight: 600, color: "#374151", display: "block", marginBottom: "6px" }}>Exercícios</span>
        {errors.exercises && <p style={{ margin: "0 0 8px", fontSize: "12px", color: "#ef4444", fontWeight: 500 }}>⚠ {errors.exercises}</p>}
        <div style={{ display: "flex", flexDirection: "column", gap: "12px" }}>
          {exercises.map((ex, i) => (
            <div key={ex.id ?? i} style={{ background: "#f8fafc", border: "1.5px solid #e2e8f0", borderRadius: "12px", padding: "14px", display: "flex", flexDirection: "column", gap: "10px" }}>
              <div style={{ display: "flex", alignItems: "flex-end", gap: "8px" }}>
                <span style={{ fontSize: "13px", fontWeight: 700, color: "#94a3b8", paddingBottom: "10px", flexShrink: 0 }}>{i + 1}.</span>
                <div style={{ flex: 1 }}>
                  <Input label="Nome do exercício" value={ex.name} onChange={e => updateExercise(i, { name: e.target.value })} placeholder="Ex: Flexão, Supino, Corrida" />
                </div>
                <div style={{ display: "flex", gap: "6px", flexShrink: 0, paddingBottom: "10px" }}>
                  <button type="button" onClick={() => moveExercise(i, -1)} disabled={i === 0} title="Mover para cima"
                    style={{ padding: "8px 10px", borderRadius: "8px", border: "none", cursor: i === 0 ? "not-allowed" : "pointer", background: "#eef2ff", color: "#6366f1", fontWeight: 600, fontSize: "13px", opacity: i === 0 ? 0.4 : 1 }}>
                    ↑
                  </button>
                  <button type="button" onClick={() => moveExercise(i, 1)} disabled={i === exercises.length - 1} title="Mover para baixo"
                    style={{ padding: "8px 10px", borderRadius: "8px", border: "none", cursor: i === exercises.length - 1 ? "not-allowed" : "pointer", background: "#eef2ff", color: "#6366f1", fontWeight: 600, fontSize: "13px", opacity: i === exercises.length - 1 ? 0.4 : 1 }}>
                    ↓
                  </button>
                  <button type="button" onClick={() => setExercises(list => list.filter((_, idx) => idx !== i))} title="Remover exercício"
                    style={{ padding: "8px 12px", borderRadius: "8px", border: "none", cursor: "pointer", background: "#fee2e2", color: "#dc2626", fontWeight: 600, fontSize: "13px" }}>
                    ✕
                  </button>
                </div>
              </div>
              <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(110px, 1fr))", gap: "10px" }}>
                <NumberInput label="Séries" value={ex.sets} onChange={v => updateExercise(i, { sets: v })} placeholder="—" />
                <NumberInput label="Repetições" value={ex.repetitions} onChange={v => updateExercise(i, { repetitions: v })} placeholder="—" />
                <NumberInput label="Carga (kg)" value={ex.weight} onChange={v => updateExercise(i, { weight: v })} placeholder="—" decimal />
                <NumberInput label="Tempo (min)" value={ex.durationMinutes} onChange={v => updateExercise(i, { durationMinutes: v })} placeholder="—" />
                <NumberInput label="Distância (km)" value={ex.distanceKm} onChange={v => updateExercise(i, { distanceKm: v })} placeholder="—" decimal />
              </div>
              <TextArea label="Observações (opcional)" value={ex.notes} onChange={e => updateExercise(i, { notes: e.target.value })} placeholder="Ex: Descanso de 60s entre séries" />
            </div>
          ))}
        </div>
        <button type="button" onClick={() => setExercises(list => [...list, emptyExercise()])}
          style={{ marginTop: "10px", padding: "9px 16px", borderRadius: "10px", border: "1.5px dashed #cbd5e1", background: "white", color: "#6366f1", cursor: "pointer", fontSize: "13px", fontWeight: 600, width: "100%" }}>
          + Adicionar exercício
        </button>
      </div>
    </Modal>
  );
}
