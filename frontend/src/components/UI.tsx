/* =============================================
   Badge, EmptyState, Spinner — Shared UI
   ============================================= */

import type { ReactNode } from "react";

/* ── Status Badge ── */
const statusMap: Record<string, { bg: string; color: string; label: string }> = {
  PENDENTE:  { bg: "#fef3c7", color: "#b45309", label: "Pendente" },
  Ativa:     { bg: "#dbeafe", color: "#1d4ed8", label: "Ativa" },
  Concluida: { bg: "#d1fae5", color: "#047857", label: "Concluída" },
  Cancelada: { bg: "#fee2e2", color: "#b91c1c", label: "Cancelada" },
};

export function StatusBadge({ status }: { status: string }) {
  const s = statusMap[status] || { bg: "#f3f4f6", color: "#6b7280", label: status };
  return (
    <span style={{ display: "inline-flex", padding: "3px 10px", borderRadius: "var(--radius-full)", fontSize: "12px", fontWeight: 600, background: s.bg, color: s.color }}>
      {s.label}
    </span>
  );
}

/* ── Priority Badge ── */
const prioridadeMap: Record<string, { bg: string; color: string; icon: string }> = {
  alta:  { bg: "#fee2e2", color: "#dc2626", icon: "🔴" },
  media: { bg: "#fef3c7", color: "#d97706", icon: "🟡" },
  baixa: { bg: "#dbeafe", color: "#2563eb", icon: "🔵" },
};

export function PrioridadeBadge({ prioridade }: { prioridade: string }) {
  const p = prioridadeMap[prioridade] || { bg: "#f3f4f6", color: "#6b7280", icon: "⚪" };
  return (
    <span style={{ display: "inline-flex", alignItems: "center", gap: "4px", padding: "3px 10px", borderRadius: "var(--radius-full)", fontSize: "12px", fontWeight: 600, background: p.bg, color: p.color }}>
      {p.icon} {prioridade.charAt(0).toUpperCase() + prioridade.slice(1)}
    </span>
  );
}

/* ── Empty State ── */
export function EmptyState({ icon, title, text, actionLabel, onAction }: { icon: string; title: string; text: string; actionLabel?: string; onAction?: () => void }) {
  return (
    <div className="animate-fade-in" style={{ display: "flex", flexDirection: "column", alignItems: "center", justifyContent: "center", padding: "60px 20px", textAlign: "center" }}>
      <span style={{ fontSize: "56px", marginBottom: "16px" }}>{icon}</span>
      <h3 style={{ fontSize: "20px", fontWeight: 700, color: "var(--color-text)", marginBottom: "8px" }}>{title}</h3>
      <p style={{ fontSize: "14px", color: "var(--color-text-secondary)", marginBottom: "24px", maxWidth: "400px" }}>{text}</p>
      {actionLabel && onAction && (
        <button onClick={onAction} style={{ padding: "12px 24px", background: "var(--color-primary)", color: "white", border: "none", borderRadius: "var(--radius-md)", cursor: "pointer", fontSize: "14px", fontWeight: 600, transition: "all var(--transition-fast)" }}
          onMouseEnter={e => { e.currentTarget.style.background = "var(--color-primary-hover)"; e.currentTarget.style.transform = "translateY(-1px)"; }}
          onMouseLeave={e => { e.currentTarget.style.background = "var(--color-primary)"; e.currentTarget.style.transform = "translateY(0)"; }}>
          {actionLabel}
        </button>
      )}
    </div>
  );
}

/* ── Spinner ── */
export function Spinner({ text }: { text?: string }) {
  return (
    <div style={{ display: "flex", flexDirection: "column", alignItems: "center", justifyContent: "center", padding: "80px 20px", gap: "16px" }}>
      <div style={{ width: "40px", height: "40px", border: "3px solid var(--color-border)", borderTopColor: "var(--color-primary)", borderRadius: "50%", animation: "spin 0.8s linear infinite" }} />
      {text && <p style={{ fontSize: "14px", color: "var(--color-text-secondary)", fontWeight: 500 }}>{text}</p>}
    </div>
  );
}

