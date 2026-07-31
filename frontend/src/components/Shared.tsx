import { forwardRef, type ComponentPropsWithoutRef } from "react";

/* ══════════════════════════════════════════
   Button — variantes: primary, secondary, outline, ghost, destructive
   ══════════════════════════════════════════ */

type ButtonVariant = "primary" | "secondary" | "outline" | "ghost" | "destructive";

export interface ButtonProps extends Omit<ComponentPropsWithoutRef<"button">, "size"> {
  variant?: ButtonVariant;
  loading?: boolean;
  size?: "sm" | "md" | "lg";
}

const variantStyles: Record<ButtonVariant, React.CSSProperties> = {
  primary:      { background: "#6366f1", color: "white", border: "none" },
  secondary:    { background: "#f1f5f9", color: "#334155", border: "1.5px solid #e2e8f0" },
  outline:      { background: "transparent", color: "#6366f1", border: "1.5px solid #6366f1" },
  ghost:        { background: "transparent", color: "#64748b", border: "none" },
  destructive:  { background: "#ef4444", color: "white", border: "none" },
};

const sizeStyles: Record<string, React.CSSProperties> = {
  sm: { height: "32px", padding: "0 12px", fontSize: "12px", borderRadius: "8px" },
  md: { height: "40px", padding: "0 18px", fontSize: "14px", borderRadius: "10px" },
  lg: { height: "48px", padding: "0 24px", fontSize: "16px", borderRadius: "12px" },
};

export const Button = forwardRef<HTMLButtonElement, ButtonProps>(
  function Button({ variant = "primary", loading, disabled, size = "md", children, style, onMouseEnter, onMouseLeave, ...props }, ref) {
    const isDisabled = disabled || loading;

    return (
      <button
        ref={ref}
        disabled={isDisabled}
        style={{
          display: "inline-flex", alignItems: "center", justifyContent: "center", gap: "6px",
          fontWeight: 600, cursor: isDisabled ? "not-allowed" : "pointer",
          opacity: isDisabled ? 0.55 : 1,
          transition: "all 0.15s ease",
          ...sizeStyles[size],
          ...variantStyles[variant],
          ...style,
        }}
        onMouseEnter={e => {
          if (!isDisabled) {
            e.currentTarget.style.transform = "translateY(-1px)";
            e.currentTarget.style.filter = "brightness(0.95)";
          }
          onMouseEnter?.(e);
        }}
        onMouseLeave={e => {
          e.currentTarget.style.transform = "translateY(0)";
          e.currentTarget.style.filter = "none";
          onMouseLeave?.(e);
        }}
        {...props}
      >
        {loading && <span style={{ width: "14px", height: "14px", border: "2px solid rgba(255,255,255,0.3)", borderTopColor: "currentColor", borderRadius: "50%", animation: "spin 0.6s linear infinite", flexShrink: 0 }} />}
        {children}
      </button>
    );
  }
);

/* ══════════════════════════════════════════
   Card — container padronizado
   ══════════════════════════════════════════ */
export interface CardProps {
  children: React.ReactNode;
  accent?: string;
  onClick?: () => void;
  style?: React.CSSProperties;
}

export function Card({ children, accent, onClick, style }: CardProps) {
  return (
    <div
      onClick={onClick}
      style={{
        background: "white", borderRadius: "14px", padding: "22px",
        boxShadow: "0 1px 3px rgba(0,0,0,0.04), 0 1px 2px rgba(0,0,0,0.06)",
        border: "1px solid #f1f5f9",
        borderLeft: accent ? `4px solid ${accent}` : undefined,
        transition: "all 0.2s cubic-bezier(0.4, 0, 0.2, 1)",
        cursor: onClick ? "pointer" : undefined,
        display: "flex", flexDirection: "column",
        ...style,
      }}
      onMouseEnter={e => {
        e.currentTarget.style.transform = "translateY(-2px)";
        e.currentTarget.style.boxShadow = "0 4px 12px rgba(0,0,0,0.08)";
      }}
      onMouseLeave={e => {
        e.currentTarget.style.transform = "translateY(0)";
        e.currentTarget.style.boxShadow = "0 1px 3px rgba(0,0,0,0.04), 0 1px 2px rgba(0,0,0,0.06)";
      }}
    >
      {children}
    </div>
  );
}

/* ══════════════════════════════════════════
   StatCard — widget de estatística
   ══════════════════════════════════════════ */
export interface StatCardProps {
  icon: React.ReactNode;
  label: string;
  value: string;
  color: string;
}

export function StatCard({ icon, label, value, color }: StatCardProps) {
  return (
    <div style={{
      background: "white", borderRadius: "14px", padding: "16px 18px",
      boxShadow: "0 1px 3px rgba(0,0,0,0.04)", border: "1px solid #f1f5f9",
      display: "flex", flexDirection: "column", gap: "6px",
      borderLeft: `4px solid ${color}`,
    }}>
      <div style={{ display: "flex", alignItems: "center", gap: "8px" }}>
        <span style={{ color, display: "flex" }}>{icon}</span>
        <span style={{ fontSize: "12px", fontWeight: 500, color: "#64748b" }}>{label}</span>
      </div>
      <span style={{ fontSize: "20px", fontWeight: 700, color: "#0f172a" }}>{value}</span>
    </div>
  );
}

/* ══════════════════════════════════════════
   Badge — tag colorida
   ══════════════════════════════════════════ */
export interface BadgeProps {
  children: React.ReactNode;
  color?: string;
  bg?: string;
  style?: React.CSSProperties;
}

export function Badge({ children, color = "#6366f1", bg = "#eef2ff", style }: BadgeProps) {
  return (
    <span style={{
      display: "inline-flex", alignItems: "center", gap: "4px",
      padding: "3px 10px", borderRadius: "9999px",
      fontSize: "11px", fontWeight: 600,
      background: bg, color,
      ...style,
    }}>
      {children}
    </span>
  );
}
