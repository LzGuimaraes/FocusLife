import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { toast } from "sonner";
import { TrendingUp, Receipt, Pencil, Trash2, FolderOpen, DollarSign, Briefcase, Copy } from "lucide-react";
import api from "../api/api";
import Layout from "../components/Layout";
import Modal from "../components/Modal";
import { Input, Select } from "../components/Form";
import { Button } from "../components/Shared";
import { Card, StatCard, Badge } from "../components/Shared";
import { PageHeader, CardGrid, EmptyState, Spinner } from "../components/UI";

type TipoCarteira = "INVESTIMENTO" | "DESPESAS";
// Após a divisão, cada carteira vive em uma tabela própria (carteira_investimento /
// carteira_dividas). O "tipo" agora é derivado da lista/endpoint — o campo
// tipo_carteira não existe mais na resposta.
interface Financa { id: number; nome: string; moeda: string; tipo: TipoCarteira; }
interface FormData { nome: string; moeda: string; tipo: TipoCarteira; }
const moedaS: Record<string, string> = { BRL: "R$", USD: "$", EUR: "€", GBP: "£", JPY: "¥" };
type Errors = Partial<Record<keyof FormData, string>>;

const tipoInfo: Record<TipoCarteira, { icon: typeof TrendingUp; label: string; color: string; bg: string; desc: string }> = {
  INVESTIMENTO: { icon: TrendingUp, label: "Investimentos", color: "#6366f1", bg: "#eef2ff", desc: "Renda Fixa, Ações, FIIs, ETFs, Cripto" },
  DESPESAS: { icon: Receipt, label: "Despesas", color: "#ef4444", bg: "#fef2f2", desc: "Contas e gastos mensais" },
};

// A rota de detalhe agora inclui o tipo: os IDs das duas tabelas podem colidir
// (ambas começam em 1), então não dá para identificar só pelo id.
const rotaDetalhe = (tipo: TipoCarteira, id: number) =>
  tipo === "INVESTIMENTO" ? `/financas/carteiras/investimento/${id}` : `/financas/carteiras/dividas/${id}`;

const endpointBase = (tipo: TipoCarteira) =>
  tipo === "INVESTIMENTO" ? "/carteiras-investimento" : "/carteiras-dividas";

