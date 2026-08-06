import { useEffect, useState } from "react";
import { toast } from "sonner";
import api from "../api/api";
import Layout from "../components/Layout";
import Modal from "../components/Modal";
import { Input, Select, NumberInput, DateInput } from "../components/Form";
import { PageHeader, CardGrid, EmptyState, Spinner } from "../components/UI";
import { InvestInfo, totalInvestido, saldoAtual, lucro, rentabilidade, fmtPct, fmtSigned } from "../components/InvestInfo";
import { formatLocalDate, parseLocalDate } from "../utils/date";
import ContaLogsModal from "../components/ContaLogsModal";
import AtivoAutocomplete from "../components/AtivoAutocomplete";

type Categoria = "CONTA" | "INVESTIMENTO";
type CatInvest = "RENDA_FIXA" | "TESOURO_DIRETO" | "ACOES" | "FIIS" | "ETFS" | "CRIPTOMOEDAS";
type TipoCarteira = "INVESTIMENTO" | "DESPESAS";

interface Ativo {
  id: number; nome: string; categoria: Categoria;
  categoriaInvestimento: CatInvest | null; quantidade: number | null;
  valorUnitario: number | null; precoAtual: number | null; saldo: number;
  instituicao: string | null; dataAplicacao: string | null;
  vencimento: string | null; dataVencimento: string | null;
  rentabilidade: number | null; pago: boolean | null;
  carteira_investimento_id: number | null;
  carteira_dividas_id: number | null;
}
interface Financa { id: number; nome: string; moeda: string; tipo: TipoCarteira; }
interface FormData {
  nome: string; categoria: Categoria; categoriaInvestimento: CatInvest | "";
  quantidade: string; valorUnitario: string; precoAtual: string; saldo: string;
  instituicao: string; dataAplicacao: string; vencimento: string; dataVencimento: string; rentabilidade: string;
  carteira_id: string; pago: boolean;
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

const emptyForm: FormData = { nome: "", categoria: "INVESTIMENTO", categoriaInvestimento: "", quantidade: "", valorUnitario: "", precoAtual: "", saldo: "0", instituicao: "", dataAplicacao: "", vencimento: "", dataVencimento: "", rentabilidade: "", carteira_id: "", pago: false };
type Errors = Partial<Record<keyof FormData, string>>;

export default function Contas() {
  const [ativos, setAtivos] = useState<Ativo[]>([]);
  const [financas, setFinancas] = useState<Financa[]>([]);
  const [loading, setLoading] = useState(true);
  const [showModal, setShowModal] = useState(false);
  const [editing, setEditing] = useState<Ativo | null>(null);
  
  
  const [filterFinanca, setFilterFinanca] = useState("all");
  const [viewMode, setViewMode] = useState<"cards" | "list">("cards");
  const [form, setForm] = useState<FormData>(emptyForm);
  const [errors, setErrors] = useState<Errors>({});
  const [logsConta, setLogsConta] = useState<Ativo | null>(null);

  useEffect(() => { fetchCarteiras(); }, []);
  useEffect(() => { fetchAtivos(); }, [filterFinanca]);

  const fetchCarteiras = async () => {
    try {
      const [inv, div] = await Promise.all([
        api.get("/carteiras-investimento/all?page=0&size=100"),
        api.get("/carteiras-dividas/all?page=0&size=100"),
      ]);
      const invest: Financa[] = (inv.data.content ?? []).map((c: any) => ({ id: c.id, nome: c.nome, moeda: c.moeda, tipo: "INVESTIMENTO" }));
      const despesas: Financa[] = (div.data.content ?? []).map((c: any) => ({ id: c.id, nome: c.nome, moeda: c.moeda, tipo: "DESPESAS" }));
      setFinancas([...invest, ...despesas]);
    } catch { /* silencioso */ }
  };

  const mapAtivo = (x: any): Ativo => ({ id: x.id, nome: x.nome, categoria: "INVESTIMENTO", categoriaInvestimento: x.categoriaInvestimento ?? null, quantidade: x.quantidade ?? null, valorUnitario: x.valorUnitario ?? null, precoAtual: x.precoAtual ?? null, saldo: x.saldo ?? 0, instituicao: x.instituicao ?? null, dataAplicacao: x.dataAplicacao ?? null, vencimento: x.vencimento ?? null, dataVencimento: x.dataVencimento ?? null, rentabilidade: x.rentabilidade ?? null, pago: null, carteira_investimento_id: x.carteira_investimento_id ?? null, carteira_dividas_id: null });
  const mapDespesa = (x: any): Ativo => ({ id: x.id, nome: x.nome, categoria: "CONTA", categoriaInvestimento: null, quantidade: null, valorUnitario: null, precoAtual: null, saldo: x.saldo ?? 0, instituicao: null, dataAplicacao: null, vencimento: null, dataVencimento: x.dataVencimento ?? null, rentabilidade: null, pago: x.pago ?? null, carteira_investimento_id: null, carteira_dividas_id: x.carteira_dividas_id ?? null });

  const fetchAtivos = async () => {
    setLoading(true);
    try {
      if (filterFinanca === "all") {
        const [a, d] = await Promise.all([
          api.get(`/ativos/all?page=0&size=200`),
          api.get(`/despesas/all?page=0&size=200`),
        ]);
        const invest: Ativo[] = (a.data.content ?? []).map(mapAtivo);
        const despesas: Ativo[] = (d.data.content ?? []).map(mapDespesa);
        setAtivos([...invest, ...despesas]);
      } else {
        const [tipo, id] = filterFinanca.split(":");
        if (tipo === "INVESTIMENTO") {
          const r = await api.get(`/ativos/by-carteira/${id}`);
          setAtivos((r.data ?? []).map(mapAtivo));
        } else {
          const r = await api.get(`/despesas/by-carteira/${id}`);
          setAtivos((r.data ?? []).map(mapDespesa));
        }
      }
    } catch { toast.error("Erro ao carregar"); } finally { setLoading(false); }
  };

  const selectedFinanca = filterFinanca !== "all" ? financas.find(f => `${f.tipo}:${f.id}` === filterFinanca) : null;
  const walletType = selectedFinanca?.tipo;
  const isInvestBranch = walletType === "INVESTIMENTO" || (!walletType && form.categoria === "INVESTIMENTO");
  const isDespesaBranch = walletType === "DESPESAS" || (!walletType && form.categoria === "CONTA");
  const isRendaVariavel = isInvestBranch && ["ACOES", "FIIS", "ETFS", "CRIPTOMOEDAS"].includes(form.categoriaInvestimento);

  const validate = (): boolean => {
    const e: Errors = {};
    if (!form.nome.trim()) e.nome = "O nome é obrigatório.";
    if (!form.dataVencimento) e.dataVencimento = "Informe a data de vencimento.";
    if (!form.carteira_id) e.carteira_id = "Selecione uma carteira.";
    if (form.categoria === "INVESTIMENTO" && !form.categoriaInvestimento) e.categoriaInvestimento = "Selecione a categoria.";
    const ci = catInfo[form.categoriaInvestimento as CatInvest];
    if (ci?.autoCalc) {
      const vu = parseFloat(form.valorUnitario); const q = parseFloat(form.quantidade);
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
    const carteiraKey = walletType ? filterFinanca : form.carteira_id;
    const [cTipo, cId] = carteiraKey.split(":");
    const base = form.categoria === "INVESTIMENTO" ? "/ativos" : "/despesas";
    const payload: any = { nome: form.nome, dataVencimento: formatLocalDate(parseLocalDate(form.dataVencimento)) };
    if (cTipo === "INVESTIMENTO") payload.carteira_investimento_id = parseInt(cId);
    else payload.carteira_dividas_id = parseInt(cId);
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
        payload.quantidade = parseFloat(form.quantidade) || 0;
      } else {
        payload.saldo = parseFloat(form.saldo) || 0;
      }
    } else {
      payload.saldo = parseFloat(form.saldo) || 0;
      payload.pago = form.pago;
    }
    const promise = editing ? api.put(`${base}/alter/${editing.id}`, payload) : api.post(`${base}/create`, payload);
    toast.promise(promise, { loading: "Salvando...", success: () => { closeModal(); fetchAtivos(); return editing ? "Atualizado!" : "Criado!"; }, error: (err: any) => err?.response?.data?.message || "Erro ao salvar" });
  };

