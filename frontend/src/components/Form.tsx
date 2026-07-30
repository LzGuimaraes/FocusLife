import { useState, useId, type InputHTMLAttributes, type SelectHTMLAttributes, type TextareaHTMLAttributes } from "react";

/* ──────────────────────────────────────────────
   Design Tokens
   ────────────────────────────────────────────── */
const baseInput: React.CSSProperties = {
  width: "100%", padding: "10px 14px", fontSize: "14px",
  fontFamily: "inherit", lineHeight: 1.5,
  border: "1.5px solid #e2e8f0", borderRadius: "10px",
  background: "#f8fafc", color: "#0f172a",
  outline: "none", transition: "all 0.2s ease",
  boxSizing: "border-box",
};

const focusRing = "0 0 0 3px rgba(99,102,241,0.15)";

/* ──────────────────────────────────────────────
   Wrapper (label + field + error)
   ────────────────────────────────────────────── */
function Field({ label, htmlFor, error, children }: {
  label: string; htmlFor: string; error?: string; children: React.ReactNode;
}) {
  return (
    <div>
      <label htmlFor={htmlFor} style={{
        display: "block", fontSize: "13px", fontWeight: 600,
        color: "#374151", marginBottom: "5px",
      }}>
        {label}
      </label>
      {children}
      {error && (
        <p style={{ margin: "4px 0 0", fontSize: "12px", color: "#ef4444", fontWeight: 500, display: "flex", alignItems: "center", gap: "4px" }}>
          ⚠ {error}
        </p>
      )}
    </div>
  );
}

/* ──────────────────────────────────────────────
   Text Input
   ────────────────────────────────────────────── */
interface InputProps extends Omit<InputHTMLAttributes<HTMLInputElement>, "style"> {
  label: string;
  error?: string;
}

export function Input({ label, error, id, ...props }: InputProps) {
  const autoId = useId();
  const inputId = id || autoId;
  const [focused, setFocused] = useState(false);

  return (
    <Field label={label} htmlFor={inputId} error={error}>
      <input
        id={inputId}
        style={{
          ...baseInput,
          borderColor: error ? "#fca5a5" : focused ? "#a5b4fc" : "#e2e8f0",
          boxShadow: focused ? focusRing : "none",
          background: error ? "#fef2f2" : focused ? "#fff" : "#f8fafc",
        }}
        onFocus={e => { setFocused(true); props.onFocus?.(e); }}
        onBlur={e => { setFocused(false); props.onBlur?.(e); }}
        {...props}
      />
    </Field>
  );
}

/* ──────────────────────────────────────────────
   Select
   ────────────────────────────────────────────── */
interface SelectProps extends Omit<SelectHTMLAttributes<HTMLSelectElement>, "style"> {
  label: string;
  error?: string;
  children: React.ReactNode;
}

export function Select({ label, error, id, children, ...props }: SelectProps) {
  const autoId = useId();
  const selectId = id || autoId;
  const [focused, setFocused] = useState(false);

  return (
    <Field label={label} htmlFor={selectId} error={error}>
      <select
        id={selectId}
        style={{
          ...baseInput,
          cursor: "pointer", appearance: "none",
          backgroundImage: `url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='12' height='12' viewBox='0 0 12 12'%3E%3Cpath fill='%236b7280' d='M6 8L1.5 3h9z'/%3E%3C/svg%3E")`,
          backgroundRepeat: "no-repeat",
          backgroundPosition: "right 14px center",
          paddingRight: "38px",
          borderColor: error ? "#fca5a5" : focused ? "#a5b4fc" : "#e2e8f0",
          boxShadow: focused ? focusRing : "none",
          background: error ? "#fef2f2" : focused ? "#fff" : "#f8fafc",
        }}
        onFocus={e => { setFocused(true); props.onFocus?.(e); }}
        onBlur={e => { setFocused(false); props.onBlur?.(e); }}
        {...props}
      >
        {children}
      </select>
    </Field>
  );
}

