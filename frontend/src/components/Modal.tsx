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
          <button onClick={onClose} aria-label="Fechar"
            style={{ background: "#f1f5f9", border: "none", borderRadius: "8px", width: "32px", height: "32px", display: "flex", alignItems: "center", justifyContent: "center", cursor: "pointer", fontSize: "16px", color: "#64748b", transition: "all 0.15s ease" }}
            onMouseEnter={e => { e.currentTarget.style.background = "#e2e8f0"; }}
            onMouseLeave={e => { e.currentTarget.style.background = "#f1f5f9"; }}
          >✕</button>
        </div>

        {/* IMPORTANTE: preventDefault impede o envio nativo do <form>, que recarregava a página e abortava a requisição axios em andamento */}
        <form onSubmit={(e) => { e.preventDefault(); onSubmit?.(e); }} style={{ padding: "24px", display: "flex", flexDirection: "column", gap: "16px" }}>
          {children}
          {onSubmit && (
            <div style={{ display: "flex", gap: "10px", justifyContent: "flex-end", marginTop: "8px" }}>
              <button type="button" onClick={onClose}
                style={{ padding: "10px 20px", background: "#f1f5f9", border: "1.5px solid #e2e8f0", borderRadius: "10px", cursor: "pointer", fontSize: "14px", fontWeight: 500, color: "#64748b", transition: "all 0.15s ease" }}
                onMouseEnter={e => { e.currentTarget.style.background = "#e2e8f0"; }}
                onMouseLeave={e => { e.currentTarget.style.background = "#f1f5f9"; }}>
                Cancelar
              </button>
              <button type="submit"
                style={{ padding: "10px 24px", background: "#6366f1", color: "white", border: "none", borderRadius: "10px", cursor: "pointer", fontSize: "14px", fontWeight: 600, transition: "all 0.15s ease" }}
                onMouseEnter={e => { e.currentTarget.style.background = "#4f46e5"; e.currentTarget.style.transform = "translateY(-1px)"; }}
                onMouseLeave={e => { e.currentTarget.style.background = "#6366f1"; e.currentTarget.style.transform = "translateY(0)"; }}>
                {submitLabel || "Salvar"}
              </button>
            </div>
          )}
        </form>
      </div>
    </div>
  );
}
