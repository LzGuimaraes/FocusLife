import { useState, useId, forwardRef, type ComponentPropsWithoutRef } from "react";

/* ══════════════════════════════════════════
   Design Tokens
   ══════════════════════════════════════════ */
const RADIUS = "10px";
const TRANSITION = "all 0.2s cubic-bezier(0.4, 0, 0.2, 1)";

const tokens = {
  bg: { default: "#f8fafc", hover: "#fff", focus: "#fff", error: "#fef2f2", success: "#f0fdf4", disabled: "#f1f5f9" },
  border: { default: "#e2e8f0", hover: "#cbd5e1", focus: "#a5b4fc", error: "#fca5a5", success: "#86efac", disabled: "#e2e8f0" },
  text: { default: "#0f172a", error: "#991b1b", success: "#166534", disabled: "#94a3b8" },
  ring: "0 0 0 3px rgba(99,102,241,0.18)",
};

const baseInput: React.CSSProperties = {
  width: "100%", height: "40px", padding: "10px 14px",
  fontSize: "14px", fontFamily: "inherit", lineHeight: 1.5,
  borderRadius: RADIUS, border: "1.5px solid #e2e8f0",
  background: "#f8fafc", color: "#0f172a",
  outline: "none", boxSizing: "border-box",
  transition: TRANSITION,
};

/* ══════════════════════════════════════════
   Field Wrapper
   ══════════════════════════════════════════ */
interface FieldProps { label: string; htmlFor: string; required?: boolean; hint?: string; error?: string; success?: string; errorId?: string; hintId?: string; children: React.ReactNode; }

function Field({ label, htmlFor, required, hint, error, success, errorId, hintId, children }: FieldProps) {
  const isError = !!error;
  return (
    <div>
      <label htmlFor={htmlFor} style={{ display: "flex", alignItems: "center", gap: "4px", fontSize: "13px", fontWeight: 600, color: "#374151", marginBottom: "5px" }}>
        {label}{required && <span style={{ color: "#ef4444", fontSize: "14px" }} aria-hidden="true">*</span>}
      </label>
      {children}
      {hint && !isError && !success && <p id={hintId} style={{ margin: "4px 0 0", fontSize: "12px", color: "#94a3b8" }}>{hint}</p>}
      {isError && <p id={errorId} role="alert" style={{ margin: "4px 0 0", fontSize: "12px", color: "#ef4444", fontWeight: 500, display: "flex", alignItems: "center", gap: "4px" }}>⚠ {error}</p>}
      {!isError && success && <p style={{ margin: "4px 0 0", fontSize: "12px", color: "#166534", fontWeight: 500 }}>✓ {success}</p>}
    </div>
  );
}

/* ══════════════════════════════════════════
   Shared Style Hook
   ══════════════════════════════════════════ */
interface FieldState { focused: boolean; hovered: boolean; isError: boolean; isSuccess: boolean; isDisabled: boolean; }
function useFieldState() { const [focused, setFocused] = useState(false); const [hovered, setHovered] = useState(false); return { focused, hovered, setFocused, setHovered }; }

function fieldCss(s: FieldState): React.CSSProperties {
  return {
    ...baseInput,
    borderColor: s.isError ? tokens.border.error : s.isSuccess ? tokens.border.success : s.focused ? tokens.border.focus : s.hovered && !s.isDisabled ? tokens.border.hover : s.isDisabled ? tokens.border.disabled : tokens.border.default,
    boxShadow: s.focused && !s.isError ? tokens.ring : "none",
    background: s.isError ? tokens.bg.error : s.isSuccess ? tokens.bg.success : s.isDisabled ? tokens.bg.disabled : s.focused ? tokens.bg.focus : s.hovered ? tokens.bg.hover : tokens.bg.default,
    color: s.isError ? tokens.text.error : s.isSuccess ? tokens.text.success : s.isDisabled ? tokens.text.disabled : tokens.text.default,
    cursor: s.isDisabled ? "not-allowed" : undefined,
    opacity: s.isDisabled ? 0.6 : undefined,
  };
}

/* ══════════════════════════════════════════
   Input
   ══════════════════════════════════════════ */
export interface InputProps extends Omit<ComponentPropsWithoutRef<"input">, "size"> { label: string; error?: string; success?: string; hint?: string; required?: boolean; loading?: boolean; }

