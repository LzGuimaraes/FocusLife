import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { useAuth } from "../auth/AuthProvider";
import { Input } from "../components/Form";

export default function Login() {
  const [email, setEmail] = useState("");
  const [senha, setSenha] = useState("");
  const [error, setError] = useState("");
  const [isLoading, setIsLoading] = useState(false);
  const navigate = useNavigate();
  const { login } = useAuth();

  const handleLogin = async (e: React.FormEvent) => {
    e.preventDefault();
    setError("");
    setIsLoading(true);
    try { await login(email, senha); navigate("/dashboard"); }
    catch (error: any) {
      if (error.response) setError(error.response.data?.message || "E-mail ou senha incorretos");
      else if (error.request) setError("Não foi possível conectar ao servidor.");
      else setError("Ocorreu um erro inesperado.");
    } finally { setIsLoading(false); }
  };

  return (
    <div style={{
      minHeight: "100dvh", display: "flex", alignItems: "center", justifyContent: "center",
      background: "linear-gradient(135deg, #eef2ff 0%, #e0e7ff 50%, #f0f4ff 100%)", padding: "16px",
    }}>
      <div className="animate-scale-in" style={{
        position: "relative", background: "white", borderRadius: "16px",
        padding: "clamp(28px, 5vw, 44px) clamp(20px, 4vw, 40px)",
        boxShadow: "0 20px 25px -5px rgba(0,0,0,0.1)", width: "100%",
        maxWidth: "420px", display: "flex", flexDirection: "column", gap: "clamp(14px, 2vw, 22px)",
      }}>
        <div style={{ height: "4px", background: "linear-gradient(90deg, #6366f1, #8b5cf6, #a855f7)", borderRadius: "16px 16px 0 0", position: "absolute", top: 0, left: 0, right: 0 }} />
        <div style={{ textAlign: "center" }}>
          <span style={{ fontSize: "clamp(28px, 4vw, 36px)" }}>⚡</span>
          <h1 style={{ fontSize: "clamp(22px, 3vw, 26px)", fontWeight: 800, color: "#0f172a", margin: "6px 0 2px", letterSpacing: "-0.5px" }}>FocusLife Hub</h1>
          <p style={{ fontSize: "clamp(12px, 1.5vw, 14px)", color: "#64748b", margin: 0 }}>Entre para gerenciar sua produtividade</p>
        </div>

        {error && (
          <div style={{ display: "flex", alignItems: "center", gap: "8px", padding: "11px 14px", background: "#fef2f2", borderRadius: "10px", fontSize: "13px", color: "#ef4444", fontWeight: 500, border: "1px solid #fecaca" }}>
            ⚠️ {error}
          </div>
        )}

        <form onSubmit={handleLogin} style={{ display: "flex", flexDirection: "column", gap: "14px" }}>
          <Input label="E-mail" type="email" value={email} onChange={e => setEmail(e.target.value)} placeholder="seu@email.com" required disabled={isLoading} />
          <Input label="Senha" type="password" value={senha} onChange={e => setSenha(e.target.value)} placeholder="••••••••" required disabled={isLoading} />
          <button type="submit" disabled={isLoading}
            style={{
              padding: "12px", background: "linear-gradient(135deg, #6366f1, #8b5cf6)", color: "white",
              border: "none", borderRadius: "10px", cursor: isLoading ? "not-allowed" : "pointer",
              fontSize: "15px", fontWeight: 600, transition: "all 0.15s ease", opacity: isLoading ? 0.7 : 1, marginTop: "4px",
            }}
            onMouseEnter={e => { if (!isLoading) { e.currentTarget.style.transform = "translateY(-1px)"; e.currentTarget.style.boxShadow = "0 8px 20px rgba(99,102,241,0.35)"; }}}
            onMouseLeave={e => { e.currentTarget.style.transform = "translateY(0)"; e.currentTarget.style.boxShadow = "none"; }}>
            {isLoading ? "Entrando..." : "Entrar"}
          </button>
        </form>
        <div style={{ textAlign: "center", display: "grid", gap: "10px" }}>
          <button onClick={() => navigate("/auth/forgot-password")} disabled={isLoading} style={{ background: "none", border: "none", color: "#6366f1", cursor: "pointer", fontSize: "13px", fontWeight: 600, padding: 0 }}>Esqueceu a senha?</button>
          <div>
            <span style={{ fontSize: "13px", color: "#64748b" }}>Não tem uma conta? </span>
            <button onClick={() => navigate("/auth/register")} disabled={isLoading} style={{ background: "none", border: "none", color: "#6366f1", cursor: "pointer", fontSize: "13px", fontWeight: 600, padding: 0 }}>Criar conta</button>
          </div>
        </div>
      </div>
    </div>
  );
}