  const handleDelete = async (a: Ativo) => { const base = a.categoria === "INVESTIMENTO" ? "/ativos" : "/despesas"; toast("Excluir?", { action: { label: "Sim", onClick: () => { toast.promise(api.delete(`${base}/delete/${a.id}`), { loading: "Excluindo...", success: () => { fetchAtivos(); return "Excluído!"; }, error: "Erro" }); }}, cancel: { label: "Cancelar", onClick: () => {} } }); };

  const togglePago = async (ativo: Ativo) => {
    const novo = !ativo.pago;
    try { await api.put(`/despesas/alter/${ativo.id}`, { ...ativo, pago: novo }); setAtivos(prev => prev.map(a => a.id === ativo.id ? { ...a, pago: novo } : a)); toast.success(novo ? "Pago!" : "Desmarcado"); }
    catch { toast.error("Erro"); }
  };

  const openModal = (a: Ativo | null = null) => {
    setErrors({});
    const defCat: Categoria = walletType === "INVESTIMENTO" ? "INVESTIMENTO" : "CONTA";
    if (a) {
      setEditing(a);
      const ckey = a.carteira_investimento_id != null
        ? `INVESTIMENTO:${a.carteira_investimento_id}`
        : `DESPESAS:${a.carteira_dividas_id}`;
      setForm({ nome: a.nome, categoria: a.categoria, categoriaInvestimento: a.categoriaInvestimento || "", quantidade: a.quantidade?.toString() || "", valorUnitario: a.valorUnitario?.toString() || "", precoAtual: a.precoAtual?.toString() || "", saldo: a.saldo?.toString() || "0", instituicao: a.instituicao || "", dataAplicacao: a.dataAplicacao || "", vencimento: a.vencimento || "", dataVencimento: a.dataVencimento || "", rentabilidade: a.rentabilidade?.toString() || "", carteira_id: ckey, pago: a.pago ?? false });
    }
    else { setEditing(null); const def = financas.find(f => f.tipo === (defCat === "INVESTIMENTO" ? "INVESTIMENTO" : "DESPESAS")) || financas[0]; setForm({ ...emptyForm, categoria: defCat, carteira_id: filterFinanca !== "all" ? filterFinanca : (def ? `${def.tipo}:${def.id}` : "") }); }
    setShowModal(true);
  };
  const closeModal = () => { setShowModal(false); setEditing(null); };
  const getCarteiraDeAtivo = (a: Ativo): Financa | undefined => {
    if (a.carteira_investimento_id != null) return financas.find(f => f.tipo === "INVESTIMENTO" && f.id === a.carteira_investimento_id);
    if (a.carteira_dividas_id != null) return financas.find(f => f.tipo === "DESPESAS" && f.id === a.carteira_dividas_id);
    return undefined;
  };
  const getMoeda = (a: Ativo) => getCarteiraDeAtivo(a)?.moeda || "BRL";