/* ── Progress Bar ── */
export function ProgressBar({ value, color }: { value: number; color?: string }) {
  const c = color || (value >= 80 ? "#10b981" : value >= 50 ? "#f59e0b" : "#ef4444");
  return (
    <div style={{ width: "100%", height: "8px", background: "var(--color-border)", borderRadius: "var(--radius-full)", overflow: "hidden" }}>
      <div style={{ height: "100%", width: `${Math.min(100, Math.max(0, value))}%`, background: c, borderRadius: "var(--radius-full)", transition: "width 0.6s cubic-bezier(0.4, 0, 0.2, 1)" }} />
    </div>
  );
}

/* ── Page Header ── */
export function PageHeader({ icon, title, subtitle, actionLabel, onAction }: { icon: string; title: string; subtitle: string; actionLabel?: string; onAction?: () => void }) {
  return (
    <div style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-start", flexWrap: "wrap", gap: "16px", marginBottom: "24px" }} className="animate-fade-in">
      <div style={{ display: "flex", alignItems: "center", gap: "12px" }}>
        <span style={{ fontSize: "32px" }}>{icon}</span>
        <div>
          <h1 style={{ fontSize: "24px", fontWeight: 700, color: "var(--color-text)", lineHeight: 1.2 }}>{title}</h1>
          <p style={{ fontSize: "13px", color: "var(--color-text-secondary)", marginTop: "2px" }}>{subtitle}</p>
        </div>
      </div>
      {actionLabel && onAction && (
        <button onClick={onAction} style={{ padding: "11px 22px", background: "var(--color-primary)", color: "white", border: "none", borderRadius: "var(--radius-md)", cursor: "pointer", fontSize: "14px", fontWeight: 600, display: "flex", alignItems: "center", gap: "6px", transition: "all var(--transition-fast)", whiteSpace: "nowrap" }}
          onMouseEnter={e => { e.currentTarget.style.background = "var(--color-primary-hover)"; e.currentTarget.style.transform = "translateY(-1px)"; e.currentTarget.style.boxShadow = "var(--shadow-md)"; }}
          onMouseLeave={e => { e.currentTarget.style.background = "var(--color-primary)"; e.currentTarget.style.transform = "translateY(0)"; e.currentTarget.style.boxShadow = "none"; }}>
          + {actionLabel}
        </button>
      )}
    </div>
  );
}

/* ── Card Grid ── */
export function CardGrid({ children }: { children: ReactNode }) {
  return (
    <div style={{
      display: "grid",
      gridTemplateColumns: "repeat(auto-fill, minmax(min(100%, 320px), 1fr))",
      gap: "clamp(12px, 2vw, 20px)",
    }} className="animate-fade-in">
      {children}
    </div>
  );
}

/* ── Filter Bar ── */
export function FilterBar({ options, selected, onChange }: { options: { value: string; label: string }[]; selected: string; onChange: (v: string) => void }) {
  return (
    <div style={{ display: "flex", gap: "8px", flexWrap: "wrap", marginBottom: "20px" }}>
      {options.map(o => (
        <button key={o.value} onClick={() => onChange(o.value)}
          style={{
            padding: "7px 16px", borderRadius: "var(--radius-full)", border: "none", cursor: "pointer",
            fontSize: "13px", fontWeight: 500, transition: "all var(--transition-fast)",
            background: selected === o.value ? "var(--color-primary)" : "white",
            color: selected === o.value ? "white" : "var(--color-text-secondary)",
            boxShadow: selected === o.value ? "var(--shadow-md)" : "var(--shadow-sm)",
          }}>
          {o.label}
        </button>
      ))}
    </div>
  );
}
