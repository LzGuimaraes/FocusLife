import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { toast } from "sonner";
import api from "../api/api";
import Layout from "../components/Layout";
import Modal from "../components/Modal";
import { Input, Select } from "../components/Form";
import { PageHeader, CardGrid, EmptyState, Spinner } from "../components/UI";

type TipoCarteira = "INVESTIMENTO" | "DESPESAS";
interface Financa { id: number; nome: string; moeda: string; tipoCarteira: TipoCarteira; }
interface FormData { nome: string; moeda: string; tipoCarteira: TipoCarteira; }
const moedaS: Record<string, string> = { BRL: "R$", USD: "$", EUR: "€", GBP: "£", JPY: "¥" };
type Errors = Partial<Record<keyof FormData, string>>;

const tipoInfo: Record<TipoCarteira, { icon: string; label: string; color: string; bg: string; desc: string; btnColor: string }> = {
  INVESTIMENTO: { icon: "📈", label: "Investimentos", color: "#8b5cf6", bg: "#f5f3ff", desc: "Renda Fixa, Ações, FIIs, ETFs, Cripto", btnColor: "#8b5cf6" },
  DESPESAS: { icon: "📋", label: "Despesas", color: "#ef4444", bg: "#fef2f2", desc: "Contas e gastos mensais", btnColor: "#ef4444" },
};

