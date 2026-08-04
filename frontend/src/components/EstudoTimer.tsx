import { useEffect, useRef, useState } from "react";
import api from "../api/api";

interface Props {
  materiaId: number;
  totalSegundosIniciais: number;
}

function fmtHHMMSS(segundos: number): string {
  const s = Math.max(0, Math.floor(segundos));
  const h = String(Math.floor(s / 3600)).padStart(2, "0");
  const m = String(Math.floor((s % 3600) / 60)).padStart(2, "0");
  const sec = String(s % 60).padStart(2, "0");
  return `${h}:${m}:${sec}`;
}

export default function EstudoTimer({ materiaId, totalSegundosIniciais }: Props) {
  const [totalSegundos, setTotalSegundos] = useState<number>(totalSegundosIniciais);
  const [sessaoId, setSessaoId] = useState<number | null>(null);
  const [running, setRunning] = useState(false);
  const [elapsed, setElapsed] = useState(0);
  const startRef = useRef<number | null>(null);

  // Cronômetro a cada 1s; clearInterval no cleanup (desmontar ou ao parar)
  useEffect(() => {
    if (!running) return;
    startRef.current = Date.now();
    setElapsed(0);
    const id = setInterval(() => {
      setElapsed(Math.floor((Date.now() - (startRef.current ?? Date.now())) / 1000));
    }, 1000);
    return () => clearInterval(id);
  }, [running]);

  const iniciar = async () => {
    try {
      const r = await api.post(`/materias/${materiaId}/sessoes/iniciar`);
      setSessaoId(r.data.sessaoId);
      setRunning(true);
    } catch {
      // falha silenciosa (ex.: já existe sessão aberta)
    }
  };

  const pausar = async () => {
    if (sessaoId == null) return;
    try {
      const r = await api.put(`/materias/${materiaId}/sessoes/${sessaoId}/pausar`);
      setTotalSegundos(r.data.totalSegundosMateria); // fonte da verdade = backend
    } catch {
      // falha silenciosa
    }
    setRunning(false);
    setSessaoId(null);
  };

  // Enquanto roda: total acumulado + tempo da sessão atual
  const displaySegundos = totalSegundos + elapsed;

  return (
    <div style={{ display: "flex", alignItems: "center", gap: "10px" }}>
      <span style={{ fontVariantNumeric: "tabular-nums", fontWeight: 700, fontSize: "14px", color: "var(--color-text)" }}>
        ⏱ {fmtHHMMSS(displaySegundos)}
      </span>
      <button
        type="button"
        onClick={running ? pausar : iniciar}
        style={{ padding: "6px 12px", borderRadius: "8px", border: "none", cursor: "pointer", fontWeight: 600, fontSize: "12px", background: running ? "#fef3c7" : "#d1fae5", color: running ? "#b45309" : "#047857" }}
      >
        {running ? "⏸ Pausar" : "▶ Iniciar"}
      </button>
    </div>
  );
}

// ── Exemplo de uso (componente plugado na tela de matérias) ──
// <EstudoTimer materiaId={materia.id} totalSegundosIniciais={materia.totalSegundos ?? 0} />