  /* ── Sumários ── */
  const contas = ativos.filter(a => a.categoria === "CONTA");
  const investimentos = ativos.filter(a => a.categoria === "INVESTIMENTO");
  const totalInvestidoCarteira = investimentos.reduce((s, a) => s + totalInvestido(a), 0);
  const valorAtualCarteira = investimentos.reduce((s, a) => s + saldoAtual(a), 0);
  const lucroTotal = valorAtualCarteira - totalInvestidoCarteira;
  const rentTotal = totalInvestidoCarteira > 0 ? ((valorAtualCarteira - totalInvestidoCarteira) / totalInvestidoCarteira) * 100 : 0;
  const totalContas = contas.reduce((s, a) => s + (a.saldo || 0), 0);
  const contasPagas = contas.filter(a => a.pago).reduce((s, a) => s + (a.saldo || 0), 0);
  const contasNaoPagas = contas.filter(a => !a.pago).reduce((s, a) => s + (a.saldo || 0), 0);

  // Distribuição por categoria de investimento (por valor de mercado atual)
  const distCategorias = (Object.keys(catInfo) as CatInvest[]).map(ci => {
    const items = investimentos.filter(a => a.categoriaInvestimento === ci);
    const total = items.reduce((s, a) => s + saldoAtual(a), 0);
    return { key: ci, ...catInfo[ci], items, total };
  }).filter(d => d.items.length > 0);

