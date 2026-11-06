import { useAuth } from "../auth/AuthProvider";
import { useNavigate } from "react-router-dom";
import { ResumoJornadas } from "./Jornadas";
import Layout from "../components/Layout";
import ResumoCarteiraIdealCard from "../components/ResumoCarteiraIdealCard";

const cards = [
  { path: "/jornadas", icon: "🧭", title: "Jornadas", desc: "Planeje atividades, registre avanços e acompanhe seu ritmo", color: "#06b6d4", bg: "#ecfeff", stat: "Progresso e previsão" },
  { path: "/financas", icon: "💰", title: "Finanças", desc: "Controle suas carteiras e contas", color: "#10b981", bg: "#ecfdf5", stat: "Carteiras" },
  { path: "/planejamento", icon: "📐", title: "Planejamento", desc: "Carteira ideal, avaliações e prioridade de aportes", color: "#6366f1", bg: "#eef2ff", stat: "Metodologia" },
  { path: "/como-funciona", icon: "❓", title: "Como Funciona", desc: "Aprenda a usar todas as funcionalidades", color: "#8b5cf6", bg: "#f5f3ff", stat: "Guia" },
  { path: "/suporte", icon: "💬", title: "Suporte", desc: "Solicite músicas, ativos e melhorias ou tire suas dúvidas", color: "#6366f1", bg: "#eef2ff", stat: "Chamados" },
];

export default function Dashboard() {
  const { user } = useAuth();
  const navigate = useNavigate();

  return (
    <Layout>
      <div className="animate-fade-in">
        {/* Welcome Banner */}
        <div style={{
          background: "linear-gradient(135deg, #6366f1 0%, #8b5cf6 50%, #a855f7 100%)",
          borderRadius: "14px", padding: "clamp(20px, 4vw, 36px)", marginBottom: "clamp(16px, 3vw, 28px)",
          color: "white", position: "relative", overflow: "hidden",
        }}>
          <div style={{ position: "absolute", top: -30, right: -30, width: 160, height: 160, borderRadius: "50%", background: "rgba(255,255,255,0.08)", pointerEvents: "none" }} />
          <div style={{ position: "absolute", bottom: -40, right: 80, width: 100, height: 100, borderRadius: "50%", background: "rgba(255,255,255,0.06)", pointerEvents: "none" }} />
          <div style={{ position: "relative", zIndex: 1 }}>
            <h1 style={{ fontSize: "clamp(20px, 3.5vw, 28px)", fontWeight: 800, marginBottom: "4px", letterSpacing: "-0.5px" }}>
              Olá, {user?.nome?.split(" ")[0] || "Usuário"}! 👋
            </h1>
            <p style={{ fontSize: "clamp(13px, 1.5vw, 15px)", opacity: 0.85, margin: 0 }}>Pronto para um dia produtivo? Escolha uma área abaixo para começar.</p>
          </div>
        </div>

        <ResumoJornadas />

        {/* Carteira Atual × Ideal (planejamento) */}
        <ResumoCarteiraIdealCard />

        {/* Navigation Cards */}
        <h2 style={{ fontSize: "clamp(16px, 2vw, 20px)", fontWeight: 700, color: "#0f172a", marginBottom: "clamp(12px, 2vw, 20px)" }}>Acesso Rápido</h2>
        <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fill, minmax(min(100%, 280px), 1fr))", gap: "clamp(12px, 2vw, 20px)" }}>
          {cards.map((c) => (
            <div key={c.path} onClick={() => navigate(c.path)}
              style={{
                background: "white", borderRadius: "12px", padding: "clamp(18px, 3vw, 28px) clamp(16px, 2.5vw, 24px)",
                boxShadow: "0 1px 2px rgba(0,0,0,0.05)", cursor: "pointer",
                borderTop: `4px solid ${c.color}`,
                transition: "all 0.25s ease", position: "relative", overflow: "hidden",
              }}
              onMouseEnter={e => { e.currentTarget.style.transform = "translateY(-4px)"; e.currentTarget.style.boxShadow = "0 10px 25px rgba(0,0,0,0.1)"; }}
              onMouseLeave={e => { e.currentTarget.style.transform = "translateY(0)"; e.currentTarget.style.boxShadow = "0 1px 2px rgba(0,0,0,0.05)"; }}
            >
              <div style={{ width: "clamp(40px, 5vw, 52px)", height: "clamp(40px, 5vw, 52px)", borderRadius: "10px", background: c.bg, display: "flex", alignItems: "center", justifyContent: "center", marginBottom: "clamp(10px, 2vw, 16px)", fontSize: "clamp(20px, 3vw, 26px)" }}>
                {c.icon}
              </div>
              <h3 style={{ fontSize: "clamp(15px, 1.8vw, 18px)", fontWeight: 700, color: "#0f172a", marginBottom: "4px" }}>{c.title}</h3>
              <p style={{ fontSize: "clamp(12px, 1.3vw, 13px)", color: "#64748b", lineHeight: 1.5, margin: "0 0 12px" }}>{c.desc}</p>
              <div style={{ display: "flex", alignItems: "center", gap: "6px" }}>
                <span style={{ fontSize: "11px", fontWeight: 600, color: c.color, background: c.bg, padding: "3px 10px", borderRadius: "9999px" }}>{c.stat}</span>
                <span style={{ marginLeft: "auto", fontSize: "14px", color: c.color }}>→</span>
              </div>
            </div>
          ))}
        </div>
      </div>
    </Layout>
  );
}