export const Input = forwardRef<HTMLInputElement, InputProps>(
  function Input({ label, error, success, hint, required, loading, disabled, readOnly, id, onFocus, onBlur, onMouseEnter, onMouseLeave, ...props }, ref) {
    const autoId = useId(); const inputId = id || autoId; const errId = `${inputId}-err`; const hId = `${inputId}-hint`;
    const { focused, hovered, setFocused, setHovered } = useFieldState();
    const isErr = !!error; const isOk = !!success && !isErr; const isDis = !!(disabled || loading);
    return (
      <Field label={label} htmlFor={inputId} required={required} hint={hint} error={error} success={success} errorId={errId} hintId={hId}>
        <div style={{ position: "relative" }}>
          <input ref={ref} id={inputId} disabled={isDis} readOnly={readOnly} aria-invalid={isErr} aria-describedby={isErr ? errId : hint ? hId : undefined} aria-required={required}
            style={fieldCss({ focused, hovered, isError: isErr, isSuccess: isOk, isDisabled: isDis })}
            onFocus={e => { setFocused(true); onFocus?.(e); }} onBlur={e => { setFocused(false); onBlur?.(e); }}
            onMouseEnter={e => { setHovered(true); onMouseEnter?.(e); }} onMouseLeave={e => { setHovered(false); onMouseLeave?.(e); }}
            {...props} />
          {loading && <span style={{ position: "absolute", right: "12px", top: "50%", marginTop: "-8px", width: "16px", height: "16px", border: "2px solid #e2e8f0", borderTopColor: "#6366f1", borderRadius: "50%", animation: "spin 0.6s linear infinite" }} />}
        </div>
      </Field>
    );
  }
);

/* ══════════════════════════════════════════
   Select
   ══════════════════════════════════════════ */
export interface SelectProps extends Omit<ComponentPropsWithoutRef<"select">, "size"> { label: string; error?: string; success?: string; hint?: string; required?: boolean; loading?: boolean; }

export const Select = forwardRef<HTMLSelectElement, SelectProps>(
  function Select({ label, error, success, hint, required, loading, disabled, id, children, onFocus, onBlur, onMouseEnter, onMouseLeave, ...props }, ref) {
    const autoId = useId(); const selId = id || autoId; const errId = `${selId}-err`; const hId = `${selId}-hint`;
    const { focused, hovered, setFocused, setHovered } = useFieldState();
    const isErr = !!error; const isOk = !!success && !isErr; const isDis = !!(disabled || loading);
    return (
      <Field label={label} htmlFor={selId} required={required} hint={hint} error={error} success={success} errorId={errId} hintId={hId}>
        <select ref={ref} id={selId} disabled={isDis} aria-invalid={isErr} aria-describedby={isErr ? errId : hint ? hId : undefined} aria-required={required}
          style={{ ...fieldCss({ focused, hovered, isError: isErr, isSuccess: isOk, isDisabled: isDis }), appearance: "none", backgroundImage: `url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='12' height='12' viewBox='0 0 12 12'%3E%3Cpath fill='%236b7280' d='M6 8L1.5 3h9z'/%3E%3C/svg%3E")`, backgroundRepeat: "no-repeat", backgroundPosition: "right 14px center", paddingRight: "38px" }}
          onFocus={e => { setFocused(true); onFocus?.(e); }} onBlur={e => { setFocused(false); onBlur?.(e); }}
          onMouseEnter={e => { setHovered(true); onMouseEnter?.(e); }} onMouseLeave={e => { setHovered(false); onMouseLeave?.(e); }}
          {...props}>{children}</select>
      </Field>
    );
  }
);

/* ══════════════════════════════════════════
   DateInput
   ══════════════════════════════════════════ */
export interface DateInputProps extends Omit<ComponentPropsWithoutRef<"input">, "type" | "size"> { label: string; error?: string; success?: string; hint?: string; required?: boolean; }

export const DateInput = forwardRef<HTMLInputElement, DateInputProps>(
  function DateInput({ label, error, success, hint, required, disabled, id, onFocus, onBlur, onMouseEnter, onMouseLeave, ...props }, ref) {
    const autoId = useId(); const inputId = id || autoId; const errId = `${inputId}-err`; const hId = `${inputId}-hint`;
    const { focused, hovered, setFocused, setHovered } = useFieldState();
    const isErr = !!error; const isOk = !!success && !isErr; const isDis = !!disabled;
    return (
      <Field label={label} htmlFor={inputId} required={required} hint={hint} error={error} success={success} errorId={errId} hintId={hId}>
        <input ref={ref} id={inputId} type="date" disabled={isDis} aria-invalid={isErr} aria-describedby={isErr ? errId : hint ? hId : undefined} aria-required={required}
          style={{ ...fieldCss({ focused, hovered, isError: isErr, isSuccess: isOk, isDisabled: isDis }), colorScheme: "light" }}
          onFocus={e => { setFocused(true); onFocus?.(e); }} onBlur={e => { setFocused(false); onBlur?.(e); }}
          onMouseEnter={e => { setHovered(true); onMouseEnter?.(e); }} onMouseLeave={e => { setHovered(false); onMouseLeave?.(e); }}
          {...props} />
      </Field>
    );
  }
);

