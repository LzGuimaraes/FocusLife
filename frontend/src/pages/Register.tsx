import { useState } from "react";
import api from "../api/api";
import { useNavigate } from "react-router-dom";
import { Input } from "../components/Form";

export default function Register() {
  const [nome, setNome] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();

  const handleRegister = async (e: React.FormEvent) => {
    e.preventDefault();
    setError("");
    if (password !== confirmPassword) { setError("As senhas não coincidem."); return; }
    if (password.length < 8) { setError("A senha deve ter pelo menos 8 caracteres."); return; }
    setLoading(true);
    try {
      await api.post("/auth/register", { name: nome, email, password }, { withCredentials: true });
      navigate("/auth/login", { state: { registered: true } });
    } catch (error: any) { setError(error.response?.data?.message || error.response?.data || "Erro ao registrar."); }
    finally { setLoading(false); }
  };

  return (
    <div style={{ minHeight: "100dvh", display: "flex", alignItems: "center", justifyContent: "center", background: "linear-gradient(135deg, #ecfdf5 0%, #d1fae5 50%, #eef2ff 100%)", padding: "16px" }}>
      <div className="animate-scale-in" style={{ position: "relative", background: "white", borderRadius: "16px", padding: "clamp(28px, 5vw, 44px) clamp(20px, 4vw, 40px)", boxShadow: "0 20px 25px -5px rgba(0,0,0,0.1)", width: "100%", maxWidth: "440px", display: "flex", flexDirection: "column", gap: "clamp(14px, 2vw, 22px)" }}>
        <div style={{ height: "4px", background: "linear-gradient(90deg, #10b981, #34d399, #6ee7b7)", borderRadius: "16px 16px 0 0", position: "absolute", top: 0, left: 0, right: 0 }} />
        <div style={{ textAlign: "center" }}><span style={{ fontSize: "clamp(28px, 4vw, 36px)" }}>🚀</span><h1 style={{ fontSize: "clamp(22px, 3vw, 26px)", fontWeight: 800, color: "#0f172a", margin: "6px 0 2px", letterSpacing: "-0.5px" }}>Criar Conta</h1><p style={{ fontSize: "clamp(12px, 1.5vw, 14px)", color: "#64748b", margin: 0 }}>Comece sua jornada de produtividade</p></div>
        {error && <div style={{ display: "flex", alignItems: "center", gap: "8px", padding: "11px 14px", background: "#fef2f2", borderRadius: "10px", fontSize: "13px", color: "#ef4444", fontWeight: 500, border: "1px solid #fecaca" }}>⚠️ {error}</div>}
        <form onSubmit={handleRegister} style={{ display: "flex", flexDirection: "column", gap: "14px" }}>
          <Input label="Nome" type="text" value={nome} onChange={e => setNome(e.target.value)} placeholder="Seu nome completo" required disabled={loading} />
          <Input label="E-mail" type="email" value={email} onChange={e => setEmail(e.target.value)} placeholder="seu@email.com" required disabled={loading} />
          <Input label="Senha" type="password" value={password} onChange={e => setPassword(e.target.value)} placeholder="Mínimo 8 caracteres" required disabled={loading} />
          <Input label="Confirmar Senha" type="password" value={confirmPassword} onChange={e => setConfirmPassword(e.target.value)} placeholder="Repita a senha" required disabled={loading} />
          <button type="submit" disabled={loading} style={{ padding: "12px", background: "linear-gradient(135deg, #10b981, #34d399)", color: "white", border: "none", borderRadius: "10px", cursor: loading ? "not-allowed" : "pointer", fontSize: "15px", fontWeight: 600, opacity: loading ? 0.7 : 1, marginTop: "4px", transition: "all 0.15s ease" }}
            onMouseEnter={e => { if (!loading) { e.currentTarget.style.transform = "translateY(-1px)"; e.currentTarget.style.boxShadow = "0 8px 20px rgba(16,185,129,0.35)"; }}}
            onMouseLeave={e => { e.currentTarget.style.transform = "translateY(0)"; e.currentTarget.style.boxShadow = "none"; }}>{loading ? "Criando..." : "Criar Conta"}</button>
        </form>
        <div style={{ textAlign: "center" }}><span style={{ fontSize: "13px", color: "#64748b" }}>Já tem uma conta? </span><button onClick={() => navigate("/auth/login")} disabled={loading} style={{ background: "none", border: "none", color: "#6366f1", cursor: "pointer", fontSize: "13px", fontWeight: 600, padding: 0 }}>Fazer login</button></div>
      </div>
    </div>
  );
}
