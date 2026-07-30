import { useEffect, useState } from "react";
import { toast } from "sonner";
import api from "../api/api";
import Layout from "../components/Layout";
import Modal from "../components/Modal";
import { Input, Select, NumberInput, DateInput } from "../components/Form";
import { PageHeader, CardGrid, EmptyState, Spinner } from "../components/UI";

type Categoria = "CONTA" | "INVESTIMENTO";
type CatInvest = "RENDA_FIXA" | "TESOURO_DIRETO" | "ACOES" | "FIIS" | "ETFS" | "CRIPTOMOEDAS";
type TipoCarteira = "INVESTIMENTO" | "DESPESAS";

interface Ativo {
  id: number; nome: string; categoria: Categoria;
  categoriaInvestimento: CatInvest | null; quantidade: number | null;
  valorUnitario: number | null; precoAtual: number | null; saldo: number;
  instituicao: string | null; dataAplicacao: string | null;
  vencimento: string | null; rentabilidade: number | null; pago: boolean | null;
  financas_id: number;
}
interface Financa { id: number; nome: string; moeda: string; tipoCarteira: TipoCarteira; }
interface FormData {
  nome: string; categoria: Categoria; categoriaInvestimento: CatInvest | "";
  quantidade: string; valorUnitario: string; precoAtual: string; saldo: string;
  instituicao: string; dataAplicacao: string; vencimento: string; rentabilidade: string;
  financas_id: string; pago: boolean;
}
const moedaS: Record<string, string> = { BRL: "R$", USD: "$", EUR: "€", GBP: "£", JPY: "¥" };
const fmt = (v: number | undefined | null, m: string) => v != null ? `${moedaS[m] || m} ${v.toFixed(2)}` : "-";
const pct = (parte: number, total: number) => total > 0 ? ((parte / total) * 100).toFixed(1) : "0.0";

const catInfo: Record<CatInvest, { icon: string; label: string; color: string; bg: string; autoCalc: boolean }> = {
  RENDA_FIXA:       { icon: "📊", label: "Renda Fixa",       color: "#3b82f6", bg: "#dbeafe", autoCalc: false },
  TESOURO_DIRETO:   { icon: "🏛️", label: "Tesouro Direto",   color: "#10b981", bg: "#d1fae5", autoCalc: false },
  ACOES:            { icon: "📈", label: "Ações",            color: "#6366f1", bg: "#eef2ff", autoCalc: true },
  FIIS:             { icon: "🏢", label: "FIIs",             color: "#8b5cf6", bg: "#ede9fe", autoCalc: true },
  ETFS:             { icon: "📦", label: "ETFs",             color: "#06b6d4", bg: "#ecfeff", autoCalc: true },
  CRIPTOMOEDAS:     { icon: "₿",  label: "Criptomoedas",     color: "#f59e0b", bg: "#fef3c7", autoCalc: true },
};

const emptyForm: FormData = { nome: "", categoria: "INVESTIMENTO", categoriaInvestimento: "", quantidade: "", valorUnitario: "", precoAtual: "", saldo: "0", instituicao: "", dataAplicacao: "", vencimento: "", rentabilidade: "", financas_id: "", pago: false };
type Errors = Partial<Record<keyof FormData, string>>;