/* ══════════════════════════════════════════
   NumberInput
   ══════════════════════════════════════════ */
export interface NumberInputProps extends Omit<ComponentPropsWithoutRef<"input">, "type" | "size" | "onChange" | "value"> { label: string; error?: string; success?: string; hint?: string; required?: boolean; decimal?: boolean; highPrecision?: boolean; value: string; onChange: (value: string) => void; }

export const NumberInput = forwardRef<HTMLInputElement, NumberInputProps>(
  function NumberInput({ label, error, success, hint, required, decimal = false, highPrecision = false, disabled, readOnly, id, value, onChange, onFocus, onBlur, onMouseEnter, onMouseLeave, ...props }, ref) {
    const autoId = useId(); const inputId = id || autoId; const errId = `${inputId}-err`; const hId = `${inputId}-hint`;
    const { focused, hovered, setFocused, setHovered } = useFieldState();
    const isErr = !!error; const isOk = !!success && !isErr; const isDis = !!disabled;
    const filter = (raw: string): string => {
      if (decimal || highPrecision) { let clean = raw.replace(/[^0-9.]/g, ""); const parts = clean.split("."); if (parts.length > 2) clean = parts[0] + "." + parts.slice(1).join(""); if (parts.length === 2) { const max = highPrecision ? 8 : 2; if (parts[1].length > max) clean = parts[0] + "." + parts[1].slice(0, max); } return clean; }
      return raw.replace(/[^0-9]/g, "");
    };
    return (
      <Field label={label} htmlFor={inputId} required={required} hint={hint} error={error} success={success} errorId={errId} hintId={hId}>
        <input ref={ref} id={inputId} type="text" inputMode={decimal || highPrecision ? "decimal" : "numeric"} value={value} onChange={e => onChange(filter(e.target.value))} disabled={isDis} readOnly={readOnly} aria-invalid={isErr} aria-describedby={isErr ? errId : hint ? hId : undefined} aria-required={required}
          style={fieldCss({ focused, hovered, isError: isErr, isSuccess: isOk, isDisabled: isDis })}
          onFocus={e => { setFocused(true); onFocus?.(e); }} onBlur={e => { setFocused(false); onBlur?.(e); }}
          onMouseEnter={e => { setHovered(true); onMouseEnter?.(e); }} onMouseLeave={e => { setHovered(false); onMouseLeave?.(e); }}
          {...props} />
      </Field>
    );
  }
);

/* ══════════════════════════════════════════
   TextArea
   ══════════════════════════════════════════ */
export interface TextAreaProps extends Omit<ComponentPropsWithoutRef<"textarea">, "size"> { label: string; error?: string; success?: string; hint?: string; required?: boolean; }

export const TextArea = forwardRef<HTMLTextAreaElement, TextAreaProps>(
  function TextArea({ label, error, success, hint, required, disabled, readOnly, id, onFocus, onBlur, onMouseEnter, onMouseLeave, ...props }, ref) {
    const autoId = useId(); const areaId = id || autoId; const errId = `${areaId}-err`; const hId = `${areaId}-hint`;
    const { focused, hovered, setFocused, setHovered } = useFieldState();
    const isErr = !!error; const isOk = !!success && !isErr; const isDis = !!disabled;
    return (
      <Field label={label} htmlFor={areaId} required={required} hint={hint} error={error} success={success} errorId={errId} hintId={hId}>
        <textarea ref={ref} id={areaId} disabled={isDis} readOnly={readOnly} aria-invalid={isErr} aria-describedby={isErr ? errId : hint ? hId : undefined} aria-required={required}
          style={{ ...fieldCss({ focused, hovered, isError: isErr, isSuccess: isOk, isDisabled: isDis }), height: "auto", minHeight: "80px", resize: "vertical" }}
          onFocus={e => { setFocused(true); onFocus?.(e); }} onBlur={e => { setFocused(false); onBlur?.(e); }}
          onMouseEnter={e => { setHovered(true); onMouseEnter?.(e); }} onMouseLeave={e => { setHovered(false); onMouseLeave?.(e); }}
          {...props} />
      </Field>
    );
  }
);

