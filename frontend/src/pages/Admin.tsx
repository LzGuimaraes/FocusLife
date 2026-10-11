import { useEffect, useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";
import api from "../api/api";
import Layout from "../components/Layout";
import { Button } from "../components/Shared";
import { Input } from "../components/Form";
import { PageHeader, Spinner, EmptyState, ErrorState, FilterBar, Pagination } from "../components/UI";
import { useAuth } from "../auth/AuthProvider";

interface AdminUser {
  id: number;
  nome: string;
  email: string;
  role: string;
}

/* Itens por página da listagem. A paginação é client-side porque o endpoint
   GET /admin/users devolve a lista completa (não aceita página/busca). */
const TAMANHO_PAGINA = 8;

/* Busca que ignora maiúsculas/minúsculas e acentos (nomes em pt-BR). */
const normalizar = (texto: string) =>
  texto.normalize("NFD").replace(/[\u0300-\u036f]/g, "").toLowerCase();

const roleBadge = (role: string) =>
  role === "ADMIN"
    ? { background: "var(--color-warning-light)", color: "var(--color-warning)", label: "🛡️ Admin" }
    : { background: "var(--color-info-light)", color: "var(--color-info)", label: "Usuário" };

export default function Admin() {
  const navigate = useNavigate();
  const { user: me } = useAuth();
  const [usuarios, setUsuarios] = useState<AdminUser[]>([]);
  const [carregando, setCarregando] = useState(true);
  const [erro, setErro] = useState(false);
  const [busca, setBusca] = useState("");
  const [filtroRole, setFiltroRole] = useState("all");
  const [pagina, setPagina] = useState(0);

  const carregarUsuarios = async () => {
    setCarregando(true);
    setErro(false);
    try {
      const res = await api.get("/admin/users");
      setUsuarios(res.data);
    } catch {
      setErro(true);
    } finally {
      setCarregando(false);
    }
  };

  useEffect(() => { carregarUsuarios(); }, []);

  const filtrando = busca.trim() !== "" || filtroRole !== "all";

  const filtrados = useMemo(() => {
    const termo = normalizar(busca.trim());
    return usuarios.filter((u) => {
      if (filtroRole !== "all" && u.role !== filtroRole) return false;
      if (!termo) return true;
      return normalizar(u.nome).includes(termo) || normalizar(u.email).includes(termo);
    });
  }, [usuarios, busca, filtroRole]);

  const totalPaginas = Math.max(1, Math.ceil(filtrados.length / TAMANHO_PAGINA));
  const paginaAtual = Math.min(pagina, totalPaginas - 1);
  const visiveis = filtrados.slice(paginaAtual * TAMANHO_PAGINA, (paginaAtual + 1) * TAMANHO_PAGINA);

  const abrirDetalhe = (id: number) => navigate(`/admin/usuarios/${id}`);
  const alterarBusca = (valor: string) => { setBusca(valor); setPagina(0); };
  const alterarFiltroRole = (valor: string) => { setFiltroRole(valor); setPagina(0); };
  const limparFiltros = () => { setBusca(""); setFiltroRole("all"); setPagina(0); };

  return (
    <Layout>
      <PageHeader icon="🛡️" title="Administração" subtitle="Painel administrativo — usuários da plataforma" />

      <div style={{ maxWidth: "360px", marginBottom: "14px" }}>
        <Input label="Pesquisar" placeholder="Nome ou e-mail..." value={busca} onChange={(e) => alterarBusca(e.target.value)} />
      </div>
      <FilterBar
        options={[{ value: "all", label: "Todos" }, { value: "ADMIN", label: "🛡️ Admins" }, { value: "USER", label: "Usuários" }]}
        selected={filtroRole}
        onChange={alterarFiltroRole}
      />

      {carregando ? (
        <Spinner text="Carregando usuários..." />
      ) : erro ? (
        <ErrorState text="Não foi possível carregar a lista de usuários." onRetry={carregarUsuarios} />
      ) : usuarios.length === 0 ? (
        <EmptyState icon="👥" title="Nenhum usuário" text="Nenhum usuário cadastrado na plataforma ainda." />
      ) : filtrados.length === 0 ? (
        <EmptyState icon="🔍" title="Nenhum resultado" text="Nenhum usuário corresponde à pesquisa e aos filtros atuais." actionLabel="Limpar filtros" onAction={limparFiltros} />
      ) : (
        <>
          <p style={{ fontSize: "13px", color: "var(--color-text-secondary)", marginBottom: "12px" }}>
            {filtrando
              ? `${filtrados.length} de ${usuarios.length} usuário${usuarios.length === 1 ? "" : "s"}`
              : `${usuarios.length} usuário${usuarios.length === 1 ? "" : "s"} cadastrado${usuarios.length === 1 ? "" : "s"}`}
          </p>
          <div style={{ display: "flex", flexDirection: "column", gap: "10px" }}>
            {visiveis.map((u) => {
              const rb = roleBadge(u.role);
              const isSelf = me?.id === u.id;
              return (
                <div key={u.id} onClick={() => abrirDetalhe(u.id)}
                  style={{ background: "var(--surface)", border: "1px solid var(--border)", borderRadius: "var(--radius-lg)", padding: "14px 18px", display: "flex", alignItems: "center", gap: "12px", flexWrap: "wrap", cursor: "pointer", boxShadow: "var(--shadow-sm)", transition: "all var(--transition-fast)" }}
                  onMouseEnter={(e) => { e.currentTarget.style.boxShadow = "var(--shadow-md)"; e.currentTarget.style.transform = "translateY(-1px)"; }}
                  onMouseLeave={(e) => { e.currentTarget.style.boxShadow = "var(--shadow-sm)"; e.currentTarget.style.transform = "translateY(0)"; }}>
                  <div aria-hidden="true" style={{ width: "38px", height: "38px", borderRadius: "50%", background: u.role === "ADMIN" ? "var(--color-warning)" : "var(--color-primary)", color: "white", display: "flex", alignItems: "center", justifyContent: "center", fontSize: "15px", fontWeight: 600, flexShrink: 0 }}>{u.nome.charAt(0).toUpperCase()}</div>
                  <div style={{ flex: 1, minWidth: "180px" }}>
                    <p style={{ fontSize: "14px", fontWeight: 600, color: "var(--color-text)", margin: 0 }}>{u.nome} {isSelf && <span style={{ fontSize: "11px", color: "var(--color-text-muted)", fontWeight: 500 }}>(você)</span>}</p>
                    <p style={{ fontSize: "12px", color: "var(--color-text-secondary)", margin: 0 }}>{u.email}</p>
                  </div>
                  <span style={{ padding: "4px 12px", borderRadius: "var(--radius-full)", fontSize: "12px", fontWeight: 700, background: rb.background, color: rb.color }}>{rb.label}</span>
                  <Button size="sm" variant="outline" onClick={(e) => { e.stopPropagation(); abrirDetalhe(u.id); }}>Ver detalhes</Button>
                </div>
              );
            })}
          </div>
          <Pagination page={paginaAtual} totalPages={totalPaginas} onChange={setPagina} ariaLabel="Paginação de usuários" />
        </>
      )}
    </Layout>
  );
}