/* ──────────────────────────────────────────────
   Date Input
   ────────────────────────────────────────────── */
interface DateInputProps extends Omit<InputHTMLAttributes<HTMLInputElement>, "style" | "type"> {
  label: string;
  error?: string;
}

export function DateInput({ label, error, id, ...props }: DateInputProps) {
  const autoId = useId();
  const inputId = id || autoId;
  const [focused, setFocused] = useState(false);

  return (
    <Field label={label} htmlFor={inputId} error={error}>
      <input
        id={inputId}
        type="date"
        style={{
          ...baseInput,
          cursor: "pointer", colorScheme: "light",
          borderColor: error ? "#fca5a5" : focused ? "#a5b4fc" : "#e2e8f0",
          boxShadow: focused ? focusRing : "none",
          background: error ? "#fef2f2" : focused ? "#fff" : "#f8fafc",
        }}
        onFocus={e => { setFocused(true); props.onFocus?.(e); }}
        onBlur={e => { setFocused(false); props.onBlur?.(e); }}
        {...props}
      />
    </Field>
  );
}

/* ──────────────────────────────────────────────
   Number Input (text-based with mask)
   ────────────────────────────────────────────── */
interface NumberInputProps extends Omit<InputHTMLAttributes<HTMLInputElement>, "style" | "type" | "onChange"> {
  label: string;
  error?: string;
  decimal?: boolean;
  highPrecision?: boolean;
  onChange: (value: string) => void;
}

export function NumberInput({ label, error, id, decimal = false, highPrecision = false, value, onChange, ...props }: NumberInputProps) {
  const autoId = useId();
  const inputId = id || autoId;
  const [focused, setFocused] = useState(false);

  const filter = (raw: string) => {
    if (decimal || highPrecision) {
      let clean = raw.replace(/[^0-9.]/g, "");
      const parts = clean.split(".");
      if (parts.length > 2) clean = parts[0] + "." + parts.slice(1).join("");
      if (parts.length === 2) {
        const maxDecimals = highPrecision ? 8 : 2;
        if (parts[1].length > maxDecimals) clean = parts[0] + "." + parts[1].slice(0, maxDecimals);
      }
      return clean;
    }
    return raw.replace(/[^0-9]/g, "");
  };

  return (
    <Field label={label} htmlFor={inputId} error={error}>
      <input
        id={inputId}
        type="text"
        inputMode={decimal ? "decimal" : "numeric"}
        value={value}
        onChange={e => onChange(filter(e.target.value))}
        style={{
          ...baseInput,
          borderColor: error ? "#fca5a5" : focused ? "#a5b4fc" : "#e2e8f0",
          boxShadow: focused ? focusRing : "none",
          background: error ? "#fef2f2" : focused ? "#fff" : "#f8fafc",
        }}
        onFocus={e => { setFocused(true); props.onFocus?.(e); }}
        onBlur={e => { setFocused(false); props.onBlur?.(e); }}
        {...props}
      />
    </Field>
  );
}

/* ──────────────────────────────────────────────
   TextArea
   ────────────────────────────────────────────── */
interface TextAreaProps extends Omit<TextareaHTMLAttributes<HTMLTextAreaElement>, "style"> {
  label: string;
  error?: string;
}

export function TextArea({ label, error, id, ...props }: TextAreaProps) {
  const autoId = useId();
  const areaId = id || autoId;
  const [focused, setFocused] = useState(false);

  return (
    <Field label={label} htmlFor={areaId} error={error}>
      <textarea
        id={areaId}
        style={{
          ...baseInput,
          minHeight: "80px", resize: "vertical",
          borderColor: error ? "#fca5a5" : focused ? "#a5b4fc" : "#e2e8f0",
          boxShadow: focused ? focusRing : "none",
          background: error ? "#fef2f2" : focused ? "#fff" : "#f8fafc",
        }}
        onFocus={e => { setFocused(true); props.onFocus?.(e); }}
        onBlur={e => { setFocused(false); props.onBlur?.(e); }}
        {...props}
      />
    </Field>
  );
}
