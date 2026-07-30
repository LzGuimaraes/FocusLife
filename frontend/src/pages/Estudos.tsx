import { useEffect, useState } from "react";
import { toast } from "sonner";
import api from "../api/api";
import Layout from "../components/Layout";
import Modal from "../components/Modal";
import { Input, Select, DateInput, NumberInput, TextArea } from "../components/Form";
import { PageHeader, CardGrid, EmptyState, Spinner } from "../components/UI";

interface Estudo { id: number; nome: string; duracao_min: number; data: string; notas: string; materia_id: number; }
interface Materia { id: number; nome: string; }
interface FormData { nome: string; duracao_min: string; data: string; notas: string; materia_id: string; }
type Errors = Partial<Record<keyof FormData, string>>;

export default function Estudos() {
  const [estudos, setEstudos] = useState<Estudo[]>([]);
  const [materias, setMaterias] = useState<Materia[]>([]);
  const [loading, setLoading] = useState(true);
  const [showModal, setShowModal] = useState(false);
  const [editing, setEditing] = useState<Estudo | null>(null);
  const [currentPage, setCurrentPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [filterMateria, setFilterMateria] = useState("all");
  const [form, setForm] = useState<FormData>({ nome: "", duracao_min: "", data: "", notas: "", materia_id: "" });
  const [errors, setErrors] = useState<Errors>({});

  useEffect(() => { api.get("/materias/all?page=0&size=100").then(r => setMaterias(r.data.content || [])).catch(() => {}); }, []);
  useEffect(() => { fetchEstudos(); }, [currentPage]);
  const fetchEstudos = async () => { setLoading(true); try { const r = await api.get(`/estudos/all?page=${currentPage}&size=6`); setEstudos(r.data.content); setTotalPages(r.data.totalPages); } catch { toast.error("Erro ao carregar estudos"); } finally { setLoading(false); } };

  const validate = (): boolean => {
    const e: Errors = {};
    if (!form.nome.trim()) e.nome = "O nome da sessão é obrigatório.";
    if (!form.data) e.data = "Informe a data do estudo.";
    if (!form.materia_id) e.materia_id = "Selecione uma matéria.";
    const dur = parseInt(form.duracao_min);
    if (form.duracao_min && (isNaN(dur) || dur < 0)) e.duracao_min = "Duração inválida.";
    setErrors(e);
    return Object.keys(e).length === 0;
  };

  const handleSubmit = async () => {
    if (!validate()) return;
    const p = { nome: form.nome, duracao_min: parseInt(form.duracao_min) || 0, data: new Date(form.data).toISOString(), notas: form.notas, materia_id: parseInt(form.materia_id) };
    const promise = editing ? api.put(`/estudos/alter/${editing.id}`, p) : api.post("/estudos/create", p);
    toast.promise(promise, { loading: "Salvando...", success: () => { closeModal(); fetchEstudos(); return editing ? "Estudo atualizado!" : "Estudo registrado!"; }, error: "Erro ao salvar" });
  };

  const handleDelete = async (id: number) => {
    toast("Excluir esta sessão de estudo?", { action: { label: "Sim, excluir", onClick: () => { toast.promise(api.delete(`/estudos/delete/${id}`), { loading: "Excluindo...", success: () => { fetchEstudos(); return "Estudo excluído!"; }, error: "Erro ao excluir" }); }}, cancel: { label: "Cancelar", onClick: () => {} } });
  };

  const openModal = (e: Estudo | null = null) => { setErrors({}); if (e) { setEditing(e); setForm({ nome: e.nome, duracao_min: e.duracao_min.toString(), data: e.data ? new Date(e.data).toISOString().split('T')[0] : "", notas: e.notas || "", materia_id: e.materia_id.toString() }); } else { setEditing(null); setForm({ nome: "", duracao_min: "", data: "", notas: "", materia_id: materias[0]?.id.toString() || "" }); } setShowModal(true); };
  const closeModal = () => { setShowModal(false); setEditing(null); };
  const getMateriaNome = (id: number) => materias.find(m => m.id === id)?.nome || "N/A";
  const fmtDate = (d: string) => d ? new Date(d).toLocaleDateString('pt-BR') : "";
  const fmtDur = (m: number) => m < 60 ? `${m}min` : `${Math.floor(m / 60)}h ${m % 60}min`;
  const durColor = (m: number) => m >= 120 ? "#10b981" : m >= 60 ? "#3b82f6" : m >= 30 ? "#f59e0b" : "#ef4444";
  const filtered = filterMateria === "all" ? estudos : estudos.filter(e => e.materia_id.toString() === filterMateria);

  return (
    <Layout>
      <PageHeader icon="📖" title="Registro de Estudos" subtitle="Acompanhe suas sessões de estudo" actionLabel="Novo Estudo" onAction={() => openModal()} />
      <div style={{ display: "flex", gap: "8px", flexWrap: "wrap", marginBottom: "20px" }}><button onClick={() => setFilterMateria("all")} style={fb("all"===filterMateria)}>Todas</button>{materias.map(m => (<button key={m.id} onClick={() => setFilterMateria(m.id.toString())} style={fb(m.id.toString()===filterMateria)}>{m.nome}</button>))}</div>
      {loading ? <Spinner text="Carregando estudos..." /> : filtered.length === 0 ? <EmptyState icon="📖" title="Nenhum estudo" text="Registre sua primeira sessão!" actionLabel="Registrar Estudo" onAction={() => openModal()} /> :
        <CardGrid>{filtered.map(e => (
          <div key={e.id} style={cardStyle} onMouseEnter={ev => { ev.currentTarget.style.transform = "translateY(-3px)"; ev.currentTarget.style.boxShadow = "var(--shadow-lg)"; }} onMouseLeave={ev => { ev.currentTarget.style.transform = "translateY(0)"; ev.currentTarget.style.boxShadow = "var(--shadow-sm)"; }}>
            <div style={{ display: "flex", justifyContent: "space-between", alignItems: "start", marginBottom: "10px" }}><span style={{ fontSize: "12px", fontWeight: 600, padding: "4px 10px", borderRadius: "var(--radius-full)", background: "#eef2ff", color: "#6366f1" }}>{getMateriaNome(e.materia_id)}</span><span style={{ fontSize: "14px", fontWeight: 700, color: durColor(e.duracao_min) }}>⏱ {fmtDur(e.duracao_min)}</span></div>
            <h3 style={{ fontSize: "16px", fontWeight: 600, marginBottom: "4px" }}>{e.nome}</h3>
            <p style={{ fontSize: "12px", color: "var(--color-text-muted)", marginBottom: "8px" }}>📅 {fmtDate(e.data)}</p>
            {e.notas && <p style={{ fontSize: "13px", color: "var(--color-text-secondary)", fontStyle: "italic", marginBottom: "10px" }}>"{e.notas}"</p>}
            <div style={{ display: "flex", gap: "8px", marginTop: "auto" }}><button onClick={() => openModal(e)} style={btnSm}>✏️ Editar</button><button onClick={() => handleDelete(e.id)} style={{ ...btnSm, background: "var(--color-danger-light)", color: "var(--color-danger)" }}>🗑</button></div>
          </div>))}</CardGrid>}
      {totalPages > 1 && (<div style={{ display: "flex", justifyContent: "center", gap: "8px", marginTop: "24px" }}>{Array.from({ length: totalPages }, (_, i) => (<button key={i} onClick={() => setCurrentPage(i)} style={{ padding: "8px 14px", borderRadius: "var(--radius-md)", border: "none", cursor: "pointer", fontWeight: 600, fontSize: "13px", background: currentPage === i ? "var(--color-primary)" : "white", color: currentPage === i ? "white" : "var(--color-text-secondary)", boxShadow: "var(--shadow-sm)" }}>{i + 1}</button>))}</div>)}
      <Modal open={showModal} onClose={closeModal} title={editing ? "Editar Estudo" : "Novo Estudo"} onSubmit={handleSubmit} submitLabel="Salvar" width="520px">
        <Input label="Nome da Sessão" value={form.nome} onChange={e => setForm({ ...form, nome: e.target.value })} placeholder="Ex: Revisão de Cálculo" error={errors.nome} />
        <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "14px" }}>
          <NumberInput label="Duração (min)" value={form.duracao_min} onChange={v => setForm({ ...form, duracao_min: v })} placeholder="60" error={errors.duracao_min} />
          <DateInput label="Data" value={form.data} onChange={e => setForm({ ...form, data: e.target.value })} error={errors.data} />
        </div>
        <Select label="Matéria" value={form.materia_id} onChange={e => setForm({ ...form, materia_id: e.target.value })} error={errors.materia_id}>{materias.map(m => <option key={m.id} value={m.id}>{m.nome}</option>)}</Select>
        <TextArea label="Anotações" value={form.notas} onChange={e => setForm({ ...form, notas: e.target.value })} placeholder="Anotações da sessão..." />
      </Modal>
    </Layout>
  );
}

const fb = (active: boolean): React.CSSProperties => ({ padding: "7px 16px", borderRadius: "var(--radius-full)", border: "none", cursor: "pointer", fontSize: "13px", fontWeight: 500, background: active ? "var(--color-primary)" : "white", color: active ? "white" : "var(--color-text-secondary)", boxShadow: "var(--shadow-sm)" });
const cardStyle: React.CSSProperties = { background: "white", borderRadius: "var(--radius-lg)", padding: "22px", boxShadow: "var(--shadow-sm)", transition: "all var(--transition-base)", display: "flex", flexDirection: "column", borderLeft: "4px solid #6366f1" };
const btnSm: React.CSSProperties = { padding: "7px 14px", borderRadius: "var(--radius-md)", border: "none", cursor: "pointer", fontSize: "12px", fontWeight: 600, background: "var(--color-bg)", color: "var(--color-text-secondary)" };
