import type { ReactNode, FormEvent } from "react";

interface ModalProps {
  open: boolean;
  onClose: () => void;
  title: string;
  children: ReactNode;
  onSubmit?: (e: FormEvent) => void;
  submitLabel?: string;
  width?: string;
}

export default function Modal({ open, onClose, title, children, onSubmit, submitLabel, width = "480px" }: ModalProps) {
  if (!open) return null;

  return (
    <div onClick={onClose} style={{
      position: "fixed", inset: 0, zIndex: 1000,
      background: "rgba(15,23,42,0.5)", backdropFilter: "blur(4px)",
      display: "flex", alignItems: "center", justifyContent: "center",
      animation: "fadeIn 0.2s ease-out",
    }}>
      <div onClick={e => e.stopPropagation()} style={{
        background: "white", borderRadius: "14px", width,
        maxWidth: "95vw", maxHeight: "85dvh", overflow: "auto",
        boxShadow: "0 20px 25px -5px rgba(0,0,0,0.15)", animation: "scaleIn 0.25s ease-out",
        margin: "16px",
      }}>
        <div style={{ padding: "clamp(14px, 2.5vw, 20px) clamp(16px, 3vw, 24px)", borderBottom: "1px solid #e2e8f0", display: "flex", justifyContent: "space-between", alignItems: "center", position: "sticky", top: 0, background: "white", zIndex: 1 }}>
          <h2 style={{ fontSize: "clamp(16px, 2vw, 18px)", fontWeight: 700, color: "#0f172a", margin: 0 }}>{title}</h2>
          <button onClick={onClose}
            style={{ background: "var(--color-bg)", border: "none", borderRadius: "var(--radius-sm)", width: "32px", height: "32px", display: "flex", alignItems: "center", justifyContent: "center", cursor: "pointer", fontSize: "18px", color: "var(--color-text-secondary)", transition: "all var(--transition-fast)" }}
            onMouseEnter={e => { e.currentTarget.style.background = "#e2e8f0"; }}
            onMouseLeave={e => { e.currentTarget.style.background = "var(--color-bg)"; }}
          >✕</button>
        </div>

        <form onSubmit={onSubmit} style={{ padding: "24px", display: "flex", flexDirection: "column", gap: "16px" }}>
          {children}
          {onSubmit && (
            <div style={{ display: "flex", gap: "10px", justifyContent: "flex-end", marginTop: "8px" }}>
              <button type="button" onClick={onClose}
                style={{ padding: "10px 20px", background: "var(--color-bg)", border: "1px solid var(--color-border)", borderRadius: "var(--radius-md)", cursor: "pointer", fontSize: "14px", fontWeight: 500, color: "var(--color-text-secondary)", transition: "all var(--transition-fast)" }}>
                Cancelar
              </button>
              <button type="submit"
                style={{ padding: "10px 24px", background: "var(--color-primary)", color: "white", border: "none", borderRadius: "var(--radius-md)", cursor: "pointer", fontSize: "14px", fontWeight: 600, transition: "all var(--transition-fast)" }}
                onMouseEnter={e => { e.currentTarget.style.background = "var(--color-primary-hover)"; }}
                onMouseLeave={e => { e.currentTarget.style.background = "var(--color-primary)"; }}>
                {submitLabel || "Salvar"}
              </button>
            </div>
          )}
        </form>
      </div>
    </div>
  );
}

/* Shared input styles */
export const inputStyle: React.CSSProperties = {
  width: "100%", padding: "11px 14px", fontSize: "14px",
  border: "1px solid var(--color-border)", borderRadius: "var(--radius-md)",
  background: "var(--color-bg)", color: "var(--color-text)",
  outline: "none", transition: "all var(--transition-fast)",
};

export const labelStyle: React.CSSProperties = {
  fontSize: "13px", fontWeight: 600, color: "var(--color-text-secondary)",
  marginBottom: "2px", display: "block",
};

export const selectStyle: React.CSSProperties = {
  ...inputStyle, cursor: "pointer", appearance: "none" as const,
  backgroundImage: `url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='12' height='12' viewBox='0 0 12 12'%3E%3Cpath fill='%2364748b' d='M6 8L1 3h10z'/%3E%3C/svg%3E")`,
  backgroundRepeat: "no-repeat", backgroundPosition: "right 12px center", paddingRight: "36px",
};
