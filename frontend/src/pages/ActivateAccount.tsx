import { useEffect, useState } from "react";
import { useSearchParams, useNavigate } from "react-router-dom";
import api from "../api/api";

export default function ActivateAccount() {
  const [searchParams] = useSearchParams();
  const code = searchParams.get("code") || "";
  const [message, setMessage] = useState("Ativando conta...");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(true);
  const navigate = useNavigate();

  useEffect(() => {
    if (!code) {
      setError("Código de ativação não encontrado.");
      setLoading(false);
      return;
    }

    api.get(`/auth/activate?code=${encodeURIComponent(code)}`)
      .then((response) => {
        setMessage(response.data?.message || "Conta ativada com sucesso.");
        setTimeout(() => navigate("/auth/login"), 3000);
      })
      .catch((err) => {
        setError(err.response?.data?.message || "Erro ao ativar conta.");
      })
      .finally(() => setLoading(false));
  }, [code, navigate]);

  return (
    <div style={{ minHeight: "100dvh", display: "flex", alignItems: "center", justifyContent: "center", background: "linear-gradient(135deg, #f8fafc 0%, #e2e8f0 50%, #eff6ff 100%)", padding: "16px" }}>
      <div className="animate-scale-in" style={{ position: "relative", background: "white", borderRadius: "16px", padding: "clamp(28px, 5vw, 44px) clamp(20px, 4vw, 40px)", boxShadow: "0 20px 25px -5px rgba(0,0,0,0.1)", width: "100%", maxWidth: "440px", display: "flex", flexDirection: "column", gap: "clamp(14px, 2vw, 22px)" }}>
        <div style={{ height: "4px", background: "linear-gradient(90deg, #6366f1, #8b5cf6)", borderRadius: "16px 16px 0 0", position: "absolute", top: 0, left: 0, right: 0 }} />
        <div style={{ textAlign: "center" }}>
          <h1 style={{ fontSize: "clamp(22px, 3vw, 26px)", fontWeight: 800, color: "#0f172a", margin: "6px 0 2px", letterSpacing: "-0.5px" }}>Ativação de conta</h1>
          <p style={{ fontSize: "clamp(12px, 1.5vw, 14px)", color: "#64748b", margin: 0 }}>Aguarde enquanto ativamos sua conta.</p>
        </div>

        {loading && <div style={{ textAlign: "center", padding: "14px", color: "#475569" }}>Processando...</div>}
        {error && <div style={{ padding: "14px", background: "#fef2f2", borderRadius: "10px", color: "#b91c1c", border: "1px solid #fecaca" }}>⚠️ {error}</div>}
        {!loading && !error && <div style={{ padding: "14px", background: "#ecfdf5", borderRadius: "10px", color: "#166534", border: "1px solid #bbf7d0" }}>✅ {message}</div>}

        <button onClick={() => navigate("/auth/login")} disabled={loading} style={{ padding: "12px", background: "linear-gradient(135deg, #6366f1, #8b5cf6)", color: "white", border: "none", borderRadius: "10px", cursor: loading ? "not-allowed" : "pointer", fontSize: "15px", fontWeight: 600, marginTop: "4px" }}>Ir para login</button>
      </div>
    </div>
  );
}
