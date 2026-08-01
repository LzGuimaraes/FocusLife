import { useEffect, useState } from "react";
import { toast } from "sonner";
import api from "../api/api";
import Layout from "../components/Layout";
import { Button } from "../components/Shared";
import { PageHeader, Spinner, EmptyState } from "../components/UI";
import { useAuth } from "../auth/AuthProvider";

interface AdminUser {
  id: number;
  nome: string;
  email: string;
  role: string;
}

const roleBadge = (role: string) =>
  role === "ADMIN"
    ? { bg: "#fef3c7", color: "#b45309", label: "🛡️ Admin" }
    : { bg: "#eef2ff", color: "#4338ca", label: "Usuário" };

export default function Admin() {
  const { user: me } = useAuth();
  const [users, setUsers] = useState<AdminUser[]>([]);
  const [loading, setLoading] = useState(true);

  const fetchUsers = async () => {
    setLoading(true);
    try {
      const res = await api.get("/admin/users");
      setUsers(res.data);
    } catch {
      toast.error("Erro ao carregar usuários");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { fetchUsers(); }, []);

  const toggleRole = async (u: AdminUser) => {
    const newRole = u.role === "ADMIN" ? "USER" : "ADMIN";
    toast.promise(api.put(`/admin/users/${u.id}/role`, { role: newRole }), {
      loading: "Atualizando...",
      success: () => { fetchUsers(); return `${u.nome} agora é ${newRole === "ADMIN" ? "admin" : "usuário"}`; },
      error: "Erro ao atualizar role",
    });
  };

  const handleDelete = (u: AdminUser) => {
    if (me?.id === u.id) { toast.error("Você não pode excluir sua própria conta."); return; }
    toast("Excluir usuário?", {
      description: `${u.nome} (${u.email})`,
      action: {
        label: "Excluir",
        onClick: () => {
          toast.promise(api.delete(`/admin/users/${u.id}`), {
            loading: "Excluindo...",
            success: () => { fetchUsers(); return "Usuário excluído!"; },
            error: (err: any) => err?.response?.data?.message || "Erro ao excluir",
          });
        },
      },
      cancel: { label: "Cancelar", onClick: () => {} },
    });
  };

  return (
    <Layout>
      <PageHeader icon="🛡️" title="Administração" subtitle="Gestão de usuários da plataforma" />
      {loading ? <Spinner text="Carregando usuários..." /> : users.length === 0 ? (
        <EmptyState icon="👥" title="Nenhum usuário" text="Nenhum usuário cadastrado ainda." />
      ) : (
        <div style={{ display: "flex", flexDirection: "column", gap: "12px" }}>
          {users.map((u) => {
            const rb = roleBadge(u.role);
            const isSelf = me?.id === u.id;
            return (
              <div key={u.id} style={{ background: "white", borderRadius: "12px", padding: "16px 18px", boxShadow: "0 1px 3px rgba(0,0,0,0.06)", display: "flex", alignItems: "center", gap: "12px", flexWrap: "wrap" }}>
                <div style={{ width: "38px", height: "38px", borderRadius: "50%", background: u.role === "ADMIN" ? "#f59e0b" : "#6366f1", color: "white", display: "flex", alignItems: "center", justifyContent: "center", fontSize: "15px", fontWeight: 600, flexShrink: 0 }}>{u.nome.charAt(0).toUpperCase()}</div>
                <div style={{ flex: 1, minWidth: "180px" }}>
                  <p style={{ fontSize: "14px", fontWeight: 600, color: "#0f172a", margin: 0 }}>{u.nome} {isSelf && <span style={{ fontSize: "11px", color: "#64748b", fontWeight: 500 }}>(você)</span>}</p>
                  <p style={{ fontSize: "12px", color: "#64748b", margin: 0 }}>{u.email}</p>
                </div>
                <span style={{ padding: "4px 12px", borderRadius: "var(--radius-full)", fontSize: "12px", fontWeight: 700, background: rb.bg, color: rb.color }}>{rb.label}</span>
                <div style={{ display: "flex", gap: "8px" }}>
                  <Button size="sm" variant={u.role === "ADMIN" ? "outline" : "primary"} onClick={() => toggleRole(u)}>{u.role === "ADMIN" ? "Rebaixar" : "Promover"}</Button>
                  <Button size="sm" variant="destructive" disabled={isSelf} onClick={() => handleDelete(u)}>Excluir</Button>
                </div>
              </div>
            );
          })}
        </div>
      )}
    </Layout>
  );
}