export default function Contas() {
  const [ativos, setAtivos] = useState<Ativo[]>([]);
  const [financas, setFinancas] = useState<Financa[]>([]);
  const [loading, setLoading] = useState(true);
  const [showModal, setShowModal] = useState(false);
  const [editing, setEditing] = useState<Ativo | null>(null);
  
  
  const [filterFinanca, setFilterFinanca] = useState("all");
  const [form, setForm] = useState<FormData>(emptyForm);
  const [errors, setErrors] = useState<Errors>({});

  useEffect(() => { api.get("/financas/all?page=0&size=100").then(r => setFinancas(r.data.content)).catch(() => {}); }, []);
  useEffect(() => { fetchAtivos(); }, [filterFinanca]);

  const fetchAtivos = async () => { setLoading(true); try { let r; if (filterFinanca === "all") { r = await api.get(`/contas/all?page=0&size=200`); setAtivos(r.data.content); } else { r = await api.get(`/contas/by-financa/${filterFinanca}`); setAtivos(r.data); } } catch { toast.error("Erro ao carregar"); } finally { setLoading(false); } };

  const selectedFinanca = filterFinanca !== "all" ? financas.find(f => f.id.toString() === filterFinanca) : null;
  const walletType = selectedFinanca?.tipoCarteira;

  const validate = (): boolean => {
    const e: Errors = {};
    if (!form.nome.trim()) e.nome = "O nome é obrigatório.";
    if (!form.financas_id) e.financas_id = "Selecione uma carteira.";
    if (form.categoria === "INVESTIMENTO" && !form.categoriaInvestimento) e.categoriaInvestimento = "Selecione a categoria.";
    const ci = catInfo[form.categoriaInvestimento as CatInvest];
    if (ci?.autoCalc) {
      const vu = parseFloat(form.valorUnitario); const q = parseInt(form.quantidade);
      if (!form.valorUnitario || isNaN(vu) || vu <= 0) e.valorUnitario = "Informe o preço médio.";
      if (!form.quantidade || isNaN(q) || q <= 0) e.quantidade = "Informe a quantidade.";
    } else if (form.categoria === "INVESTIMENTO") {
      const s = parseFloat(form.saldo);
      if (form.saldo === "" || isNaN(s) || s < 0) e.saldo = "Valor inválido.";
    }
    if (form.categoria === "CONTA") { const s = parseFloat(form.saldo); if (form.saldo === "" || isNaN(s) || s < 0) e.saldo = "Saldo inválido."; }
    setErrors(e);
    return Object.keys(e).length === 0;
  };

  const handleSubmit = async () => {
    if (!validate()) return;
    const payload: any = { nome: form.nome, categoria: form.categoria, financas_id: parseInt(form.financas_id) };
    if (form.categoria === "INVESTIMENTO") {
      payload.categoriaInvestimento = form.categoriaInvestimento;
      payload.instituicao = form.instituicao || null;
      payload.dataAplicacao = form.dataAplicacao || null;
      payload.vencimento = form.vencimento || null;
      payload.rentabilidade = form.rentabilidade ? parseFloat(form.rentabilidade) : null;
      payload.precoAtual = form.precoAtual ? parseFloat(form.precoAtual) : null;
      const ci = catInfo[form.categoriaInvestimento as CatInvest];
      if (ci?.autoCalc) {
        payload.valorUnitario = parseFloat(form.valorUnitario) || 0;
        payload.quantidade = parseInt(form.quantidade) || 0;
      } else {
        payload.saldo = parseFloat(form.saldo) || 0;
      }
    }
    if (form.categoria === "CONTA") { payload.saldo = parseFloat(form.saldo) || 0; payload.pago = form.pago; }
    const promise = editing ? api.put(`/contas/alter/${editing.id}`, payload) : api.post("/contas/create", payload);
    toast.promise(promise, { loading: "Salvando...", success: () => { closeModal(); fetchAtivos(); return editing ? "Atualizado!" : "Criado!"; }, error: (err: any) => err?.response?.data?.message || "Erro ao salvar" });
  };

  const handleDelete = async (id: number) => { toast("Excluir?", { action: { label: "Sim", onClick: () => { toast.promise(api.delete(`/contas/delete/${id}`), { loading: "Excluindo...", success: () => { fetchAtivos(); return "Excluído!"; }, error: "Erro" }); }}, cancel: { label: "Cancelar", onClick: () => {} } }); };

  const togglePago = async (ativo: Ativo) => {
    const novo = !ativo.pago;
    try { await api.put(`/contas/alter/${ativo.id}`, { ...ativo, pago: novo, financas_id: ativo.financas_id }); setAtivos(prev => prev.map(a => a.id === ativo.id ? { ...a, pago: novo } : a)); toast.success(novo ? "Pago!" : "Desmarcado"); }
    catch { toast.error("Erro"); }
  };

  const openModal = (a: Ativo | null = null) => {
    setErrors({});
    const defCat: Categoria = walletType === "INVESTIMENTO" ? "INVESTIMENTO" : "CONTA";
    if (a) { setEditing(a); setForm({ nome: a.nome, categoria: a.categoria, categoriaInvestimento: a.categoriaInvestimento || "", quantidade: a.quantidade?.toString() || "", valorUnitario: a.valorUnitario?.toString() || "", precoAtual: a.precoAtual?.toString() || "", saldo: a.saldo?.toString() || "0", instituicao: a.instituicao || "", dataAplicacao: a.dataAplicacao || "", vencimento: a.vencimento || "", rentabilidade: a.rentabilidade?.toString() || "", financas_id: a.financas_id.toString(), pago: a.pago ?? false }); }
    else { setEditing(null); setForm({ ...emptyForm, categoria: defCat, financas_id: filterFinanca !== "all" ? filterFinanca : (financas[0]?.id.toString() || "") }); }
    setShowModal(true);
  };
  const closeModal = () => { setShowModal(false); setEditing(null); };
  const getMoeda = (fid: number) => financas.find(f => f.id === fid)?.moeda || "BRL";

  /* ── Sumários ── */
  const contas = ativos.filter(a => a.categoria === "CONTA");
  const investimentos = ativos.filter(a => a.categoria === "INVESTIMENTO");
  const totalInvest = investimentos.reduce((s, a) => s + (a.saldo || 0), 0);
  const totalContas = contas.reduce((s, a) => s + (a.saldo || 0), 0);
  const contasPagas = contas.filter(a => a.pago).reduce((s, a) => s + (a.saldo || 0), 0);
  const contasNaoPagas = contas.filter(a => !a.pago).reduce((s, a) => s + (a.saldo || 0), 0);

  // Distribuição por categoria de investimento
  const distCategorias = (Object.keys(catInfo) as CatInvest[]).map(ci => {
    const items = investimentos.filter(a => a.categoriaInvestimento === ci);
    const total = items.reduce((s, a) => s + (a.saldo || 0), 0);
    return { key: ci, ...catInfo[ci], items, total };
  }).filter(d => d.items.length > 0);

  const summaryCards = walletType === "INVESTIMENTO"
    ? [{ icon: "💰", label: "Patrimônio Total", value: fmt(totalInvest, "BRL"), color: "#10b981", bg: "#ecfdf5" }, ...distCategorias.slice(0, 3).map(d => ({ icon: d.icon, label: d.label, value: fmt(d.total, "BRL"), color: d.color, bg: d.bg }))]
    : [{ icon: "📋", label: "Contas do Mês", value: fmt(totalContas, "BRL"), color: "#6366f1", bg: "#eef2ff" },{ icon: "✅", label: "Pagas", value: fmt(contasPagas, "BRL"), color: "#10b981", bg: "#d1fae5" },{ icon: "⏳", label: "Pendentes", value: fmt(contasNaoPagas, "BRL"), color: "#f59e0b", bg: "#fef3c7" }];

  return (
    <Layout>
      <PageHeader icon="💳" title={selectedFinanca ? `Ativos: ${selectedFinanca.nome}` : "Gestão de Ativos"} subtitle={walletType === "INVESTIMENTO" ? "Patrimônio e investimentos" : "Contas e despesas mensais"} actionLabel="Novo Ativo" onAction={() => openModal()} />

      {/* ── Sumário ── */}
      {!loading && ativos.length > 0 && (
        <div style={{ marginBottom: "20px" }} className="animate-fade-in">
          <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(min(100%, 190px), 1fr))", gap: "clamp(8px, 1.5vw, 12px)", marginBottom: walletType === "INVESTIMENTO" && distCategorias.length > 0 ? "16px" : "0" }}>
            {summaryCards.map((sc, i) => (
              <div key={i} style={{ background: "white", borderRadius: "12px", padding: "clamp(12px, 2vw, 16px)", boxShadow: "0 1px 3px rgba(0,0,0,0.06)", display: "flex", flexDirection: "column", gap: "4px", borderLeft: `4px solid ${sc.color}` }}>
                <div style={{ display: "flex", alignItems: "center", gap: "6px" }}><span style={{ fontSize: "16px" }}>{sc.icon}</span><span style={{ fontSize: "11px", fontWeight: 600, color: "#64748b" }}>{sc.label}</span></div>
                <span style={{ fontSize: "clamp(15px, 2.2vw, 18px)", fontWeight: 800, color: sc.color }}>{sc.value}</span>
              </div>
            ))}
          </div>

          {/* Distribuição % por categoria */}
          {walletType === "INVESTIMENTO" && distCategorias.length > 0 && (
            <div style={{ background: "white", borderRadius: "12px", padding: "clamp(14px, 2vw, 18px)", boxShadow: "0 1px 3px rgba(0,0,0,0.06)" }}>
              <h3 style={{ fontSize: "14px", fontWeight: 700, color: "#0f172a", marginBottom: "12px" }}>📊 Distribuição do Patrimônio</h3>
              <div style={{ display: "flex", flexDirection: "column", gap: "8px" }}>
                {distCategorias.map(d => (
                  <div key={d.key} style={{ display: "flex", alignItems: "center", gap: "10px" }}>
                    <span style={{ fontSize: "13px", fontWeight: 600, color: d.color, minWidth: "110px" }}>{d.icon} {d.label}</span>
                    <div style={{ flex: 1, height: "10px", background: "#f1f5f9", borderRadius: "5px", overflow: "hidden" }}>
                      <div style={{ height: "100%", width: `${Math.min(100, parseFloat(pct(d.total, totalInvest)))}%`, background: d.color, borderRadius: "5px", transition: "width 0.5s ease" }} />
                    </div>
                    <span style={{ fontSize: "12px", fontWeight: 700, color: "#0f172a", minWidth: "65px", textAlign: "right" }}>{pct(d.total, totalInvest)}%</span>
                    <span style={{ fontSize: "12px", color: "#64748b", minWidth: "80px", textAlign: "right" }}>{fmt(d.total, "BRL")}</span>
                  </div>
                ))}
              </div>
            </div>
          )}
        </div>
      )}

      {/* Filtro */}
      <div style={{ display: "flex", gap: "8px", flexWrap: "wrap", marginBottom: "20px" }}>
        <button onClick={() => setFilterFinanca("all")} style={fb(filterFinanca === "all")}>Todas</button>
        {financas.map(f => (<button key={f.id} onClick={() => setFilterFinanca(f.id.toString())} style={fb(filterFinanca === f.id.toString())}>{f.tipoCarteira === "INVESTIMENTO" ? "📈" : "📋"} {f.nome}</button>))}
      </div>

      {loading ? <Spinner text="Carregando..." /> : ativos.length === 0 ? <EmptyState icon="💳" title="Nenhum ativo" text="Crie seu primeiro ativo!" actionLabel="Criar Ativo" onAction={() => openModal()} /> :
        <CardGrid>{ativos.map(a => {
          const moeda = getMoeda(a.financas_id);
          const ci = catInfo[a.categoriaInvestimento as CatInvest];
          const isConta = a.categoria === "CONTA";
          const isInvest = a.categoria === "INVESTIMENTO";
          return (
            <div key={a.id} style={{ ...cardStyle, borderLeftColor: isConta ? (a.pago ? "#10b981" : "#f59e0b") : (ci?.color || "#8b5cf6") }}
              onMouseEnter={e => { e.currentTarget.style.transform = "translateY(-3px)"; e.currentTarget.style.boxShadow = "0 10px 25px rgba(0,0,0,0.1)"; }}
              onMouseLeave={e => { e.currentTarget.style.transform = "translateY(0)"; e.currentTarget.style.boxShadow = "0 1px 2px rgba(0,0,0,0.05)"; }}>
              <div style={{ display: "flex", justifyContent: "space-between", alignItems: "start", marginBottom: "8px", gap: "6px", flexWrap: "wrap" }}>
                {isInvest && ci && <span style={{ fontSize: "11px", fontWeight: 600, padding: "4px 10px", borderRadius: "var(--radius-full)", background: ci.bg, color: ci.color }}>{ci.icon} {ci.label}</span>}
                {isConta && <span style={{ fontSize: "12px", fontWeight: 600, padding: "4px 10px", borderRadius: "var(--radius-full)", background: "#dbeafe", color: "#1d4ed8" }}>💳 Conta</span>}
                {isConta && (<span style={{ fontSize: "11px", fontWeight: 700, padding: "4px 10px", borderRadius: "var(--radius-full)", background: a.pago ? "#d1fae5" : "#fef3c7", color: a.pago ? "#047857" : "#b45309" }}>{a.pago ? "✅ Pago" : "⏳ Pendente"}</span>)}
              </div>
              <h3 style={{ fontSize: "16px", fontWeight: 600, marginBottom: "6px" }}>{a.nome}</h3>
              {isInvest && a.quantidade != null && a.valorUnitario != null && (
                <div style={{ display: "flex", gap: "10px", flexWrap: "wrap", marginBottom: "6px" }}>
                  <span style={{ fontSize: "11px", color: "#64748b" }}>📦 {a.quantidade} un.</span>
                  <span style={{ fontSize: "11px", color: "#64748b" }}>💵 {fmt(a.valorUnitario, moeda)}/un.</span>
                </div>
              )}
              {isInvest && a.precoAtual != null && (
                <div style={{ marginBottom: "6px" }}><span style={{ fontSize: "11px", fontWeight: 600, color: a.precoAtual >= (a.valorUnitario || 0) ? "#10b981" : "#ef4444" }}>📊 Atual: {fmt(a.precoAtual, moeda)}</span></div>
              )}
              <p style={{ fontSize: "20px", fontWeight: 800, color: isConta ? (a.pago ? "#10b981" : "#f59e0b") : "#10b981", marginBottom: "14px" }}>{fmt(a.saldo, moeda)}</p>
              <div style={{ display: "flex", gap: "8px", marginTop: "auto", flexWrap: "wrap" }}>
                {isConta && (<button onClick={() => togglePago(a)} style={{ ...btnSm, background: a.pago ? "#fef3c7" : "#d1fae5", color: a.pago ? "#b45309" : "#047857", fontWeight: 700 }}>{a.pago ? "↩ Desmarcar" : "✓ Pagar"}</button>)}
                <button onClick={() => openModal(a)} style={btnSm}>✏️ Editar</button>
                <button onClick={() => handleDelete(a.id)} style={{ ...btnSm, background: "#fee2e2", color: "#ef4444" }}>🗑</button>
              </div>
            </div>
          );
        })}</CardGrid>
      }

      {/* ── Modal ── */}
      <Modal open={showModal} onClose={closeModal} title={editing ? "Editar Ativo" : "Novo Ativo"} onSubmit={handleSubmit} submitLabel="Salvar" width="540px">
        <Input label="Nome / Ticker / Ativo" value={form.nome} onChange={e => setForm({ ...form, nome: e.target.value })} placeholder={walletType === "INVESTIMENTO" ? "Ex: PETR4, BTC, Tesouro Selic..." : "Ex: Aluguel, Internet..."} error={errors.nome} />

        {/* Categoria travada */}
        {walletType ? (
          <div style={{ padding: "10px 14px", background: "#f8fafc", borderRadius: "10px", border: "1.5px solid #e2e8f0" }}>
            <span style={{ fontSize: "13px", fontWeight: 600, color: "#374151" }}>Categoria: </span>
            <span style={{ fontSize: "13px", fontWeight: 700, color: walletType === "INVESTIMENTO" ? "#8b5cf6" : "#6366f1" }}>{walletType === "INVESTIMENTO" ? "📈 Investimento" : "💳 Conta"}</span>
          </div>
        ) : (
          <Select label="Categoria" value={form.categoria} onChange={e => { const cat = e.target.value as Categoria; setForm({ ...form, categoria: cat, categoriaInvestimento: cat === "CONTA" ? "" : form.categoriaInvestimento, quantidade: "", valorUnitario: "", precoAtual: "", instituicao: "", dataAplicacao: "", vencimento: "", rentabilidade: "" }); }}>
            <option value="CONTA">💳 Conta</option><option value="INVESTIMENTO">📈 Investimento</option>
          </Select>
        )}

        {/* Pago (CONTA) */}
        {(walletType === "DESPESAS" || (!walletType && form.categoria === "CONTA")) && (
          <div style={{ display: "flex", alignItems: "center", gap: "10px", padding: "10px 14px", background: form.pago ? "#d1fae5" : "#fef3c7", borderRadius: "10px", border: `1.5px solid ${form.pago ? "#10b981" : "#f59e0b"}` }}>
            <input type="checkbox" id="pago-check" checked={form.pago} onChange={e => setForm({ ...form, pago: e.target.checked })} style={{ width: "18px", height: "18px", accentColor: "#10b981", cursor: "pointer" }} />
            <label htmlFor="pago-check" style={{ fontSize: "14px", fontWeight: 600, color: form.pago ? "#047857" : "#b45309", cursor: "pointer" }}>{form.pago ? "✅ Pago" : "⏳ Pendente"}</label>
          </div>
        )}

        {/* Categoria de Investimento */}
        {(walletType === "INVESTIMENTO" || (!walletType && form.categoria === "INVESTIMENTO")) && (
          <Select label="Categoria de Investimento" value={form.categoriaInvestimento} onChange={e => { setForm({ ...form, categoriaInvestimento: e.target.value as CatInvest | "", quantidade: "", valorUnitario: "", precoAtual: "", saldo: "0", instituicao: "", dataAplicacao: "", vencimento: "", rentabilidade: "" }); }} error={errors.categoriaInvestimento}>
            <option value="">Selecione...</option>
            {(Object.keys(catInfo) as CatInvest[]).map(ci => <option key={ci} value={ci}>{catInfo[ci].icon} {catInfo[ci].label}</option>)}
          </Select>
        )}

        {/* Campos específicos por categoria de investimento */}
        {form.categoriaInvestimento && (() => {
          const ci = catInfo[form.categoriaInvestimento as CatInvest];
          if (!ci) return null;
          return (
            <>
              {/* Renda Fixa: campos específicos */}
              {form.categoriaInvestimento === "RENDA_FIXA" && (
                <>
                  <Input label="Instituição Financeira" value={form.instituicao} onChange={e => setForm({ ...form, instituicao: e.target.value })} placeholder="Ex: Banco do Brasil, XP..." />
                  <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "14px" }}>
                    <DateInput label="Data da Aplicação" value={form.dataAplicacao} onChange={e => setForm({ ...form, dataAplicacao: e.target.value })} />
                    <NumberInput label="Rentabilidade (%)" decimal value={form.rentabilidade} onChange={v => setForm({ ...form, rentabilidade: v })} placeholder="Ex: 12.5" />
                  </div>
                  <DateInput label="Vencimento (opcional)" value={form.vencimento} onChange={e => setForm({ ...form, vencimento: e.target.value })} />
                  <NumberInput label="Valor Aplicado" decimal value={form.saldo} onChange={v => setForm({ ...form, saldo: v })} error={errors.saldo} placeholder="1000.00" />
                </>
              )}

              {/* Tesouro Direto */}
              {form.categoriaInvestimento === "TESOURO_DIRETO" && (
                <>
                  <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "14px" }}>
                    <DateInput label="Data da Compra" value={form.dataAplicacao} onChange={e => setForm({ ...form, dataAplicacao: e.target.value })} />
                    <DateInput label="Vencimento" value={form.vencimento} onChange={e => setForm({ ...form, vencimento: e.target.value })} />
                  </div>
                  <NumberInput label="Quantidade de Títulos (opcional)" value={form.quantidade} onChange={v => setForm({ ...form, quantidade: v })} placeholder="Ex: 5" />
                  <NumberInput label="Valor Investido" decimal value={form.saldo} onChange={v => setForm({ ...form, saldo: v })} error={errors.saldo} placeholder="1000.00" />
                </>
              )}

              {/* Ações, FIIs, ETFs: preço médio × quantidade */}
              {(form.categoriaInvestimento === "ACOES" || form.categoriaInvestimento === "FIIS" || form.categoriaInvestimento === "ETFS") && (
                <>
                  <Input label={form.categoriaInvestimento === "ACOES" ? "Nome da Empresa (opcional)" : "Nome do Fundo (opcional)"} value={form.instituicao} onChange={e => setForm({ ...form, instituicao: e.target.value })} placeholder="Opcional" />
                  <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "14px" }}>
                    <NumberInput label="Preço Médio" decimal value={form.valorUnitario} onChange={v => setForm({ ...form, valorUnitario: v })} error={errors.valorUnitario} placeholder="Ex: 35.50" />
                    <NumberInput label="Quantidade" value={form.quantidade} onChange={v => setForm({ ...form, quantidade: v })} error={errors.quantidade} placeholder="Ex: 100" />
                  </div>
                  <NumberInput label="Preço Atual (opcional)" decimal value={form.precoAtual} onChange={v => setForm({ ...form, precoAtual: v })} placeholder="Ex: 38.20" />
                  {form.valorUnitario && form.quantidade && (
                    <div style={{ padding: "10px 14px", background: "#ecfdf5", borderRadius: "10px", border: "1.5px solid #10b981", display: "flex", justifyContent: "space-between", alignItems: "center" }}>
                      <span style={{ fontSize: "13px", fontWeight: 600, color: "#047857" }}>💰 Valor da Posição</span>
                      <span style={{ fontSize: "18px", fontWeight: 800, color: "#10b981" }}>{fmt(parseFloat(form.valorUnitario) * parseInt(form.quantidade || "0"), "BRL")}</span>
                    </div>
                  )}
                </>
              )}

              {/* Criptomoedas */}
              {form.categoriaInvestimento === "CRIPTOMOEDAS" && (
                <>
                  <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "14px" }}>
                    <NumberInput label="Preço Médio" decimal value={form.valorUnitario} onChange={v => setForm({ ...form, valorUnitario: v })} error={errors.valorUnitario} placeholder="Ex: 350000.00" />
                    <NumberInput label="Quantidade" highPrecision value={form.quantidade} onChange={v => setForm({ ...form, quantidade: v })} error={errors.quantidade} placeholder="Ex: 0.05" />
                  </div>
                  <NumberInput label="Preço Atual (opcional)" decimal value={form.precoAtual} onChange={v => setForm({ ...form, precoAtual: v })} placeholder="Ex: 365000.00" />
                  {form.valorUnitario && form.quantidade && (
                    <div style={{ padding: "10px 14px", background: "#ecfdf5", borderRadius: "10px", border: "1.5px solid #10b981", display: "flex", justifyContent: "space-between", alignItems: "center" }}>
                      <span style={{ fontSize: "13px", fontWeight: 600, color: "#047857" }}>💰 Valor da Posição</span>
                      <span style={{ fontSize: "18px", fontWeight: 800, color: "#10b981" }}>{fmt(parseFloat(form.valorUnitario) * parseFloat(form.quantidade || "0"), "BRL")}</span>
                    </div>
                  )}
                </>
              )}
            </>
          );
        })()}

        {/* Conta: saldo + carteira */}
        {(walletType === "DESPESAS" || (!walletType && form.categoria === "CONTA")) && (
          <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "14px" }}>
            <NumberInput label="Saldo" decimal value={form.saldo} onChange={v => setForm({ ...form, saldo: v })} error={errors.saldo} placeholder="0.00" />
            <Select label="Carteira" value={form.financas_id} onChange={e => setForm({ ...form, financas_id: e.target.value })} error={errors.financas_id}>{financas.map(f => <option key={f.id} value={f.id}>{f.nome}</option>)}</Select>
          </div>
        )}
        {(walletType === "INVESTIMENTO" || (!walletType && form.categoria === "INVESTIMENTO")) && (
          <Select label="Carteira" value={form.financas_id} onChange={e => setForm({ ...form, financas_id: e.target.value })} error={errors.financas_id}>{financas.filter(f => f.tipoCarteira === "INVESTIMENTO").map(f => <option key={f.id} value={f.id}>{f.nome}</option>)}</Select>
        )}
      </Modal>
    </Layout>
  );
}

const fb = (active: boolean): React.CSSProperties => ({ padding: "7px 16px", borderRadius: "var(--radius-full)", border: "none", cursor: "pointer", fontSize: "13px", fontWeight: 500, background: active ? "#6366f1" : "white", color: active ? "white" : "#64748b", boxShadow: "0 1px 2px rgba(0,0,0,0.05)" });
const cardStyle: React.CSSProperties = { background: "white", borderRadius: "12px", padding: "20px", boxShadow: "0 1px 2px rgba(0,0,0,0.05)", transition: "all 0.25s ease", display: "flex", flexDirection: "column", borderLeft: "4px solid #6366f1" };
const btnSm: React.CSSProperties = { padding: "7px 14px", borderRadius: "8px", border: "none", cursor: "pointer", fontSize: "12px", fontWeight: 600, background: "#f1f5f9", color: "#64748b" };