export default function Financas() {
  const navigate = useNavigate();
  const [financas, setFinancas] = useState<Financa[]>([]);
  const [loading, setLoading] = useState(true);
  const [showModal, setShowModal] = useState(false);
  const [editing, setEditing] = useState<Financa | null>(null);
  const [form, setForm] = useState<FormData>({ nome: "", moeda: "BRL", tipoCarteira: "DESPESAS" });
  const [errors, setErrors] = useState<Errors>({});

  useEffect(() => { fetchFinancas(); }, []);
  const fetchFinancas = async () => { setLoading(true); try { const r = await api.get("/financas/all?page=0&size=100"); setFinancas(r.data.content); } catch { toast.error("Erro ao carregar"); } finally { setLoading(false); } };

  const validate = (): boolean => { const e: Errors = {}; if (!form.nome.trim()) e.nome = "O nome é obrigatório."; setErrors(e); return Object.keys(e).length === 0; };

  const handleSubmit = async () => { if (!validate()) return; const p = { nome: form.nome, moeda: form.moeda.toUpperCase(), tipoCarteira: form.tipoCarteira }; const promise = editing ? api.put(`/financas/alter/${editing.id}`, p) : api.post("/financas/create", p); toast.promise(promise, { loading: "Salvando...", success: () => { closeModal(); fetchFinancas(); return editing ? "Atualizada!" : "Criada!"; }, error: "Erro" }); };

  const handleDelete = async (id: number) => { toast("Excluir esta carteira?", { action: { label: "Sim", onClick: () => { toast.promise(api.delete(`/financas/delete/${id}`), { loading: "Excluindo...", success: () => { fetchFinancas(); return "Excluída!"; }, error: "Erro" }); }}, cancel: { label: "Cancelar", onClick: () => {} } }); };

  const openModal = (f: Financa | null = null) => { setErrors({}); if (f) { setEditing(f); setForm({ nome: f.nome, moeda: f.moeda, tipoCarteira: f.tipoCarteira }); } else { setEditing(null); setForm({ nome: "", moeda: "BRL", tipoCarteira: "DESPESAS" }); } setShowModal(true); };
  const closeModal = () => { setShowModal(false); setEditing(null); };

  const invest = financas.filter(f => f.tipoCarteira === "INVESTIMENTO");
  const despesas = financas.filter(f => f.tipoCarteira === "DESPESAS");

  return (
    <Layout>
      <PageHeader icon="💰" title="Carteiras" subtitle="Gerencie suas carteiras de investimentos e despesas" actionLabel="Nova Carteira" onAction={() => openModal()} />

      {!loading && (
        <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(min(100%, 220px), 1fr))", gap: "clamp(8px, 1.5vw, 14px)", marginBottom: "20px" }} className="animate-fade-in">
          {[{ icon: "📈", label: "Investimentos", value: invest.length, color: "#8b5cf6", bg: "#f5f3ff" },{ icon: "📋", label: "Despesas", value: despesas.length, color: "#ef4444", bg: "#fef2f2" },{ icon: "💼", label: "Total", value: financas.length, color: "#6366f1", bg: "#eef2ff" }].map((s, i) => (
            <div key={i} style={{ background: "white", borderRadius: "12px", padding: "clamp(14px, 2vw, 18px)", boxShadow: "0 1px 3px rgba(0,0,0,0.06)", display: "flex", flexDirection: "column", gap: "4px", borderLeft: `4px solid ${s.color}` }}>
              <div style={{ display: "flex", alignItems: "center", gap: "8px" }}><span style={{ fontSize: "18px" }}>{s.icon}</span><span style={{ fontSize: "12px", fontWeight: 600, color: "#64748b" }}>{s.label}</span></div>
              <span style={{ fontSize: "22px", fontWeight: 800, color: s.color }}>{s.value}</span>
            </div>
          ))}
        </div>
      )}

      {loading ? <Spinner text="Carregando carteiras..." /> : financas.length === 0 ? <EmptyState icon="💼" title="Nenhuma carteira" text="Crie sua primeira carteira!" actionLabel="Criar Carteira" onAction={() => openModal()} /> :
        <CardGrid>{financas.map(f => { const ti = tipoInfo[f.tipoCarteira]; return (
          <div key={f.id} style={{ ...cardStyle, borderLeftColor: ti.color, cursor: "pointer" }}
            onClick={() => navigate(`/financas/carteiras/${f.id}`)}
            onMouseEnter={e => { e.currentTarget.style.transform = "translateY(-4px)"; e.currentTarget.style.boxShadow = "0 12px 30px rgba(0,0,0,0.12)"; }}
            onMouseLeave={e => { e.currentTarget.style.transform = "translateY(0)"; e.currentTarget.style.boxShadow = "0 1px 2px rgba(0,0,0,0.05)"; }}>
            <div style={{ display: "flex", justifyContent: "space-between", alignItems: "start", marginBottom: "12px", gap: "6px" }}>
              <span style={{ fontSize: "12px", fontWeight: 700, padding: "5px 12px", borderRadius: "var(--radius-full)", background: ti.bg, color: ti.color }}>{ti.icon} {ti.label}</span>
              <span style={{ fontSize: "11px", fontWeight: 600, background: "#ecfdf5", color: "#10b981", padding: "4px 10px", borderRadius: "var(--radius-full)" }}>{moedaS[f.moeda] || f.moeda}</span>
            </div>
            <h3 style={{ fontSize: "18px", fontWeight: 700, marginBottom: "4px" }}>{f.nome}</h3>
            <p style={{ fontSize: "12px", color: "#94a3b8", marginBottom: "16px" }}>{ti.desc}</p>
            <div style={{ display: "flex", gap: "8px", marginTop: "auto", flexWrap: "wrap" }}
              onClick={e => e.stopPropagation()}>
              <button onClick={() => navigate(`/financas/carteiras/${f.id}`)} style={{ ...btnSm, background: ti.bg, color: ti.color, fontWeight: 600 }}>📂 Abrir</button>
              <button onClick={() => openModal(f)} style={btnSm}>✏️ Editar</button>
              <button onClick={() => handleDelete(f.id)} style={{ ...btnSm, background: "#fee2e2", color: "#ef4444" }}>🗑</button>
            </div>
          </div>
        );})}</CardGrid>
      }

      <Modal open={showModal} onClose={closeModal} title={editing ? "Editar Carteira" : "Nova Carteira"} onSubmit={handleSubmit} submitLabel="Salvar">
        <Input label="Nome da Carteira" value={form.nome} onChange={e => setForm({ ...form, nome: e.target.value })} placeholder="Ex: Carteira Principal" error={errors.nome} />
        <Select label="Tipo da Carteira" value={form.tipoCarteira} onChange={e => setForm({ ...form, tipoCarteira: e.target.value as TipoCarteira })}>
          <option value="DESPESAS">📋 Despesas (Contas mensais)</option>
          <option value="INVESTIMENTO">📈 Investimentos (Renda Fixa/Variável)</option>
        </Select>
        <Select label="Moeda" value={form.moeda} onChange={e => setForm({ ...form, moeda: e.target.value })}>
          <option value="BRL">R$ Real (BRL)</option><option value="USD">$ Dólar (USD)</option><option value="EUR">€ Euro (EUR)</option><option value="GBP">£ Libra (GBP)</option><option value="JPY">¥ Iene (JPY)</option>
        </Select>
      </Modal>
    </Layout>
  );
}

const cardStyle: React.CSSProperties = { background: "white", borderRadius: "14px", padding: "24px", boxShadow: "0 1px 3px rgba(0,0,0,0.06)", transition: "all 0.25s ease", display: "flex", flexDirection: "column", borderLeft: "5px solid #10b981" };
const btnSm: React.CSSProperties = { padding: "7px 14px", borderRadius: "8px", border: "none", cursor: "pointer", fontSize: "12px", fontWeight: 600, background: "#f1f5f9", color: "#64748b", transition: "all 0.15s ease" };
