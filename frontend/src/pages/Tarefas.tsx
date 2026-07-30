import { useEffect, useState } from "react";
import { toast } from "sonner";
import api from "../api/api";
import Layout from "../components/Layout";
import Modal from "../components/Modal";
import { Input, Select, DateInput } from "../components/Form";
import { PageHeader, CardGrid, FilterBar, EmptyState, Spinner, StatusBadge, PrioridadeBadge } from "../components/UI";

interface Tarefa { id: number; titulo: string; status: string; prioridade: string; prazo: string; }
interface FormData { titulo: string; status: string; prioridade: string; prazo: string; }

const emptyForm: FormData = { titulo: "", status: "PENDENTE", prioridade: "media", prazo: "" };
type Errors = Partial<Record<keyof FormData, string>>;

export default function Tarefas() {
  const [tarefas, setTarefas] = useState<Tarefa[]>([]);
  const [loading, setLoading] = useState(true);
  const [showModal, setShowModal] = useState(false);
  const [editing, setEditing] = useState<Tarefa | null>(null);
  const [currentPage, setCurrentPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [filter, setFilter] = useState("all");
  const [form, setForm] = useState<FormData>(emptyForm);
  const [errors, setErrors] = useState<Errors>({});

  useEffect(() => { fetchTarefas(); }, [currentPage]);
  const fetchTarefas = async () => {
    setLoading(true);
    try { const r = await api.get(`/tarefas/all?page=${currentPage}&size=6`); setTarefas(r.data.content); setTotalPages(r.data.totalPages); }
    catch { toast.error("Erro ao carregar tarefas"); } finally { setLoading(false); }
  };

  const validate = (): boolean => {
    const e: Errors = {};
    if (!form.titulo.trim()) e.titulo = "O título é obrigatório.";
    if (!form.prazo) e.prazo = "Informe um prazo.";
    setErrors(e);
    return Object.keys(e).length === 0;
  };

  const handleSubmit = async () => {
    if (!validate()) return;
    const payload = { titulo: form.titulo, status: form.status, prioridade: form.prioridade, prazo: form.prazo ? new Date(form.prazo).toISOString() : null };
    const promise = editing ? api.put(`/tarefas/alter/${editing.id}`, payload) : api.post("/tarefas/create", payload);
    toast.promise(promise, { loading: "Salvando...", success: () => { closeModal(); fetchTarefas(); return editing ? "Tarefa atualizada!" : "Tarefa criada!"; }, error: "Erro ao salvar" });
  };

  const handleDelete = async (id: number) => {
    toast("Excluir esta tarefa?", {
      action: { label: "Sim, excluir", onClick: () => {
        toast.promise(api.delete(`/tarefas/delete/${id}`), { loading: "Excluindo...", success: () => { fetchTarefas(); return "Tarefa excluída!"; }, error: "Erro ao excluir" });
      }},
      cancel: { label: "Cancelar", onClick: () => {} },
    });
  };

  const openModal = (t: Tarefa | null = null) => {
    setErrors({});
    if (t) { setEditing(t); setForm({ titulo: t.titulo, status: t.status, prioridade: t.prioridade, prazo: t.prazo ? new Date(t.prazo).toISOString().split('T')[0] : "" }); }
    else { setEditing(null); setForm(emptyForm); }
    setShowModal(true);
  };
  const closeModal = () => { setShowModal(false); setEditing(null); };

  const fmtDate = (d: string) => d ? new Date(d).toLocaleDateString('pt-BR') : "Sem prazo";
  const filtered = filter === "all" ? tarefas : tarefas.filter(t => t.status === filter);

  return (
    <Layout>
      <PageHeader icon="✓" title="Gestão de Tarefas" subtitle="Organize seu dia com tarefas priorizadas" actionLabel="Nova Tarefa" onAction={() => openModal()} />
      <FilterBar options={[{ value: "all", label: "Todas" },{ value: "PENDENTE", label: "Pendentes" },{ value: "Ativa", label: "Ativas" },{ value: "Concluida", label: "Concluídas" }]} selected={filter} onChange={setFilter} />
      {loading ? <Spinner text="Carregando tarefas..." /> :
        filtered.length === 0 ? <EmptyState icon="📋" title="Nenhuma tarefa" text="Crie sua primeira tarefa!" actionLabel="Criar Tarefa" onAction={() => openModal()} /> :
        <CardGrid>{filtered.map(t => (
          <div key={t.id} style={cardStyle} onMouseEnter={e => { e.currentTarget.style.transform = "translateY(-3px)"; e.currentTarget.style.boxShadow = "var(--shadow-lg)"; }} onMouseLeave={e => { e.currentTarget.style.transform = "translateY(0)"; e.currentTarget.style.boxShadow = "var(--shadow-sm)"; }}>
            <div style={{ display: "flex", justifyContent: "space-between", alignItems: "start", marginBottom: "12px" }}><StatusBadge status={t.status} /><PrioridadeBadge prioridade={t.prioridade} /></div>
            <h3 style={{ fontSize: "16px", fontWeight: 600, color: "var(--color-text)", marginBottom: "8px" }}>{t.titulo}</h3>
            <p style={{ fontSize: "12px", color: "var(--color-text-muted)", marginBottom: "16px" }}>📅 {fmtDate(t.prazo)}</p>
            <div style={{ display: "flex", gap: "8px", marginTop: "auto" }}><button onClick={() => openModal(t)} style={btnSm}>✏️ Editar</button><button onClick={() => handleDelete(t.id)} style={{ ...btnSm, background: "var(--color-danger-light)", color: "var(--color-danger)" }}>🗑 Excluir</button></div>
          </div>))}</CardGrid>
      }
      {totalPages > 1 && (<div style={{ display: "flex", justifyContent: "center", gap: "8px", marginTop: "24px" }}>{Array.from({ length: totalPages }, (_, i) => (<button key={i} onClick={() => setCurrentPage(i)} style={{ padding: "8px 14px", borderRadius: "var(--radius-md)", border: "none", cursor: "pointer", fontWeight: 600, fontSize: "13px", background: currentPage === i ? "var(--color-primary)" : "white", color: currentPage === i ? "white" : "var(--color-text-secondary)", boxShadow: "var(--shadow-sm)" }}>{i + 1}</button>))}</div>)}
      <Modal open={showModal} onClose={closeModal} title={editing ? "Editar Tarefa" : "Nova Tarefa"} onSubmit={handleSubmit} submitLabel="Salvar">
        <Input label="Título" value={form.titulo} onChange={e => setForm({ ...form, titulo: e.target.value })} placeholder="Título da tarefa" error={errors.titulo} />
        <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "14px" }}>
          <Select label="Status" value={form.status} onChange={e => setForm({ ...form, status: e.target.value })}><option value="PENDENTE">Pendente</option><option value="Ativa">Ativa</option><option value="Concluida">Concluída</option><option value="Cancelada">Cancelada</option></Select>
          <Select label="Prioridade" value={form.prioridade} onChange={e => setForm({ ...form, prioridade: e.target.value })}><option value="baixa">🔵 Baixa</option><option value="media">🟡 Média</option><option value="alta">🔴 Alta</option></Select>
        </div>
        <DateInput label="Prazo" value={form.prazo} onChange={e => setForm({ ...form, prazo: e.target.value })} error={errors.prazo} />
      </Modal>
    </Layout>
  );
}

const cardStyle: React.CSSProperties = { background: "white", borderRadius: "var(--radius-lg)", padding: "22px", boxShadow: "var(--shadow-sm)", transition: "all var(--transition-base)", display: "flex", flexDirection: "column", borderLeft: "4px solid #06b6d4" };
const btnSm: React.CSSProperties = { padding: "7px 14px", borderRadius: "var(--radius-md)", border: "none", cursor: "pointer", fontSize: "12px", fontWeight: 600, background: "var(--color-bg)", color: "var(--color-text-secondary)", transition: "all var(--transition-fast)" };
