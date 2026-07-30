import { useEffect, useState } from "react";
import { toast } from "sonner";
import api from "../api/api";
import Layout from "../components/Layout";
import Modal from "../components/Modal";
import { Input } from "../components/Form";
import { PageHeader, CardGrid, EmptyState, Spinner } from "../components/UI";

interface Estudo { id: number; nome: string; duracao_min: number; data: string; notas: string; materia_id: number; }
interface Materia { id: number; nome: string; descricao: string; estudos: Estudo[]; }
interface FormData { nome: string; descricao: string; }
type Errors = Partial<Record<keyof FormData, string>>;

export default function Materias() {
  const [materias, setMaterias] = useState<Materia[]>([]);
  const [loading, setLoading] = useState(true);
  const [showModal, setShowModal] = useState(false);
  const [editing, setEditing] = useState<Materia | null>(null);
  const [currentPage, setCurrentPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [form, setForm] = useState<FormData>({ nome: "", descricao: "" });
  const [errors, setErrors] = useState<Errors>({});

  useEffect(() => { fetchMaterias(); }, [currentPage]);
  const fetchMaterias = async () => { setLoading(true); try { const r = await api.get(`/materias/all?page=${currentPage}&size=6`); setMaterias(r.data.content); setTotalPages(r.data.totalPages); } catch { toast.error("Erro ao carregar matérias"); } finally { setLoading(false); } };

  const validate = (): boolean => { const e: Errors = {}; if (!form.nome.trim()) e.nome = "O nome da matéria é obrigatório."; setErrors(e); return Object.keys(e).length === 0; };

  const handleSubmit = async () => { if (!validate()) return; const promise = editing ? api.put(`/materias/alter/${editing.id}`, form) : api.post("/materias/create", form); toast.promise(promise, { loading: "Salvando...", success: () => { closeModal(); fetchMaterias(); return editing ? "Matéria atualizada!" : "Matéria criada!"; }, error: "Erro ao salvar" }); };

  const handleDelete = async (id: number) => { toast("Excluir esta matéria? Estudos serão perdidos.", { action: { label: "Sim, excluir", onClick: () => { toast.promise(api.delete(`/materias/delete/${id}`), { loading: "Excluindo...", success: () => { fetchMaterias(); return "Matéria excluída!"; }, error: "Erro ao excluir" }); }}, cancel: { label: "Cancelar", onClick: () => {} } }); };

  const openModal = (m: Materia | null = null) => { setErrors({}); if (m) { setEditing(m); setForm({ nome: m.nome, descricao: m.descricao }); } else { setEditing(null); setForm({ nome: "", descricao: "" }); } setShowModal(true); };
  const closeModal = () => { setShowModal(false); setEditing(null); };
  const totalHoras = (estudos: Estudo[]) => { const min = estudos.reduce((s, e) => s + (e.duracao_min || 0), 0); return `${Math.floor(min / 60)}h ${min % 60}min`; };

  return (
    <Layout>
      <PageHeader icon="📝" title="Gestão de Matérias" subtitle="Organize seus estudos por disciplina" actionLabel="Nova Matéria" onAction={() => openModal()} />
      {loading ? <Spinner text="Carregando matérias..." /> : materias.length === 0 ? <EmptyState icon="📚" title="Nenhuma matéria" text="Crie sua primeira matéria!" actionLabel="Criar Matéria" onAction={() => openModal()} /> :
        <CardGrid>{materias.map(m => (
          <div key={m.id} style={cardStyle} onMouseEnter={e => { e.currentTarget.style.transform = "translateY(-3px)"; e.currentTarget.style.boxShadow = "var(--shadow-lg)"; }} onMouseLeave={e => { e.currentTarget.style.transform = "translateY(0)"; e.currentTarget.style.boxShadow = "var(--shadow-sm)"; }}>
            <div style={{ display: "flex", justifyContent: "space-between", alignItems: "start", marginBottom: "10px" }}><span style={{ fontSize: "12px", fontWeight: 600, background: "#fdf2f8", color: "#ec4899", padding: "4px 10px", borderRadius: "var(--radius-full)" }}>{m.estudos.length} {m.estudos.length === 1 ? "estudo" : "estudos"}</span><span style={{ fontSize: "12px", color: "var(--color-text-muted)" }}>⏱ {totalHoras(m.estudos)}</span></div>
            <h3 style={{ fontSize: "17px", fontWeight: 700, marginBottom: "6px" }}>{m.nome}</h3>
            {m.descricao && <p style={{ fontSize: "13px", color: "var(--color-text-secondary)", marginBottom: "12px" }}>{m.descricao}</p>}
            <div style={{ display: "flex", gap: "8px", marginTop: "auto" }}><button onClick={() => openModal(m)} style={btnSm}>✏️ Editar</button><button onClick={() => handleDelete(m.id)} style={{ ...btnSm, background: "var(--color-danger-light)", color: "var(--color-danger)" }}>🗑 Excluir</button></div>
          </div>))}</CardGrid>}
      {totalPages > 1 && (<div style={{ display: "flex", justifyContent: "center", gap: "8px", marginTop: "24px" }}>{Array.from({ length: totalPages }, (_, i) => (<button key={i} onClick={() => setCurrentPage(i)} style={{ padding: "8px 14px", borderRadius: "var(--radius-md)", border: "none", cursor: "pointer", fontWeight: 600, fontSize: "13px", background: currentPage === i ? "var(--color-primary)" : "white", color: currentPage === i ? "white" : "var(--color-text-secondary)", boxShadow: "var(--shadow-sm)" }}>{i + 1}</button>))}</div>)}
      <Modal open={showModal} onClose={closeModal} title={editing ? "Editar Matéria" : "Nova Matéria"} onSubmit={handleSubmit} submitLabel="Salvar">
        <Input label="Nome da Matéria" value={form.nome} onChange={e => setForm({ ...form, nome: e.target.value })} placeholder="Ex: Matemática" error={errors.nome} />
        <Input label="Descrição" value={form.descricao} onChange={e => setForm({ ...form, descricao: e.target.value })} placeholder="Descrição (opcional)" />
      </Modal>
    </Layout>
  );
}

const cardStyle: React.CSSProperties = { background: "white", borderRadius: "var(--radius-lg)", padding: "22px", boxShadow: "var(--shadow-sm)", transition: "all var(--transition-base)", display: "flex", flexDirection: "column", borderLeft: "4px solid #ec4899" };
const btnSm: React.CSSProperties = { padding: "7px 14px", borderRadius: "var(--radius-md)", border: "none", cursor: "pointer", fontSize: "12px", fontWeight: 600, background: "var(--color-bg)", color: "var(--color-text-secondary)" };