  const summaryCards = walletType === "INVESTIMENTO"
    ? [
        { icon: "💰", label: "Total Investido", value: fmt(totalInvestidoCarteira, "BRL"), color: "#6366f1", bg: "#eef2ff" },
        { icon: "📈", label: "Valor Atual da Carteira", value: fmt(valorAtualCarteira, "BRL"), color: "#10b981", bg: "#ecfdf5" },
        { icon: lucroTotal >= 0 ? "📈" : "📉", label: "Lucro/Prejuízo Total", value: fmtSigned(lucroTotal, "BRL"), color: lucroTotal >= 0 ? "#10b981" : "#ef4444", bg: lucroTotal >= 0 ? "#ecfdf5" : "#fef2f2" },
        { icon: "🎯", label: "Rentabilidade Total", value: fmtPct(rentTotal), color: rentTotal > 0 ? "#10b981" : rentTotal < 0 ? "#ef4444" : "#64748b", bg: rentTotal > 0 ? "#ecfdf5" : rentTotal < 0 ? "#fef2f2" : "#f1f5f9" },
      ]
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
                      <div style={{ height: "100%", width: `${Math.min(100, parseFloat(pct(d.total, valorAtualCarteira)))}%`, background: d.color, borderRadius: "5px", transition: "width 0.5s ease" }} />
                    </div>
                    <span style={{ fontSize: "12px", fontWeight: 700, color: "#0f172a", minWidth: "65px", textAlign: "right" }}>{pct(d.total, valorAtualCarteira)}%</span>
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
        {financas.map(f => { const key = `${f.tipo}:${f.id}`; return (<button key={key} onClick={() => setFilterFinanca(key)} style={fb(filterFinanca === key)}>{f.tipo === "INVESTIMENTO" ? "📈" : "📋"} {f.nome}</button>); })}

        {/* Alternância de visualização: Cards ↔ Lista */}
        <div style={{ marginLeft: "auto", display: "flex", gap: "4px", background: "#eef2f7", padding: "3px", borderRadius: "10px" }}>
          <button onClick={() => setViewMode("cards")} style={viewBtn(viewMode === "cards")} aria-label="Visualizar em cards">▦ Cards</button>
          <button onClick={() => setViewMode("list")} style={viewBtn(viewMode === "list")} aria-label="Visualizar em lista">☰ Lista</button>
        </div>
      </div>

