import { useEffect, useState } from "react";
import { toast } from "sonner";
import api from "../api/api";
import Layout from "../components/Layout";
import Modal from "../components/Modal";
import { Input, Select, DateInput, NumberInput } from "../components/Form";
import { PageHeader, CardGrid, FilterBar, EmptyState, Spinner, StatusBadge, ProgressBar } from "../components/UI";

interface Meta { id: number; titulo: string; descricao: string; prograsso: number; prazo: string; status: string; }
interface FormData { titulo: string; descricao: string; prograsso: string; prazo: string; status: string; }
const emptyForm: FormData = { titulo: "", descricao: "", prograsso: "0", prazo: "", status: "PENDENTE" };
type Errors = Partial<Record<keyof FormData, string>>;

export default function Metas() {
  const [metas, setMetas] = useState<Meta[]>([]);
  const [loading, setLoading] = useState(true);
  const [showModal, setShowModal] = useState(false);
  const [editing, setEditing] = useState<Meta | null>(null);
  const [currentPage, setCurrentPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [filter, setFilter] = useState("all");
  const [form, setForm] = useState<FormData>(emptyForm);
  const [errors, setErrors] = useState<Errors>({});

  useEffect(() => { fetchMetas(); }, [currentPage]);
  const fetchMetas = async () => { setLoading(true); try { const r = await api.get(`/metas/all?page=${currentPage}&size=6`); setMetas(r.data.content); setTotalPages(r.data.totalPages); } catch { toast.error("Erro ao carregar metas"); } finally { setLoading(false); } };

  const validate = (): boolean => {
    const e: Errors = {};
    if (!form.titulo.trim()) e.titulo = "O título é obrigatório.";
    if (!form.prazo) e.prazo = "Informe um prazo.";
    const p = parseFloat(form.prograsso);
    if (isNaN(p) || p < 0 || p > 100) e.prograsso = "Progresso deve estar entre 0 e 100.";
    setErrors(e);
    return Object.keys(e).length === 0;
  };

  const handleSubmit = async () => {
    if (!validate()) return;
    const p = { titulo: form.titulo, descricao: form.descricao, prograsso: parseFloat(form.prograsso), prazo: new Date(form.prazo).toISOString(), status: form.status };
    const promise = editing ? api.put(`/metas/alter/${editing.id}`, p) : api.post("/metas/create", p);
    toast.promise(promise, { loading: "Salvando...", success: () => { closeModal(); fetchMetas(); return editing ? "Meta atualizada!" : "Meta criada!"; }, error: "Erro ao salvar" });
  };

  const handleDelete = async (id: number) => {
    toast("Excluir esta meta?", { action: { label: "Sim, excluir", onClick: () => { toast.promise(api.delete(`/metas/delete/${id}`), { loading: "Excluindo...", success: () => { fetchMetas(); return "Meta excluída!"; }, error: "Erro ao excluir" }); }}, cancel: { label: "Cancelar", onClick: () => {} } });
  };

  const openModal = (m: Meta | null = null) => { setErrors({}); if (m) { setEditing(m); setForm({ titulo: m.titulo, descricao: m.descricao, prograsso: m.prograsso.toString(), prazo: m.prazo ? new Date(m.prazo).toISOString().split('T')[0] : "", status: m.status }); } else { setEditing(null); setForm(emptyForm); } setShowModal(true); };
  const closeModal = () => { setShowModal(false); setEditing(null); };
  const fmtDate = (d: string) => d ? new Date(d).toLocaleDateString('pt-BR') : "";
  const filtered = filter === "all" ? metas : metas.filter(m => m.status === filter);

  return (
    <Layout>
      <PageHeader icon="🎯" title="Gestão de Metas" subtitle="Defina e acompanhe seus objetivos" actionLabel="Nova Meta" onAction={() => openModal()} />
      <FilterBar options={[{ value: "all", label: "Todas" },{ value: "PENDENTE", label: "Pendentes" },{ value: "Ativa", label: "Ativas" },{ value: "Concluida", label: "Concluídas" }]} selected={filter} onChange={setFilter} />
      {loading ? <Spinner text="Carregando metas..." /> : filtered.length === 0 ? <EmptyState icon="🎯" title="Nenhuma meta" text="Crie sua primeira meta!" actionLabel="Criar Meta" onAction={() => openModal()} /> :
        <CardGrid>{filtered.map(m => (
          <div key={m.id} style={cardStyle} onMouseEnter={e => { e.currentTarget.style.transform = "translateY(-3px)"; e.currentTarget.style.boxShadow = "var(--shadow-lg)"; }} onMouseLeave={e => { e.currentTarget.style.transform = "translateY(0)"; e.currentTarget.style.boxShadow = "var(--shadow-sm)"; }}>
            <div style={{ display: "flex", justifyContent: "space-between", alignItems: "start", marginBottom: "10px" }}><StatusBadge status={m.status} /><span style={{ fontSize: "12px", color: "var(--color-text-muted)" }}>📅 {fmtDate(m.prazo)}</span></div>
            <h3 style={{ fontSize: "16px", fontWeight: 600, marginBottom: "6px" }}>{m.titulo}</h3>
            {m.descricao && <p style={{ fontSize: "13px", color: "var(--color-text-secondary)", marginBottom: "14px" }}>{m.descricao}</p>}
            <div style={{ marginBottom: "8px" }}><div style={{ display: "flex", justifyContent: "space-between", marginBottom: "4px" }}><span style={{ fontSize: "12px", fontWeight: 600, color: "var(--color-text-secondary)" }}>Progresso</span><span style={{ fontSize: "12px", fontWeight: 700, color: m.prograsso >= 80 ? "#10b981" : m.prograsso >= 50 ? "#f59e0b" : "#ef4444" }}>{m.prograsso}%</span></div><ProgressBar value={m.prograsso} /></div>
            <div style={{ display: "flex", gap: "8px", marginTop: "8px" }}><button onClick={() => openModal(m)} style={btnSm}>✏️ Editar</button><button onClick={() => handleDelete(m.id)} style={{ ...btnSm, background: "var(--color-danger-light)", color: "var(--color-danger)" }}>🗑 Excluir</button></div>
          </div>))}</CardGrid>}
      {totalPages > 1 && (<div style={{ display: "flex", justifyContent: "center", gap: "8px", marginTop: "24px" }}>{Array.from({ length: totalPages }, (_, i) => (<button key={i} onClick={() => setCurrentPage(i)} style={{ padding: "8px 14px", borderRadius: "var(--radius-md)", border: "none", cursor: "pointer", fontWeight: 600, fontSize: "13px", background: currentPage === i ? "var(--color-primary)" : "white", color: currentPage === i ? "white" : "var(--color-text-secondary)", boxShadow: "var(--shadow-sm)" }}>{i + 1}</button>))}</div>)}
      <Modal open={showModal} onClose={closeModal} title={editing ? "Editar Meta" : "Nova Meta"} onSubmit={handleSubmit} submitLabel="Salvar">
        <Input label="Título" value={form.titulo} onChange={e => setForm({ ...form, titulo: e.target.value })} placeholder="Título da meta" error={errors.titulo} />
        <Input label="Descrição" value={form.descricao} onChange={e => setForm({ ...form, descricao: e.target.value })} placeholder="Descrição (opcional)" />
        <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr 1fr", gap: "14px" }}>
          <NumberInput label="Progresso (%)" value={form.prograsso} onChange={v => setForm({ ...form, prograsso: v })} placeholder="0" error={errors.prograsso} />
          <DateInput label="Prazo" value={form.prazo} onChange={e => setForm({ ...form, prazo: e.target.value })} error={errors.prazo} />
          <Select label="Status" value={form.status} onChange={e => setForm({ ...form, status: e.target.value })}><option value="PENDENTE">Pendente</option><option value="Ativa">Ativa</option><option value="Concluida">Concluída</option><option value="Cancelada">Cancelada</option></Select>
        </div>
      </Modal>
    </Layout>
  );
}

const cardStyle: React.CSSProperties = { background: "white", borderRadius: "var(--radius-lg)", padding: "22px", boxShadow: "var(--shadow-sm)", transition: "all var(--transition-base)", display: "flex", flexDirection: "column", borderLeft: "4px solid #f59e0b" };
const btnSm: React.CSSProperties = { padding: "7px 14px", borderRadius: "var(--radius-md)", border: "none", cursor: "pointer", fontSize: "12px", fontWeight: 600, background: "var(--color-bg)", color: "var(--color-text-secondary)" };
