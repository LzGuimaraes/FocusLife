import { useState, useEffect, type ReactNode } from "react";
import { useNavigate, useLocation } from "react-router-dom";
import { toast } from "sonner";
import { useAuth } from "../auth/AuthProvider";

interface LayoutProps { children: ReactNode; }

const navItems = [
  { path: "/dashboard", label: "Dashboard", icon: "🏠", color: "#6366f1" },
  { path: "/tarefas",   label: "Tarefas",   icon: "✓",  color: "#06b6d4" },
  { path: "/materias",  label: "Matérias",  icon: "📝", color: "#ec4899" },
  { path: "/metas",     label: "Metas",     icon: "🎯", color: "#f59e0b" },
  { path: "/financas",  label: "Finanças",  icon: "💰", color: "#10b981" },
  { path: "/como-funciona", label: "Como Funciona", icon: "❓", color: "#8b5cf6" },
];

export default function Layout({ children }: LayoutProps) {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [sidebarOpen, setSidebarOpen] = useState(false);
  const [loggingOut, setLoggingOut] = useState(false);
  const [isDesktop, setIsDesktop] = useState(window.innerWidth >= 1024);

  useEffect(() => {
    const onResize = () => setIsDesktop(window.innerWidth >= 1024);
    window.addEventListener("resize", onResize);
    return () => window.removeEventListener("resize", onResize);
  }, []);

  useEffect(() => { setSidebarOpen(false); }, [location.pathname]);

  const handleLogout = async () => {
    setLoggingOut(true);
    try { await logout(); toast.success("Até logo!"); navigate("/auth/login"); }
    catch { navigate("/auth/login"); }
    finally { setLoggingOut(false); }
  };

  return (
    <div style={{ display: "flex", minHeight: "100vh", width: "100%" }}>
      {sidebarOpen && !isDesktop && (
        <div onClick={() => setSidebarOpen(false)} style={{ position: "fixed", inset: 0, background: "rgba(0,0,0,0.4)", zIndex: 90 }} />
      )}
      <aside style={{
        position: isDesktop ? "sticky" : "fixed", top: 0, left: 0, bottom: 0, zIndex: 100,
        width: "260px", minWidth: "260px", background: "#1e1b4b",
        transform: isDesktop ? "none" : (sidebarOpen ? "translateX(0)" : "translateX(-100%)"),
        transition: "transform 0.25s ease", display: "flex", flexDirection: "column",
        height: isDesktop ? "100vh" : "100dvh",
      }}>
        <div style={{ padding: "22px 20px", borderBottom: "1px solid rgba(255,255,255,0.08)" }}>
          <h1 style={{ fontSize: "20px", fontWeight: 700, color: "white", letterSpacing: "-0.5px", margin: 0 }}>⚡ FocusLife</h1>
          <p style={{ fontSize: "11px", color: "#a5b4fc", marginTop: "2px" }}>Hub de Produtividade</p>
        </div>
        <nav style={{ flex: 1, padding: "10px 8px", display: "flex", flexDirection: "column", gap: "2px", overflowY: "auto" }}>
          {navItems.map((item) => {
            const active = location.pathname === item.path || (item.path === "/dashboard" && location.pathname === "/");
            return (
              <button key={item.path} onClick={() => { navigate(item.path); setSidebarOpen(false); }}
                style={{ display: "flex", alignItems: "center", gap: "10px", padding: "10px 12px", borderRadius: "8px", background: active ? "rgba(99,102,241,0.2)" : "transparent", color: active ? "white" : "#c7d2fe", border: "none", cursor: "pointer", fontSize: "14px", fontWeight: active ? 600 : 400, transition: "all 0.15s ease", textAlign: "left" as const, width: "100%" }}
                onMouseEnter={e => { if (!active) { e.currentTarget.style.background = "rgba(255,255,255,0.06)"; e.currentTarget.style.color = "white"; }}}
                onMouseLeave={e => { if (!active) { e.currentTarget.style.background = "transparent"; e.currentTarget.style.color = "#c7d2fe"; }}}
              ><span style={{ fontSize: "16px", width: "22px", textAlign: "center" }}>{item.icon}</span>{item.label}</button>
            );
          })}
        </nav>
        <div style={{ padding: "14px 18px", borderTop: "1px solid rgba(255,255,255,0.08)", display: "flex", alignItems: "center", gap: "10px" }}>
          <div style={{ width: "34px", height: "34px", borderRadius: "50%", background: "#6366f1", color: "white", display: "flex", alignItems: "center", justifyContent: "center", fontSize: "14px", fontWeight: 600, flexShrink: 0 }}>{user?.nome?.charAt(0).toUpperCase() || "U"}</div>
          <div style={{ flex: 1, minWidth: 0 }}>
            <p style={{ fontSize: "13px", fontWeight: 500, color: "white", overflow: "hidden", textOverflow: "ellipsis", whiteSpace: "nowrap", margin: 0 }}>{user?.nome || "Usuário"}</p>
            <p style={{ fontSize: "11px", color: "#a5b4fc", overflow: "hidden", textOverflow: "ellipsis", whiteSpace: "nowrap", margin: 0 }}>{user?.email}</p>
          </div>
          <button onClick={handleLogout} disabled={loggingOut} style={{ background: "rgba(239,68,68,0.15)", color: "#fca5a5", border: "none", borderRadius: "6px", padding: "5px 10px", cursor: "pointer", fontSize: "12px", fontWeight: 500, transition: "all 0.15s ease", whiteSpace: "nowrap" }}
            onMouseEnter={e => { e.currentTarget.style.background = "rgba(239,68,68,0.3)"; }} onMouseLeave={e => { e.currentTarget.style.background = "rgba(239,68,68,0.15)"; }}>{loggingOut ? "..." : "Sair"}</button>
        </div>
      </aside>
      <div style={{ flex: 1, display: "flex", flexDirection: "column", minWidth: 0 }}>
        {!isDesktop && (
          <header style={{ background: "white", padding: "12px 16px", boxShadow: "0 1px 2px rgba(0,0,0,0.05)", display: "flex", alignItems: "center", gap: "12px", position: "sticky", top: 0, zIndex: 50 }}>
            <button onClick={() => setSidebarOpen(true)} style={{ background: "#f1f5f9", border: "none", borderRadius: "6px", padding: "8px 10px", cursor: "pointer", fontSize: "18px", display: "flex", alignItems: "center", lineHeight: 1 }}>☰</button>
            <span style={{ fontWeight: 600, fontSize: "15px", color: "#0f172a" }}>{navItems.find(i => i.path === location.pathname)?.label || "FocusLife"}</span>
          </header>
        )}
        <main style={{ flex: 1, padding: isDesktop ? "28px 32px" : "16px", overflow: "auto" }}>
          {children}
        </main>
      </div>
    </div>
  );
}