      {loading ? <Spinner text="Carregando..." /> : ativos.length === 0 ? <EmptyState icon="💳" title="Nenhum ativo" text="Crie seu primeiro ativo!" actionLabel="Criar Ativo" onAction={() => openModal()} /> :
        viewMode === "cards" ? (
          <CardGrid>{ativos.map(a => {
          const moeda = getMoeda(a);
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
              <InvestInfo ativo={a} moeda={moeda} />
              <p style={{ fontSize: "20px", fontWeight: 800, color: isConta ? (a.pago ? "#10b981" : "#f59e0b") : "#10b981", marginBottom: "14px" }}>{fmt(saldoAtual(a), moeda)}</p>
              <div style={{ display: "flex", gap: "8px", marginTop: "auto", flexWrap: "wrap" }}>
                {isConta && (<button onClick={() => togglePago(a)} style={{ ...btnSm, background: a.pago ? "#fef3c7" : "#d1fae5", color: a.pago ? "#b45309" : "#047857", fontWeight: 700 }}>{a.pago ? "↩ Desmarcar" : "✓ Pagar"}</button>)}
                <button onClick={() => openModal(a)} style={btnSm}>✏️ Editar</button>
                <button onClick={() => handleDelete(a)} style={{ ...btnSm, background: "#fee2e2", color: "#ef4444" }}>🗑</button>
              </div>
            </div>
          );
        })}
          </CardGrid>
        ) : (
          <div className="animate-fade-in" style={{ background: "white", borderRadius: "12px", boxShadow: "0 1px 2px rgba(0,0,0,0.05)", overflow: "hidden" }}>
            <div style={{ overflowX: "auto" }}>
              <table style={{ width: "100%", borderCollapse: "collapse", fontSize: "13px" }}>
                <thead>
                  <tr style={{ background: "#f8fafc" }}>
                    <th style={thStyle}>Ativo</th>
                    <th style={thStyle}>Detalhes</th>
                    <th style={thStyle}>Vencimento</th>
                    <th style={{ ...thStyle, textAlign: "right" }}>Valor</th>
                    <th style={thStyle}>Ações</th>
                  </tr>
                </thead>
                <tbody>
                  {ativos.map(a => {
                    const moeda = getMoeda(a);
                    const ci = catInfo[a.categoriaInvestimento as CatInvest];
                    const isConta = a.categoria === "CONTA";
                    const isInvest = a.categoria === "INVESTIMENTO";
                    const temQtd = isInvest && a.quantidade != null && a.valorUnitario != null;
                    const vencida = a.dataVencimento != null && a.dataVencimento < formatLocalDate(new Date());
                    return (
                      <tr key={a.id} style={{ borderTop: "1px solid #f1f5f9", background: vencida ? "#fef2f2" : undefined }}>
                        <td style={tdStyle}>
                          <div style={{ display: "flex", alignItems: "center", gap: "8px", flexWrap: "wrap" }}>
                            <span style={{ fontWeight: 600, color: "#0f172a" }}>{a.nome}</span>
                            {isInvest && ci && <span style={{ fontSize: "11px", fontWeight: 600, padding: "3px 9px", borderRadius: "var(--radius-full)", background: ci.bg, color: ci.color }}>{ci.icon} {ci.label}</span>}
                            {isConta && <span style={{ fontSize: "11px", fontWeight: 600, padding: "3px 9px", borderRadius: "var(--radius-full)", background: "#dbeafe", color: "#1d4ed8" }}>💳 Conta</span>}
                            {isConta && <span style={{ fontSize: "11px", fontWeight: 700, padding: "3px 9px", borderRadius: "var(--radius-full)", background: a.pago ? "#d1fae5" : "#fef3c7", color: a.pago ? "#047857" : "#b45309" }}>{a.pago ? "✅ Pago" : "⏳ Pendente"}</span>}
                          </div>
                        </td>
                        <td style={tdStyle}>
                          <div style={{ display: "flex", flexDirection: "column", gap: "2px" }}>
                            {temQtd && <span style={{ fontSize: "12px", color: "#64748b" }}>📦 {a.quantidade} un · 💵 {fmt(a.valorUnitario, moeda)}/un</span>}
                            {isInvest && a.precoAtual != null && (
                              <span style={{ fontSize: "12px", fontWeight: 600, color: a.precoAtual >= (a.valorUnitario || 0) ? "#10b981" : "#ef4444" }}>📊 Atual: {fmt(a.precoAtual, moeda)}</span>
                            )}
                            {isInvest && temQtd && <span style={{ fontSize: "12px", color: "#475569" }}>💰 Total Investido: <b>{fmt(totalInvestido(a), moeda)}</b></span>}
                            {isInvest && temQtd && <span style={{ fontSize: "12px", color: "#475569" }}>📈 Saldo Atual: <b style={{ color: "#10b981" }}>{fmt(saldoAtual(a), moeda)}</b></span>}
                            {isInvest && temQtd && <span style={{ fontSize: "12px", color: "#475569" }}>📊 Lucro/Prejuízo: <b style={{ color: lucro(a) > 0 ? "#10b981" : lucro(a) < 0 ? "#ef4444" : "#64748b" }}>{fmtSigned(lucro(a), moeda)}</b></span>}
                            {isInvest && temQtd && (() => { const r = rentabilidade(a); const c = r > 0 ? "#10b981" : r < 0 ? "#ef4444" : "#64748b"; const arrow = r > 0 ? "▲" : r < 0 ? "▼" : "•"; return <span style={{ fontSize: "12px", fontWeight: 700, color: c }}>🎯 Rentabilidade: {arrow} {fmtPct(r)}</span>; })()}
                            {isInvest && !temQtd && a.precoAtual == null && a.instituicao && <span style={{ fontSize: "12px", color: "#64748b" }}>🏦 {a.instituicao}</span>}
                            {isInvest && !temQtd && a.precoAtual == null && a.dataAplicacao && <span style={{ fontSize: "12px", color: "#64748b" }}>🗓 {a.dataAplicacao}</span>}
                          </div>
                        </td>                        <td style={tdStyle}>
                          <span style={{ fontSize: "13px", fontWeight: 600, color: vencida ? "#ef4444" : "#475569", whiteSpace: "nowrap" }}>
                            {a.dataVencimento ? parseLocalDate(a.dataVencimento).toLocaleDateString('pt-BR') : "-"}
                          </span>
                        </td>                        <td style={{ ...tdStyle, textAlign: "right" }}>
                          <span style={{ fontSize: "15px", fontWeight: 800, color: isConta ? (a.pago ? "#10b981" : "#f59e0b") : "#10b981", whiteSpace: "nowrap" }}>{fmt(saldoAtual(a), moeda)}</span>
                        </td>
                        <td style={tdStyle}>
                          <div style={{ display: "flex", gap: "6px", flexWrap: "wrap" }}>
                            {isConta && <button onClick={() => togglePago(a)} style={{ ...btnSm, background: a.pago ? "#fef3c7" : "#d1fae5", color: a.pago ? "#b45309" : "#047857", fontWeight: 700 }}>{a.pago ? "↩ Desmarcar" : "✓ Pagar"}</button>}
                            <button onClick={() => openModal(a)} style={btnSm}>✏️ Editar</button>
                            {isConta && <button onClick={() => setLogsConta(a)} style={btnSm} title="Ver logs">📜</button>}
                            <button onClick={() => handleDelete(a)} style={{ ...btnSm, background: "#fee2e2", color: "#ef4444" }}>🗑</button>
                          </div>
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          </div>
        )
      }

      {/* ── Modal ── */}
      <Modal open={showModal} onClose={closeModal} title={editing ? "Editar Ativo" : "Novo Ativo"} onSubmit={handleSubmit} submitLabel="Salvar" width="540px">
        {/* Categoria (travada ou seleção) */}
        {walletType ? (
          <div style={{ padding: "10px 14px", background: "#f8fafc", borderRadius: "10px", border: "1.5px solid #e2e8f0" }}>
            <span style={{ fontSize: "13px", fontWeight: 600, color: "#374151" }}>Categoria: </span>
            <span style={{ fontSize: "13px", fontWeight: 700, color: walletType === "INVESTIMENTO" ? "#8b5cf6" : "#6366f1" }}>{walletType === "INVESTIMENTO" ? "📈 Investimento" : "💳 Conta"}</span>
          </div>
        ) : (
          <Select label="Categoria" required value={form.categoria} onChange={e => { const cat = e.target.value as Categoria; setForm({ ...form, categoria: cat, categoriaInvestimento: cat === "CONTA" ? "" : form.categoriaInvestimento, quantidade: "", valorUnitario: "", precoAtual: "", instituicao: "", dataAplicacao: "", vencimento: "", rentabilidade: "" }); }}>
            <option value="CONTA">💳 Conta</option><option value="INVESTIMENTO">📈 Investimento</option>
          </Select>
        )}

        {/* Categoria de Investimento (antes do Ativo, para o autocomplete saber o tipo) */}
        {isInvestBranch && (
          <Select label="Categoria de Investimento" required value={form.categoriaInvestimento} onChange={e => { setForm({ ...form, categoriaInvestimento: e.target.value as CatInvest | "", nome: "", precoAtual: "", quantidade: "", valorUnitario: "", saldo: "0", instituicao: "", dataAplicacao: "", vencimento: "", rentabilidade: "" }); }} error={errors.categoriaInvestimento}>
            <option value="">Selecione...</option>
            {(Object.keys(catInfo) as CatInvest[]).map(ci => <option key={ci} value={ci}>{catInfo[ci].icon} {catInfo[ci].label}</option>)}
          </Select>
        )}

        {/* Ativo: autocomplete (renda variável) ou texto */}
        {isInvestBranch ? (
          isRendaVariavel ? (
            <div>
              <label style={{ fontSize: "13px", fontWeight: 600, color: "#374151", display: "block", marginBottom: "6px" }}>Ativo (ticker) *</label>
              <AtivoAutocomplete value={form.nome} error={errors.nome} onSelect={a => setForm({ ...form, nome: a.nome, precoAtual: a.precoAtual != null ? String(a.precoAtual) : form.precoAtual })} />
            </div>
          ) : (
            <Input label="Nome / Ticker / Ativo" required value={form.nome} onChange={e => setForm({ ...form, nome: e.target.value })} placeholder="Ex: Tesouro Selic 2029, CDB..." error={errors.nome} />
          )
        ) : (
          <Input label="Nome / Ticker / Ativo" required value={form.nome} onChange={e => setForm({ ...form, nome: e.target.value })} placeholder="Ex: Aluguel, Internet..." error={errors.nome} />
        )}

        <DateInput label="Data de Vencimento" value={form.dataVencimento} onChange={e => setForm({ ...form, dataVencimento: e.target.value })} error={errors.dataVencimento} required />

        {/* Pago (CONTA) */}
        {isDespesaBranch && (
          <div style={{ display: "flex", alignItems: "center", gap: "10px", padding: "10px 14px", background: form.pago ? "#d1fae5" : "#fef3c7", borderRadius: "10px", border: `1.5px solid ${form.pago ? "#10b981" : "#f59e0b"}` }}>
            <input type="checkbox" id="pago-check" checked={form.pago} onChange={e => setForm({ ...form, pago: e.target.checked })} style={{ width: "18px", height: "18px", accentColor: "#10b981", cursor: "pointer" }} />
            <label htmlFor="pago-check" style={{ fontSize: "14px", fontWeight: 600, color: form.pago ? "#047857" : "#b45309", cursor: "pointer" }}>{form.pago ? "✅ Pago" : "⏳ Pendente"} (opcional)</label>
          </div>
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
                  <Input label="Instituição Financeira (opcional)" value={form.instituicao} onChange={e => setForm({ ...form, instituicao: e.target.value })} placeholder="Ex: Banco do Brasil, XP..." />
                  <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "14px" }}>
                    <DateInput label="Data da Aplicação (opcional)" value={form.dataAplicacao} onChange={e => setForm({ ...form, dataAplicacao: e.target.value })} />
                    <NumberInput label="Rentabilidade % (opcional)" decimal value={form.rentabilidade} onChange={v => setForm({ ...form, rentabilidade: v })} placeholder="Ex: 12.5" />
                  </div>
                  <DateInput label="Vencimento (opcional)" value={form.vencimento} onChange={e => setForm({ ...form, vencimento: e.target.value })} />
                  <NumberInput label="Valor Aplicado" required decimal value={form.saldo} onChange={v => setForm({ ...form, saldo: v })} error={errors.saldo} placeholder="1000.00" />
                </>
              )}

              {/* Tesouro Direto */}
              {form.categoriaInvestimento === "TESOURO_DIRETO" && (
                <>
                  <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "14px" }}>
                    <DateInput label="Data da Compra (opcional)" value={form.dataAplicacao} onChange={e => setForm({ ...form, dataAplicacao: e.target.value })} />
                    <DateInput label="Vencimento (opcional)" value={form.vencimento} onChange={e => setForm({ ...form, vencimento: e.target.value })} />
                  </div>
                  <NumberInput label="Quantidade de Títulos (opcional)" value={form.quantidade} onChange={v => setForm({ ...form, quantidade: v })} placeholder="Ex: 5" />
                  <NumberInput label="Valor Investido" required decimal value={form.saldo} onChange={v => setForm({ ...form, saldo: v })} error={errors.saldo} placeholder="1000.00" />
                </>
              )}

              {/* Ações, FIIs, ETFs: preço médio × quantidade */}
              {(form.categoriaInvestimento === "ACOES" || form.categoriaInvestimento === "FIIS" || form.categoriaInvestimento === "ETFS") && (
                <>
                  <Input label={form.categoriaInvestimento === "ACOES" ? "Nome da Empresa (opcional)" : "Nome do Fundo (opcional)"} value={form.instituicao} onChange={e => setForm({ ...form, instituicao: e.target.value })} placeholder="Opcional" />
                  <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "14px" }}>
                    <NumberInput label="Preço Médio" required decimal value={form.valorUnitario} onChange={v => setForm({ ...form, valorUnitario: v })} error={errors.valorUnitario} placeholder="Ex: 35.50" />
                    <NumberInput label="Quantidade" required value={form.quantidade} onChange={v => setForm({ ...form, quantidade: v })} error={errors.quantidade} placeholder="Ex: 100" />
                  </div>
                  <NumberInput label="Preço Atual (opcional)" decimal value={form.precoAtual} onChange={v => setForm({ ...form, precoAtual: v })} placeholder="Ex: 38.20" />
                  {form.valorUnitario && form.quantidade && (
                    <div style={{ padding: "10px 14px", background: "#ecfdf5", borderRadius: "10px", border: "1.5px solid #10b981", display: "flex", justifyContent: "space-between", alignItems: "center" }}>
                      <span style={{ fontSize: "13px", fontWeight: 600, color: "#047857" }}>💰 Valor da Posição</span>
                      <span style={{ fontSize: "18px", fontWeight: 800, color: "#10b981" }}>{fmt(parseFloat(form.valorUnitario) * parseFloat(form.quantidade || "0"), "BRL")}</span>
                    </div>
                  )}
                </>
              )}

              {/* Criptomoedas */}
              {form.categoriaInvestimento === "CRIPTOMOEDAS" && (
                <>
                  <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "14px" }}>
                    <NumberInput label="Preço Médio" required decimal value={form.valorUnitario} onChange={v => setForm({ ...form, valorUnitario: v })} error={errors.valorUnitario} placeholder="Ex: 350000.00" />
                    <NumberInput label="Quantidade" required highPrecision value={form.quantidade} onChange={v => setForm({ ...form, quantidade: v })} error={errors.quantidade} placeholder="Ex: 0.05" />
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
            <NumberInput label="Saldo" required decimal value={form.saldo} onChange={v => setForm({ ...form, saldo: v })} error={errors.saldo} placeholder="0.00" hint="Valor da conta/despesa neste vencimento" />
            <Select label="Carteira" required value={form.carteira_id} onChange={e => setForm({ ...form, carteira_id: e.target.value })} error={errors.carteira_id}>{financas.map(f => <option key={`${f.tipo}:${f.id}`} value={`${f.tipo}:${f.id}`}>{f.tipo === "INVESTIMENTO" ? "📈" : "📋"} {f.nome}</option>)}</Select>
          </div>
        )}
        {(walletType === "INVESTIMENTO" || (!walletType && form.categoria === "INVESTIMENTO")) && (
          <Select label="Carteira" required value={form.carteira_id} onChange={e => setForm({ ...form, carteira_id: e.target.value })} error={errors.carteira_id}>{financas.filter(f => f.tipo === "INVESTIMENTO").map(f => <option key={`${f.tipo}:${f.id}`} value={`${f.tipo}:${f.id}`}>{f.nome}</option>)}</Select>
        )}
      </Modal>
      {logsConta && (
        <ContaLogsModal open contaId={logsConta.id} contaNome={logsConta.nome} onClose={() => setLogsConta(null)} />
      )}
    </Layout>
  );
}