/* ══════════════════════════════════════════
   Checkbox
   ══════════════════════════════════════════ */
export interface CheckboxProps extends Omit<ComponentPropsWithoutRef<"input">, "type" | "size"> { label: string; description?: string; }

export const Checkbox = forwardRef<HTMLInputElement, CheckboxProps>(
  function Checkbox({ label, description, disabled, id, ...props }, ref) {
    const autoId = useId(); const cbId = id || autoId;
    return (
      <div style={{ display: "flex", alignItems: "flex-start", gap: "10px", padding: "10px 14px", background: disabled ? "#f1f5f9" : "#f8fafc", borderRadius: RADIUS, border: `1.5px solid ${tokens.border.default}`, transition: TRANSITION }}>
        <input ref={ref} id={cbId} type="checkbox" disabled={disabled} style={{ width: "18px", height: "18px", marginTop: "1px", accentColor: "#6366f1", cursor: disabled ? "not-allowed" : "pointer", flexShrink: 0 }} {...props} />
        <div>
          <label htmlFor={cbId} style={{ fontSize: "14px", fontWeight: 600, color: disabled ? tokens.text.disabled : "#374151", cursor: disabled ? "not-allowed" : "pointer", userSelect: "none" }}>{label}</label>
          {description && <p style={{ margin: "2px 0 0", fontSize: "12px", color: "#94a3b8" }}>{description}</p>}
        </div>
      </div>
    );
  }
);

/* ══════════════════════════════════════════
   Switch
   ══════════════════════════════════════════ */
export interface SwitchProps { label: string; checked: boolean; onChange: (checked: boolean) => void; disabled?: boolean; id?: string; }

export const Switch = forwardRef<HTMLButtonElement, SwitchProps>(
  function Switch({ label, checked, onChange, disabled, id }, ref) {
    const autoId = useId(); const swId = id || autoId;
    return (
      <div style={{ display: "flex", alignItems: "center", gap: "10px" }}>
        <button ref={ref} id={swId} type="button" role="switch" aria-checked={checked} disabled={disabled} onClick={() => !disabled && onChange(!checked)}
          style={{ width: "44px", height: "24px", borderRadius: "12px", border: "none", cursor: disabled ? "not-allowed" : "pointer", background: checked ? "#6366f1" : "#cbd5e1", position: "relative", transition: "background 0.2s ease", opacity: disabled ? 0.5 : 1 }}>
          <span style={{ position: "absolute", top: "2px", left: checked ? "22px" : "2px", width: "20px", height: "20px", borderRadius: "50%", background: "white", boxShadow: "0 1px 3px rgba(0,0,0,0.2)", transition: "left 0.2s ease" }} />
        </button>
        <label htmlFor={swId} style={{ fontSize: "14px", fontWeight: 500, color: disabled ? tokens.text.disabled : "#374151", cursor: disabled ? "not-allowed" : "pointer", userSelect: "none" }}>{label}</label>
      </div>
    );
  }
);

/* ══════════════════════════════════════════
   SearchInput
   ══════════════════════════════════════════ */
export interface SearchInputProps extends Omit<ComponentPropsWithoutRef<"input">, "size"> { label: string; onSearch?: (value: string) => void; }

export const SearchInput = forwardRef<HTMLInputElement, SearchInputProps>(
  function SearchInput({ label, onSearch, id, onKeyDown, ...props }, ref) {
    const autoId = useId(); const inputId = id || autoId;
    return (
      <div style={{ position: "relative" }}>
        <label htmlFor={inputId} style={{ position: "absolute", width: 1, height: 1, overflow: "hidden" }}>{label}</label>
        <svg style={{ position: "absolute", left: "14px", top: "50%", transform: "translateY(-50%)", color: "#94a3b8", pointerEvents: "none" }} width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round"><circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/></svg>
        <input ref={ref} id={inputId} type="search" style={{ ...baseInput, paddingLeft: "40px" }} aria-label={label}
          onKeyDown={e => { if (e.key === "Enter") onSearch?.(e.currentTarget.value); onKeyDown?.(e); }} {...props} />
      </div>
    );
  }
);
