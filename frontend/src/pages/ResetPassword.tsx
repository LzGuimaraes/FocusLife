import { useState } from "react";
import { useSearchParams, useNavigate } from "react-router-dom";
import api from "../api/api";
import { Input } from "../components/Form";

export default function ResetPassword() {
  const [searchParams] = useSearchParams();
  const token = searchParams.get("token") || "";
  const [password, setPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError("");
    setSuccess("");

    if (password !== confirmPassword) {
      setError("As senhas não coincidem.");
      return;
    }

    setLoading(true);
    try {
      const response = await api.post("/auth/reset-password", { token, password });
      setSuccess(response.data?.message || "Senha redefinida com sucesso.");
    } catch (err: any) {
      setError(err.response?.data?.message || "Erro ao redefinir a senha.");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div style={{ minHeight: "100dvh", display: "flex", alignItems: "center", justifyContent: "center", background: "linear-gradient(135deg, #f8fafc 0%, #e2e8f0 50%, #eff6ff 100%)", padding: "16px" }}>
      <div className="animate-scale-in" style={{ position: "relative", background: "white", borderRadius: "16px", padding: "clamp(28px, 5vw, 44px) clamp(20px, 4vw, 40px)", boxShadow: "0 20px 25px -5px rgba(0,0,0,0.1)", width: "100%", maxWidth: "440px", display: "flex", flexDirection: "column", gap: "clamp(14px, 2vw, 22px)" }}>
        <div style={{ height: "4px", background: "linear-gradient(90deg, #10b981, #34d399)", borderRadius: "16px 16px 0 0", position: "absolute", top: 0, left: 0, right: 0 }} />
        <div style={{ textAlign: "center" }}>
          <h1 style={{ fontSize: "clamp(22px, 3vw, 26px)", fontWeight: 800, color: "#0f172a", margin: "6px 0 2px", letterSpacing: "-0.5px" }}>Redefinir senha</h1>
          <p style={{ fontSize: "clamp(12px, 1.5vw, 14px)", color: "#64748b", margin: 0 }}>Defina uma nova senha para sua conta.</p>
        </div>

        {error && <div style={{ display: "flex", alignItems: "center", gap: "8px", padding: "11px 14px", background: "#fef2f2", borderRadius: "10px", fontSize: "13px", color: "#ef4444", fontWeight: 500, border: "1px solid #fecaca" }}>⚠️ {error}</div>}
        {success && <div style={{ display: "flex", alignItems: "center", gap: "8px", padding: "11px 14px", background: "#ecfdf5", borderRadius: "10px", fontSize: "13px", color: "#166534", fontWeight: 500, border: "1px solid #bbf7d0" }}>✅ {success}</div>}

        <form onSubmit={handleSubmit} style={{ display: "flex", flexDirection: "column", gap: "14px" }}>
          <Input label="Nova senha" type="password" value={password} onChange={e => setPassword(e.target.value)} placeholder="••••••••" required disabled={loading} />
          <Input label="Confirmar senha" type="password" value={confirmPassword} onChange={e => setConfirmPassword(e.target.value)} placeholder="••••••••" required disabled={loading} />
          <button type="submit" disabled={loading} style={{ padding: "12px", background: "linear-gradient(135deg, #10b981, #34d399)", color: "white", border: "none", borderRadius: "10px", cursor: loading ? "not-allowed" : "pointer", fontSize: "15px", fontWeight: 600, opacity: loading ? 0.7 : 1, marginTop: "4px", transition: "all 0.15s ease" }}>{loading ? "Carregando..." : "Redefinir senha"}</button>
        </form>

        <button onClick={() => navigate("/auth/login")} disabled={loading} style={{ background: "none", border: "none", color: "#6366f1", cursor: "pointer", fontSize: "13px", fontWeight: 600, padding: 0 }}>Voltar ao login</button>
      </div>
    </div>
  );
}