const fb = (active: boolean): React.CSSProperties => ({ padding: "7px 16px", borderRadius: "var(--radius-full)", border: "none", cursor: "pointer", fontSize: "13px", fontWeight: 500, background: active ? "#6366f1" : "white", color: active ? "white" : "#64748b", boxShadow: "0 1px 2px rgba(0,0,0,0.05)" });
const viewBtn = (active: boolean): React.CSSProperties => ({ padding: "7px 14px", borderRadius: "8px", border: "none", cursor: "pointer", fontSize: "13px", fontWeight: 600, background: active ? "white" : "transparent", color: active ? "#6366f1" : "#64748b", boxShadow: active ? "0 1px 3px rgba(0,0,0,0.12)" : "none", transition: "all 0.15s ease" });
const thStyle: React.CSSProperties = { padding: "12px 16px", fontSize: "11px", fontWeight: 700, color: "#64748b", textTransform: "uppercase", letterSpacing: "0.04em", whiteSpace: "nowrap" };
const tdStyle: React.CSSProperties = { padding: "12px 16px", borderTop: "1px solid #f1f5f9", verticalAlign: "middle" };
const cardStyle: React.CSSProperties = { background: "white", borderRadius: "12px", padding: "20px", boxShadow: "0 1px 2px rgba(0,0,0,0.05)", transition: "all 0.25s ease", display: "flex", flexDirection: "column", borderLeft: "4px solid #6366f1" };
const btnSm: React.CSSProperties = { padding: "7px 14px", borderRadius: "8px", border: "none", cursor: "pointer", fontSize: "12px", fontWeight: 600, background: "#f1f5f9", color: "#64748b" };