export default function Financas() {
  const navigate = useNavigate();
  const [financas, setFinancas] = useState<Financa[]>([]);
  const [loading, setLoading] = useState(true);
  const [showModal, setShowModal] = useState(false);
  const [editing, setEditing] = useState<Financa | null>(null);
  const [form, setForm] = useState<FormData>({ nome: "", moeda: "BRL", tipo: "DESPESAS" });
  const [errors, setErrors] = useState<Errors>({});

  useEffect(() => { fetchFinancas(); }, []);
  const fetchFinancas = async () => {
    setLoading(true);
    try {
      const [inv, div] = await Promise.all([
        api.get("/carteiras-investimento/all?page=0&size=100"),
        api.get("/carteiras-dividas/all?page=0&size=100"),
      ]);
      const invest: Financa[] = (inv.data.content ?? []).map((c: any) => ({ id: c.id, nome: c.nome, moeda: c.moeda, tipo: "INVESTIMENTO" }));
      const despesas: Financa[] = (div.data.content ?? []).map((c: any) => ({ id: c.id, nome: c.nome, moeda: c.moeda, tipo: "DESPESAS" }));
      setFinancas([...invest, ...despesas]);
    } catch { toast.error("Erro ao carregar"); }
    finally { setLoading(false); }
  };

  const validate = (): boolean => { const e: Errors = {}; if (!form.nome.trim()) e.nome = "O nome é obrigatório."; setErrors(e); return Object.keys(e).length === 0; };

  const handleSubmit = async () => {
    if (!validate()) return;
    const p = { nome: form.nome, moeda: form.moeda.toUpperCase() };
    const base = endpointBase(form.tipo);
    const promise = editing
      ? api.put(`${base}/alter/${editing.id}`, p)
      : api.post(`${base}/create`, p);
    toast.promise(promise, { loading: "Salvando...", success: () => { closeModal(); fetchFinancas(); return editing ? "Atualizada!" : "Criada!"; }, error: "Erro" });
  };

  const handleDelete = async (f: Financa) => {
    toast("Excluir esta carteira?", { action: { label: "Sim", onClick: () => { toast.promise(api.delete(`${endpointBase(f.tipo)}/delete/${f.id}`), { loading: "Excluindo...", success: () => { fetchFinancas(); return "Excluída!"; }, error: "Erro" }); }}, cancel: { label: "Cancelar", onClick: () => {} } });
  };

  const handleDuplicate = async (f: Financa) => {
    toast.promise(api.post(`${endpointBase(f.tipo)}/duplicar/${f.id}`), {
      loading: "Duplicando carteira...",
      success: () => { fetchFinancas(); return `Cópia de "${f.nome}" criada!`; },
      error: "Erro ao duplicar",
    });
  };

  const openModal = (f: Financa | null = null) => { setErrors({}); if (f) { setEditing(f); setForm({ nome: f.nome, moeda: f.moeda, tipo: f.tipo }); } else { setEditing(null); setForm({ nome: "", moeda: "BRL", tipo: "DESPESAS" }); } setShowModal(true); };
  const closeModal = () => { setShowModal(false); setEditing(null); };

  const invest = financas.filter(f => f.tipo === "INVESTIMENTO");
  const despesas = financas.filter(f => f.tipo === "DESPESAS");

  return (
    <Layout>
      <PageHeader icon="💰" title="Carteiras" subtitle="Gerencie suas carteiras de investimentos e despesas" actionLabel="Nova Carteira" onAction={() => openModal()} />

      {/* ── Stats Row ── */}
      {!loading && (
        <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(min(100%, 190px), 1fr))", gap: "12px", marginBottom: "24px" }} className="animate-fade-in">
          <StatCard icon={<TrendingUp size={18} />} label="Investimentos" value={invest.length.toString()} color="#6366f1" />
          <StatCard icon={<Receipt size={18} />} label="Despesas" value={despesas.length.toString()} color="#ef4444" />
          <StatCard icon={<Briefcase size={18} />} label="Total" value={financas.length.toString()} color="#10b981" />
        </div>
      )}

      {loading ? <Spinner text="Carregando carteiras..." /> : financas.length === 0 ? (
        <EmptyState icon="💼" title="Nenhuma carteira" text="Crie sua primeira carteira!" actionLabel="Criar Carteira" onAction={() => openModal()} />
      ) : (
        <CardGrid>
          {financas.map(f => {
            const ti = tipoInfo[f.tipo];
            const Icon = ti.icon;
            return (
              <Card key={`${f.tipo}:${f.id}`} accent={ti.color} onClick={() => navigate(rotaDetalhe(f.tipo, f.id))}>
                <div style={{ display: "flex", justifyContent: "space-between", alignItems: "start", marginBottom: "14px" }}>
                  <Badge color={ti.color} bg={ti.bg}>
                    <Icon size={13} /> {ti.label}
                  </Badge>
                  <Badge color="#059669" bg="#ecfdf5">
                    <DollarSign size={12} /> {moedaS[f.moeda] || f.moeda}
                  </Badge>
                </div>
                <h3 style={{ fontSize: "17px", fontWeight: 700, color: "#0f172a", marginBottom: "4px" }}>{f.nome}</h3>
                <p style={{ fontSize: "12px", color: "#94a3b8", marginBottom: "18px" }}>{ti.desc}</p>
                <div style={{ display: "flex", gap: "8px", marginTop: "auto" }}
                  onClick={e => e.stopPropagation()}>
                  <Button variant="secondary" size="sm" onClick={() => navigate(rotaDetalhe(f.tipo, f.id))}>
                    <FolderOpen size={13} /> Abrir
                  </Button>
                  <Button variant="ghost" size="sm" onClick={() => openModal(f)}>
                    <Pencil size={13} />
                  </Button>
                  <Button variant="ghost" size="sm" onClick={() => handleDuplicate(f)}
                    title="Duplicar carteira" aria-label={`Duplicar ${f.nome}`}>
                    <Copy size={13} />
                  </Button>
                  <Button variant="ghost" size="sm" onClick={() => handleDelete(f)}
                    style={{ color: "#ef4444" }}>
                    <Trash2 size={13} />
                  </Button>
                </div>
              </Card>
            );
          })}
        </CardGrid>
      )}

      {/* ── Modal ── */}
      <Modal open={showModal} onClose={closeModal} title={editing ? "Editar Carteira" : "Nova Carteira"} onSubmit={handleSubmit} submitLabel="Salvar">
        <Input label="Nome da Carteira" value={form.nome} onChange={e => setForm({ ...form, nome: e.target.value })} placeholder="Ex: Carteira Principal" error={errors.nome} />
        <Select label="Tipo da Carteira" value={form.tipo} onChange={e => setForm({ ...form, tipo: e.target.value as TipoCarteira })}>
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
